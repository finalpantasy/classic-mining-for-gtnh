from pathlib import Path
import subprocess
ROOT=Path(__file__).resolve().parent
JDK=Path('F:/GTNH/build-tools/jdk-25.0.4.1+1/bin')
classes=ROOT/'build/test-classes'
classes.mkdir(parents=True,exist_ok=True)
BASE=ROOT.parent/'classic-mining'
cp=';'.join(str(p) for p in [ROOT/'build/classic-mining-0.1.0.jar',BASE/'qa-server/lwjgl3ify-forgePatches.jar',classes])
subprocess.run([str(JDK/'javac.exe'),'--release','8','-cp',cp,'-d',str(classes),*[str(p) for p in (ROOT/'test-src').rglob('*.java')]],check=True)
gt=next((BASE/'qa-server/mods').glob('gregtech-*.jar'))
subprocess.run([str(JDK/'java.exe'),'-cp',cp,'classicmining.Verification',str(gt)],check=True)
subprocess.run([str(JDK/'java.exe'),'-cp',cp,'classicmining.ScienceVerification',str(ROOT/'reports/science6-planner')],check=True)
subprocess.run([str(JDK/'java.exe'),'-cp',cp,'classicmining.BulkVerification'],check=True)
