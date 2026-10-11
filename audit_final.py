"""Fail closed on final candidate coverage, native gates, and recipe/table content parity."""
from pathlib import Path
from collections import Counter
import csv,gzip,hashlib,json,statistics
ROOT=Path(__file__).resolve().parent;BASE=ROOT.parent/'classic-mining'
def rows(path):return list(csv.DictReader(path.open()))
def status(label,sha):
 p=ROOT/'reports'/label
 assert json.loads((p/'horizonqa/horizonqa-result.json').read_text())['status']=='passed'
 r=json.loads((p/'run.json').read_text());assert r['jar_sha256']['classic-mining-0.1.0.jar']==sha
 r.pop('command',None);return r
def parity(path):return {(r['kind'],r['name']):r for r in rows(path/'progression-parity.csv')}
def main():
 sha=hashlib.sha256((ROOT/'build/classic-mining-0.1.0.jar').read_bytes()).hexdigest()
 bulk=ROOT/'reports/science6-v2-bulk/results.json';b=json.loads(bulk.read_text())
 assert b['main_sha256']==sha and b['provisional_factory_target_passed']
 labels=['science6-v2-catalog','science6-v2-readiness','science6-v2-processing-line3','science6-v2-economy-furnace']
 manifest={l:status(l,sha) for l in labels}
 p=ROOT/'reports/science6-v2-catalog';catalog=rows(p/'catalog.csv')
 assert len(catalog)==1848 and len({r['material'] for r in catalog})==256 and len({r['dimension'] for r in catalog})==38
 assert all(all(r[k]=='true' for k in ['placementPassed','itemPassed','miningDropPassed','drillDetectionPassed']) for r in catalog)
 bands={}
 for r in catalog:bands.setdefault((r['dimension'],r['material']),[]).append((int(r['minY']),int(r['maxY'])))
 observed=Counter();violations=[];paths=list((p/'planets').glob('*/ores.csv.gz'));assert len(paths)==38
 for path in paths:
  with gzip.open(path,'rt') as f:
   for r in csv.DictReader(f):
    if r['custom']!='true':continue
    key=(path.parent.name,r['material']);y=int(r['y'])
    height=(1<=y<=255) if key[0]=='The End' else any(lo<=y<=hi for lo,hi in bands.get(key,[]))
    if key not in bands or not height:violations.append((key,y))
    observed[r['material']]+=1
 assert not violations and sum(observed.values())>1000
 critical={m:observed[m] for m in ['MysteriousCrystal','Honeaite','BArTiMaEuSNeK']};assert all(critical.values())
 stock=parity(BASE/'reports/science5-economy-stock4');custom=parity(ROOT/'reports/science6-v2-economy-furnace')
 diff=[dict(kind=k[0],name=k[1],stock=stock.get(k),custom=custom.get(k)) for k in sorted(stock.keys()|custom.keys()) if stock.get(k)!=custom.get(k)]
 ca=parity(BASE/'reports/science5-economy-stock2');cb=parity(BASE/'reports/science5-economy-stock3')
 variable={k[1] for k in ca.keys()|cb.keys() if k[0]=='machine' and ca.get(k)!=cb.get(k)}
 raw=[r for r in diff if r['kind']=='machine'];required=[r for r in diff if r['kind']!='machine']
 assert not required,required
 assert all(r['name'] in variable and r['stock']['entries']==r['custom']['entries'] for r in raw)
 pp=rows(ROOT/'reports/science6-v2-processing-line3/native-processing-line.csv')
 stages={(r['material'],r['stage']) for r in pp};assert len(stages)==24
 assert all(int(r['energyProbeEU'])>0 and int(r['energyProbeTicks'])==4 for r in pp)
 readiness=ROOT/'reports/science6-v2-readiness'
 # Exact stock control lifecycle and native tool gates must remain unchanged.
 def properties(path):return dict(l.split('=',1) for l in path.read_text().splitlines() if l and not l.startswith('#'))
 assert properties(readiness/'starter-tool.properties')==properties(BASE/'reports/science5-readiness-stock3/starter-tool.properties')
 assert (readiness/'starter-harvest-gates.csv').read_bytes()==(BASE/'reports/science5-readiness-stock3/starter-harvest-gates.csv').read_bytes()
 prospect=rows(readiness/'prospector-payloads.csv')
 assert len(prospect)==256 and all(all(r[k]=='true' for k in ['directProspectorAccepted','packetIdentityPassed','roundTripPassed']) for r in prospect)
 prior=json.loads((BASE/'reports/science5/results.json').read_text());planetary={}
 for item in ['crushed:Naquadah','dust:NaquadahOxideMixture','dust:Enriched-NaquadahOxideMixture','dust:NaquadriaOxideMixture','crushed:Neutronium','crushed:Adamantium','crushed:Trinium']:
  stock_rates=[prior['runs'][l]['drill']['output_items_per_hour'].get(item,0) for l in ['native4-maahes-stock-iv','science5-stock-maahes2']]
  custom_rates=[prior['runs'][l]['drill']['output_items_per_hour'].get(item,0) for l in ['science5-final-maahes1','science5-final-maahes2']]
  planetary[item]=dict(stock_rates=stock_rates,algorithm5_rates=custom_rates,mean_change_percent=100*(statistics.mean(custom_rates)/statistics.mean(stock_rates)-1))
 result=dict(algorithm=6,profile='bulk-20261010-v2',candidate_sha256=sha,bulk=b,
  catalog=dict(fixture_cases=len(catalog),materials=256,dimensions=38,natural_custom_blocks=sum(observed.values()),naturally_observed_materials=len(observed),unobserved_in_natural_sample=sorted({r['material'] for r in catalog}-observed.keys()),critical_material_blocks=critical,violations=violations),
  progression_parity=dict(required_content_differences=required,raw_ordered_differences=raw,stock_control_variable_maps=sorted(variable),machine_maps=sum(k[0]=='machine' for k in custom),machine_recipes=sum(int(r['entries']) for k,r in custom.items() if k[0]=='machine'),void_tables=sum(k[0].startswith(('dropMaps','extraDrops')) for k in custom),assemblyline_recipes=sum(int(r['entries']) for k,r in custom.items() if k[0]=='assemblyline'),stargate_recipes=sum(k[0]=='stargate' for k in custom)),
  processing=dict(materials=8,completed_native_stages=24,positive_native_energy_probes=True),starter_native_control_identical=True,run_manifest=manifest,
  retained_planetary_baseline=dict(algorithm=5,main_sha256=prior['candidate_sha256'],new_algorithm6_drill_measurement=False,source='retained science5/results.json',two_paired_maahes_sites=planetary),
  limitations=b['limits']+['Planetary placement plans retain algorithm5. Previous two Maahes site pairs retain rare-material output outliers; no universal planet balance claim.','Catalog fixtures test every active material role, not a natural occurrence guarantee for every material in finite terrain. Asteroid channels and empty Underdark catalog remain native.','Recipe content audit includes native quantities, NBT, behavior flags, assembly-line research, void tables, Stargate and SpaceMiningData. Native output ordering and other auxiliary metadata are outside parity.','Prospecting identity/payload tests do not prove rendered scanner UI or cached vein-label accuracy.','No complete survival playthrough or elapsed time to two Stargates has been measured.'])
 out=ROOT/'reports/science6-final';out.mkdir(exist_ok=False)
 (out/'results.json').write_text(json.dumps(result,indent=2)+'\n')
 print(json.dumps({k:v for k,v in result.items() if k in ['candidate_sha256','catalog','processing','starter_native_control_identical']},indent=2))
 print('Required recipe/table differences:',len(required),'strict ordered differences:',len(raw))
if __name__=='__main__':main()
