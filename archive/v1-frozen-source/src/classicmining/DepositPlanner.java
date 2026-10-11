package classicmining;

import java.util.*;

/** Pure geometry. Each owner has an independent seed and a disjoint vanilla population window. */
public final class DepositPlanner {
    public static final int MIN_SIZE = 6, MAX_SIZE = 320, RADIUS = 12;
    private static final int[][] DIRS = {{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
    public static long seed(long worldSeed, String dimension, int cx, int cz) {
        return mix(worldSeed ^ mix(dimension.hashCode()) ^ mix(cx * 0x9e3779b97f4a7c15L) ^ mix(cz * 0x632be59bd9b4e019L));
    }
    private static long mix(long x) {
        x = (x ^ (x >>> 30)) * 0xbf58476d1ce4e5b9L;
        x = (x ^ (x >>> 27)) * 0x94d049bb133111ebL;
        return x ^ (x >>> 31);
    }
    public static int populationOwner(int block) { return Math.floorDiv(block-8,16); }
    public static int depth(Random rng,int lo,int hi) {
        int y=lo+rng.nextInt(hi-lo+1);
        // Broad, high-altitude bands otherwise waste most attempts in the sky. Preserve the full band,
        // but favor its lower, usually underground portion. Mountain-only ores still require mountains.
        if(lo>=50 && hi>=120) {
            y=Math.min(y,lo+rng.nextInt(hi-lo+1));
            y=Math.min(y,lo+rng.nextInt(hi-lo+1));
        }
        return y;
    }
    public static double richness(long worldSeed,String dim,int cx,int cz) {
        // Smooth +/-15% variation over 256 blocks. No empty regions and no ore-seed grid.
        int rx=Math.floorDiv(cx,16),rz=Math.floorDiv(cz,16);
        double tx=Math.floorMod(cx,16)/16.0,tz=Math.floorMod(cz,16)/16.0;
        tx=tx*tx*(3-2*tx);tz=tz*tz*(3-2*tz);
        double a=regional(worldSeed,dim,rx,rz)*(1-tx)+regional(worldSeed,dim,rx+1,rz)*tx;
        double b=regional(worldSeed,dim,rx,rz+1)*(1-tx)+regional(worldSeed,dim,rx+1,rz+1)*tx;
        return .85+.30*(a*(1-tz)+b*tz);
    }
    public static double preference(long worldSeed,String dim,String family,int cx,int cz) {
        // 70% uniform background plus 30% mineral-specific geology. Never excludes an eligible layer.
        double field=(richness(worldSeed^mix(family.hashCode()),dim,cx,cz)-.85)/.30;
        return .70+.60*field;
    }
    public static int pickRole(Random rng,double[] weights) {
        double total=0;for(double w:weights)total+=w;
        double roll=rng.nextDouble()*total;
        for(int i=0;i<weights.length;i++){roll-=weights[i];if(roll<0)return i;}
        return weights.length-1;
    }
    public static int[] coherentRoles(final List<int[]> blocks,double[] weights) {
        // Stratified, related minerals: secondary -> interface mineral -> primary, with a tip pocket.
        // Quotas preserve the requested material mix, rather than sprinkling independent random blocks.
        double total=0;for(double w:weights)total+=w;
        int n=blocks.size();int[] quota=new int[4];double[] remainder=new double[4];int allocated=0;
        for(int i=0;i<4;i++){double exact=n*weights[i]/total;quota[i]=(int)exact;remainder[i]=exact-quota[i];allocated+=quota[i];}
        while(allocated<n){int best=0;for(int i=1;i<4;i++)if(remainder[i]>remainder[best])best=i;quota[best]++;remainder[best]=-1;allocated++;}
        int axis=0,largest=-1;
        for(int a=0;a<3;a++){int lo=Integer.MAX_VALUE,hi=Integer.MIN_VALUE;for(int[] p:blocks){lo=Math.min(lo,p[a]);hi=Math.max(hi,p[a]);}if(hi-lo>largest){largest=hi-lo;axis=a;}}
        final int along=axis;
        List<Integer> order=new ArrayList<>();for(int i=0;i<n;i++)order.add(i);
        Collections.sort(order,new Comparator<Integer>(){public int compare(Integer a,Integer b){
            for(int k=0;k<3;k++){int d=(along+k)%3;int cmp=Integer.compare(blocks.get(a)[d],blocks.get(b)[d]);if(cmp!=0)return cmp;}return 0;
        }});
        int[] roles=new int[n];int index=0;
        for(int role:new int[]{0,2,1,3})for(int j=0;j<quota[role];j++)roles[order.get(index++)]=role;
        return roles;
    }
    private static double regional(long seed,String dim,int x,int z) {
        return new Random(seed(seed^0x57b9a271L,dim,x,z)).nextDouble();
    }
    public static List<int[]> cluster(Random rng, int x, int y, int z, int minY, int maxY) {
        return cluster(rng,x,y,z,minY,maxY,false);
    }
    public static List<int[]> cluster(Random rng, int x, int y, int z, int minY, int maxY,boolean compactFind) {
        int roll=rng.nextInt(1000);
        int count=roll<940?6+rng.nextInt(compactFind?3:11):roll<995?28+rng.nextInt(37):160+rng.nextInt(161);
        int axis=rng.nextInt(3);
        List<int[]> blocks = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        blocks.add(new int[]{x,y,z}); seen.add(x+","+y+","+z);
        for (int attempts = 0; blocks.size() < count && attempts < 20000; attempts++) {
            int[] base = blocks.get(rng.nextInt(blocks.size()));
            // An axis-biased connected growth gives irregular seams, rather than spherical blobs.
            int direction=rng.nextInt(10)<6?axis*2+rng.nextInt(2):rng.nextInt(6);
            int[] dir = DIRS[direction];
            int nx=base[0]+dir[0], ny=base[1]+dir[1], nz=base[2]+dir[2];
            if (ny < minY || ny > maxY || Math.abs(nx-x)+Math.abs(ny-y)+Math.abs(nz-z)>RADIUS) continue;
            if (seen.add(nx+","+ny+","+nz)) blocks.add(new int[]{nx,ny,nz});
        }
        if (blocks.size() != count) throw new IllegalStateException("Cluster geometry could not grow");
        return blocks;
    }
}
