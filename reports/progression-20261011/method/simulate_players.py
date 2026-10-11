"""Observable-only mining agents on exported native GTNH terrain.
No agent is allowed to query hidden blocks to select a destination. The map is used
only for adjacent exposed faces or the next physical tunnel/shaft block.
Finite-volume failures are right-censored, not extrapolated into completion times.
"""
from pathlib import Path
from collections import Counter,defaultdict
import json,gzip,csv,math,random,time
ROOT=Path(__file__).resolve().parents[1]
OLD=ROOT/'inputs/reference-reports'
EVID=ROOT/'inputs/native-evidence'
if not (EVID/'tool-calibration.jsonl').exists():EVID=ROOT/'inputs/native-evidence'
CAL={}
for l in (EVID/'tool-calibration.jsonl').open(encoding='utf-8'):
 d=json.loads(l);CAL[d['tool'],d['material']]=d
YIELDS={r['material']:r for r in csv.DictReader((OLD/'science6-v2-catalog/material-yields.csv').open())}
FURNACE={}
fp=ROOT/'inputs/native-evidence/furnace-recipes.jsonl'
if not fp.exists():fp=ROOT/'inputs/repair-and-furnace/furnace-recipes.jsonl'
if fp.exists():
 for l in fp.open(encoding='utf-8'):
  d=json.loads(l);a=d['input'];b=d['output']
  if a.get('identity','').startswith('rawOre:'):
   FURNACE[a['identity'].split(':')[1]]=(b.get('identity','').split(':')[-1],b['count'])
NEIGH=[(1,0,0),(-1,0,0),(0,1,0),(0,-1,0),(0,0,1),(0,0,-1)]
DEPTH={'Iron':[28,40,56],'Copper':[32,48,56],'Tin':[56,32,20],'Calcite':[32,40],'Gypsum':[56,50],'Mica':[32,40],'Nickel':[32,40,24],'Gold':[32,24],'Redstone':[12,16],'Aluminium':[24,40,56],'Titanium':[24,40],'Tungsten':[20,32]}
LAYER_MATS=defaultdict(set);MAT_LAYERS=defaultdict(set)
for row in csv.DictReader((OLD/'science6-v2-catalog/catalog.csv').open()):
 if row['dimension']=='Overworld':
  LAYER_MATS[row['layer']].add(row['material']);MAT_LAYERS[row['material']].add(row['layer'])
class Terrain:
 def __init__(self,p):
  self.path=p;self.props=dict(l.split('=',1) for l in (p/'world.properties').read_text().splitlines() if '=' in l)
  self.size=int(self.props['size']);self.width=self.size*16;self.x0=int(self.props['minChunkX'])*16;self.z0=int(self.props['minChunkZ'])*16
  self.solid=gzip.decompress((p/'terrain-solid.bin.gz').read_bytes());self.ores={}
  for r in csv.DictReader(gzip.open(p/'ores.csv.gz','rt')):
   if r['small']=='false':self.ores[int(r['x']),int(r['y']),int(r['z'])]=r['material']
 def inside(self,p):x,y,z=p;return self.x0<=x<self.x0+self.width and self.z0<=z<self.z0+self.width and 1<=y<100
 def filled(self,p):
  x,y,z=p
  if not self.inside(p):return False
  cx=(x-self.x0)//16;cz=(z-self.z0)//16;lx=x%16;lz=z%16;i=(cx*self.size+cz)*65536+(lx*16+lz)*256+y
  return i//8<len(self.solid) and bool(self.solid[i//8]&(1<<(i%8)))
def man(a,b):return sum(abs(x-y) for x,y in zip(a,b))
class Player:
 def __init__(self,w,policy,seed,tool):
  self.w=w;self.policy=policy;self.rng=random.Random(seed);self.tool=tool;self.inventory=Counter();self.mined=set();self.known=set();self.seconds=0.;self.digSeconds=0.;self.walkSeconds=0.;self.overheadSeconds=0.;self.blocks=0;self.oreBlocks=0;self.repairs=0;self.wear=0.;self.searches=0;self.denied=0;self.misses=0;self.base=(w.x0+8,64,w.z0+8);self.pos=self.base;self.goal={};self.trace=[];self.phaseSeconds=0
  # Expected reinforced wear; calibrated native initial durability and repair curves.
  self.durability={'FlintBoosted':113,'Copper':135,'Bronze':285,'Steel':300,'Alumite':413,'Cobalt':600}[tool]
  self.wearRate={'FlintBoosted':1.,'Copper':1.,'Bronze':.8,'Steel':.6,'Alumite':.6,'Cobalt':.6}[tool]
  self.repairGain=.75*self.durability
  curve=ROOT/'inputs/native-evidence/repair-curve.jsonl'
  if curve.exists():
   for l in curve.open(encoding='utf-8'):
    d=json.loads(l)
    if d['tool']==tool.replace('Boosted','') and d['repairs']:
     self.repairGain=sum(r['gain'] for r in d['repairs'])/len(d['repairs'])
     self.wearRate=(sum(r['gain'] for r in d['repairs'])+d['finalDamage'])/d['stoneBlocks']
 def addwalk(self,n):self.walkSeconds+=n/3.5;self.seconds+=n/3.5
 def satisfied(self):return all(self.inventory[k]>=n for k,n in self.goal.items())
 def value(self,mat):
  # Direct smelting uses native furnace identities where available. Other mineral
  # goals are ore-block proxies, never interpreted as refined-metal yields.
  if mat in self.goal:return mat,1
  if mat in FURNACE:
   k,n=FURNACE[mat];return (k,n) if k in self.goal else (None,0)
  row=YIELDS.get(mat,{});k=row.get('directSmelting');n=int(row.get('smeltingMultiplier') or 0)
  return (k,max(1,n)) if k in self.goal else (None,0)
 def wanted(self,mat):k,n=self.value(mat);return k is not None and self.inventory[k]<self.goal[k]
 def dig(self,p,ore=False):
  if not self.w.inside(p):return False
  if p in self.mined:return True
  mat=self.w.ores.get(p);c=CAL.get((self.tool,mat or 'STONE'),CAL[self.tool,'STONE'])
  if not c['gate']:self.denied+=1;return False
  self.mined.add(p)
  if not self.w.filled(p):return True
  ticks=c['ticks'];t=ticks/20+.15 # Human targeting/click/reposition overhead per block.
  self.digSeconds+=ticks/20;self.overheadSeconds+=.15;self.seconds+=t;self.blocks+=1
  self.wear+=self.wearRate*(2 if mat else 1)
  if mat:
   self.oreBlocks+=1;k,n=self.value(mat)
   if k:self.inventory[k]+=n
  if self.wear>=self.durability-8:
   self.wear=max(0,self.wear-self.repairGain);self.repairs+=1;self.seconds+=20;self.overheadSeconds+=20
   if self.tool=='Bronze':self.goal['Copper']=self.goal.get('Copper',0)+.75;self.goal['Tin']=self.goal.get('Tin',0)+.25
   elif self.tool=='Copper':self.goal['Copper']=self.goal.get('Copper',0)+1
   elif self.tool=='Steel':self.goal['Iron']=self.goal.get('Iron',0)+1
   elif self.tool=='FlintBoosted':self.seconds+=3;self.overheadSeconds+=3 # Assumed marginal gravel/flint collection.
  if self.blocks%128==0:self.seconds+=20;self.overheadSeconds+=20 # Torches, food, inventory handling.
  return True
 def connect(self,target):
  # Paid axis-aligned connector with short, observable detours around blocked
  # shaft ore. Never abandon every future lane because one shaft met hard ore.
  p=list(self.pos)
  for axis in [1,2,0]:
   while p[axis]!=target[axis]:
    q=list(p);q[axis]+=1 if target[axis]>p[axis] else -1;q=tuple(q)
    if not self.w.inside(q):return False
    clear=True
    for yy in [q[1],q[1]+1]:
     if not self.dig((q[0],yy,q[2])):clear=False;break
     self.expose((q[0],yy,q[2]))
    if not clear:
     detoured=False
     for side in [0,2]:
      if side==axis:continue
      for sign in [1,-1]:
       a=list(p);a[side]+=sign;b=list(q);b[side]+=sign
       if not self.w.inside(tuple(a)) or not self.w.inside(tuple(b)):continue
       if all(self.dig((v[0],yy,v[2])) for v in [a,b] for yy in [v[1],v[1]+1]):
        p=b;self.pos=tuple(p);self.addwalk(2);self.expose(self.pos);detoured=True;break
      if detoured:break
     if not detoured:return False
    else:p=list(q);self.addwalk(1);self.pos=q
    if self.seconds-self.phaseSeconds>=24*3600:return False
  return True
 def follow(self,p):
  if not self.w.inside(p) or p in self.mined:return
  mat=self.w.ores.get(p)
  if not mat or not self.wanted(mat):return
  stack=[(p,0)];active=set()
  while stack and not self.satisfied() and self.seconds-self.phaseSeconds<24*3600:
   q,i=stack[-1]
   if i==0:
    if q in active or q in self.mined or not self.dig(q,True):stack.pop();continue
    active.add(q);self.addwalk(1)
   if i==6:stack.pop();self.addwalk(1);continue
   stack[-1]=(q,i+1);dx,dy,dz=NEIGH[i];n=(q[0]+dx,q[1]+dy,q[2]+dz)
   if self.w.inside(n) and n not in self.mined:
    mat=self.w.ores.get(n)
    if mat:self.known.add(n)
    if mat and self.wanted(mat):stack.append((n,0))
 def expose(self,p):
  for dx,dy,dz in NEIGH:
   q=(p[0]+dx,p[1]+dy,p[2]+dz)
   if q in self.w.ores:
    self.known.add(q)
    if self.wanted(self.w.ores[q]):self.follow(q)
 def resupply(self):
  attempted=set()
  while not self.satisfied():
   opts=[p for p in self.known if p not in self.mined and p not in attempted and self.wanted(self.w.ores[p]) and CAL.get((self.tool,self.w.ores[p]),{}).get('gate',False)]
   if not opts:break
   p=min(opts,key=lambda p:man(self.pos,p));attempted.add(p);self.addwalk(man(self.pos,p));self.follow(p);self.pos=p
   if self.seconds-self.phaseSeconds>=24*3600:break
 def vein_sweep(self,x,y,z):
  # Search the published stock vein envelope after a genuine ore discovery.
  # The next block/face is inspected only after physically digging to it.
  for yy in [y,max(8,y-2),min(63,y+2)]:
   for zz in range(z-20,z+21,3):
    if self.satisfied() or self.seconds-self.phaseSeconds>=24*3600:return
    if not self.connect((max(self.w.x0,x-20),yy,zz)):continue
    for xx in range(x-20,x+21):
     if self.satisfied():return
     if not self.w.inside((xx,yy,zz)):continue
     if self.dig((xx,yy,zz)):
      self.expose((xx,yy,zz));self.addwalk(1);self.pos=(xx,yy,zz)
     else:break
 def branch(self):
  margin=8;xs=self.w.x0+margin;xe=self.w.x0+self.w.width-margin-1
  # Randomize phase/order across independent starting lanes; never use ore locations.
  lanes=list(range(self.w.z0+margin+self.rng.randrange(4),self.w.z0+self.w.width-margin,4))
  goals=list(self.goal);depthCursor=Counter();visited=set()
  for lane in lanes:
   self.resupply()
   if self.satisfied():break
   missing=[g for g in goals if self.inventory[g]<self.goal[g]]
   g=max(missing,key=lambda k:(self.goal[k]-self.inventory[k])/max(1,self.goal[k]))
   depths=DEPTH.get(g,[28,40,56]);y=depths[depthCursor[g]%len(depths)];depthCursor[g]+=1
   # Pay a physical access shaft/corridor for each newly selected lane/depth.
   start=(xs,y,lane);self.addwalk(man(self.pos,self.base));self.pos=self.base
   if not self.connect(start):continue
   self.searches+=1;before=sum(self.inventory.values())
   for x in range(xs,xe+1):
    if self.satisfied() or self.seconds-self.phaseSeconds>=24*3600:break
    okay=True
    for yy in [y,y+1]:
     p=(x,yy,lane);mat=self.w.ores.get(p)
     if mat and self.wanted(mat):self.follow(p)
     if not self.dig(p):okay=False
     self.expose(p)
    if not okay:break
    self.addwalk(1);self.pos=(x,y,lane)
   if sum(self.inventory.values())==before:self.misses+=1
   if self.seconds-self.phaseSeconds>=24*3600:break
 def grid(self):
  # GregTech's verified installed EQUAL_SPACING ore-chunk rule. Search all centers,
  # rather than choosing a wanted vein from the hidden map.
  centers=[(cx*16+8,cz*16+8) for cx in range(self.w.x0//16,(self.w.x0+self.w.width)//16) for cz in range(self.w.z0//16,(self.w.z0+self.w.width)//16) if cx%3==1 and cz%3==1]
  origin=centers[self.rng.randrange(len(centers))];centers.sort(key=lambda c:abs(c[0]-origin[0])+abs(c[1]-origin[1]))
  for x,z in centers:
   self.resupply()
   if self.satisfied():break
   self.addwalk(man(self.pos,(x,64,z)));self.pos=(x,64,z);self.searches+=1;before=sum(self.inventory.values())
   # A 1x2 staircase proxy charges two blocks per depth plus descent/ascent.
   searched=set()
   for y in range(63,7,-1):
    if self.satisfied() or self.seconds-self.phaseSeconds>=24*3600:break
    okay=True
    for xx in [x,x+1]:
     p=(xx,y,z);mat=self.w.ores.get(p)
     if mat and self.wanted(mat):
      self.follow(p)
     if mat and not self.satisfied() and CAL.get((self.tool,mat),{}).get('gate',False):
      # Recognize native vein companions using public catalog knowledge. A
      # soapstone/olivine indicator warrants a nickel search, for example.
      layers=[layer for layer in MAT_LAYERS[mat] if layer not in searched and any(self.wanted(m) for m in LAYER_MATS[layer])]
      if layers:
       searched.update(layers);self.pos=p;self.vein_sweep(x,y,z)
     if not self.dig(p):okay=False
     self.expose(p)
    if not okay:break
    self.addwalk(1);self.pos=(x,y,z)
   self.addwalk(64-self.pos[1]);self.pos=(x,64,z)
   if sum(self.inventory.values())==before:self.misses+=1
   if self.seconds-self.phaseSeconds>=24*3600:break
 def run(self,goal):
  self.goal=dict(goal);self.phaseSeconds=self.seconds
  if self.tool=='Bronze':self.goal['Copper']=self.goal.get('Copper',0)+1.5;self.goal['Tin']=self.goal.get('Tin',0)+.5
  elif self.tool=='Copper':self.goal['Copper']=self.goal.get('Copper',0)+2
  elif self.tool=='Steel':self.goal['Iron']=self.goal.get('Iron',0)+2
  if self.tool=='FlintBoosted':self.seconds+=274*12/20+4*20;self.overheadSeconds+=4*20;self.repairs+=4
  (self.grid if self.policy=='grid' else self.branch)()
  self.addwalk(man(self.pos,self.base))
  complete=self.satisfied()
  return dict(complete=complete,hours=self.seconds/3600,miningHours=self.digSeconds/3600,travelHours=self.walkSeconds/3600,overheadHours=self.overheadSeconds/3600,blocks=self.blocks,oreBlocks=self.oreBlocks,repairs=self.repairs,repairMaterial='Flint' if self.tool=='FlintBoosted' else self.tool,searches=self.searches,failedSearches=self.misses,harvestDenials=self.denied,missing={k:max(0,n-self.inventory[k]) for k,n in self.goal.items() if self.inventory[k]<n},inventory=dict(self.inventory),adjustedGoal=self.goal)
def datasets():
 pairs=[]
 for i,(stock,custom) in enumerate([('native4-iv-stock','science6-v2-ow1'),('native4-iv-stock2','science6-v2-ow2'),('science6-stock-heldout3','science6-v2-ow3'),('science6-stock-heldout4','science6-v2-ow4')],1):
  s=OLD/stock/'drill-before';c=OLD/custom/'drill-before'
  if not s.exists():s=(ROOT/'inputs/legacy-reference-reports')/stock/'drill-before'
  if (s/'terrain-solid.bin.gz').exists() and (c/'terrain-solid.bin.gz').exists():pairs.append((f'old{i}',s,c))
 pairs.extend([('new1',ROOT/'inputs/fresh/seed1-stock-v3',ROOT/'inputs/fresh/seed1-custom-v2'),('new2',ROOT/'inputs/fresh/seed2-stock',ROOT/'inputs/fresh/seed2-custom')])
 return pairs
def main():
 cases=[('Flint bootstrap','FlintBoosted',{'Iron':32,'Copper':12,'Tin':4}),
 ('Steam starter factory','Bronze',{'Iron':76,'Copper':93,'Tin':31}),
 ('Bricked blast furnace minerals','Bronze',{'Gypsum':32,'Calcite':16}),
 ('Early LV working stock','Bronze',{'Iron':726,'Copper':294,'Tin':244,'Gold':8}),
 ('LV blast furnace mica route','Steel',{'Mica':32,'Nickel':64,'Copper':128}),
 ('LV to MV common-metal reserve','Steel',{'Iron':256,'Copper':256,'Tin':128,'Gold':24}),
 ('MV to HV factory expansion','Steel',{'Iron':1024,'Copper':512,'Tin':256,'Nickel':128,'Gold':64}),
 ('Steam iron stress test','Bronze',{'Iron':4000}),
 ('EV drill common-metal subtotal','Alumite',{'Iron':418,'Copper':272,'Tin':24,'Nickel':192,'Gold':40})]
 rows=[];start=time.time()
 for label,sp,cp in datasets():
  for mode,p in [('stock',sp),('custom',cp)]:
   w=Terrain(p)
   for name,tool,goal in cases:
    for policy in (['grid','branch'] if mode=='stock' else ['branch']):
     for replicate in range(8):
      r=Player(w,policy,1731+replicate,tool).run(goal);r.update(case=name,goal=goal,tool=tool,seed=int(w.props['seed']),dataset=label,mode=mode,policy=policy,replicate=replicate);rows.append(r)
   print(label,mode,'simulated',len(rows),'elapsed',round(time.time()-start),flush=True)
 (ROOT/'player-simulations.json').write_text(json.dumps(rows,indent=2))
 print('SIMULATIONS COMPLETE',len(rows),round(time.time()-start),flush=True)
if __name__=='__main__':main()
