package classicminingqa;
import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.*;
import java.io.*;import java.lang.reflect.*;import java.util.*;
import static classicmining.Reflection.*;
import static classicminingqa.NativeMiningTests.*;

@GameTestHolder("classicminingqa")
public class ProcessingLineTests {
    static Object ore(String prefix,String material,long count)throws Exception {
        return type("gregtech.api.util.GTOreDictUnificator").getMethod("get",type("gregtech.api.enums.OrePrefixes"),Object.class,long.class).invoke(null,type("gregtech.api.enums.OrePrefixes").getField(prefix).get(null),material,count);
    }
    static class Stage {
        Map<String,Long> counts=new TreeMap<>();Map<String,Object> representative=new HashMap<>();long ticks,probeEU;int probeTicks;
        void collect(Object tile,Object meta)throws Exception {
            int first=((Number)call(method(meta.getClass(),0,"getOutputSlot"),meta)).intValue();
            int n=Array.getLength(get(meta,"mOutputItems"));
            for(int slot=first;slot<first+n;slot++) {
                Object item=call(method(tile.getClass(),1,"getStackInSlot","func_70301_a"),tile,slot);
                if(item==null)continue;String key=identity(item);
                counts.put(key,counts.getOrDefault(key,0L)+size(item));
                representative.putIfAbsent(key,call(method(item.getClass(),0,"copy","func_77946_l"),item));
                call(method(tile.getClass(),2,"setInventorySlotContents","func_70299_a"),tile,slot,null);
            }
        }
    }
    static Stage run(Object world,GameTestHelper h,String machineName,Object input,boolean water)throws Exception {
        h.assertTrue(input!=null,"Native processing input exists");
        int x=4,y=220,z=4;
        call(method(world.getClass(),3,"setBlockToAir","func_147468_f"),world,x,y,z);
        Object tile=machine(world,x,y,z,machineName),meta=call(method(tile.getClass(),0,"getMetaTileEntity"),tile);
        int inputSlot=((Number)call(method(meta.getClass(),0,"getInputSlot"),meta)).intValue();
        call(method(tile.getClass(),2,"setInventorySlotContents","func_70299_a"),tile,inputSlot,input);
        Object warp=type("com.gtnewhorizons.horizonqa.api.gt.GTNHGameTestHelper").getConstructor(GameTestHelper.class,type("net.minecraft.world.WorldServer"),int.class,int.class,int.class).newInstance(h,world,0,220,0);
        call(method(warp.getClass(),1,"withWarpRange"),warp,8);
        long amperage=((Number)get(meta,"mAmperage")).longValue();
        h.assertTrue(((Number)get(meta,"mTier")).intValue()==2 && amperage>0,"Native MV machine tier and input amperage");
        call(method(warp.getClass(),6,"supplyEUAbsolute"),warp,x,y,z,128L,amperage,400000);
        long capacity=((Number)call(method(tile.getClass(),0,"getEUCapacity"),tile)).longValue();
        call(method(tile.getClass(),1,"setStoredEU"),tile,capacity);
        Stage result=new Stage();boolean probed=false;
        while(result.ticks<200000) {
            if(water) {
                Object fluid=type("net.minecraftforge.fluids.FluidRegistry").getMethod("getFluid",String.class).invoke(null,"water");
                Object stack=type("net.minecraftforge.fluids.FluidStack").getConstructor(type("net.minecraftforge.fluids.Fluid"),int.class).newInstance(fluid,16000);
                call(method(tile.getClass(),3,"fill"),tile,direction("UNKNOWN"),stack,true);
            }
            // Inspect startup tick by tick: a 100-tick sample can alias a 100-tick
            // macerator recipe and miss every active cycle, falsely reporting no power use.
            int step=probed?100:1;
            call(method(warp.getClass(),1,"fastForwardTicks"),warp,step);result.ticks+=step;result.collect(tile,meta);
            int progress=((Number)get(meta,"mMaxProgresstime")).intValue();
            if(!probed && progress>0) {
                ((List<?>)get(get(warp,"dynamo"),"jobs")).clear();
                long before=((Number)call(method(tile.getClass(),0,"getStoredEU"),tile)).longValue();
                call(method(warp.getClass(),1,"fastForwardTicks"),warp,4);result.ticks+=4;
                result.probeEU=before-((Number)call(method(tile.getClass(),0,"getStoredEU"),tile)).longValue();result.probeTicks=4;probed=true;
                call(method(warp.getClass(),6,"supplyEUAbsolute"),warp,x,y,z,128L,amperage,400000);result.collect(tile,meta);
            }
            Object remaining=call(method(tile.getClass(),1,"getStackInSlot","func_70301_a"),tile,inputSlot);
            if(remaining==null && ((Number)get(meta,"mMaxProgresstime")).intValue()==0)break;
        }
        h.assertTrue(result.ticks<200000 && !result.counts.isEmpty(),"Native "+machineName+" must complete actual recipes; amps="+amperage+", EUt="+get(meta,"mEUt")+", progress="+get(meta,"mMaxProgresstime")+", storedEU="+call(method(tile.getClass(),0,"getStoredEU"),tile));
        h.assertTrue(result.probeEU>0,"Native "+machineName+" must consume buffered electricity");
        return result;
    }
    static Object primary(Stage stage,String id,GameTestHelper h)throws Exception {
        Long count=stage.counts.get(id);h.assertTrue(count!=null && count>0 && count<=64,"Native primary product "+id);
        Object item=call(method(stage.representative.get(id).getClass(),0,"copy","func_77946_l"),stage.representative.get(id));
        findField(item,"stackSize","field_77994_a").set(item,count.intValue());return item;
    }
    @GameTest(timeoutTicks=2000,batch="classicmining_processing_line")
    public static void nativeMVLine(GameTestHelper h)throws Exception {
        Object world=call(method(h.getClass(),0,"getWorld"),h);
        Object furnace=call(method(type("net.minecraft.item.crafting.FurnaceRecipes"),0,"smelting","func_77602_a"),null);
        try(PrintWriter csv=new PrintWriter(new File(MiningWorldTests.output(),"native-processing-line.csv"))) {
            csv.println("material,stage,inputCount,ticks,energyProbeEU,energyProbeTicks,output,count,furnaceOutput,smeltingPotential");
            for(String mat:new String[]{"Magnetite","BrownLimonite","BandedIron","Pyrite","Chalcopyrite","Malachite","Cassiterite","CassiteriteSand"}) {
                Object input=ore("crushed",mat,32);
                String[] machines={"Machine_MV_OreWasher","Machine_MV_ThermalCentrifuge","Machine_MV_Macerator"};
                String[] prefix={"crushedPurified","crushedCentrifuged","dust"};
                for(int i=0;i<3;i++) {
                    int n=size(input);Stage s=run(world,h,machines[i],input,i==0);
                    for(Map.Entry<String,Long> output:s.counts.entrySet()) {
                        Object product=s.representative.get(output.getKey());
                        Object smelt=call(method(furnace.getClass(),1,"getSmeltingResult","func_151395_a"),furnace,product);
                        csv.println(mat+","+machines[i]+","+n+","+s.ticks+","+s.probeEU+","+s.probeTicks+","+output.getKey()+","+output.getValue()+","+(smelt==null?"noFurnaceRecipe":identity(smelt))+","+output.getValue()*size(smelt));
                    }
                    input=primary(s,prefix[i]+":"+mat,h);csv.flush();
                }
            }
        }
        System.out.println("[ClassicMiningQA] Native MV washing/thermal centrifuge/maceration: eight materials, 24 completed machine stages");h.succeed();
    }
}
