"""Plot actual pre-mining block exports. No simulated/reconstructed ore positions."""
from pathlib import Path
import argparse,csv,gzip,json,hashlib
from collections import Counter
from PIL import Image,ImageDraw,ImageFont

ROOT=Path(__file__).resolve().parent
PALETTE={'Iron-bearing': '#e6a36c','Copper-bearing':'#42d5c0','Tin-bearing':'#90b8ff','Coal / lignite':'#adb5c4','Redstone':'#ef7188','Other minerals':'#d6d071'}
BG='#101a25';FIELD='#1c2938';TEXT='#edf3f9';MUTED='#b3c2d3'

def read(folder):
    props=dict(line.split('=',1) for line in (folder/'world.properties').read_text().splitlines() if '=' in line)
    with gzip.open(folder/'ores.csv.gz','rt') as stream:
        rows=[(int(r['x']),int(r['y']),int(r['z']),r['material']) for r in csv.DictReader(stream) if r['small']=='false']
    return props,rows

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('--data-root',type=Path,required=True,help='Local Science6 reports directory, not included in public source')
    parser.add_argument('--font-dir',type=Path,default=Path('C:/Windows/Fonts'))
    args=parser.parse_args()
    out=ROOT/'reports/science6-map';out.mkdir(exist_ok=True)
    labels=['native4-iv-stock2','science6-v2-ow1']
    datasets=[read(args.data_root/label/'drill-before') for label in labels]
    before,after=[v[0] for v in datasets]
    for key in ['seed','dimension','minChunkX','minChunkZ','size']: assert before[key]==after[key],key
    assert before['custom']=='false' and after['custom']=='true'
    assert before['dimension']=='Overworld' and before['seed']=='92837465' and before['size']=='18'
    run=json.loads((args.data_root/labels[1]/'run.json').read_text())
    jar_sha=run['jar_sha256']['classic-mining-0.1.0.jar']
    assert jar_sha=='4b4e285f2e4b8a90edbb57aa33328fd64911a36a4ed451beb246ad67aefa6272'
    with (ROOT/'reports/science6-v2-catalog/material-yields.csv').open() as stream:
        smelts={r['material']:r['directSmelting'] for r in csv.DictReader(stream)}
    def group(material):
        output=smelts.get(material,'')
        if output in ['Iron','Copper','Tin']:return output+'-bearing'
        if material in ['Coal','Lignite']:return 'Coal / lignite'
        if material=='Redstone':return 'Redstone'
        return 'Other minerals'
    minx=int(before['minChunkX'])*16;minz=int(before['minChunkZ'])*16;n=int(before['size'])*16
    lo,hi=1,99;zslice=minz+n//2
    assert n==288
    selected=[[r for r in rows if lo<=r[1]<=hi] for props,rows in datasets]
    counts=[len(rows) for rows in selected]
    stats=[]
    maps=[];sections=[]
    # A fixed first calibration pair and midpoint section avoid picking a flattering showcase.
    for index,rows in enumerate(selected):
        im=Image.new('RGB',(n,n),FIELD)
        section=Image.new('RGB',(n,hi-lo+1),FIELD)
        for x,y,z,m in sorted(rows,key=lambda r:r[1]):
            assert minx<=x<minx+n and minz<=z<minz+n
            im.putpixel((x-minx,z-minz),Image.new('RGB',(1,1),PALETTE[group(m)]).getpixel((0,0)))
            if z==zslice:section.putpixel((x-minx,hi-y),Image.new('RGB',(1,1),PALETTE[group(m)]).getpixel((0,0)))
        maps.append(im);sections.append(section)
        stats.append(dict(label=labels[index],full_ore_blocks=counts[index],ore_columns=len({(r[0],r[2]) for r in rows}),materials=len({r[3] for r in rows}),groups=dict(Counter(group(r[3]) for r in rows)),midpoint_section_blocks=sum(r[2]==zslice for r in rows)))
    def font(size,bold=False):return ImageFont.truetype(str(args.font_dir/('segoeuib.ttf' if bold else 'segoeui.ttf')),size)
    canvas=Image.new('RGB',(1600,1490),BG);d=ImageDraw.Draw(canvas)
    d.text((50,28),'Before & after: actual underground ore maps',font=font(43,True),fill=TEXT)
    d.text((50,91),'Same Overworld seed 92837465 · Same 288 × 288-block region · Full GT ores at Y 1–99',font=font(25),fill=MUTED)
    d.text((50,128),f'World X {minx}–{minx+n-1} · Z {minz}–{minz+n-1} · Each source pixel = one block column',font=font(23),fill=MUTED)
    for i,title in enumerate(['BEFORE · Stock GTNH','AFTER · Classic Mining Science 6']):
        left=50+i*780
        d.text((left,173),title,font=font(30,True),fill=TEXT)
        canvas.paste(maps[i].resize((720,720),Image.Resampling.NEAREST),(left,220))
        d.rectangle((left,220,left+719,939),outline='#52677e',width=1)
        # Light guide lines every 3 chunks. These are guides, not an assertion of a vein at every cell.
        for offset in range(48,n,48):
            v=round(offset*720/n)
            d.line((left+v,220,left+v,939),fill='#354858',width=1)
            d.line((left,220+v,left+719,220+v),fill='#354858',width=1)
        d.text((left,953),f'{counts[i]:,} full ore blocks · {stats[i]["ore_columns"]:,} occupied columns',font=font(23),fill=TEXT)
    for i,(name,color) in enumerate(PALETTE.items()):
        x=50+(i%3)*515;y=1002+(i//3)*35
        d.rectangle((x,y+5,x+19,y+24),fill=color)
        d.text((x+31,y),name,font=font(22),fill=TEXT)
    d.text((50,1081),f'Fixed middle cross-section · Z {zslice} · Y 99 at top, Y 1 at bottom · Same scale in both panels',font=font(24,True),fill=TEXT)
    for i,section in enumerate(sections):
        left=50+i*780
        canvas.paste(section.resize((720,248),Image.Resampling.NEAREST),(left,1128))
        d.rectangle((left,1128,left+719,1375),outline='#52677e',width=1)
    d.text((50,1395),'Top maps project ores through Y 1–99; the highest ore in each column sets its color. Colors group native smelting families.',font=font(21),fill=MUTED)
    d.text((50,1427),'Dark = no exported full ore in the selection, not necessarily air. Small ores omitted. Actual server data, not game screenshots.',font=font(21),fill=MUTED)
    png=out/'before-after-ore-map.png';canvas.save(png)
    result=dict(seed=int(before['seed']),dimension=before['dimension'],candidate_sha256=jar_sha,algorithm=6,profile='bulk-20261010-v2',region=dict(min_x=minx,min_z=minz,width=n,height=n,min_y=lo,max_y=hi,section_z=zslice),variants=stats,source_exports=[dict(label=label,ore_export_sha256=hashlib.sha256((args.data_root/label/'drill-before/ores.csv.gz').read_bytes()).hexdigest()) for label in labels],selection='First paired calibration seed, complete drill footprint, pre-mining exports; fixed midpoint section. Not selected for visual prominence.',projection='Highest selected full ore per X/Z column; small ores omitted. Family colors use native direct-smelting identity, not guaranteed output rate.',limits=['Paired seed, coordinates and world settings, not guaranteed identical host terrain.','Three-chunk guides are visual references, not guaranteed vein centers.','A single region does not establish all-seed or endgame balance.','This is data visualization, not rendered gameplay.'])
    (out/'map-data.json').write_text(json.dumps(result,indent=2)+'\n')
    (out/'README.md').write_text('# Actual before-and-after ore maps\n\n![Matched stock and Science6 underground ore maps](before-after-ore-map.png)\n\nSeed **92837465**, Overworld, **18 × 18 chunks** at X1024–1311, Z−1408–−1121. The complete first paired drill footprint is shown **before any branch mining or drill extraction**. This is the first calibration pair, with a fixed midpoint cross-section, not a selected jackpot showcase.\n\nTop panels project all full GT ore blocks at **Y1–99** onto X/Z columns. The highest selected ore supplies each column color; stacked ores can be hidden. Six colors group native direct-smelting iron/copper/tin identities, coal/lignite, redstone and other minerals. Small ores are omitted. Dark space means no exported full ore in the selection, not necessarily an air block. Guide lines are 48 blocks apart. Lower panels show one actual vertical slice through the region midpoint, **Z−1264**.\n\nBefore: '+f'{counts[0]:,} full ore blocks. After: {counts[1]:,} full ore blocks. Counts are blocks, not ingot yields or mining time.\n\n'+'The worlds share seed, coordinates and settings; native terrain and generation order can differ. This is an actual server block export visualization, not a rendered Minecraft screenshot. It illustrates distribution in one region and does not prove population-wide balance. [Metadata, counts and source hashes](map-data.json). Reproduce with `python make_ore_map.py --data-root <local-Science6-reports>`; raw exports/dependencies must be obtained separately.\n',encoding='utf-8')
    print(json.dumps(result,indent=2))
    print(png)

if __name__=='__main__':main()
