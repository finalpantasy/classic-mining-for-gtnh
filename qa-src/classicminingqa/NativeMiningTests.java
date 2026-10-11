package classicminingqa;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.*;
import classicmining.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import static classicmining.Reflection.*;

@GameTestHolder("classicminingqa")
public class NativeMiningTests {
    static Field findField(Object o,String... names) {
        for(String n:names) for(Class<?> c=o.getClass();c!=null;c=c.getSuperclass()) try {
            Field f=c.getDeclaredField(n);f.setAccessible(true);return f;
        } catch(NoSuchFieldException e) {}
        throw new IllegalStateException("Missing field "+Arrays.toString(names)+" on "+o.getClass());
    }
    static Object get(Object o,String... n) { try{return findField(o,n).get(o);}catch(Exception e){throw new IllegalStateException(e);} }
    static void set(Object o,String n,Object value) {try{findField(o,n).set(o,value);}catch(Exception e){throw new IllegalStateException(e);} }
    static Object entry(String n) throws Exception { return type("gregtech.api.enums.ItemList").getField(n).get(null); }
    static Object stack(String n) throws Exception { Object e=entry(n);return call(method(e.getClass(),2,"get"),e,1L,new Object[0]); }
    static Object direction(String n) throws Exception {return type("net.minecraftforge.common.util.ForgeDirection").getField(n).get(null);}
    static Object machine(Object world,int x,int y,int z,String name) throws Exception {
        Object block=type("gregtech.api.GregTechAPI").getField("sBlockMachines").get(null);
        call(method(world.getClass(),6,"setBlock","func_147465_d"),world,x,y,z,block,0,3);
        Object tile=call(method(world.getClass(),3,"getTileEntity","func_147438_o"),world,x,y,z);
        Object item=stack(name);int id=((Number)call(method(item.getClass(),0,"getItemDamage","func_77960_j"),item)).intValue();
        call(method(tile.getClass(),2,"setInitialValuesAsNBT"),tile,null,(short)id);
        call(method(tile.getClass(),1,"setFrontFacing"),tile,direction("NORTH"));
        call(method(tile.getClass(),1,"setOwnerName"),tile,"ClassicMiningQA");
        call(method(tile.getClass(),1,"setOwnerUuid"),tile,UUID.fromString("845c3de0-8534-4f7a-bae2-ef6a690a0040"));
        return tile;
    }
    static void force(Object world,GameTestHelper h,int cx,int cz,int size) throws Exception {
        final Object loader=type("com.gtnewhorizons.horizonqa.HorizonQAMod").getField("CHUNK_LOADER").get(null);
        call(method(loader.getClass(),7,"forceChunksStrict"),loader,world,(cx-3)*16,0,(cz-3)*16,(cx+size+3)*16-1,255,(cz+size+3)*16-1);
        h.afterTest(new Runnable(){public void run(){call(method(loader.getClass(),0,"releaseAll"),loader);}});
    }
    static String identity(Object item) {
        Object data=call(method(type("gregtech.api.util.GTOreDictUnificator"),1,"getItemData"),null,item);
        if(data!=null) return get(data,"mPrefix")+":"+MiningHooks.bridge().material(get(get(data,"mMaterial"),"mMaterial"));
        return String.valueOf(call(method(item.getClass(),0,"getUnlocalizedName","func_77977_a"),item));
    }
    static int size(Object s) {return s==null?0:((Number)get(s,"stackSize","field_77994_a")).intValue();}
    static void collect(Object inventory,Map<String,Long> counts) {
        int n=((Number)call(method(inventory.getClass(),0,"getSizeInventory","func_70302_i_"),inventory)).intValue();
        for(int i=0;i<n;i++) {
            Object s=call(method(inventory.getClass(),1,"getStackInSlot","func_70301_a"),inventory,i);
            if(s!=null){String key=identity(s);counts.put(key,counts.getOrDefault(key,0L)+size(s));call(method(inventory.getClass(),2,"setInventorySlotContents","func_70299_a"),inventory,i,null);}
        }
    }
    static void table(File file,Map<String,Long> counts) throws Exception {
        try(PrintWriter p=new PrintWriter(file)){p.println("item,count");for(Map.Entry<String,Long> e:counts.entrySet())p.println(e.getKey()+","+e.getValue());}
    }
    @GameTest(timeoutTicks=6000,batch="classicmining_native_drill")
    public static void automatedOreDrill(final GameTestHelper helper) throws Exception {
        Object chosen=call(method(helper.getClass(),0,"getWorld"),helper);
        String planet=System.getProperty("classicmining.qa.nativePlanet","Overworld");
        if(!planet.equalsIgnoreCase("Overworld")){
            chosen=null;Object[] worlds=(Object[])call(method(type("net.minecraftforge.common.DimensionManager"),0,"getWorlds"),null);
            for(Object candidate:worlds)if(MiningHooks.bridge().dim(candidate).equalsIgnoreCase(planet))chosen=candidate;
            if(chosen==null)throw new IllegalStateException("Requested native test dimension unavailable: "+planet);
        }
        final Object world=chosen;
        final int tier=Integer.getInteger("classicmining.qa.drillTier",1), radius=tier==1?3:9,size=radius*2;
        final int cx=64,cz=-88,x=(cx+radius)*16+4,z=(cz+radius)*16+4,y=100;
        force(world,helper,cx,cz,size);
        final int[] wait={0}; final Drill[] drill={null};
        helper.onEachTick("native formed drill through actual terrain",new Runnable(){public void run(){try{
            if(wait[0]++<50)return;
            if(drill[0]==null) {
                File before=new File(MiningWorldTests.output(),"drill-before");
                MiningWorldTests.scan(world,method(world.getClass(),2,"getChunkFromChunkCoords","func_72964_e"),cx,cz,size,before);
                drill[0]=new Drill(world,helper,x,y,z,tier);
            }
            for(int b=0;b<(tier==1?10:100);b++) {
                drill[0].advance(tier==1?500:50);
                if(String.valueOf(get(drill[0].controller,"workState")).equals("UPWARD") || String.valueOf(call(method(type("gregtech.common.tileentities.machines.multi.MTEDrillerBase"),0,"getShutdownReason"),drill[0].controller)).equals("GT5U.gui.text.drill_exhausted")) {
                    drill[0].finish(cx,cz,size,true);helper.succeed();return;
                }
                if(drill[0].ticks>=6000000)throw new IllegalStateException("Drill tick budget exceeded: "+drill[0].status());
            }
        }catch(Exception e){throw new IllegalStateException(e);}}});
    }
    static class Drill {
        Object world,controller,tile,energy,input,output,pipeBus,warp;
        long ticks=0,energyUsed=0,fluidAdded=0,fluidUsed=0; boolean powerProbed=false; int maintenanceRepairs=0;Map<String,Long> drops=new TreeMap<>();
        int tier;File folder=MiningWorldTests.output();
        Drill(Object w,GameTestHelper h,int x,int y,int z,int t) throws Exception {
            world=w;tier=t;String voltage=t==1?"MV":"IV";
            // Clear only the fixture's above-ground envelope, leave ore scan volume untouched.
            for(int dx=-2;dx<=2;dx++)for(int dz=-1;dz<=3;dz++)for(int dy=0;dy<8;dy++)call(method(w.getClass(),3,"setBlockToAir","func_147468_f"),w,x+dx,y+dy,z+dz);
            tile=machine(w,x,y,z,"OreDrill"+t);controller=call(method(tile.getClass(),0,"getMetaTileEntity"),tile);
            call(method(controller.getClass(),2,"construct"),controller,stack("OreDrill"+t),false);
            energy=machine(w,x-1,y,z,"Hatch_Energy_"+voltage);
            input=machine(w,x+1,y,z,"Hatch_Input_"+voltage);
            output=machine(w,x-1,y,z+1,"Hatch_Output_Bus_"+voltage);
            pipeBus=machine(w,x+1,y,z+1,"Hatch_Input_Bus_"+voltage);
            machine(w,x-1,y,z+2,"Hatch_Maintenance");
            call(method(type("gregtech.api.metatileentity.implementations.MTEMultiBlockBase"),0,"fixAllIssues"),controller);
            call(method(type("gregtech.api.metatileentity.implementations.MTEMultiBlockBase"),2,"checkStructure"),controller,true,tile);
            h.assertTrue((Boolean)get(controller,"mMachine"),"Native ore drill structure must form");
            // Same startup-check handling as HorizonQA Multiblock.assertFormed, no formation override.
            set(controller,"mStartUpCheck",-1);
            Class<?> wc=type("net.minecraft.world.WorldServer");
            warp=type("com.gtnewhorizons.horizonqa.api.gt.GTNHGameTestHelper").getConstructor(GameTestHelper.class,wc,int.class,int.class,int.class).newInstance(h,w,x-2,y,z-1);
            call(method(warp.getClass(),1,"withWarpRange"),warp,8);
            call(method(warp.getClass(),6,"supplyEUAbsolute"),warp,x-1,y,z,t==1?128L:8192L,1L,6000000);
            System.out.println("[ClassicMiningQA] Native OreDrill"+t+" formed, area "+(t==1?36:324)+" chunks, "+voltage+" power buffer fixture.");
        }
        long stored(Object te){return ((Number)call(method(te.getClass(),0,"getStoredEU"),te)).longValue();}
        int fluid() {
            Object m=call(method(input.getClass(),0,"getMetaTileEntity"),input);
            Object f=call(method(m.getClass(),0,"getFillableStack"),m);return f==null?0:((Number)get(f,"amount")).intValue();
        }
        void advance(int count) throws Exception {
            collect(output,drops);
            long cap=((Number)call(method(energy.getClass(),0,"getEUCapacity"),energy)).longValue();
            if(ticks==0)call(method(energy.getClass(),1,"setStoredEU"),energy,cap);
            if(((Number)call(method(type("gregtech.api.metatileentity.implementations.MTEMultiBlockBase"),0,"getRepairStatus"),controller)).intValue()<6){call(method(type("gregtech.api.metatileentity.implementations.MTEMultiBlockBase"),0,"fixAllIssues"),controller);maintenanceRepairs++;}
            Object m=call(method(input.getClass(),0,"getMetaTileEntity"),input);
            int fcap=((Number)call(method(m.getClass(),0,"getCapacity"),m)).intValue(),fBefore=fluid();
            Object fluid=type("gregtech.api.enums.ItemList").getField("sDrillingFluid").get(null);
            Object fs=type("net.minecraftforge.fluids.FluidStack").getConstructor(type("net.minecraftforge.fluids.Fluid"),int.class).newInstance(fluid,fcap-fBefore);
            int added=((Number)call(method(input.getClass(),3,"fill"),input,direction("UNKNOWN"),fs,true)).intValue();fluidAdded+=added;
            Object pipes=call(method(type("gregtech.api.util.GTModHandler"),2,"getIC2Item"),null,"miningPipe",64L);
            call(method(pipeBus.getClass(),2,"setInventorySlotContents","func_70299_a"),pipeBus,0,pipes);
            call(method(warp.getClass(),1,"fastForwardTicks"),warp,count);
            ticks+=count;fluidUsed+=fBefore+added-fluid();
            if(!powerProbed && !drops.isEmpty() && ((Number)get(controller,"mEUt")).intValue()<0){
                Object dynamo=get(warp,"dynamo");List<?> jobs=(List<?>)get(dynamo,"jobs");jobs.clear();
                long before=stored(energy);call(method(warp.getClass(),1,"fastForwardTicks"),warp,4);ticks+=4;energyUsed=before-stored(energy);powerProbed=true;
                call(method(warp.getClass(),6,"supplyEUAbsolute"),warp,((Number)get(energy,"xCoord","field_145851_c")).intValue(),((Number)get(energy,"yCoord","field_145848_d")).intValue(),((Number)get(energy,"zCoord","field_145849_e")).intValue(),tier==1?128L:8192L,1L,6000000);
            }
            fluidUsed=fluidAdded-fluid();
            collect(output,drops);
            if(ticks>=50000 && drops.isEmpty())throw new IllegalStateException("Native drill stalled: "+status());
            if(ticks%100000<=(tier==1?500:50)){System.out.println("[ClassicMiningQA] "+status());write(false);}
        }
        String status(){return "OreDrill"+tier+" ticks="+ticks+" fluid="+fluidUsed+" items="+drops.values().stream().mapToLong(Long::longValue).sum()+" state="+get(controller,"workState")+" formed="+get(controller,"mMachine")+" allowed="+call(method(tile.getClass(),0,"isAllowedToWork"),tile)+" failure="+call(method(type("gregtech.api.metatileentity.implementations.MTEMultiBlockBase"),0,"getCheckRecipeResult"),controller)+" shutdown="+call(method(type("gregtech.common.tileentities.machines.multi.MTEDrillerBase"),0,"getShutdownReason"),controller)+" head="+get(controller,"yHead")+" EUt="+get(controller,"mEUt")+" progress="+get(controller,"mProgresstime")+"/"+get(controller,"mMaxProgresstime");}
        void write(boolean done)throws Exception {
            table(new File(folder,"drill-drops.csv"),drops);
            try(PrintWriter p=new PrintWriter(new File(folder,"drill.properties"))){p.println("tier="+tier);p.println("dimension="+MiningHooks.bridge().dim(world));p.println("ticks="+ticks);p.println("energyProbeEU="+energyUsed);p.println("energyProbeTicks=4");p.println("collectionIntervalTicks="+(tier==1?500:50));p.println("maintenanceRepairs="+maintenanceRepairs);p.println("fluidUsed="+fluidUsed);p.println("completed="+done);p.println("state="+get(controller,"workState"));p.println("formed="+get(controller,"mMachine"));p.println("EUt="+get(controller,"mEUt"));}
        }
        void finish(int cx,int cz,int size,boolean done)throws Exception {
            collect(output,drops);write(done);
            MiningWorldTests.scan(world,method(world.getClass(),2,"getChunkFromChunkCoords","func_72964_e"),cx,cz,size,new File(folder,"drill-after"));
            if(drops.isEmpty()||fluidUsed<2000||energyUsed<=0)throw new IllegalStateException("Drill must deliver real drops and consume resources");
            System.out.println("[ClassicMiningQA] Drill completed: "+status());
        }
    }
    @GameTest(timeoutTicks=2000,batch="classicmining_native_branch")
    public static void branchMining(final GameTestHelper helper) throws Exception {
        final Object world=call(method(helper.getClass(),0,"getWorld"),helper);
        force(world,helper,120,-88,24);final int[] wait={0};
        helper.onEachTick("survival native branch harvest",new Runnable(){public void run(){try{
            if(wait[0]++<50)return;
            Branch b=new Branch(world);
            try(PrintWriter csv=new PrintWriter(new File(MiningWorldTests.output(),"branch-routes.csv"))){
                csv.println("tool,y,route,steps,tunnelBlocks,oreBlocks,blocked,toolReplacements,estimatedDigTicks,firstIron,firstCopper,firstTin,firstMica");
                for(String tool:new String[]{"Flint","Bronze","Steel"})for(int level:new int[]{28,32,56})for(int r=0;r<4;r++)b.route(tool,level,r,csv);
            }
            table(new File(MiningWorldTests.output(),"branch-drops.csv"),b.drops);
            helper.assertTrue(b.totalOres>0,"Actual branch player must collect ores");
            System.out.println("[ClassicMiningQA] Survival native branches mined "+b.totalOres+" ore blocks; drops "+b.drops.values().stream().mapToLong(Long::longValue).sum());
            helper.succeed();
        }catch(Exception e){throw new IllegalStateException(e);}}});
    }
    static class Branch {
        Object world,player,inventory,manager;Map<String,Long> drops=new TreeMap<>();long totalOres=0;
        int tunnel,ores,blocked,replacements;double digTicks;String tool;Map<String,Integer> first;
        Branch(Object w)throws Exception {
            world=w;player=call(method(type("net.minecraftforge.common.util.FakePlayerFactory"),1,"getMinecraft"),null,w);
            inventory=get(player,"inventory","field_71071_by");manager=get(player,"theItemInWorldManager","field_71134_c");
            Object server=call(method(type("net.minecraft.server.MinecraftServer"),0,"getServer","func_71276_C"),null);
            Object network=type("net.minecraft.network.NetworkManager").getConstructor(boolean.class).newInstance(false);
            Object handler=type("net.minecraft.network.NetHandlerPlayServer").getConstructor(type("net.minecraft.server.MinecraftServer"),type("net.minecraft.network.NetworkManager"),type("net.minecraft.entity.player.EntityPlayerMP")).newInstance(server,network,player);
            findField(player,"playerNetServerHandler","field_71135_a").set(player,handler);
            Object survival=type("net.minecraft.world.WorldSettings$GameType").getField("SURVIVAL").get(null);
            call(method(manager.getClass(),1,"setGameType","func_73076_a"),manager,survival);
        }
        String ore(int x,int y,int z) {
            Object block=call(method(world.getClass(),3,"getBlock","func_147439_a"),world,x,y,z);
            int meta=((Number)call(method(world.getClass(),3,"getBlockMetadata","func_72805_g"),world,x,y,z)).intValue();
            Object oi=call(method(type("gregtech.common.ores.OreManager"),2,"getOreInfo"),null,block,meta);
            if(oi==null)return null;String mat=MiningHooks.bridge().material(get(oi,"material"));call(method(oi.getClass(),0,"close"),oi);return mat;
        }
        void equip()throws Exception {
            Map<?,?> materials=(Map<?,?>)type("tconstruct.library.TConstructRegistry").getField("toolMaterials").get(null);
            int id=-1,wood=-1;
            for(Map.Entry<?,?> e:materials.entrySet()){
                String name=String.valueOf(get(e.getValue(),"materialName"));
                if(name.equalsIgnoreCase(tool))id=((Number)e.getKey()).intValue();
                if(name.equalsIgnoreCase("Wood"))wood=((Number)e.getKey()).intValue();
            }
            if(id<0||wood<0)throw new IllegalStateException("Missing TConstruct tool material "+tool);
            Constructor<?> ctor=type("net.minecraft.item.ItemStack").getConstructor(type("net.minecraft.item.Item"),int.class,int.class);
            Object head=ctor.newInstance(type("tconstruct.tools.TinkerTools").getField("pickaxeHead").get(null),1,id);
            Object handle=ctor.newInstance(type("tconstruct.tools.TinkerTools").getField("toolRod").get(null),1,wood);
            Object binding=ctor.newInstance(type("tconstruct.tools.TinkerTools").getField("binding").get(null),1,wood);
            Object builder=type("tconstruct.library.crafting.ToolBuilder").getField("instance").get(null);
            Object pick=call(method(builder.getClass(),4,"buildTool"),builder,head,handle,binding,tool+" QA Pickaxe");
            if(pick==null)throw new IllegalStateException("Native TConstruct pickaxe creation failed");
            call(method(inventory.getClass(),2,"setInventorySlotContents","func_70299_a"),inventory,0,pick);
            findField(inventory,"currentItem","field_70461_c").set(inventory,0);
            findField(player,"onGround","field_70122_E").set(player,true);
        }
        boolean harvest(int x,int y,int z,boolean isOre)throws Exception {
            Object block=call(method(world.getClass(),3,"getBlock","func_147439_a"),world,x,y,z);
            Object material=call(method(type("net.minecraft.block.Block"),0,"getMaterial","func_149688_o"),block);
            if((Boolean)call(method(material.getClass(),0,"isLiquid","func_76224_d"),material)){blocked++;return false;}
            float hard=((Number)call(method(type("net.minecraft.block.Block"),4,"getBlockHardness","func_149712_f"),block,world,x,y,z)).floatValue();
            if(hard<0){blocked++;return false;}
            if(hard==0&&ore(x,y,z)==null)return true;
            Object held=call(method(inventory.getClass(),1,"getStackInSlot","func_70301_a"),inventory,0);
            if(held!=null){
                Object tag=call(method(held.getClass(),0,"getTagCompound","func_77978_p"),held);
                Object data=call(method(tag.getClass(),1,"getCompoundTag","func_74775_l"),tag,"InfiTool");
                boolean broken=(Boolean)call(method(data.getClass(),1,"getBoolean","func_74767_n"),data,"Broken");
                if(broken){equip();replacements++;held=call(method(inventory.getClass(),1,"getStackInSlot","func_70301_a"),inventory,0);}
            }else{equip();replacements++;held=call(method(inventory.getClass(),1,"getStackInSlot","func_70301_a"),inventory,0);}
            call(method(player.getClass(),3,"setPosition","func_70107_b"),player,(double)x,(double)y,(double)z);
            if(!(Boolean)call(method(type("net.minecraft.block.Block"),2,"canHarvestBlock"),block,player,((Number)call(method(world.getClass(),3,"getBlockMetadata","func_72805_g"),world,x,y,z)).intValue())){blocked++;if(tunnel==0)System.out.println("[ClassicMiningQA] Branch harvest gate tool="+held+" block="+block+" hardness="+hard);return false;}
            float strength=((Number)call(method(type("net.minecraft.block.Block"),5,"getPlayerRelativeBlockHardness","func_149737_a"),block,player,world,x,y,z)).floatValue();
            if(strength<=0){blocked++;System.out.println("[ClassicMiningQA] Branch zero strength tool="+held+" block="+block);return false;}
            if(!(Boolean)call(method(manager.getClass(),3,"tryHarvestBlock","func_73084_b"),manager,x,y,z)){blocked++;if(tunnel==0)System.out.println("[ClassicMiningQA] Branch manager denied tool="+held+" block="+block+" strength="+strength);return false;}
            digTicks+=Math.ceil(1.0/strength);
            if(isOre){ores++;totalOres++;}else tunnel++;
            // Collect spawned native drops immediately; do not advance hunger or entity physics.
            Object box=call(method(type("net.minecraft.util.AxisAlignedBB"),6,"getBoundingBox","func_72330_a"),null,x-4.,y-4.,z-4.,x+5.,y+5.,z+5.);
            List<?> entities=(List<?>)call(method(world.getClass(),2,"getEntitiesWithinAABB","func_72872_a"),world,type("net.minecraft.entity.item.EntityItem"),box);
            for(Object entity:entities){Object s=call(method(entity.getClass(),0,"getEntityItem","func_92059_d"),entity);String id=identity(s);drops.put(id,drops.getOrDefault(id,0L)+size(s));call(method(entity.getClass(),0,"setDead","func_70106_y"),entity);}
            return true;
        }
        void follow(int x,int y,int z,int step)throws Exception {
            ArrayDeque<int[]> queue=new ArrayDeque<>();Set<String> seen=new HashSet<>();queue.add(new int[]{x,y,z});int n=0;
            while(!queue.isEmpty()&&n<256){int[] p=queue.remove();String key=Arrays.toString(p);if(!seen.add(key))continue;
                String mat=ore(p[0],p[1],p[2]);if(mat==null)continue;
                if(!harvest(p[0],p[1],p[2],true))continue;n++;
                Object m=type("gregtech.api.enums.Materials").getField(mat).get(null);Object smelt=get(m,"mDirectSmelting");String useful=MiningHooks.bridge().material(smelt);
                if(Arrays.asList("Iron","Copper","Tin","Mica").contains(useful))first.putIfAbsent(useful,step);
                for(int[] d:new int[][]{{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}})queue.add(new int[]{p[0]+d[0],p[1]+d[1],p[2]+d[2]});
            }
        }
        void route(String t,int y,int r,PrintWriter csv)throws Exception {
            tool=t;tunnel=ores=blocked=replacements=0;digTicks=0;first=new HashMap<>();equip();
            int x0=120*16+12,z0=-88*16+16+(tool.equals("Flint")?0:tool.equals("Bronze")?120:240)+r*28;
            int steps=0;
            for(int s=0;s<256;s++){
                int x=x0+s,z=z0;boolean okay=true;
                for(int dy=0;dy<2;dy++){if(ore(x,y+dy,z)!=null)follow(x,y+dy,z,s+1);if(!harvest(x,y+dy,z,false))okay=false;}
                if(!okay)break;steps++;
                for(int[] p:new int[][]{{x-1,y,z},{x+1,y,z},{x,y-1,z},{x,y+2,z},{x,y,z-1},{x,y+1,z-1},{x,y,z+1},{x,y+1,z+1}})if(ore(p[0],p[1],p[2])!=null)follow(p[0],p[1],p[2],s+1);
            }
            csv.println(tool+","+y+","+r+","+steps+","+tunnel+","+ores+","+blocked+","+replacements+","+(long)digTicks+","+first.getOrDefault("Iron",-1)+","+first.getOrDefault("Copper",-1)+","+first.getOrDefault("Tin",-1)+","+first.getOrDefault("Mica",-1));csv.flush();
        }
    }
}
