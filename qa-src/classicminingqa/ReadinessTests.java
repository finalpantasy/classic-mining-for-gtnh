package classicminingqa;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.*;
import classicmining.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import static classicmining.Reflection.*;
import static classicminingqa.NativeMiningTests.*;

/** QA-only native tool lifecycle and scanner payload checks. No gameplay patch. */
@GameTestHolder("classicminingqa")
public class ReadinessTests {
    static Object vanilla(String owner,String... names) throws Exception {
        for(String name:names)try{return type(owner).getField(name).get(null);}catch(NoSuchFieldException ignored){}
        throw new IllegalStateException("Missing native field "+Arrays.toString(names));
    }
    static Object held(Branch b) { return call(method(b.inventory.getClass(),1,"getStackInSlot","func_70301_a"),b.inventory,0); }
    static Object data(Object pick) {
        Object tag=call(method(pick.getClass(),0,"getTagCompound","func_77978_p"),pick);
        return call(method(tag.getClass(),1,"getCompoundTag","func_74775_l"),tag,"InfiTool");
    }
    static long leveling(String name,Object pick) { return ((Number)call(method(type("iguanaman.iguanatweakstconstruct.leveling.LevelingLogic"),1,name),null,data(pick))).longValue(); }
    static boolean boosted(Object pick) { return (Boolean)call(method(type("iguanaman.iguanatweakstconstruct.leveling.LevelingLogic"),1,"isBoosted"),null,data(pick)); }
    static int damage(Object pick) { return ((Number)call(method(data(pick).getClass(),1,"getInteger","func_74762_e"),data(pick),"Damage")).intValue(); }
    static void repair(Branch b,GameTestHelper h) throws Exception {
        Object original=held(b);long xp=leveling("getXp",original),boost=leveling("getBoostXp",original),level=leveling("getLevel",original),harvest=leveling("getHarvestLevel",original);
        Object flint=type("net.minecraft.item.ItemStack").getConstructor(type("net.minecraft.item.Item"),int.class,int.class).newInstance(vanilla("net.minecraft.init.Items","flint","field_151145_ak"),1,0);
        Object inputs=Array.newInstance(type("net.minecraft.item.ItemStack"),1);Array.set(inputs,0,flint);
        Object builder=type("tconstruct.library.crafting.ModifyBuilder").getField("instance").get(null);
        Object result=call(method(builder.getClass(),2,"modifyItem"),builder,original,inputs);
        h.assertTrue(result!=null,"Native flint repair must be available");
        h.assertTrue(damage(result)<damage(original),"Native repair must restore durability");
        h.assertTrue(leveling("getXp",result)==xp && leveling("getBoostXp",result)==boost && leveling("getLevel",result)==level && leveling("getHarvestLevel",result)==harvest,"Repair must retain native tool progression");
        call(method(b.inventory.getClass(),2,"setInventorySlotContents","func_70299_a"),b.inventory,0,result);
    }
    @GameTest(timeoutTicks=1000,batch="classicmining_readiness")
    public static void starterToolLifecycle(GameTestHelper h) throws Exception {
        Object world=call(method(h.getClass(),0,"getWorld"),h);Branch b=new Branch(world);b.tool="Flint";b.equip();
        Object stone=vanilla("net.minecraft.init.Blocks","stone","field_150348_b");
        long initial=leveling("getHarvestLevel",held(b));int mined=0,repairs=0,boostAt=-1;
        // Actual native harvest callbacks award XP. Never inject XP, levels or durability.
        while(mined<20000 && (boostAt<0 || mined<boostAt+300)) {
            if(damage(held(b))>80){repair(b,h);repairs++;}
            call(MiningHooks.bridge().set,world,8,220,8,stone,0,2);
            h.assertTrue(b.harvest(8,220,8,false),"Flint must harvest ordinary stone while developing");mined++;
            if(boostAt<0 && boosted(held(b)))boostAt=mined;
        }
        h.assertTrue(boostAt>0 && leveling("getHarvestLevel",held(b))>initial,"Ordinary native mining must unlock flint mining boost");
        h.assertTrue(repairs>0 && b.replacements==0,"Keep a repaired tool rather than replace its progression");
        Properties props=new Properties();props.setProperty("stoneMined",String.valueOf(mined));props.setProperty("boostAtStone",String.valueOf(boostAt));props.setProperty("repairs",String.valueOf(repairs));props.setProperty("initialHarvestLevel",String.valueOf(initial));props.setProperty("boostedHarvestLevel",String.valueOf(leveling("getHarvestLevel",held(b))));props.setProperty("nativeBoostXP",String.valueOf(leveling("getBoostXp",held(b))));props.setProperty("nativeXP",String.valueOf(leveling("getXp",held(b))));props.setProperty("freshPickReplacements",String.valueOf(b.replacements));
        try(FileOutputStream out=new FileOutputStream(new File(MiningWorldTests.output(),"starter-tool.properties"))){props.store(out,"Synthetic stone fixture; native XP and repair, not survival time");}
        try(PrintWriter csv=new PrintWriter(new File(MiningWorldTests.output(),"starter-harvest-gates.csv"))){
            csv.println("tool,material,harvestLevel,canHarvest,actuallyHarvested");
            for(String tool:new String[]{"FlintBoosted","Copper","Bronze","Steel"}){
                if(!tool.equals("FlintBoosted")){b.tool=tool;b.equip();}
                for(String material:new String[]{"Tetrahedrite","Chalcopyrite","Malachite","Cassiterite","Magnetite","Pyrite","Mica"}){
                    Object mat=type("gregtech.api.enums.Materials").getField(material).get(null);
                    call(MiningHooks.bridge().set,world,8,220,8,stone,0,2);h.assertTrue(MiningHooks.bridge().place(world,8,220,8,mat),"Probe ore placement");
                    Object block=call(method(world.getClass(),3,"getBlock","func_147439_a"),world,8,220,8);int meta=((Number)call(method(world.getClass(),3,"getBlockMetadata","func_72805_g"),world,8,220,8)).intValue();
                    boolean gate=(Boolean)call(method(type("net.minecraft.block.Block"),2,"canHarvestBlock"),block,b.player,meta);
                    boolean got=gate && b.harvest(8,220,8,true);
                    h.assertTrue(gate==got,"Native gate and actual harvest must agree");
                    csv.println(tool+","+material+","+leveling("getHarvestLevel",held(b))+","+gate+","+got);
                }
            }
        }
        System.out.println("[ClassicMiningQA] Native flint boost after "+boostAt+" stone, "+repairs+" repairs, no replacements");h.succeed();
    }
    @GameTest(timeoutTicks=1000,batch="classicmining_readiness_prospecting")
    public static void nativeProspectorPayload(GameTestHelper h) throws Exception {
        Object world=call(method(h.getClass(),0,"getWorld"),h);Branch b=new Branch(world);
        Object behaviour=type("gregtech.common.items.behaviors.BehaviourProspecting").getConstructor(int.class,int.class).newInstance(1,1);
        Object stone=vanilla("net.minecraft.init.Blocks","stone","field_150348_b");
        Set<String> seen=new TreeSet<>();int cases=0;
        Object[] worlds=(Object[])call(method(type("net.minecraftforge.common.DimensionManager"),0,"getWorlds"),null);
        try(PrintWriter csv=new PrintWriter(new File(MiningWorldTests.output(),"prospector-payloads.csv"))){
            csv.println("material,directProspectorAccepted,packetIdentityPassed,roundTripPassed");
            for(Object w:worlds){
                Object def=call(method(MiningHooks.bridge().dimensionClass,1,"getDefForWorld"),null,w);
                if(def==null || !(Boolean)call(method(def.getClass(),0,"generatesOre"),def))continue;
                for(MiningHooks.Layer layer:MiningHooks.catalog(MiningHooks.bridge().dim(w)))for(Object mat:layer.materials){
                String name=MiningHooks.bridge().material(mat);if(!seen.add(name))continue;
                // Isolated API fixture: eligibility was independently checked in the 38-dimension suite.
                call(MiningHooks.bridge().set,world,8,220,8,stone,0,2);h.assertTrue(MiningHooks.bridge().place(world,8,220,8,mat),"Prospector fixture placement "+name);
                Object block=call(method(world.getClass(),3,"getBlock","func_147439_a"),world,8,220,8);int meta=((Number)call(method(world.getClass(),3,"getBlockMetadata","func_72805_g"),world,8,220,8)).intValue();
                boolean accepted=(Boolean)call(method(behaviour.getClass(),11,"onItemUseFirst"),behaviour,null,null,b.player,world,8,220,8,direction("UP"),0f,0f,0f);
                h.assertTrue(accepted,"Native direct prospector must accept ore "+name);
                Object packet=type("detrav.net.ProspectingPacket").getConstructor(int.class,int.class,int.class,int.class,int.class,int.class).newInstance(0,0,8,8,1,0);
                call(method(packet.getClass(),5,"addBlock"),packet,8,220,8,block,meta);
                String internal=String.valueOf(call(method(mat.getClass(),0,"getInternalName"),mat));
                Collection<?> names=((Map<?,?>)get(packet,"oreMaterialNames")).values();
                h.assertTrue(names.size()==1 && names.contains(internal),"Electric packet must identify "+name+" accurately");
                h.assertTrue(((Map<?,?>)get(packet,"map")).size()==1,"Electric packet must contain the ore coordinate");
                ByteArrayOutputStream bytes=new ByteArrayOutputStream();call(method(packet.getClass(),1,"encode"),packet,bytes);
                Object decoded=call(method(packet.getClass(),1,"decode"),null,new ByteArrayInputStream(bytes.toByteArray()));
                h.assertTrue(get(decoded,"map").equals(get(packet,"map")) && get(decoded,"oreMaterialNames").equals(get(packet,"oreMaterialNames")),"Serialized electric prospecting payload must survive "+name);
                csv.println(name+",true,true,true");cases++;
            }
            }
        }
        h.assertTrue(cases==256,"All 256 catalog materials must reach prospecting payloads");
        System.out.println("[ClassicMiningQA] Direct prospecting and electric payload round trip: "+cases+" materials");h.succeed();
    }
}
