from pathlib import Path
import subprocess, zipfile, hashlib, json, os

ROOT = Path(__file__).resolve().parent
JDK = Path('F:/GTNH/build-tools/jdk-25.0.4.1+1/bin')
BASE = ROOT.parent/'classic-mining'
cp = ';'.join(str(p) for p in [BASE/'qa-server/lwjgl3ify-forgePatches.jar', BASE/'qa-server/forge-1.7.10-10.13.4.1614-1.7.10-universal.jar'])
def main():
    classes=ROOT/'build/classes'
    classes.mkdir(parents=True,exist_ok=True)
    sources=sorted((ROOT/'src').rglob('*.java'))
    subprocess.run([str(JDK/'javac.exe'),'--release','8','-cp',cp,'-d',str(classes),*[str(s) for s in sources]],check=True)
    jar=ROOT/'build/classic-mining-0.1.0.jar'
    manifest='Manifest-Version: 1.0\nFMLCorePlugin: classicmining.CorePlugin\nFMLCorePluginContainsFMLMod: true\n\n'
    entries={'META-INF/MANIFEST.MF':manifest.encode(), 'mcmod.info':json.dumps([dict(modid='classicmining',name='Classic Mining for GTNH',version='0.1.0',mcversion='1.7.10',description='Small randomized full ores using GTNH material, dimension and depth definitions.')]).encode()}
    entries.update({f.relative_to(classes).as_posix():f.read_bytes() for f in classes.rglob('*.class')})
    staged=jar.with_suffix('.jar.partial')
    with zipfile.ZipFile(staged,'w') as z:
        for name,data in sorted(entries.items()):
            info=zipfile.ZipInfo(name,(1980,1,1,0,0,0));info.compress_type=zipfile.ZIP_DEFLATED;z.writestr(info,data)
    os.replace(staged,jar)
    print(jar)
    print('SHA256',hashlib.sha256(jar.read_bytes()).hexdigest())
if __name__=='__main__': main()
