"""Summarize actual native harvest/drill runs and broad physical resource availability."""
from pathlib import Path
from collections import Counter
import csv,gzip,json,statistics,argparse
ROOT=Path(__file__).resolve().parent

def ores(path):
    with gzip.open(path,'rt') as f:return {(int(r['x']),int(r['y']),int(r['z'])):r for r in csv.DictReader(f)}
def analyze(label):
    p=ROOT/'reports'/label
    qa=json.loads((p/'horizonqa/horizonqa-result.json').read_text())
    assert qa['status']=='passed',(label,qa['status'])
    props=dict(l.split('=',1) for l in (p/'drill.properties').read_text().splitlines())
    before=ores(p/'drill-before/ores.csv.gz');after=ores(p/'drill-after/ores.csv.gz')
    removed={k:r for k,r in before.items() if k not in after}
    low_left={k:r for k,r in after.items() if k[1]<100}
    changed={k:r for k,r in after.items() if k in before and r['material']!=before[k]['material']}
    drops={r['item']:int(r['count']) for r in csv.DictReader((p/'drill-drops.csv').open())}
    hours=int(props['ticks'])/20/3600
    result={'run':json.loads((p/'run.json').read_text()),'native_horizonqa':qa['counts'],'drill':{
      'properties':props,'wall_independent_game_hours':hours,'physical_ore_blocks_before':len(before),'physical_ore_blocks_removed':len(removed),
      'full_ore_blocks_removed':sum(r['small']=='false' for r in removed.values()),'small_ore_blocks_removed':sum(r['small']=='true' for r in removed.values()),
      'removed_materials':dict(Counter(r['material'] for r in removed.values())),'physical_ore_blocks_after':len(after),'left_below_controller':len(low_left),
      'left_below_controller_materials':dict(Counter(r['material'] for r in low_left.values())),'changed_material_positions':len(changed),
      'output_items':sum(drops.values()),'output_item_types':len(drops),'drops':drops,'ore_blocks_per_game_hour':len(removed)/hours,'full_ore_blocks_per_game_hour':sum(r['small']=='false' for r in removed.values())/hours,
      'drilling_fluid_buckets':int(props['fluidUsed'])/1000}}
    if (p/'branch-routes.csv').exists():
      rows=list(csv.DictReader((p/'branch-routes.csv').open()));groups={}
      for tool in sorted({r['tool'] for r in rows}):
       for y in sorted({int(r['y']) for r in rows}):
        rr=[r for r in rows if r['tool']==tool and int(r['y'])==y];steps=sum(int(r['steps']) for r in rr);ore=sum(int(r['oreBlocks']) for r in rr)
        groups[f'{tool}-Y{y}']={'routes':len(rr),'steps':steps,'tunnel_blocks':sum(int(r['tunnelBlocks']) for r in rr),'ore_blocks':ore,
        'ore_per_100_steps':ore/steps*100 if steps else None,'tool_replacements':sum(int(r['toolReplacements']) for r in rr),
        'estimated_dig_minutes':sum(int(r['estimatedDigTicks']) for r in rr)/20/60,
        'first_resource':{res:{'found_routes':sum(int(r['first'+res])>=0 for r in rr),'first_steps':[int(r['first'+res]) for r in rr]} for res in ['Iron','Copper','Tin','Mica']}}
      result['branch']={'routes':rows,'groups':groups,'actual_drop_items':{r['item']:int(r['count']) for r in csv.DictReader((p/'branch-drops.csv').open())}}
    return result

def footprints():
    from mining_access import metadata,group
    meta=metadata('access4-seed2');out={}
    for mode,labels in {'stock':['before1b','before2'],'custom':['access4-seed1','access4-seed2']}.items():
      sites=[]
      for label in labels:
        p=ROOT/'reports'/label;props=dict(l.split('=',1) for l in (p/'world.properties').read_text().splitlines());cx=int(props['minChunkX']);cz=int(props['minChunkZ']);n=int(props['size'])
        chunks={}
        for row in ores(p/'ores.csv.gz').values():
          if row['small']=='true' or int(row['y'])>=100:continue
          key=(int(row['x'])//16,int(row['z'])//16);res,mult=group(row['material'],meta)
          chunks.setdefault(key,Counter())[res]+=mult
        for i in range(cx,cx+n-5,6):
          for j in range(cz,cz+n-5,6):sites.append(sum((chunks.get((x,z),Counter()) for x in range(i,i+6) for z in range(j,j+6)),Counter()))
      resources=sorted(set().union(*(s.keys() for s in sites)));out[mode]={}
      for resource in resources:
        values=sorted(s.get(resource,0) for s in sites)
        out[mode][resource]={'sites':len(values),'min':values[0],'p10':values[3],'median':statistics.median(values),'mean':statistics.mean(values),'max':values[-1],'zero':values.count(0)}
    return out

if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--labels',nargs='+',required=True);a=p.parse_args()
 results={l:analyze(l) for l in a.labels};results['footprints']=footprints()
 furnace=ROOT/'reports/native4-furnace2'
 if (furnace/'horizonqa/horizonqa-result.json').exists():
  status=json.loads((furnace/'horizonqa/horizonqa-result.json').read_text());assert status['status']=='passed'
  results['native_furnace']={l:{v['item']:int(v['count']) for v in csv.DictReader((furnace/(l+'-furnace-totals.csv')).open())} for l in a.labels if (furnace/(l+'-furnace-totals.csv')).exists()}
 results['limits']=[
 'GTNH native server, real block states, proper TConstruct tools, survival ItemInWorldManager harvesting, and real formed/ticked native drills. QA fixtures provide tools, electricity, fluid, pipes, maintenance and immediate output collection; production infrastructure is not simulated.',
 'Branches are scripted parallel two-high/one-wide tunnels with exposed connected-ore harvesting capped at256 per excursion. Permission and harvest checks remain native. Hunger, combat, lighting, travel and hazard avoidance are not simulated. Dig time is summed relative-block-hardness ticks, not wall playtime.',
 'Drill totals are completed native simulated machine ticks at20TPS, not computer benchmark time. Power consumption is separately measured over a four-tick unsupplied probe; total EU is not directly metered.',
 'Drill controller Y100 leaves native high-altitude ores above it untouched. Work area and bedrock floor are native. Leftover positions belowY100 are reported, not silently omitted.',
 'Footprint metal quantities use native direct-smelting multipliers on full GT ores only, not a complete ingot/advanced-byproduct processing audit.32 sites per variant across2seeds are not a universal guarantee.',
 'Furnace totals query the initialized native recipe registry for standard ore-dictionary output items. They are simple-smelting potential from collected drops, not a ticked furnace or advanced processing/byproduct yield.',
 'Headless outcomes cannot prove fun or a complete GTNH quest progression playthrough. Geology recommendations are not installed.'
 ]
 (ROOT/'reports/native-mining-summary.json').write_text(json.dumps(results,indent=2))
 for l in a.labels:
  d=results[l]['drill'];print(l,'removed',d['physical_ore_blocks_removed'],'left below',d['left_below_controller'],'hours',round(d['wall_independent_game_hours'],2),'outputs',d['output_items'],'types',d['output_item_types'])
