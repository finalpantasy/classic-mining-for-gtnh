"""Continuous, bounded mining campaign. Carries dug terrain, exposed deposits,
surplus and tool repair demand across stages. This is a mining/processing model,
not an executed survival quest completion or a full factory dependency planner.
"""
import json,time
from collections import defaultdict
from simulate_players import *
PROC=defaultdict(list)
for r in json.loads((ROOT/'basic-processing.json').read_text()):PROC[r['material'],r['voltage']].append(r)
class CampaignPlayer(Player):
 def __init__(self,*args):
  super().__init__(*args);self.voltage=0;self.processing=0.;self.raw=Counter();self.storedRaw=Counter();self.phase=0
 def value(self,mat):
  opts=[r for r in PROC[mat,self.voltage] if r['goal'] in self.goal and self.inventory[r['goal']]<self.goal[r['goal']]]
  if opts:
   r=max(opts,key=lambda r:(self.goal[r['goal']]-self.inventory[r['goal']])/max(1,self.goal[r['goal']]))
   return r['goal'],r['yield_']
  return super().value(mat)
 def dig(self,p,ore=False):
  fresh=p not in self.mined;mat=self.w.ores.get(p);goal,n=self.value(mat) if mat else (None,0)
  yes=super().dig(p,ore)
  if yes and fresh and mat and self.w.filled(p):
   self.raw[mat]+=1
   if goal:
    r=next((r for r in PROC[mat,self.voltage] if r['goal']==goal),None)
    self.processing+=r['seconds'] if r else 10
   else:self.storedRaw[mat]+=1
  return yes
 def run(self,goal):
  self.goal=dict(goal)
  # Keep incidental ores encountered while digging. A human can process this
  # stock later rather than somehow forgetting it at each technology upgrade.
  for mat,count in list(self.storedRaw.items()):
   while self.storedRaw[mat]>0:
    k,n=self.value(mat)
    if not k or self.inventory[k]>=self.goal[k]:break
    self.inventory[k]+=n;self.storedRaw[mat]-=1
    r=next((r for r in PROC[mat,self.voltage] if r['goal']==k),None)
    self.processing+=r['seconds'] if r else 10
  return super().run(goal)
 def upgrade(self,tool):
  model=Player(self.w,self.policy,0,tool)
  self.tool=tool;self.durability=model.durability;self.wearRate=model.wearRate;self.repairGain=model.repairGain;self.wear=0
 def stage(self,name,tool,voltage,goal):
  self.upgrade(tool);self.voltage=voltage
  before=(self.seconds,self.processing,self.repairs,self.blocks,self.oreBlocks)
  if tool=='Copper':
   # Deliberately train/repair the retained head before deep mining. Native
   # lifecycle observed harvest boost at 458 stone blocks. Cost is charged,
   # never treated as a free pre-upgraded tool.
   self.seconds+=458*9/20+4*20;self.blocks+=458;self.repairs+=4
   self.digSeconds+=458*9/20;self.overheadSeconds+=4*20
  r=self.run(goal)
  r.update(stage=name,hours=(self.seconds-before[0])/3600,processingSingleLaneHours=(self.processing-before[1])/3600,repairs=self.repairs-before[2],blocks=self.blocks-before[3],oreBlocks=self.oreBlocks-before[4],voltage=voltage)
  # Every stage pays its resource budget, including extra materials consumed by repairs.
  if r['complete']:
   for k,n in self.goal.items():self.inventory[k]-=n
  self.phase+=1
  return r
STAGES=[
 ('Starter copper head','FlintBoosted',0,{'Copper':6}),
 ('Bronze tooling and initial iron','Copper',0,{'Copper':12,'Tin':4,'Iron':32}),
 ('Steam starter machines','Bronze',0,{'Iron':76,'Copper':93,'Tin':31}),
 ('Bricked blast furnace minerals','Bronze',0,{'Gypsum':32,'Calcite':16}),
 ('Steam steel and LV working stock','Bronze',32,{'Iron':726,'Copper':294,'Tin':244,'Gold':8}),
 ('LV EBF mica and nickel reserve','Steel',32,{'Mica':32,'Nickel':64,'Copper':128}),
 ('LV to MV common-metal reserve','Steel',32,{'Iron':256,'Copper':256,'Tin':128,'Gold':24}),
 ('MV to HV common-metal expansion','Steel',32,{'Iron':1024,'Copper':512,'Tin':256,'Nickel':128,'Gold':64}),
 ('EV drill common-metal subtotal','Alumite',32,{'Iron':418,'Copper':272,'Tin':24,'Nickel':192,'Gold':40})]
def main():
 # Additional native copper calibration, exported separately to preserve provenance.
 cp=ROOT/'inputs/copper-boost-evidence/tool-calibration.jsonl'
 if not cp.exists():raise RuntimeError('Copper calibration must finish first')
 for l in cp.open(encoding='utf-8'):
  r=json.loads(l)
  if r['tool']=='CopperBoosted':CAL['Copper',r['material']]=r
 rows=[];started=time.time()
 for label,stock,custom in datasets():
  for mode,path in [('stock',stock),('custom',custom)]:
   w=Terrain(path)
   for replicate in range(8):
    for policy in (['grid','branch'] if mode=='stock' else ['branch']):
     p=CampaignPlayer(w,policy,1731+replicate,'FlintBoosted');stages=[]
     for name,tool,voltage,goal in STAGES:
      r=p.stage(name,tool,voltage,goal);stages.append(r)
      if not r['complete']:break
     rows.append(dict(dataset=label,seed=int(w.props['seed']),mode=mode,policy=policy,replicate=replicate,stages=stages,completedStages=sum(s['complete'] for s in stages),miningCampaignHours=p.seconds/3600,processingSingleLaneHours=p.processing/3600))
   print(label,mode,'campaigns',len(rows),'seconds',round(time.time()-started),flush=True)
 (ROOT/'sequential-campaigns.json').write_text(json.dumps(rows,indent=2))
 print('Completed bounded mining campaigns',len(rows),flush=True)
if __name__=='__main__':main()
