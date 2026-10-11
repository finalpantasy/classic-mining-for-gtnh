package classicminingqa;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.*;
import cpw.mods.fml.common.Mod;
import classicmining.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.zip.GZIPOutputStream;
import static classicmining.Reflection.*;

@Mod(modid="classicminingqa",name="Classic Mining QA",version="0.1.0",dependencies="required-after:classicmining;required-after:horizonqa")
@GameTestHolder("classicminingqa")
public class MiningWorldTests {
    static File output() { File dir=new File(System.getProperty("classicmining.qa.output","qa-output")); dir.mkdirs(); return dir; }
    @GameTest(timeoutTicks=5000,batch="classicmining_world")
    public static void actualTerrain(final GameTestHelper helper) throws Exception {
        final Object world=call(method(helper.getClass(),0,"getWorld"),helper);
        final Method chunk=method(world.getClass(),2,"getChunkFromChunkCoords","func_72964_e");
        final int size=Integer.getInteger("classicmining.qa.size",24);
        final int minX=Integer.getInteger("classicmining.qa.cx",64), minZ=Integer.getInteger("classicmining.qa.cz",-88);
        final Object loader=type("com.gtnewhorizons.horizonqa.HorizonQAMod").getField("CHUNK_LOADER").get(null);
        if(Boolean.getBoolean("classicmining.qa.reverse")) {
            Object provider=call(method(world.getClass(),0,"getChunkProvider","func_72863_F"),world);
            Method load=method(provider.getClass(),2,"loadChunk","func_73158_c");
            for(int cx=minX+size+2;cx>=minX-3;cx--) for(int cz=minZ+size+2;cz>=minZ-3;cz--) call(load,provider,cx,cz);
        }
        call(method(loader.getClass(),7,"forceChunksStrict"),loader,world,(minX-3)*16,0,(minZ-3)*16,(minX+size+3)*16-1,255,(minZ+size+3)*16-1);
        helper.afterTest(new Runnable(){public void run(){call(method(loader.getClass(),0,"releaseAll"),loader);}});
        final int[] wait={0};
        helper.onEachTick("generate then inspect real chunks",new Runnable(){public void run(){
            // Let queued GregTech worldgen finish before reading persisted block state.
            if(wait[0]++<50) return;
            try {
                int count=scan(world,chunk,minX,minZ,size);
                helper.assertTrue(count>1000,"Expected real naturally generated GT ores; found "+count);
                if(MiningHooks.enabled) helper.assertTrue(MiningHooks.generatedChunks>size*size,"Custom hook must actually execute in the live server");
                helper.succeed();
            } catch(Exception e) { throw new IllegalStateException(e); }
        }});
    }
    static int scan(Object world,Method getChunk,int minX,int minZ,int size) throws Exception {
        return scan(world,getChunk,minX,minZ,size,output());
    }
    static int scan(Object world,Method getChunk,int minX,int minZ,int size,File folder) throws Exception {
        folder.mkdirs();
        Method info=method(type("gregtech.common.ores.OreManager"),2,"getOreInfo");
        Method block=null, metadata=null;
        IdentityHashMap<Object,Map<Integer,String>> cache=new IdentityHashMap<>();
        long seed=((Number)call(MiningHooks.bridge().seed,world)).longValue();
        String dim=MiningHooks.bridge().dim(world);
        int count=0;
        Field airField;
        try {airField=type("net.minecraft.init.Blocks").getField("air");}
        catch(NoSuchFieldException e) {airField=type("net.minecraft.init.Blocks").getField("field_150350_a");}
        Object air=airField.get(null);
        BitSet terrainSolid=new BitSet(size*size*65536);
        try(PrintWriter csv=new PrintWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(new File(folder,"ores.csv.gz"))),"UTF-8"))) {
            csv.println("x,y,z,material,small,custom,blockClass");
            for(int cx=minX;cx<minX+size;cx++) for(int cz=minZ;cz<minZ+size;cz++) {
                Object c=call(getChunk,world,cx,cz);
                if(block==null) { block=method(c.getClass(),3,"getBlock","func_150810_a"); metadata=method(c.getClass(),3,"getBlockMetadata","func_76628_c"); }
                helperPopulated(c);
                for(int x=0;x<16;x++) for(int z=0;z<16;z++) for(int y=0;y<256;y++) {
                    Object b=call(block,c,x,y,z);
                    if(b!=air) terrainSolid.set(((cx-minX)*size+(cz-minZ))*65536+(x*16+z)*256+y);
                    int meta=((Number)call(metadata,c,x,y,z)).intValue();
                    Map<Integer,String> byMeta=cache.get(b);
                    if(byMeta==null) {byMeta=new HashMap<>(); cache.put(b,byMeta);}
                    String ore;
                    if(byMeta.containsKey(meta)) ore=byMeta.get(meta);
                    else {
                        Object oi=call(info,null,b,meta); ore=null;
                        if(oi!=null) {
                            if((Boolean)field(oi,"isNatural")) ore=MiningHooks.bridge().material(field(oi,"material"))+","+field(oi,"isSmall");
                            call(method(oi.getClass(),0,"close"),oi);
                        }
                        byMeta.put(meta,ore);
                    }
                    if(ore!=null) {
                        int wx=cx*16+x,wz=cz*16+z;
                        String material=ore.substring(0,ore.indexOf(','));
                        csv.println(wx+","+y+","+wz+","+ore+","+MiningHooks.wasPlaced(world,wx,y,wz,material)+","+b.getClass().getName());count++;
                    }
                }
            }
        }
        try(OutputStream terrain=new GZIPOutputStream(new FileOutputStream(new File(folder,"terrain-solid.bin.gz")))) {
            terrain.write(terrainSolid.toByteArray());
        }
        try(PrintWriter p=new PrintWriter(new File(folder,"world.properties"))) {
            p.println("seed="+seed); p.println("dimension="+dim); p.println("custom="+MiningHooks.enabled);
            p.println("minChunkX="+minX);p.println("minChunkZ="+minZ);p.println("size="+size);p.println("ores="+count);
            p.println("customGeneratedChunks="+MiningHooks.generatedChunks);p.println("customPlacedBlocks="+MiningHooks.placedBlocks);
        }
        System.out.println("[ClassicMiningQA] Exported "+count+" actual ores in "+(size*size)+" chunks.");
        return count;
    }
    static void helperPopulated(Object chunk) throws Exception {
        Field f;
        try {f=chunk.getClass().getField("isTerrainPopulated");}
        catch(NoSuchFieldException e) {f=chunk.getClass().getField("field_76646_k");}
        if(!(Boolean)f.get(chunk)) throw new IllegalStateException("Sample chunk was not populated");
    }
    @GameTest(timeoutTicks=1000,batch="classicmining_00dimensions")
    public static void actualNetherMoonAndEnd(final GameTestHelper helper) throws Exception {
        final int[] ids={-1,28,1};
        final String[] names={"nether","moon","end"};
        final Object[] worlds=new Object[3];
        final Object loader=type("com.gtnewhorizons.horizonqa.HorizonQAMod").getField("CHUNK_LOADER").get(null);
        for(int i=0;i<3;i++) {
            worlds[i]=call(method(type("net.minecraftforge.common.DimensionManager"),1,"getWorld"),null,ids[i]);
            helper.assertTrue(worlds[i]!=null,"Missing required test dimension "+names[i]);
            call(method(loader.getClass(),7,"forceChunksStrict"),loader,worlds[i],-48,0,-48,143,255,143);
        }
        final int[] wait={0};
        helper.onEachTick("read naturally generated dimensional ores",new Runnable(){public void run(){
            if(wait[0]++<50)return;
            try {
                for(int i=0;i<3;i++) {
                    Method chunk=method(worlds[i].getClass(),2,"getChunkFromChunkCoords","func_72964_e");
                    int count=scan(worlds[i],chunk,0,0,6,new File(output(),names[i]));
                    helper.assertTrue(count>0,"No natural GT ores found in "+names[i]);
                }
                helper.succeed();
            }catch(Exception e){throw new IllegalStateException(e);}
        }});
    }
    @GameTest(timeoutTicks=200,batch="classicmining_catalog")
    public static void everyRegisteredLayerAndMaterial(GameTestHelper helper) throws Exception {
        Object[] worlds=(Object[])call(method(type("net.minecraftforge.common.DimensionManager"),0,"getWorlds"),null);
        Object loader=type("com.gtnewhorizons.horizonqa.HorizonQAMod").getField("CHUNK_LOADER").get(null);
        Method oreInfo=method(type("gregtech.common.ores.OreManager"),4,"getOreInfo");
        Method stack=method(type("gregtech.common.ores.OreManager"),2,"getStack");
        Method mine=method(type("gregtech.common.ores.OreManager"),9,"mineBlock");
        Method drillOre=method(type("gregtech.api.util.GTUtility"),2,"isOre");
        Method worldBlock=method(MiningHooks.bridge().worldClass,3,"getBlock","func_147439_a");
        Method worldMeta=method(MiningHooks.bridge().worldClass,3,"getBlockMetadata","func_72805_g");
        Set<String> covered=new TreeSet<>();
        Map<String,String> yields=new TreeMap<>();
        int placed=0;
        try(PrintWriter p=new PrintWriter(new File(output(),"catalog.csv"))) {
            p.println("dimension,layer,weight,minY,maxY,material,host,placementPassed,itemPassed,miningDropPassed,drillDetectionPassed,nativeSize,nativeDensity,nativeMass,progressionCritical,family");
            for(Object world:worlds) {
                String dim=MiningHooks.bridge().dim(world);
                Object def=call(method(MiningHooks.bridge().dimensionClass,1,"getDefForWorld"),null,world);
                if(def==null || !(Boolean)call(method(def.getClass(),0,"generatesOre"),def)) continue;
                List<MiningHooks.Layer> layers=MiningHooks.catalog(dim);
                if(layers.isEmpty()) continue;
                covered.add(dim);
                call(method(loader.getClass(),7,"forceChunksStrict"),loader,world,-16,0,-16,31,255,31);
                Object host=null;
                for(Object stone:type("gregtech.api.enums.StoneType").getEnumConstants()) {
                    if(!(Boolean)call(method(stone.getClass(),0,"isEnabled"),stone)) continue;
                    if(!(Boolean)call(method(stone.getClass(),1,"canGenerateInWorld"),stone,world)) continue;
                    if(!"Stone".equals(call(method(stone.getClass(),0,"getCategory"),stone).toString())) continue;
                    String desired=dim.equals("Nether")?"Netherrack":dim.equals("The End")?"Endstone":dim;
                    if(host==null || stone.toString().equalsIgnoreCase(desired)) host=stone;
                }
                Method findHost=method(type("gregtech.api.enums.StoneType"),4,"findStoneType");
                for(int y=255;y>0;y--) {
                    Object natural=call(findHost,null,world,8,y,8);
                    if(natural!=null && "Stone".equals(call(method(natural.getClass(),0,"getCategory"),natural).toString())) {host=natural;break;}
                }
                helper.assertTrue(host!=null,"No valid host rock for "+dim);
                Object bm=call(method(host.getClass(),0,"getStone"),host);
                Object block=call(method(bm.getClass(),0,"getBlock"),bm);
                Object meta=call(method(bm.getClass(),0,"getBlockMeta"),bm);
                for(MiningHooks.Layer layer:layers) {
                helper.assertTrue(layer.maxY>=layer.minY,"Invalid depth range "+layer.name);
                for(Object mat:layer.materials) {
                    String materialName=MiningHooks.bridge().material(mat);
                    if(!yields.containsKey(materialName)) {
                        Object gt=call(method(mat.getClass(),0,"getGTMaterial"),mat);
                        String details=",,0,0,,";
                        if(gt!=null)details=","+MiningHooks.bridge().material(field(gt,"mDirectSmelting"))+","+field(gt,"mSmeltingMultiplier")+","+field(gt,"mOreMultiplier")+","+MiningHooks.bridge().material(field(gt,"mOreReplacement"))+","+MiningHooks.bridge().material(field(gt,"mMacerateInto"));
                        yields.put(materialName,materialName+details);
                    }
                    int y=Math.max(1,Math.min(254,layer.minY));
                    int x=(placed%8)*2, z=((placed/8)%8)*2;
                    call(MiningHooks.bridge().set,world,x,y,z,block,meta,2);
                    boolean ok=MiningHooks.bridge().place(world,x,y,z,mat);
                    helper.assertTrue(ok,"GT cannot place full ore "+MiningHooks.bridge().material(mat)+" from "+layer.name+" in stone");
                    Object oi=call(oreInfo,null,world,x,y,z);
                    helper.assertTrue(oi!=null,"Placed ore cannot be read back");
                    helper.assertTrue(field(oi,"material")==mat && (Boolean)field(oi,"isNatural") && !(Boolean)field(oi,"isSmall"),"Ore metadata does not match requested full natural ore");
                    helper.assertTrue(call(stack,null,oi,1)!=null,"Ore has no usable item stack: "+MiningHooks.bridge().material(mat));
                    helper.assertTrue((Boolean)call(drillOre,null,call(worldBlock,world,x,y,z),call(worldMeta,world,x,y,z)),"GT ore drill cannot recognize "+MiningHooks.bridge().material(mat));
                    call(method(oi.getClass(),0,"close"),oi);
                    List<?> drops=(List<?>)call(mine,null,new Random(17),world,x,y,z,true,0,true,false);
                    helper.assertTrue(drops!=null && !drops.isEmpty(),"Ore drilling API returned no drops: "+MiningHooks.bridge().material(mat));
                    p.println(dim+","+layer.name+","+layer.weight+","+layer.minY+","+layer.maxY+","+MiningHooks.bridge().material(mat)+","+host+","+ok+",true,true,true,"+layer.nativeSize+","+layer.nativeDensity+","+layer.nativeMass+","+layer.progressionCritical+","+layer.family);
                    placed++;
                }
                }
            }
        }
        try(PrintWriter p=new PrintWriter(new File(output(),"material-yields.csv"))) {
            p.println("material,directSmelting,smeltingMultiplier,oreMultiplier,oreReplacement,macerateInto");
            for(String row:yields.values())p.println(row);
        }
        try(PrintWriter p=new PrintWriter(new File(output(),"dimension-registry.csv"))) {
            p.println("dimension,planetOres,asteroids,eligibleLayers,fixtureCovered");
            for(Object entry:MiningHooks.bridge().dimensionClass.getEnumConstants()) {
                Object def=field(entry,"modDimensionDef");
                String dim=(String)call(method(def.getClass(),0,"getDimensionName"),def);
                boolean planet=(Boolean)call(method(def.getClass(),0,"generatesOre"),def);
                boolean asteroids=(Boolean)call(method(def.getClass(),0,"generatesAsteroids"),def);
                int layers=MiningHooks.catalog(dim).size();
                helper.assertTrue(!planet || layers==0 || covered.contains(dim),"Eligible planet dimension omitted from fixtures: "+dim);
                p.println(dim+","+planet+","+asteroids+","+layers+","+covered.contains(dim));
            }
        }
        helper.assertTrue(placed>100,"Ore catalog unexpectedly empty");
        try(PrintWriter p=new PrintWriter(new File(output(),"covered-dimensions.txt"))){for(String dim:covered)p.println(dim);}
        System.out.println("[ClassicMiningQA] Catalog and real ore-block placement checks passed: "+placed+" across "+covered.size()+" dimensions");
        helper.succeed();
    }
    @GameTest(timeoutTicks=5000,batch="classicmining_01planets")
    public static void allRegisteredPlanetTerrain(final GameTestHelper helper) throws Exception {
        Object[] worlds=(Object[])call(method(type("net.minecraftforge.common.DimensionManager"),0,"getWorlds"),null);
        final List<Object> planets=new ArrayList<>();
        Object loader=type("com.gtnewhorizons.horizonqa.HorizonQAMod").getField("CHUNK_LOADER").get(null);
        for(Object world:worlds) {
            String dim=MiningHooks.bridge().dim(world);
            Object def=call(method(MiningHooks.bridge().dimensionClass,1,"getDefForWorld"),null,world);
            if(def==null || !(Boolean)call(method(def.getClass(),0,"generatesOre"),def) || MiningHooks.catalog(dim).isEmpty())continue;
            planets.add(world);
            int cx=dim.equals("The End")?0:20;
            int size=criticalSize(dim);
            call(method(loader.getClass(),7,"forceChunksStrict"),loader,world,(cx-1)*16,0,(cx-1)*16,(cx+size+1)*16-1,255,(cx+size+1)*16-1);
        }
        final int[] wait={0};
        helper.onEachTick("scan natural terrain in every eligible planet",new Runnable(){public void run(){
            if(wait[0]++<50)return;
            try(PrintWriter p=new PrintWriter(new File(output(),"natural-planets.csv"))) {
                p.println("dimension,chunks,naturalOreBlocks");
                for(Object world:planets) {
                    String dim=MiningHooks.bridge().dim(world);
                    int cx=dim.equals("The End")?0:20;
                    int size=criticalSize(dim);
                    File folder=new File(output(),"planets/"+dim);
                    int ores=scan(world,method(world.getClass(),2,"getChunkFromChunkCoords","func_72964_e"),cx,cx,size,folder);
                    helper.assertTrue(ores>0,"No natural ores in eligible planet dimension "+dim);
                    String critical=dim.equals("neper")?"MysteriousCrystal":dim.equals("ross128ba")?"BArTiMaEuSNeK":dim.equals("dimensionDarkWorld")?"Honeaite":null;
                    if(MiningHooks.enabled && critical!=null) {
                        boolean found=false;
                        try(BufferedReader read=new BufferedReader(new InputStreamReader(new java.util.zip.GZIPInputStream(new FileInputStream(new File(folder,"ores.csv.gz"))),"UTF-8"))) {
                            String line;while((line=read.readLine())!=null){String[] cells=line.split(",");if(cells.length>5 && cells[3].equals(critical) && cells[5].equals("true"))found=true;}
                        }
                        helper.assertTrue(found,"Progression-critical material not naturally encountered in targeted sample: "+critical);
                    }
                    p.println(dim+","+(size*size)+","+ores);
                }
                helper.assertTrue(planets.size()==38,"Expected all 38 enabled ore-generating planets");
                helper.succeed();
            }catch(Exception e){throw new IllegalStateException(e);}
        }});
    }
    static int criticalSize(String dim) {return dim.equals("neper")||dim.equals("ross128ba")||dim.equals("dimensionDarkWorld")?8:2;}
}
