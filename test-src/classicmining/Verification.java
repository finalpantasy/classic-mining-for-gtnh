package classicmining;

import java.util.*;
import java.util.jar.*;
import java.io.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

public final class Verification {
    static void check(boolean ok,String message) { if(!ok) throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        int checks=0;
        int seams=0,jackpots=0;
        for(int trial=0;trial<20000;trial++) {
            int cx=trial%101-50,cz=trial/101-99;
            long seed=DepositPlanner.seed(92837465L,"Overworld",cx,cz);
            Random a=new Random(seed), b=new Random(seed);
            int x=cx*16+8+a.nextInt(16),z=cz*16+8+a.nextInt(16);
            b.nextInt(16);b.nextInt(16);
            int lo=trial%40+1,hi=lo+(trial%2==0?1:90),y=lo+a.nextInt(hi-lo+1);
            b.nextInt(hi-lo+1);
            List<int[]> blocks=DepositPlanner.cluster(a,x,y,z,lo,hi);
            List<int[]> again=DepositPlanner.cluster(b,x,y,z,lo,hi);
            check(blocks.size()>=6 && blocks.size()<=320,"Deposit size");
            if(blocks.size()>=160)jackpots++;else if(blocks.size()>=28)seams++;
            Set<String> occupied=new HashSet<>();
            for(int i=0;i<blocks.size();i++) {
                int[] p=blocks.get(i);
                check(Arrays.equals(p,again.get(i)),"Determinism");
                check(p[1]>=lo && p[1]<=hi,"Depth restriction");
                check(Math.abs(p[0]-x)+Math.abs(p[1]-y)+Math.abs(p[2]-z)<=12,"Owner radius bound");
                check(Math.abs(DepositPlanner.populationOwner(p[0])-cx)<=1 && Math.abs(DepositPlanner.populationOwner(p[2])-cz)<=1,"Only adjacent owners required, including negatives");
                check(occupied.add(Arrays.toString(p)),"Unique positions");
                if(i>0) {
                    boolean connected=false;
                    for(int j=0;j<i;j++) {int[] q=blocks.get(j); if(Math.abs(p[0]-q[0])+Math.abs(p[1]-q[1])+Math.abs(p[2]-q[2])==1) connected=true;}
                    check(connected,"Face-connected geometry");
                }
                checks+=6;
            }
        }
        check(seams>900 && seams<1300 && jackpots>65 && jackpots<135,"Expected uncommon seams and rare jackpots");
        System.out.println("Deposit distribution across 20000 plans: seams="+seams+" jackpots="+jackpots);
        Random depth=new Random(781);
        int high=0;
        for(int i=0;i<20000;i++) {
            int y=DepositPlanner.depth(depth,60,220);check(y>=60 && y<=220,"High ore stays inside its depth band");
            if(y>=120)high++;
            double richness=DepositPlanner.richness(42,"Overworld",i-10000,-i);
            check(richness>=.85 && richness<=1.15,"Regional richness stays bounded");
        }
        check(high>0 && high<6000,"High-altitude ores retain a nonzero mountain tail while favoring underground placement");
        long same=0,edges=0;
        for(int trial=0;trial<1000;trial++) {
            Random rng=new Random(trial);
            List<int[]> blocks;
            do{blocks=DepositPlanner.cluster(rng,0,30,0,1,80);}while(blocks.size()<28);
            int[] roles=DepositPlanner.coherentRoles(blocks,new double[]{4,16,38,42});
            int[] counts=new int[4];for(int role:roles)counts[role]++;
            for(int role=0;role<4;role++)check(Math.abs(counts[role]-blocks.size()*new double[]{.04,.16,.38,.42}[role])<=1,"Coherent mix preserves quotas");
            check(Arrays.equals(roles,DepositPlanner.coherentRoles(blocks,new double[]{4,16,38,42})),"Coherent mix deterministic");
            for(int i=0;i<blocks.size();i++)for(int j=0;j<i;j++){
                int[] a=blocks.get(i),b=blocks.get(j);
                if(Math.abs(a[0]-b[0])+Math.abs(a[1]-b[1])+Math.abs(a[2]-b[2])==1){edges++;if(roles[i]==roles[j])same++;}
            }
            for(String family:new String[]{"Tin","Copper","MysteriousCrystal"}){
                double bias=DepositPlanner.preference(42,"Overworld",family,trial-500,0);
                check(bias>=.7 && bias<=1.3,"Background chance always positive and geology bounded");
                double next=DepositPlanner.preference(42,"Overworld",family,trial-499,0);
                check(Math.abs(bias-next)<.057,"Smooth regional transitions");
            }
        }
        check(same/(double)edges>.70,"Large deposits have coherent mineral sections");
        for(int trial=0;trial<1000;trial++) {
            int count=DepositPlanner.cluster(new Random(trial),0,30,0,20,40,true).size();
            check((count>=6 && count<=8)||(count>=28 && count<=64)||(count>=160 && count<=320),"Compact progression finds preserve seam/jackpot sizes");
        }
        for(String material:new String[]{"Tin","Cassiterite","Mica","Diamond","Iron","Mystery"}) {
            check(BalanceProfile.factor("moon",material)==1,"Uncalibrated planet supply unchanged");
            check(BalanceProfile.factor("Overworld",material)>=.8,"No extreme resource suppression");
        }
        System.out.println("Same-role neighboring blocks in large deposits: "+same+"/"+edges);
        try(JarFile gt=new JarFile(args[0])) {
            int variants=0;
            for(Enumeration<JarEntry> it=gt.entries();it.hasMoreElements();) {
                JarEntry e=it.nextElement();
                if(!e.getName().endsWith("gregtech/common/GTWorldgenerator$WorldGenContainer.class")) continue;
                byte[] original;
                try(InputStream in=gt.getInputStream(e);ByteArrayOutputStream out=new ByteArrayOutputStream()) {byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);original=out.toByteArray();}
                byte[] patched=new OreTransformer().transform("gregtech.common.GTWorldgenerator$WorldGenContainer","gregtech.common.GTWorldgenerator$WorldGenContainer",original);
                ClassNode node=new ClassNode();new ClassReader(patched).accept(node,0);
                for(MethodNode m:node.methods) if(m.name.equals("run")) new Analyzer<BasicValue>(new BasicVerifier()).analyze(node.name,m);
                variants++; System.out.println("Verified transformed ASM bytecode: "+e.getName());
            }
            check(variants==2,"Expected base and Java 17 GregTech class variants");
        }
        try {new OreTransformer().transform("x","gregtech.common.GTWorldgenerator$WorldGenContainer",new byte[]{1,2,3});throw new AssertionError("Must reject incompatible classes");}
        catch(IllegalArgumentException|ArrayIndexOutOfBoundsException expected) {}
        System.out.println("PASS geometry and compatibility checks: "+checks+", 20000 deposits, base and Java 17 bytecode");
    }
}
