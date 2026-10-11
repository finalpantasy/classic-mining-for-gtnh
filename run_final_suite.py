"""Sequential final-candidate servers. Main/QA artifacts must remain frozen throughout."""
from pathlib import Path
import hashlib,json,subprocess,sys,time
ROOT=Path(__file__).resolve().parent
def sha(name):return hashlib.sha256((ROOT/'build'/name).read_bytes()).hexdigest()
def main():
 expected={n:sha(n) for n in ['classic-mining-0.1.0.jar','classic-mining-qa-0.1.0.jar']}
 labels=['science6-v2-ow'+str(i) for i in range(1,5)]
 jobs=[['--label',label,'--seed',str(seed),'--native','--drill-tier','4','--branch','--custom'] for label,seed in zip(labels,[92837465,-77123984,41067213,-208193740])]
 jobs += [['--label','science6-v2-catalog','--size','24','--catalog','--all-planets','--extra-dims','--custom'],['--label','science6-v2-readiness','--readiness','--custom'],['--label','science6-v2-economy-furnace','--economy','--processing-labels',','.join(['native4-iv-stock2','science5-stock-ow-seed2','science6-stock-heldout3','science6-stock-heldout4']+labels),'--custom']]
 manifest=ROOT/'reports/v2-suite.json'
 if manifest.exists():raise SystemExit('Existing suite manifest: use a new candidate identity, not an overwrite')
 results=[]
 for args in jobs:
  assert all(sha(n)==h for n,h in expected.items())
  print('FINAL start '+args[1],flush=True);start=time.time()
  r=subprocess.run([sys.executable,str(ROOT/'run_qa.py'),*args],cwd=ROOT)
  results.append(dict(label=args[1],exit_code=r.returncode,seconds=round(time.time()-start)))
  manifest.write_text(json.dumps(dict(artifacts=expected,results=results),indent=2)+'\n')
  print('FINAL finished '+args[1]+' exit='+str(r.returncode),flush=True)
  if r.returncode:raise SystemExit(r.returncode)
if __name__=='__main__':main()
