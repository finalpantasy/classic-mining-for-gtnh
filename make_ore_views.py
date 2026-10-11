"""Orthographic and isometric views of actual pre-mining ore voxels."""
from pathlib import Path
import argparse,json,hashlib,math
from PIL import Image,ImageDraw,ImageFont,ImageColor
from make_ore_map import read,PALETTE,BG,FIELD,TEXT,MUTED

ROOT=Path(__file__).resolve().parent

def main():
    p=argparse.ArgumentParser()
    p.add_argument('--data-root',type=Path,required=True)
    p.add_argument('--font-dir',type=Path,default=Path('C:/Windows/Fonts'))
    args=p.parse_args()
    out=ROOT/'reports/science6-map'
    meta=json.loads((out/'map-data.json').read_text())
    labels=[v['label'] for v in meta['source_exports']]
    data=[read(args.data_root/label/'drill-before') for label in labels]
    for i,(props,rows) in enumerate(data):
        assert props['seed']==str(meta['seed']) and props['size']=='18'
        assert hashlib.sha256((args.data_root/labels[i]/'drill-before/ores.csv.gz').read_bytes()).hexdigest()==meta['source_exports'][i]['ore_export_sha256']
    import csv
    with (ROOT/'reports/science6-v2-catalog/material-yields.csv').open() as f:smelts={r['material']:r['directSmelting'] for r in csv.DictReader(f)}
    def group(m):
        v=smelts.get(m,'')
        if v in ['Iron','Copper','Tin']:return v+'-bearing'
        if m in ['Coal','Lignite']:return 'Coal / lignite'
        if m=='Redstone':return 'Redstone'
        return 'Other minerals'
    def color(m):return ImageColor.getrgb(PALETTE[group(m)])
    # The fixed central six-by-six chunks, with the lowest 96 underground levels.
    n=96;minx=meta['region']['min_x']+96;minz=meta['region']['min_z']+96;lo=1;hi=96;zslice=minz+48
    cubes=[[(x-minx,y-lo,z-minz,m) for x,y,z,m in rows if minx<=x<minx+n and minz<=z<minz+n and lo<=y<=hi] for props,rows in data]
    assert all(cubes)
    def font(size,bold=False):return ImageFont.truetype(str(args.font_dir/('segoeuib.ttf' if bold else 'segoeui.ttf')),size)
    def frame(title,subtitle):
        im=Image.new('RGB',(1600,1130),BG);d=ImageDraw.Draw(im)
        d.text((50,25),title,font=font(43,True),fill=TEXT)
        d.text((50,87),subtitle,font=font(24),fill=MUTED)
        d.text((50,125),f'Seed {meta["seed"]} · Central 96 × 96 × 96-block volume · X {minx}–{minx+95}, Z {minz}–{minz+95}, Y 1–96',font=font(23),fill=MUTED)
        for i,name in enumerate(['BEFORE · Stock GTNH','AFTER · Classic Mining Science 6']):
            d.text((50+i*780,178),name,font=font(30,True),fill=TEXT)
        return im
    def finish(im,footer,filename,counts):
        d=ImageDraw.Draw(im)
        for i,value in enumerate(counts):d.text((50+i*780,939),value,font=font(24),fill=TEXT)
        for i,(name,col) in enumerate(PALETTE.items()):
            x=50+(i%3)*515;y=986+(i//3)*34
            d.rectangle((x,y+5,x+19,y+24),fill=col);d.text((x+31,y),name,font=font(22),fill=TEXT)
        d.text((50,1063),footer,font=font(21),fill=MUTED)
        d.text((50,1094),'Actual exported full GT ore blocks; small ores omitted. One fixed sample, not a rendered game screenshot or balance guarantee.',font=font(20),fill=MUTED)
        im.save(out/filename)
    im=frame('Top-down view: the same central volume','Projection through Y 1–96; the highest ore in each block column supplies its color.')
    for i,rows in enumerate(cubes):
        tile=Image.new('RGB',(n,n),FIELD)
        for x,y,z,m in sorted(rows,key=lambda r:r[1]):tile.putpixel((x,z),color(m))
        im.paste(tile.resize((720,720),Image.Resampling.NEAREST),(50+i*780,215))
    finish(im,'Dark = no full ore in the projected column. Coordinates and block scale match the side and isometric views.','top-down.png',[f'{len(rows):,} ore blocks in volume · {len({(r[0],r[2]) for r in rows}):,} occupied columns' for rows in cubes])
    im=frame('Side section: an actual vertical cut',f'A single fixed plane at Z {zslice}; Y 96 at top and Y 1 at bottom. No ores projected from other Z levels.')
    sidecounts=[]
    for i,rows in enumerate(cubes):
        tile=Image.new('RGB',(n,n),FIELD);count=0
        for x,y,z,m in rows:
            if z==48:tile.putpixel((x,n-1-y),color(m));count+=1
        sidecounts.append(count);im.paste(tile.resize((720,720),Image.Resampling.NEAREST),(50+i*780,215))
    finish(im,'Dark = no full ore on this plane; it can be stone, another host rock or air. This is a section, not the entire volume.','side-section.png',[f'{v:,} full ore blocks intersect this fixed plane' for v in sidecounts])
    def isometric(rows,reverse=False):
        # Exact one-block cubes, rendered at 2x resolution and then antialiased.
        scale=2;im=Image.new('RGB',(720*scale,720*scale),FIELD);draw=ImageDraw.Draw(im)
        unit=3.35*scale
        def project(x,y,z):return (360*scale+(x-z)*math.sqrt(3)/2*unit,363*scale+((x+z)/2-y)*unit)
        points={(n-1-x,y,n-1-z):m for x,y,z,m in rows} if reverse else {(x,y,z):m for x,y,z,m in rows}
        for x,y,z in sorted(points,key=lambda pos:sum(pos)):
            m=points[(x,y,z)];rgb=color(m)
            faces=[((0,1,0),[(x,y+1,z),(x+1,y+1,z),(x+1,y+1,z+1),(x,y+1,z+1)],1.0),((1,0,0),[(x+1,y,z),(x+1,y,z+1),(x+1,y+1,z+1),(x+1,y+1,z)],.78),((0,0,1),[(x,y,z+1),(x+1,y,z+1),(x+1,y+1,z+1),(x,y+1,z+1)],.57)]
            for (dx,dy,dz),face,factor in faces:
                if (x+dx,y+dy,z+dz) in points:continue
                draw.polygon([project(*point) for point in face],fill=tuple(round(v*factor) for v in rgb))
        # Wire box keeps all three physical extents visible; host rock is intentionally hidden.
        corners=[(x,y,z) for x in [0,n] for y in [0,n] for z in [0,n]]
        for a in corners:
            for b in corners:
                if a<b and sum(u!=v for u,v in zip(a,b))==1:draw.line([project(*a),project(*b)],fill='#536d86',width=2)
        return im.resize((720,720),Image.Resampling.LANCZOS)
    for reverse,filename in [(False,'isometric.png'),(True,'isometric-reverse.png')]:
        im=frame('Isometric ore cube'+(' · opposite side' if reverse else ''),'Actual one-block ore cubes with surrounding rock hidden; nearer ore can obscure ore behind it.')
        for i,rows in enumerate(cubes):im.paste(isometric(rows,reverse),(50+i*780,215))
        finish(im,'Wireframe bounds the same 96-block cube. Shading shows voxel faces, not ore grade, rock texture or material value.',filename,[f'{len(rows):,} full ore voxels in the cube' for rows in cubes])
    result=dict(source_map_metadata='map-data.json',seed=meta['seed'],candidate_sha256=meta['candidate_sha256'],volume=dict(min_x=minx,max_x=minx+95,min_z=minz,max_z=minz+95,min_y=lo,max_y=hi,side_section_z=zslice,edge=96),full_ore_counts=[len(r) for r in cubes],section_counts=sidecounts,selection='Fixed central 6x6 chunks, Y1-96, chosen for equal cube dimensions and legibility; same crop for every view and variant.',images=['top-down.png','side-section.png','isometric.png','isometric-reverse.png'],projection='Top: highest ore per column. Side: one fixed Z plane. Isometric: opaque actual one-block voxels, host rocks hidden, occlusion retained.')
    (out/'views-data.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8')
    p=out/'README.md';text=p.read_text(encoding='utf-8')
    assert '## Multiple views' not in text
    text+='\n## Multiple views of one fixed ore cube\n\nAll four views below use the same **central 96 x 96 x 96-block** subvolume: X1120-1215, Z-1312 to -1217, Y1-96. This is a fixed central crop for readability, not a chosen jackpot. The overhead overview above still covers the whole 288 x 288-block region. The isometric images render actual one-block ore cubes with host rock omitted. Nearby ores hide distant ones; the opposite angle helps reveal that occlusion. No ore geometry is invented.\n\n'
    for title,filename in [('Top down','top-down.png'),('Side section, Z-1264','side-section.png'),('Isometric cube','isometric.png'),('Opposite isometric angle','isometric-reverse.png')]:text+='### '+title+'\n\n!['+title+' matched stock and Science6]('+filename+')\n\n'
    text+='[View metadata and counts](views-data.json). Reproduce with `python make_ore_views.py --data-root <local-Science6-reports>` after making the overview.\n'
    p.write_text(text,encoding='utf-8')
    print(json.dumps(result,indent=2))

if __name__=='__main__':main()
