"""Scan public paths/content and release archives before publication."""
from pathlib import Path
import argparse, subprocess, zipfile
p=argparse.ArgumentParser()
p.add_argument('--forbidden',action='append',required=True)
p.add_argument('--asset',action='append',default=[])
a=p.parse_args();root=Path(__file__).resolve().parent
needles=[s.lower().encode() for s in a.forbidden];violations=[]
def check(label,data):
    if any(n in label.lower().encode()+data.lower() for n in needles):violations.append(label)
for name in subprocess.check_output(['git','ls-files','-z'],cwd=root).decode().split('\0'):
    if name and (root/name).is_file():check(name,(root/name).read_bytes())
for asset in a.asset:
    f=Path(asset);check(f.name,f.read_bytes())
    if zipfile.is_zipfile(f):
        with zipfile.ZipFile(f) as z:
            for name in z.namelist():check(f.name+':'+name,z.read(name))
if violations:raise SystemExit('Privacy scan failed: '+', '.join(violations))
print('Public privacy scan passed')
