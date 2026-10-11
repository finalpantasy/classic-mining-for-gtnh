"""Conservative native furnace/macerator yields. No assumed chemical reagents,
no recursive byproduct recovery, and no hot-ingot-as-finished-metal shortcuts.
"""
from pathlib import Path
from collections import defaultdict
import json
ROOT=Path(__file__).resolve().parents[1]
E=ROOT/'inputs/native-evidence'
nodes={};edges=defaultdict(list)
for line in (E/'native-recipes.jsonl').open(encoding='utf-8'):
 r=json.loads(line)
 if r['map']!='gt.recipe.macerator':continue
 ins=[x for x in r.get('inputs',[]) if x and x.get('key') and x.get('count',0)>0]
 outs=[x for x in r.get('outputs',[]) if x and x.get('key')]
 if len(ins)!=1 or not outs:continue
 a=ins[0];b=outs[0]
 if a.get('identity','').split(':')[0] not in ['rawOre','crushed','crushedPurified','crushedCentrifuged','dustImpure','dustPure']:continue
 nodes[a['key']]=a;nodes[b['key']]=b
 edges[a['key']].append((b['key'],b['count']/a['count'],r.get('eut',0),r['ticks']/20/a['count'],'macerator'))
for line in (E/'furnace-recipes.jsonl').open(encoding='utf-8'):
 r=json.loads(line);a=r['input'];b=r['output'];nodes[a['key']]=a;nodes[b['key']]=b
 edges[a['key']].append((b['key'],b['count']/a['count'],0,10/a['count'],'furnace'))
def solve(key,voltage,path=()):
 if key in path or len(path)>5:return {}
 x=nodes[key];ident=x.get('identity','').split(':')
 if len(ident)==2 and ident[0]=='ingot':return {ident[1]:(1,0,[])}
 if len(ident)==2 and ident[0]=='dust' and ident[1] in ['Mica','Gypsum','Calcite']:return {ident[1]:(1,0,[])}
 result={}
 for target,n,eut,seconds,m in edges.get(key,[]):
  if eut>voltage:continue
  for goal,(yield_,cost,route) in solve(target,voltage,path+(key,)).items():
   candidate=(yield_*n,seconds+cost*n,[m]+route)
   if goal not in result or candidate[0]>result[goal][0] or candidate[0]==result[goal][0] and candidate[1]<result[goal][1]:result[goal]=candidate
 return result
out=[]
for k,x in nodes.items():
 if not x.get('identity','').startswith('rawOre:'):continue
 for voltage in [0,32]:
  for goal,(yield_,seconds,path) in solve(k,voltage).items():out.append(dict(material=x['identity'].split(':')[1],voltage=voltage,goal=goal,yield_=yield_,seconds=seconds,path=path))
(ROOT/'basic-processing.json').write_text(json.dumps(out,indent=2))
print('Conservative processing records',len(out))
