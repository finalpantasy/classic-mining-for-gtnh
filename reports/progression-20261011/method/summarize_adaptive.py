from pathlib import Path
import json,statistics
ROOT=Path(__file__).resolve().parents[1]
rows=json.loads((ROOT/'adaptive-campaigns.json').read_text())
def med(a):return round(statistics.median(a),3) if a else None
out={'policies':[],'matchedComparisons':[]}
assert len(rows)==60
for r in rows:
 assert all(s['complete'] for s in r['stages'][:-1])
 assert abs(sum(s['hours'] for s in r['stages'])-r['miningCampaignHours'])<1e-7
 d=r['recovery'];assert 0<=d['normalOreHarvested']<=d['normalOreInExcavatedChunks']<=d['normalOreInSample']
for policy in ['grid-with-trips','branch-with-trips','local-probes']:
 a=[r for r in rows if r['policy']==policy]
 out['policies'].append(dict(policy=policy,campaigns=len(a),throughLV=sum(r['completedStages']>=5 for r in a),throughMV=sum(r['completedStages']>=7 for r in a),medianSampleRecoveryPct=med([100*r['recovery']['recoveryOfSample'] for r in a]),medianExcavatedChunkRecoveryPct=med([100*r['recovery']['recoveryOfExcavatedChunks'] for r in a]),medianOreTrips=med([r['recovery']['oreLoadTrips'] for r in a])))
for n,name in [(5,'LV budget'),(7,'MV budget')]:
 for stockPolicy,modPolicy in [('grid-with-trips','branch-with-trips'),('grid-with-trips','local-probes'),('branch-with-trips','local-probes')]:
  left={(r['dataset'],r['replicate']):r for r in rows if r['policy']==stockPolicy and r['completedStages']>=n}
  right={(r['dataset'],r['replicate']):r for r in rows if r['policy']==modPolicy and r['completedStages']>=n}
  pairs=[(r,right[k]) for k,r in left.items() if k in right]
  def hours(r):return sum(s['hours'] for s in r['stages'][:n])
  out['matchedComparisons'].append(dict(stage=name,leftPolicy=stockPolicy,rightPolicy=modPolicy,pairs=len(pairs),seeds=len(set(r['dataset'] for r,s in pairs)),leftMedianHours=med([hours(r) for r,s in pairs]),rightMedianHours=med([hours(s) for r,s in pairs]),medianAdditionalHours=med([hours(s)-hours(r) for r,s in pairs])))
(ROOT/'adaptive-summary.json').write_text(json.dumps(out,indent=2))
print(json.dumps(out,indent=2))
