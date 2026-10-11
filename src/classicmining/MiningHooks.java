package classicmining;

import java.lang.reflect.*;
import java.io.*;
import java.util.*;
import static classicmining.Reflection.*;

public final class MiningHooks {
    public static boolean enabled = true;
    public static int deposits = 32;
    public static long generatedChunks, placedBlocks;
    private static Bridge bridge;
    private static final Map<String,List<Layer>> CATALOGS = new HashMap<>();
    private static final Set<Object> VERIFIED_WORLDS = Collections.newSetFromMap(new WeakHashMap<Object,Boolean>());
    private static final boolean AUDIT=Boolean.getBoolean("classicmining.qa.audit");
    private static final Map<Object,Map<Long,String>> AUDITED=new WeakHashMap<>();
    private static long position(int x,int y,int z) {return ((long)x&0x3ffffffL)<<34|((long)z&0x3ffffffL)<<8|(y&255);}
    public static boolean wasPlaced(Object world,int x,int y,int z,String material) {
        Map<Long,String> positions=AUDITED.get(world);
        return positions!=null && material.equals(positions.get(position(x,y,z)));
    }
    public static final class Layer {
        public final Object raw;
        public final String name;
        public final int weight, minY, maxY, nativeSize, nativeDensity;
        public final double nativeMass;
        public final Object[] materials;
        public final String[] smeltingOutputs;
        public final boolean mica;
        public final GeologyModel.Family family;
        public final boolean progressionCritical;
        Layer(Object raw, String dimension) {
            this.raw=raw;
            Class<?> c=raw.getClass();
            name=(String) call(method(c,0,"getName"),raw);
            weight=((Number) call(method(c,0,"getWeight"),raw)).intValue();
            nativeSize=((Number)field(raw,"mSize")).intValue();
            nativeDensity=((Number)field(raw,"mDensity")).intValue();
            nativeMass=NativeMassModel.expectedBlocks(nativeSize,nativeDensity);
            minY=((Number) call(method(c,1,"getMinY"),raw,dimension)).intValue();
            maxY=((Number) call(method(c,1,"getMaxY"),raw,dimension)).intValue();
            materials=new Object[]{field(raw,"mSporadic"),field(raw,"mBetween"),field(raw,"mSecondary"),field(raw,"mPrimary")};
            smeltingOutputs=new String[4];
            for(int i=0;i<4;i++) {
                Object gt=call(method(materials[i].getClass(),0,"getGTMaterial"),materials[i]);
                Object output=gt==null?null:field(gt,"mDirectSmelting");
                smeltingOutputs[i]=output==null?"":bridge().material(output);
            }
            boolean found=false;for(Object mat:materials)if("Mica".equals(bridge().material(mat)))found=true;mica=found;
            String[] names=new String[4];for(int i=0;i<4;i++)names[i]=bridge().material(materials[i]);
            family=GeologyModel.family(names);
            progressionCritical=MassAllocation.progressionCritical(nativeSize,names);
        }
        public double[] roleWeights(boolean large,String dimension) {
            double[] w=large?new double[]{4,16,38,42}:new double[]{1,2,2,2};
            for(int i=0;i<4;i++)w[i]*=BalanceProfile.factor(dimension,bridge().material(materials[i]));
            return w;
        }
        public double balance(String dimension) {
            // 74% of planned blocks are in small finds, 26% in seams/jackpots (tier-size expectation).
            double[] small=roleWeights(false,dimension),large=roleWeights(true,dimension);
            double a=0,b=0;for(double w:small)a+=w;for(double w:large)b+=w;
            return .74*a/7+.26*b/100;
        }
        public boolean mixedSmall(Random rng) {
            // Modest associated finds spread access to byproducts without adding ore blocks.
            return rng.nextInt(100)<(mica?75:50);
        }
        public double[] bulkWeights(String dimension) {
            double[] w=roleWeights(true,dimension);
            for(int i=0;i<4;i++)w[i]*=BulkProfile.factor(smeltingOutputs[i]);
            return w;
        }
        public double bulkFactor(String dimension) {
            double a=0,b=0;for(double w:roleWeights(true,dimension))a+=w;for(double w:bulkWeights(dimension))b+=w;
            return b/a;
        }
    }
    public static final class Bridge {
        public final Class<?> worldClass=type("net.minecraft.world.World");
        public final Class<?> dimensionClass=type("galacticgreg.api.enums.DimensionDef");
        public final Method seed=method(worldClass,0,"getSeed","func_72905_C");
        public final Method height=method(worldClass,0,"getActualHeight","func_72940_L");
        public final Method dimension=method(dimensionClass,1,"getDimensionName");
        public final Method effective=method(dimensionClass,3,"getEffectiveDefForChunk");
        public final Method set=method(worldClass,6,"setBlock","func_147465_d");
        public final Method oreBlock=method(type("gregtech.common.ores.OreManager"),7,"getOreBlockForWorldGen");
        public final Method original=method(type("gregtech.common.GTWorldgenerator"),2,"isOreChunk");
        private Method blockGetter,metaGetter;
        final Method materialName=method(type("gregtech.api.interfaces.IOreMaterial"),0,"getInternalName");
        public String dim(Object world) { return (String)call(dimension,null,world); }
        public String material(Object material) { return (String)call(materialName,material); }
        public boolean place(Object world, int x, int y, int z, Object material) {
            Object bm=call(oreBlock,null,world,x,y,z,null,material,false);
            if (bm==null) return false;
            if(blockGetter==null) {blockGetter=method(bm.getClass(),0,"getBlock");metaGetter=method(bm.getClass(),0,"getBlockMeta");}
            Object block=call(blockGetter,bm);
            Object meta=call(metaGetter,bm);
            return (Boolean) call(set,world,x,y,z,block,meta,2);
        }
    }
    public static Bridge bridge() { if (bridge==null) bridge=new Bridge(); return bridge; }
    public static List<Layer> catalog(String dimension) {
        if (CATALOGS.containsKey(dimension)) return CATALOGS.get(dimension);
        Class<?> qc=type("gregtech.common.worldgen.WorldgenQuery");
        Object query=call(method(qc,0,"veins"),null);
        // Explicit String overload: WorldgenQuery also overloads inDimension for World and enum definitions.
        try { call(qc.getMethod("inDimension",String.class),query,dimension); }
        catch (NoSuchMethodException e) { throw new IllegalStateException(e); }
        Method matches=method(qc,1,"matches");
        List<Layer> result=new ArrayList<>();
        for (Object raw : (List<?>)field(query,"list")) {
            if ((Boolean)call(matches,query,raw)) {
                Layer layer=new Layer(raw,dimension);
                if (layer.weight>0) result.add(layer);
            }
        }
        // Stable names make geometry independent of other mods' registration order.
        Collections.sort(result, new Comparator<Layer>() { public int compare(Layer a,Layer b) { return a.name.compareTo(b.name); }});
        CATALOGS.put(dimension,result);
        System.out.println("[ClassicMining] Eligible GT layers in " + dimension + ": " + result.size());
        return result;
    }
    public static boolean useOriginalGrid(int x,int z,Object container) {
        if (enabled) return false;
        return (Boolean)call(bridge().original,null,x,z);
    }
    public static void generate(Object container) {
        if (!enabled) return;
        Object world=field(container,"mWorld");
        verifyWorldSettings(world);
        int cx=((Number)field(container,"mX")).intValue(), cz=((Number)field(container,"mZ")).intValue();
        Bridge b=bridge();
        String dim=b.dim(world);
        List<Layer> layers=catalog(dim);
        if (layers.isEmpty()) return;
        long seed=((Number)call(b.seed,world)).longValue();
        int height=((Number)call(b.height,world)).intValue();
        Object currentDef=call(b.effective,null,world,cx,cz);
        if(currentDef==null) return;
        String currentName=(String)call(method(currentDef.getClass(),0,"getDimensionName"),currentDef);
        boolean respectHeight=(Boolean)call(method(currentDef.getClass(),0,"respectsOreVeinHeights"),currentDef);
        // Regenerate adjacent owners mathematically, then write only this disjoint +8 population window.
        // This is the same window GT uses for stone generation, so later stone passes cannot erase our ores.
        for (int ox=cx-1;ox<=cx+1;ox++) for (int oz=cz-1;oz<=cz+1;oz++) {
            Object def=call(b.effective,null,world,ox,oz);
            if (def==null || !(Boolean)call(method(def.getClass(),0,"generatesOre"),def)) continue;
            String ownerDim=(String)call(method(def.getClass(),0,"getDimensionName"),def);
            // End asteroids use their own generator; do not let island deposits bleed into them.
            if (!ownerDim.equals(currentName)) continue;
            int chance=((Number)call(method(def.getClass(),0,"getOreVeinChance"),def)).intValue();
            Random rng=new Random(DepositPlanner.seed(seed,dim,ox,oz));
            double[] weights=new double[layers.size()],mass=new double[layers.size()];boolean[] critical=new boolean[layers.size()];double total=0;
            for(int l=0;l<layers.size();l++) {
                Layer layer=layers.get(l);
                double access=("Overworld".equals(dim)&&layer.mica)?3.0:1.0;
                weights[l]=layer.weight*layer.balance(dim)*access*DepositPlanner.preference(seed,dim,b.material(layer.materials[3]),ox,oz);
                mass[l]=layer.nativeMass;critical[l]=layer.progressionCritical;
            }
            if(!"Overworld".equals(dim))weights=MassAllocation.allocate(weights,mass,critical);
            for(double w:weights)total+=w;
            double[] bulkWeights=null;double bonus=0;
            if("Overworld".equals(dim)) {
                double compactProbability=0;double[] grade=new double[layers.size()];
                for(int l=0;l<layers.size();l++) {
                    Layer layer=layers.get(l);grade[l]=layer.bulkFactor(dim);
                    if(BulkProfile.compact(b.material(layer.materials[3]),dim,layer.mica)) {
                        double tin=0,all=0;double[] small=layer.roleWeights(false,dim);
                        for(int role=0;role<4;role++){all+=small[role];if("Tin".equals(layer.smeltingOutputs[role]))tin+=small[role];}
                        // Half these ordinary finds are mixed; the other half have one sampled role.
                        compactProbability+=weights[l]/total*BulkProfile.compactOdds(tin/all);
                    }
                }
                bulkWeights=MassAllocation.allocate(weights,grade,critical);
                bonus=BulkProfile.industrialBonus(compactProbability);
            }
            int attempts=(int)Math.round((deposits/2+rng.nextInt(deposits+1))*DepositPlanner.richness(seed,dim,ox,oz));
            // The End deliberately ignores ordinary ore depth bands in GTNH. Keep all its eligible ores reachable.
            // More attempts compensate for an island occupying only a small fraction of the build height.
            if(!respectHeight) attempts*=2;
            for (int i=0;i<attempts;i++) {
                double roll=rng.nextDouble()*total;
                Layer chosen=layers.get(layers.size()-1);
                for (int l=0;l<layers.size();l++) { roll-=weights[l]; if (roll<0) { chosen=layers.get(l); break; } }
                int materialRole=DepositPlanner.pickRole(rng,chosen.roleWeights(false,dim));
                Object material=chosen.materials[materialRole];
                int lo=respectHeight?Math.max(1,chosen.minY):1, hi=respectHeight?Math.min(height-1,chosen.maxY):height-1;
                if (hi<lo) continue;
                int x=ox*16+8+rng.nextInt(16), y=respectHeight?DepositPlanner.depth(rng,lo,hi):lo+rng.nextInt(hi-lo+1), z=oz*16+8+rng.nextInt(16);
                boolean allowed=rng.nextInt(100)<chance;
                int target=GeologyModel.targetSize(rng,"Overworld".equals(dim)&&chosen.mica);
                List<int[]> cluster=GeologyModel.body(rng,x,y,z,lo,hi,target,chosen.family);
                boolean large=cluster.size()>=28;
                int[] roles=(large||chosen.mixedSmall(rng))?DepositPlanner.coherentRoles(cluster,chosen.roleWeights(large,dim)):null;
                // Consume the complete algorithm5 stream first. Independent correction streams
                // preserve every ordinary non-tin planned find and all planetary plans exactly.
                if(bulkWeights!=null && large) {
                    Random bulk=new Random(DepositPlanner.seed(seed^0x42554c4b36L,dim,ox,oz)^((long)i*0x9e3779b97f4a7c15L));
                    double select=bulk.nextDouble();int index=layers.size()-1;
                    for(int l=0;l<bulkWeights.length;l++){select-=bulkWeights[l];if(select<0){index=l;break;}}
                    chosen=layers.get(index);
                    lo=Math.max(1,chosen.minY);hi=Math.min(height-1,chosen.maxY);
                    if(hi<lo)continue;
                    y=DepositPlanner.depth(bulk,lo,hi);
                    int bulkTarget=target;
                    if(target>=1200)bulkTarget+=(int)Math.floor(bonus)+(bulk.nextDouble()<bonus-Math.floor(bonus)?1:0);
                    cluster=GeologyModel.body(bulk,x,y,z,lo,hi,bulkTarget,chosen.family);
                    roles=DepositPlanner.coherentRoles(cluster,chosen.bulkWeights(dim));
                } else if(bulkWeights!=null && target>0 && target<28 && BulkProfile.compact(b.material(chosen.materials[3]),dim,chosen.mica) && (roles!=null || "Tin".equals(chosen.smeltingOutputs[materialRole]))) {
                    Random compact=new Random(DepositPlanner.seed(seed^0x54494e36L,dim,ox,oz)^((long)i*0x9e3779b97f4a7c15L));
                    cluster=GeologyModel.body(compact,x,y,z,lo,hi,6+compact.nextInt(3),chosen.family);
                    if(roles!=null)roles=DepositPlanner.coherentRoles(cluster,chosen.roleWeights(false,dim));
                }
                for (int index=0;index<cluster.size();index++) {
                    int[] p=cluster.get(index);
                    // Large discoveries contain the GT layer's related minerals, chiefly its primary/secondary ores.
                    // Rare byproducts remain a minority instead of turning one hit into hundreds of rare ore blocks.
                    Object actual=roles!=null?chosen.materials[roles[index]]:material;
                    if (allowed && DepositPlanner.populationOwner(p[0])==cx && DepositPlanner.populationOwner(p[2])==cz && b.place(world,p[0],p[1],p[2],actual)) {
                        placedBlocks++;
                        if(AUDIT) {
                            Map<Long,String> positions=AUDITED.get(world);
                            if(positions==null) {positions=new HashMap<>();AUDITED.put(world,positions);}
                            positions.put(position(p[0],p[1],p[2]),b.material(actual));
                        }
                    }
                }
            }
        }
        generatedChunks++;
    }
    private static void verifyWorldSettings(Object world) {
        if(VERIFIED_WORLDS.contains(world)) return;
        Object handler=call(method(world.getClass(),0,"getSaveHandler","func_72860_G"),world);
        File dir=(File)call(method(handler.getClass(),0,"getWorldDirectory","func_75765_b"),handler);
        File marker=new File(dir,"classic-mining-world.properties");
        Properties props=new Properties();
        try {
            if(marker.isFile()) {
                try(InputStream in=new FileInputStream(marker)){props.load(in);}
                if(!"6".equals(props.getProperty("algorithm")) || !BulkProfile.PROFILE.equals(props.getProperty("bulkProfile")) || !Integer.toString(deposits).equals(props.getProperty("depositsPerChunk")))
                    throw new IllegalStateException("Classic Mining settings changed for an existing world. Restore its original algorithm and depositsPerChunk.");
            } else {
                long time=((Number)call(method(world.getClass(),0,"getTotalWorldTime","func_82737_E"),world)).longValue();
                if(time>0) throw new IllegalStateException("Classic Mining requires a fresh world. This existing world has no Classic Mining marker.");
                props.setProperty("algorithm","6");props.setProperty("bulkProfile",BulkProfile.PROFILE);props.setProperty("depositsPerChunk",Integer.toString(deposits));
                try(OutputStream out=new FileOutputStream(marker)){props.store(out,"Keep this world on its original Classic Mining generation settings.");}
            }
        } catch(IOException e){throw new IllegalStateException("Cannot persist Classic Mining world settings",e);}
        VERIFIED_WORLDS.add(world);
    }
}
