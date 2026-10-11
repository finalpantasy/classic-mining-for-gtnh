"""Run the real GTNH server with reproducible seeds and a headless Horizon-QA suite."""
from pathlib import Path
import argparse, subprocess, shutil, time, json, hashlib

ROOT=Path(__file__).resolve().parent
SERVER=ROOT.parent/'classic-mining'/'qa-server'
JAVA=Path('F:/GTNH/runtime/jdk-25.0.4.1+1-jre/bin/java.exe')
p=argparse.ArgumentParser()
p.add_argument('--label',required=True)
p.add_argument('--seed',type=int,default=92837465)
p.add_argument('--custom',action='store_true')
p.add_argument('--catalog',action='store_true')
p.add_argument('--extra-dims',action='store_true')
p.add_argument('--all-planets',action='store_true')
p.add_argument('--reverse',action='store_true')
p.add_argument('--size',type=int,default=24)
p.add_argument('--native',action='store_true')
p.add_argument('--drill-tier',type=int,choices=[1,4],default=1)
p.add_argument('--branch',action='store_true')
p.add_argument('--native-planet',default='Overworld')
p.add_argument('--processing-labels',default='')
p.add_argument('--economy',action='store_true')
p.add_argument('--readiness',action='store_true')
p.add_argument('--processing-line',action='store_true')
args=p.parse_args()
world='qa-'+args.label
if (SERVER/world).exists(): raise SystemExit('Refusing to reuse an existing test world: '+world)
out=ROOT/'reports'/args.label
out.mkdir(parents=True,exist_ok=False)
props=dict(l.split('=',1) for l in (SERVER/'server.properties').read_text().splitlines() if '=' in l)
props.update({'level-seed':str(args.seed),'level-name':world})
(SERVER/'server.properties').write_text('\n'.join(k+'='+v for k,v in props.items())+'\n')
# Reuse the explicit acceptance for this isolated server; never accept terms for another user.
if not (SERVER/'eula.txt').exists() or not any(l.strip().lower()=='eula=true' for l in (SERVER/'eula.txt').read_text().splitlines()):
    raise SystemExit('Accept the Minecraft EULA yourself in qa-server/eula.txt before running tests')
(SERVER/'config/classic-mining.properties').write_text('enabled='+str(args.custom).lower()+'\ndepositsPerChunk=32\n')
for name in ['classic-mining-0.1.0.jar','classic-mining-qa-0.1.0.jar']:
    shutil.copy2(ROOT/'build'/name,SERVER/'mods'/name)
tests=[] if args.native else ['actualTerrain']
if args.catalog: tests.append('everyRegisteredLayerAndMaterial')
if args.extra_dims: tests.append('actualNetherMoonAndEnd')
if args.all_planets: tests.append('allRegisteredPlanetTerrain')
selector=','.join('classicminingqa:MiningWorldTests.'+t for t in tests)
if args.native: selector='classicminingqa:NativeMiningTests.automatedOreDrill'
if args.branch: selector+=',classicminingqa:NativeMiningTests.branchMining'
if args.processing_labels: selector='classicminingqa:NativeProcessingTests.furnacePrimaryYields'
if args.economy:
    selector=(selector+',' if args.processing_labels else '')+'classicminingqa:ProgressionTests.automationAndRecipes'
if args.readiness:
    selector='classicminingqa:ReadinessTests.starterToolLifecycle,classicminingqa:ReadinessTests.nativeProspectorPayload'
if args.processing_line:
    selector='classicminingqa:ProcessingLineTests.nativeMVLine'
cmd=[str(JAVA),'-Xms1G','-Xmx6G','-Dfile.encoding=UTF-8','-Duser.language=en',
     '-Dfml.readTimeout=180','-Dhorizonqa.mode=ci','-Dhorizonqa.world=normal',
     '-Dhorizonqa.tests='+selector,'-Dhorizonqa.gridOrigin=0,220,0',
     '-Dhorizonqa.reportDir='+str(out/'horizonqa'),'-Dclassicmining.qa.output='+str(out),
     '-Dclassicmining.qa.nativePlanet='+args.native_planet,'-Dclassicmining.qa.processingLabels='+args.processing_labels,'-Dclassicmining.qa.drillTier='+str(args.drill_tier),'-Dclassicmining.qa.size='+str(args.size),'-Dclassicmining.qa.reverse='+str(args.reverse).lower(),'-Dclassicmining.qa.audit=true',
     '@java9args.txt','-jar','lwjgl3ify-forgePatches.jar','nogui']
hashes={name:hashlib.sha256((SERVER/'mods'/name).read_bytes()).hexdigest() for name in ['classic-mining-0.1.0.jar','classic-mining-qa-0.1.0.jar','horizonqa-0.15.0.jar']}
shutil.copy2(ROOT/'build/classic-mining-qa-0.1.0.jar',out/'qa-artifact.jar')
(out/'run.json').write_text(json.dumps(dict(seed=args.seed,custom=args.custom,native=args.native,drill_tier=args.drill_tier,native_planet=args.native_planet,processing_labels=args.processing_labels,economy=args.economy,readiness=args.readiness,processing_line=args.processing_line,branch=args.branch,catalog=args.catalog,all_planets=args.all_planets,reverse=args.reverse,size=args.size,command=cmd,jar_sha256=hashes),indent=2))
print('Starting real GTNH headless server:',args.label,flush=True)
start=time.time()
with (out/'console.log').open('w',encoding='utf-8') as log:
    result=subprocess.run(cmd,cwd=SERVER,stdout=log,stderr=subprocess.STDOUT)
print('Server exited:',result.returncode,'elapsed seconds:',round(time.time()-start),flush=True)
text=(out/'console.log').read_text(errors='replace')
print('\n'.join(l for l in text.splitlines() if 'ClassicMiningQA' in l or 'GameTest' in l and any(s in l for s in ['RESULT','FAILED','PASSED','Caused by']))[-3500:])
report=out/'horizonqa/horizonqa-result.json'
if not report.exists(): raise SystemExit('Native server did not produce a completed HorizonQA result')
qa=json.loads(report.read_text())
if str(qa.get('status')).lower()!='passed': raise SystemExit('HorizonQA did not pass: '+str(qa.get('status')))
raise SystemExit(result.returncode)
