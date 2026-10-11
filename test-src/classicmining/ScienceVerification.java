package classicmining;

import java.util.*;
import java.io.*;

public final class ScienceVerification {
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static String key(int[] p){return p[0]+","+p[1]+","+p[2];}
    public static void main(String[] args)throws Exception {
        File out=new File(args[0]);out.mkdirs();
        int[] bins=new int[5];long total=0,micaTotal=0;
        Random a=new Random(768124),b=new Random(981237);
        for(int i=0;i<1000000;i++) {
            int n=GeologyModel.targetSize(a,false);total+=n;
            micaTotal+=GeologyModel.targetSize(b,true);
            bins[n==0?0:n<28?1:n<160?2:n<1200?3:4]++;
        }
        double mean=total/1000000.0;
        check(Math.abs(mean-GeologyModel.expectedSize(false))<.2,"Mean mass conserved within Monte Carlo tolerance");
        check(Math.abs(micaTotal/1000000.0-GeologyModel.expectedSize(true))<.2,"Mica budget conserved");
        check(Math.abs(bins[1]/1000000.0-.94)<.002,"Ordinary discovery floor retained");
        check(bins[4]>260&&bins[4]<420,"Industrial frequency matches rare target");
        System.out.println("Science planner million draws: mean="+mean+" expected="+GeologyModel.expectedSize(false)+" tiers="+Arrays.toString(bins));
        try(PrintWriter csv=new PrintWriter(new File(out,"geometry.csv"))) {
            csv.println("family,trial,x,y,z,role");
            for(GeologyModel.Family family:GeologyModel.Family.values())for(int trial=0;trial<32;trial++) {
                int lo=trial%2==0?20:29,hi=trial%2==0?80:32,count=trial%3==0?2400:1200;
                List<int[]> body=GeologyModel.body(new Random(1000+trial),-9,30,-9,lo,hi,count,family);
                List<int[]> again=GeologyModel.body(new Random(1000+trial),-9,30,-9,lo,hi,count,family);
                check(body.size()==Math.min(count,625*(Math.min(hi,42)-Math.max(lo,18)+1)),"Clipped capacity and exact target");
                Set<String> visited=new HashSet<>();
                int[] roles=DepositPlanner.coherentRoles(body,new double[]{4,16,38,42});
                for(int i=0;i<body.size();i++) {
                    int[] p=body.get(i);check(Arrays.equals(p,again.get(i)),"Deterministic body");
                    check(p[1]>=lo&&p[1]<=hi,"Native depth band");
                    check(Math.abs(p[0]+9)<=12&&Math.abs(p[2]+9)<=12,"Adjacent population-owner bound");
                    if(i>0){boolean connected=false;for(int axis=0;axis<3;axis++)for(int sign:new int[]{-1,1}){int[] q=p.clone();q[axis]+=sign;if(visited.contains(key(q)))connected=true;}check(connected,"Every new block face-connected");}
                    check(visited.add(key(p)),"Unique body");
                    if(trial==0)csv.println(family+","+trial+","+p[0]+","+p[1]+","+p[2]+","+roles[i]);
                }
            }
        }
        double naq=NativeMassModel.expectedBlocks(32,4),neut=NativeMassModel.expectedBlocks(16,2);
        check(naq>neut*2,"Native large Naquadah prior exceeds smaller Neutronium prior");
        check(NativeMassModel.expectedBlocks(24,4)>NativeMassModel.expectedBlocks(24,2),"Native density changes mass");
        check(NativeMassModel.expectedBlocks(1,1)>0,"Tiny native vein remains eligible");
        check(GeologyModel.family(new String[]{"Mica","Kyanite"})==GeologyModel.Family.LODE,"Mica classification");
        Random allocation=new Random(821);
        for(int trial=0;trial<10000;trial++) {
            int n=1+allocation.nextInt(40);double[] base=new double[n],prior=new double[n];boolean[] important=new boolean[n];double sum=0;
            for(int i=0;i<n;i++){base[i]=1+allocation.nextInt(120);prior[i]=.01+allocation.nextDouble()*10000;important[i]=allocation.nextInt(8)==0;sum+=base[i];}
            double[] p=MassAllocation.allocate(base,prior,important);double norm=0;
            for(int i=0;i<n;i++){double original=base[i]/sum;check(p[i]>0,"Every eligible layer stays positive");check(p[i]+1e-12>=(important[i]?1:.5)*original,"Discovery floor");check(p[i]<=2*original+1e-12,"No unlimited supply-odds boost");norm+=p[i];}
            check(Math.abs(norm-1)<1e-10,"Constrained probabilities sum to one");
        }
        check(MassAllocation.progressionCritical(1,new String[]{"BArTiMaEuSNeK"}),"Tiny progression vein protected");
        try(PrintWriter p=new PrintWriter(new File(out,"planner.properties"))){p.println("draws=1000000");p.println("meanBlocks="+mean);p.println("expectedBlocks="+GeologyModel.expectedSize(false));p.println("ordinary="+bins[1]);p.println("industrial="+bins[4]);p.println("naquadahMassPrior="+naq);p.println("neutroniumMassPrior="+neut);}
        System.out.println("PASS 256 industrial bodies, determinism/connectivity/depth/ownership and native mass prior: Naquadah="+naq+", Neutronium="+neut);
    }
}
