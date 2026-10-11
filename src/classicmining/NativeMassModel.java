package classicmining;

import java.util.*;

/** Empty-host prior from GT's nine layer passes; terrain success is measured separately. */
public final class NativeMassModel {
    private static final Map<String,Double> CACHE = new HashMap<>();
    private NativeMassModel() {}
    public static synchronized double expectedBlocks(int size,int density) {
        if(size<1 || density<1) throw new IllegalArgumentException("Positive native size/density required");
        String key=size+":"+density;
        if(CACHE.containsKey(key))return CACHE.get(key);
        Random rng=new Random(0x4d415353L);
        double sum=0;
        // Reproducible 128-boundary Monte Carlo integration, summing every cell in each footprint.
        // Native margins are U[0,size-1]; no real-world tonnage is inferred from game blocks.
        for(int trial=0;trial<128;trial++) {
            int west=-rng.nextInt(size),east=16+rng.nextInt(size);
            int north=-rng.nextInt(size),south=16+rng.nextInt(size);
            for(int x=west;x<east;x++)for(int z=north;z<south;z++) {
                // Native population windows start at chunk origin +2, not at the block's own chunk.
                int dx=Math.floorDiv(x-2,16),dz=Math.floorDiv(z-2,16);
                int local=Math.max(1,density/(int)Math.sqrt(2+dx*dx+dz*dz));
                int a=Math.max(1,Math.max(Math.abs(west-x),Math.abs(east-x))/local);
                int b=Math.max(1,Math.max(Math.abs(north-z),Math.abs(south-z))/local);
                double p=1.0/a+1.0/b-1.0/(a*(double)b),q=1-p;
                // Six single-role passes + three two-role passes, then 1/7 sporadic in failures.
                sum+=9*p+3*p*q+(6*q+3*q*q)*p/7;
            }
        }
        double mean=sum/128;
        CACHE.put(key,mean);
        return mean;
    }
}
