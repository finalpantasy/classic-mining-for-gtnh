"""Paired native IV supply and branch discovery; sites, not ores, are sample units."""
from pathlib import Path
import argparse,csv,gzip,hashlib,json,statistics,zipfile
from analyze_native_mining import analyze
ROOT=Path(__file__).resolve().parent
STOCK=['native4-iv-stock2','science5-stock-ow-seed2','science6-stock-heldout3','science6-stock-heldout4']
def components(label):
 with gzip.open(ROOT/'reports'/label/'drill-before/ores.csv.gz','rt') as f:
  remaining={(int(r['x']),int(r['y']),int(r['z'])) for r in csv.DictReader(f) if r['small']=='false' and r['custom']=='true'}
 sizes=[]
 while remaining:
  stack=[remaining.pop()];count=0
  while stack:
   x,y,z=stack.pop();count+=1
   for p in [(x+1,y,z),(x-1,y,z),(x,y+1,z),(x,y-1,z),(x,y,z+1),(x,y,z-1)]:
    if p in remaining:remaining.remove(p);stack.append(p)
  sizes.append(count)
 return dict(connected_custom_full_ore_components=len(sizes),largest=max(sizes,default=0),over_500=sum(n>=500 for n in sizes),over_1000=sum(n>=1000 for n in sizes),largest_ten=sorted(sizes)[-10:],interpretation='Face-connected actual custom full ores across minerals. Overlapping bodies can merge; caves/host rejection and sample boundaries can split a planned body.')
def main():
 p=argparse.ArgumentParser();p.add_argument('--custom',nargs=4,required=True);p.add_argument('--processing',required=True);p.add_argument('--output',required=True);a=p.parse_args()
 proc=ROOT/'reports'/a.processing
 assert json.loads((proc/'horizonqa/horizonqa-result.json').read_text())['status']=='passed'
 sha=json.loads((proc/'run.json').read_text())['jar_sha256']['classic-mining-0.1.0.jar']
 runs={};fixture_comparisons=[]
 for stock,custom in zip(STOCK,a.custom):
  for label in [stock,custom]:
   d=analyze(label);r=d['run'];drill=d['drill']
   assert r['native'] and r['drill_tier']==4 and r['native_planet']=='Overworld'
   assert drill['properties']['completed']=='true' and drill['left_below_controller']==0
   props=dict(l.split('=',1) for l in (ROOT/'reports'/label/'drill-before/world.properties').read_text().splitlines() if '=' in l)
   assert (int(props['minChunkX']),int(props['minChunkZ']),int(props['size']))==(64,-88,18)
   totals={row['item']:int(row['count']) for row in csv.DictReader((proc/(label+'-furnace-totals.csv')).open())}
   d['rates']={n:totals.get('ingot:'+n,0)/drill['wall_independent_game_hours'] for n in ['Iron','Copper','Tin']}
   r.pop('command',None)
   if label==custom:
    assert r['custom'] and r['jar_sha256']['classic-mining-0.1.0.jar']==sha
    d['actual_components']=components(label)
   else:assert not r['custom']
   runs[label]=d
  assert runs[stock]['run']['seed']==runs[custom]['run']['seed']
  old=ROOT/'reports'/stock/'qa-artifact.jar';new=ROOT/'reports'/custom/'qa-artifact.jar'
  if old.exists():
   with zipfile.ZipFile(old) as s,zipfile.ZipFile(new) as c:
    names=[n for n in s.namelist() if n.startswith('classicminingqa/NativeMiningTests') and n.endswith('.class')]
    assert names and all(s.read(n)==c.read(n) for n in names)
   fixture_comparisons.append({'stock':stock,'custom':custom,'native_fixture_bytes_identical':True})
  else:fixture_comparisons.append({'stock':stock,'custom':custom,'native_fixture_bytes_identical':None,'reason':'Retained first control has recorded hash but no QA jar snapshot.'})
 resources={}
 for n in ['Iron','Copper','Tin']:
  pairs=[dict(seed=runs[s]['run']['seed'],stock=runs[s]['rates'][n],custom=runs[c]['rates'][n]) for s,c in zip(STOCK,a.custom)]
  for pair in pairs:pair['change_percent']=100*(pair['custom']/pair['stock']-1)
  sm=statistics.mean(v['stock'] for v in pairs);cm=statistics.mean(v['custom'] for v in pairs)
  resources[n]=dict(pairs=pairs,stock_mean_rate=sm,custom_mean_rate=cm,mean_change_percent=100*(cm/sm-1))
 accepted=all(abs(r['mean_change_percent'])<=20 for r in resources.values()) and max(abs(r['mean_change_percent']) for r in resources.values())<77.1
 out=ROOT/'reports'/a.output;out.mkdir(exist_ok=False)
 result=dict(main_sha256=sha,processing_label=a.processing,resources=resources,provisional_factory_target_passed=accepted,fixture_comparisons=fixture_comparisons,runs=runs,limits=['Four paired sites: two calibration seeds and two seeds held out before v1. The v2 associated-mineral fix followed observing v1 outcomes; these are not four untouched blind validation seeds. No population-wide guarantee or confidence interval.','Furnace lookup on collected native drill drops gives simple-smelting potential, not a running advanced factory.','Fixtures supply electricity, fluid, pipes, maintenance and output collection; hours are machine ticks at 20TPS.','Scripted branch routes omit hunger, combat, travel and lighting. Censored misses are retained.','The earlier algorithm5 maximum discrepancy of77.1percent came from two calibration sites. Comparing it with this four-site result is descriptive, not a matched four-site improvement claim.','Factory acceptance is a provisional common-resource check; it does not prove all-resource or two-Stargate progression parity.'])
 (out/'results.json').write_text(json.dumps(result,indent=2)+'\n')
 print(json.dumps(dict(resources=resources,provisional_factory_target_passed=accepted),indent=2))
if __name__=='__main__':main()
