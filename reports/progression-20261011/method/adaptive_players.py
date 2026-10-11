"""Sensitivity check: bounded local probing after ore finds, and ore-load trips.
This is another explicit policy, not a claim that a human has been replicated.
Full ore maps are used for retrospective recovery accounting only, never for
choosing probe destinations or deciding whether a search should be abandoned.
"""
import json,time
from collections import Counter
from sequential_players import *
class AdaptivePlayer(CampaignPlayer):
 def __init__(self,*args,probe=False):
  super().__init__(*args);self.probe=probe;self.probing=False;self.probed=set();self.carried=0;self.oreTrips=0
 def dig(self,p,ore=False):
  fresh=p not in self.mined;mat=self.w.ores.get(p)
  okay=super().dig(p,ore)
  if okay and fresh and mat and self.w.filled(p):
   self.carried+=1
   if self.carried>=768:
    # Twelve stacks reserved for ores, remaining inventory for tools/supplies.
    # Manhattan round trip is an optimistic travel proxy, not pathfinding.
    self.addwalk(2*man(p,self.base));self.seconds+=60;self.overheadSeconds+=60
    self.carried=0;self.oreTrips+=1
  return okay
 def follow(self,p):
  wanted=p not in self.mined and self.wanted(self.w.ores.get(p));before=self.oreBlocks
  super().follow(p)
  cell=tuple(v//8 for v in p)
  if not self.probe or self.probing or not wanted or self.oreBlocks==before or self.satisfied() or cell in self.probed:return
  self.probed.add(cell);self.probing=True;self.pos=p
  try:
   # Four short cross probes, in fixed geometric order. No hidden ore scoring.
   # Stop when the resource objective is met or the local probes are exhausted.
   for dx,dz in [(4,0),(-4,0),(0,4),(0,-4)]:
    if self.satisfied() or self.seconds-self.phaseSeconds>=24*3600:break
    self.connect(p)
    target=(p[0]+dx,p[1],p[2]+dz)
    if self.w.inside(target):self.connect(target)
   self.connect(p)
  finally:self.probing=False
 def recovery(self):
  chunks={(p[0]//16,p[2]//16) for p in self.mined}
  ores=[p for p in self.w.ores if self.w.inside(p) and self.w.filled(p)]
  visited=[p for p in ores if (p[0]//16,p[2]//16) in chunks]
  harvested=sum(p in self.mined for p in ores)
  return dict(excavatedChunks=len(chunks),normalOreInSample=len(ores),normalOreInExcavatedChunks=len(visited),normalOreHarvested=harvested,recoveryOfSample=harvested/len(ores) if ores else None,recoveryOfExcavatedChunks=harvested/len(visited) if visited else None,oreLoadTrips=self.oreTrips)
def main():
 for l in (ROOT/'inputs/copper-boost-evidence/tool-calibration.jsonl').open():
  r=json.loads(l)
  if r['tool']=='CopperBoosted':CAL['Copper',r['material']]=r
 rows=[];started=time.time()
 for label,stock,custom in datasets():
  for mode,path in [('stock',stock),('custom',custom)]:
   w=Terrain(path)
   for replicate in range(4):
    for probing in ([False] if mode=='stock' else [False,True]):
     p=AdaptivePlayer(w,'grid' if mode=='stock' else 'branch',1731+replicate,'FlintBoosted',probe=probing);stages=[]
     for name,tool,voltage,goal in STAGES[:7]:
      r=p.stage(name,tool,voltage,goal);r['recovery']=p.recovery();stages.append(r)
      if not r['complete']:break
     rows.append(dict(dataset=label,seed=int(w.props['seed']),mode=mode,policy='local-probes' if probing else ('grid-with-trips' if mode=='stock' else 'branch-with-trips'),replicate=replicate,stages=stages,completedStages=sum(s['complete'] for s in stages),miningCampaignHours=p.seconds/3600,processingSingleLaneHours=p.processing/3600,recovery=p.recovery()))
   print(label,mode,'sensitivity campaigns',len(rows),'seconds',round(time.time()-started),flush=True)
 (ROOT/'adaptive-campaigns.json').write_text(json.dumps(rows,indent=2))
 print('Sensitivity campaigns complete',len(rows),flush=True)
if __name__=='__main__':main()
