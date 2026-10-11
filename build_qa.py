from pathlib import Path
import subprocess, zipfile, os
from build import ROOT,JDK,cp,BASE

classes=ROOT/'build/qa-classes'
classes.mkdir(parents=True,exist_ok=True)
subprocess.run([str(JDK/'javac.exe'),'--release','8','-cp',cp+';'+str(ROOT/'build/classic-mining-0.1.0.jar')+';'+str(BASE/'reference/horizonqa-0.15.0.jar'),'-d',str(classes),*[str(p) for p in (ROOT/'qa-src').rglob('*.java')]],check=True)
jar=ROOT/'build/classic-mining-qa-0.1.0.jar'
staged=jar.with_suffix('.jar.partial')
with zipfile.ZipFile(staged,'w',zipfile.ZIP_DEFLATED) as z:
    for f in classes.rglob('*.class'): z.write(f,f.relative_to(classes).as_posix())
os.replace(staged,jar)
print(jar)
