"""Static scientific figures plus a review page, preserving individual sites and misses."""
from pathlib import Path
import csv,html,json,statistics
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
ROOT=Path(__file__).resolve().parent;OUT=ROOT/'reports/science6-final'
d=json.loads((OUT/'results.json').read_text());bulk=d['bulk'];runs=bulk['runs']
plt.rcParams.update({'font.family':'DejaVu Sans','font.size':10,'axes.spines.top':False,'axes.spines.right':False})
fig,axes=plt.subplots(1,3,figsize=(14,5),layout='constrained');colors=['#3268a8','#ac5f32','#388d68','#9c4ea0']
for ax,(name,r) in zip(axes,bulk['resources'].items()):
 for i,p in enumerate(r['pairs']):
  offset=(i-1.5)*.04;ax.plot([offset,1+offset],[p['stock'],p['custom']],'-o',color=colors[i],alpha=.85,label=f"Seed {i+1}")
 ax.scatter([0,1],[r['stock_mean_rate'],r['custom_mean_rate']],marker='_',s=650,linewidths=3,color='black',zorder=5)
 ax.set_xticks([0,1],['Stock','Algorithm 6']);ax.set_xlim(-.25,1.25);ax.set_ylim(bottom=0);ax.grid(axis='y',alpha=.18)
 ax.set_title(f"{name}: mean {r['mean_change_percent']:+.1f}%",fontweight='bold')
axes[0].set_ylabel('Native simple-smelting potential / simulated IV drill hour');axes[0].legend(loc='lower left',fontsize=9)
fig.suptitle('Useful bulk supply: four paired 324-chunk sites',fontsize=16,fontweight='bold')
fig.supxlabel('Lines pair the same world seed; black marks are equally weighted means. This is not advanced factory output or a confidence interval.',fontsize=9)
fig.savefig(OUT/'bulk-supply.png',dpi=160);plt.close(fig)
branch=[]
componentrows=[]
for i in range(1,5):
 label='science6-v2-ow'+str(i);groups=runs[label]['branch']['groups'];counts={}
 comp=runs[label]['actual_components'];componentrows.append([i,comp['largest'],comp['over_500'],comp['over_1000']])
 for resource,y in [('Tin',56),('Mica',28)]:
  selected=[groups[f'{tool}-Y{y}'] for tool in ['Bronze','Steel']]
  counts[resource]=(sum(g['first_resource'][resource]['found_routes'] for g in selected),sum(g['routes'] for g in selected))
 branch.append(dict(seed=runs[label]['run']['seed'],counts=counts))
fig,axes=plt.subplots(1,2,figsize=(11,4),layout='constrained')
for ax,res in zip(axes,['Tin','Mica']):
 vals=[r['counts'][res][0] for r in branch];ax.bar(range(1,5),vals,color='#258475');ax.set_xticks(range(1,5));ax.set_ylim(0,8.6);ax.set_yticks(range(0,9,2));ax.set_xlabel('World seed number');ax.set_title(res+' at Y'+str(56 if res=='Tin' else 28));ax.grid(axis='y',alpha=.15)
 for x,v in enumerate(vals,1):ax.text(x,v+.15,f'{v}/8',ha='center')
axes[0].set_ylabel('Bronze/steel routes that found the target');fig.suptitle('Early discovery: non-finds remain visible',fontweight='bold')
fig.supxlabel('Four routes per tool per target depth. Scripted tunnels, capped excursions and native gates; no survival timing claim.',fontsize=9)
fig.savefig(OUT/'branch-discovery.png',dpi=160);plt.close(fig)
def row(values):return '<tr>'+''.join('<td>'+html.escape(str(v))+'</td>' for v in values)+'</tr>'
componentrows=[row(v) for v in componentrows]
pairrows=[]
for name,r in bulk['resources'].items():
 for i,p in enumerate(r['pairs'],1):pairrows.append(row([name,i,p['seed'],f"{p['stock']:,.1f}",f"{p['custom']:,.1f}",f"{p['change_percent']:+.1f}%"]))
pp=list(csv.DictReader((ROOT/'reports/science6-v2-processing-line3/native-processing-line.csv').open()))
stage_rows=[]
stage_order=['Machine_MV_OreWasher','Machine_MV_ThermalCentrifuge','Machine_MV_Macerator']
for mat,machine in sorted({(r['material'],r['stage']) for r in pp},key=lambda x:(x[0],stage_order.index(x[1]))):
 records=[r for r in pp if r['material']==mat and r['stage']==machine];r=records[0]
 stage_rows.append(row([mat,machine.replace('Machine_MV_',''),r['inputCount'],r['ticks'],f"{72000*int(r['inputCount'])/int(r['ticks']):,.1f}",r['energyProbeEU'],', '.join(x['output']+' x'+x['count'] for x in records)]))
cat=d['catalog'];par=d['progression_parity'];limits=''.join('<li>'+html.escape(v)+'</li>' for v in d['limitations'])
planetrows=[]
for name,r in d['retained_planetary_baseline']['two_paired_maahes_sites'].items():
 planetrows.append(row([name]+[f'{v:,.1f}' for v in [r['stock_rates'][0],r['algorithm5_rates'][0],r['stock_rates'][1],r['algorithm5_rates'][1]]]+[f"{r['mean_change_percent']:+.1f}%"]))
page=f'''<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Classic Mining algorithm 6 evidence</title><style>body{{background:#14202b;color:#e4edf4;font:16px/1.6 system-ui;margin:0}}main{{max-width:1200px;margin:auto;padding:36px 24px}}h1{{line-height:1.15;font-size:38px}}h2{{color:#93d8c9;margin-top:40px}}img{{max-width:100%;border-radius:10px;background:white}}a{{color:#b4ddff}}table{{border-collapse:collapse;font-size:13px;width:100%}}td,th{{padding:8px;border-bottom:1px solid #344654;text-align:left}}.scroll{{overflow:auto}}.card{{padding:20px;background:#203242;border-radius:12px}}code{{font-size:12px;overflow-wrap:anywhere}}</style><main>
<p>GTNH 2.9.0-RC-2 · algorithm 6 · bulk-20261010-v2 · experimental</p><h1>Random discoveries.<br>Coherent deposits. Measured supply.</h1>
<p>Ordinary finds, seams and rare industrial jackpots coexist. Native technology gates remain. This report records what was measured and what remains uncertain.</p>
<div class="card"><b>Exact tested main artifact</b><br><code>{d['candidate_sha256']}</code><p>Fresh worlds only. Existing saves must retain their matching algorithm and profile. Local preparation is not a public release.</p></div>
<h2>Bulk supply</h2><p>Four native IV drills worked paired 324-chunk sites: two calibration seeds and two seeds held out before the v1 campaign. The v2 associated-mineral correction followed that campaign; these are not four untouched blind validation seeds. All custom drills completed with zero GT ore blocks left below controller Y100. Fixtures supplied power, fluid, pipes, maintenance and collection.</p>
<img src="bulk-supply.png" alt="Four paired stock and algorithm 6 useful-resource supply rates"><p>All three equally weighted mean rates must stay within 20% of stock under the provisional target. Individual-site changes are much larger; averaging does not guarantee identical progression on any seed. Three retained stock controls have native fixture class bytes identical to their candidate comparison. The oldest stock control records its QA hash but lacks a retained QA jar, so that byte-level comparison is unavailable.</p>
<div class="scroll"><table><thead><tr><th>Resource</th><th>Pair</th><th>Seed</th><th>Stock potential/hour</th><th>Candidate potential/hour</th><th>Change</th></tr></thead><tbody>{''.join(pairrows)}</tbody></table></div>
<h2>Early discovery</h2><img src="branch-discovery.png" alt="Tin and mica discovery counts across all four candidate seeds"><p>Each bar includes misses. Bronze and steel are tested at native harvest permissions. The repaired/leveled native flint lifecycle and 28 starter harvest gates match the retained stock control; this synthetic fixture does not measure a complete survival start.</p>
<h2>Real large finds</h2><p>These are face-connected actual custom full-ore blocks, grouped across associated minerals inside each sampled drill area. Overlapping bodies can merge, while caves, rejected hosts and sample edges can split a planned deposit. They measure natural connected hauls, not individual named industrial deposits.</p><table><thead><tr><th>Seed number</th><th>Largest connected group</th><th>Groups at least 500 blocks</th><th>Groups at least 1000 blocks</th></tr></thead><tbody>{''.join(componentrows)}</tbody></table>
<h2>Actual native processing</h2><p>Eight minerals passed washing, thermal centrifuging and maceration: 24 completed native MV stages. Primary outputs were manually handed to the next stage; water and rated MV power were supplied. Every stage consumed stored electricity during a four-tick unsupplied probe. Byproducts are reported as actual items, not silently counted as finished ingots. Batch times include fixture startup/collection granularity. Input/hour is 72000*batch input/batch ticks, a small-batch machine benchmark rather than a steady-state finished-metal forecast. Native processing capacity still requires parallel machines or upgrades; ore availability does not remove that engineering challenge.</p>
<div class="scroll"><table><thead><tr><th>Material</th><th>MV machine</th><th>Input</th><th>Batch ticks</th><th>Input/hour</th><th>4-tick EU probe</th><th>Actual outputs</th></tr></thead><tbody>{''.join(stage_rows)}</tbody></table></div>
<h2>Coverage and progression</h2><p>{cat['fixture_cases']:,} native material-role fixtures, {cat['materials']} materials, {cat['dimensions']} eligible dimensions. {cat['natural_custom_blocks']:,} generated custom blocks audited with zero material/dimension/height violations. {cat['naturally_observed_materials']} materials appeared naturally in the finite sample; unseen materials: {html.escape(', '.join(cat['unobserved_in_natural_sample']))}.</p>
<p>Critical naturally observed counts: {html.escape(str(cat['critical_material_blocks']))}. Asteroids retain their own native channels; the empty Underdark catalog remains native. The End retains its native height exception.</p>
<p>Required content differences: {len(par['required_content_differences'])}. Audit covers {par['machine_maps']} native machine maps / {par['machine_recipes']:,} recipes, {par['assemblyline_recipes']} assembly-line recipes, {par['void_tables']} void tables and {par['stargate_recipes']} Stargate recipes plus SpaceMiningData. {len(par['raw_ordered_differences'])} raw ordered hashes differ in maps also variable between stock restarts; complete normalized content is checked separately. Output priority/order and other auxiliary metadata are not claimed identical.</p>
<p>All 256 material identities passed native prospecting acceptance and packet round-trip. Powered asynchronous scan and rendered UI remain unverified; cached vein labels are a separate compatibility concern.</p>
<h2>Planetary supply: retained evidence and open outliers</h2><p>Algorithm 6 preserves algorithm-5 planetary placement plans. The table below retains that earlier two-site Maahes drill evidence, not newly ticked algorithm-6 planet drills. New algorithm-6 natural planet coverage is audited above. These outliers are why this release cannot promise unchanged endgame supply or progression hours. Counts are actual native output items, not finished ingots; a zero-stock site makes relative changes especially sensitive.</p>
<div class="scroll"><table><thead><tr><th>Native output / hour</th><th>Stock site 1</th><th>Algorithm 5 site 1</th><th>Stock site 2</th><th>Algorithm 5 site 2</th><th>Mean change</th></tr></thead><tbody>{''.join(planetrows)}</tbody></table></div>
<h2>Model and limits</h2><p>In Tin-primary layers, compact-find probability p sums q*(0.5+0.5*t): local layer probability q and native Tin-role probability t. Expected ordinary loss is 0.94*p*4; each rare 1/3000 industrial target receives 11280*p extra planned blocks. This balances expected planned mass before clipping, caves, host rejection and overlap. It does not guarantee actual tonnage. Iron/copper/tin factors 1.8/2.4/0.45 alter only large Overworld bodies under positive constrained allocation.</p>
<ul>{limits}</ul><p><a href="results.json">Complete sanitized aggregate evidence</a> · <a href="README.md">Reading guide</a></p></main></html>'''
(OUT/'comparison.html').write_text(page,encoding='utf-8')
(OUT/'README.md').write_text('# Algorithm 6 final evidence\n\nRead comparison.html for paired rates, censored discovery, processing outputs, coverage and limits. results.json retains exact artifacts and aggregate native evidence.\n\nFresh worlds only; no universal progression-time, geology or fun claim.\n')
print(OUT/'comparison.html')
