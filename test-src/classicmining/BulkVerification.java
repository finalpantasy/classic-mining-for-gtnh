package classicmining;
import java.util.*;
public final class BulkVerification {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static void main(String[] args) {
        for(double p:new double[]{0,.01,.05,.1,.25,.5,1}) {
            double ordinaryLoss=p*.94*4;
            check(Math.abs(BulkProfile.industrialBonus(p)/3000-ordinaryLoss)<1e-12,"Expected planned mass must balance");
            int target=2400+(int)Math.ceil(BulkProfile.industrialBonus(p));
            List<int[]> body=GeologyModel.body(new Random(913),0,30,0,1,100,target,GeologyModel.Family.BED);
            check(body.size()==Math.min(target,15625),"Industrial compensation stays in bounded connected geometry");
        }
        check(BulkProfile.compact("CassiteriteSand","Overworld",false),"Tin stringer compaction");
        check(!BulkProfile.compact("CassiteriteSand","Mars",false),"No planetary compaction");
        check(!BulkProfile.compact("Tin","Overworld",true),"Existing mica ordinary budget retained");
        check(BulkProfile.factor("Iron")>1&&BulkProfile.factor("Copper")>1&&BulkProfile.factor("Tin")<1,"Measured bulk correction direction");
        check(BulkProfile.factor("Mica")==1&&BulkProfile.factor("Neutronium")==1,"Unrelated resource factors remain native");
        Random rng=new Random(49121);int compact=0;
        for(int i=0;i<100000;i++){int role=DepositPlanner.pickRole(rng,new double[]{1,2,2,2.4});boolean mixed=rng.nextInt(100)<50;if(mixed||role==3)compact++;}
        check(Math.abs(compact/100000.0-BulkProfile.compactOdds(2.4/7.4))<.005,"Budget uses actual mixed-or-tin occurrence odds, preserving pure gangue finds");
        System.out.println("PASS bulk compensation identity and bounded geometry for seven discovery mixes; planetary/mica compaction exclusions");
    }
}
