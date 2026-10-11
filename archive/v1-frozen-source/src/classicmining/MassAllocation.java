package classicmining;

/** Bounded mass-prior allocation, with independent discovery safeguards. */
public final class MassAllocation {
    private MassAllocation() {}
    public static double[] allocate(double[] baseline,double[] mass,boolean[] critical) {
        int n=baseline.length;
        if(n==0 || n!=mass.length || n!=critical.length)throw new IllegalArgumentException("Allocation lengths");
        double total=0,prior=0;
        for(int i=0;i<n;i++){if(baseline[i]<=0 || mass[i]<=0)throw new IllegalArgumentException("Positive layer priors");total+=baseline[i];prior+=baseline[i]*mass[i];}
        double[] base=new double[n],raw=new double[n];
        for(int i=0;i<n;i++){base[i]=baseline[i]/total;raw[i]=baseline[i]*mass[i]/prior;}
        double low=0,high=1;
        while(sum(base,raw,critical,high)<1)high*=2;
        for(int pass=0;pass<80;pass++){double mid=(low+high)/2;if(sum(base,raw,critical,mid)<1)low=mid;else high=mid;}
        double[] result=new double[n];
        for(int i=0;i<n;i++)result[i]=bounded(base[i],raw[i],critical[i],(low+high)/2);
        return result;
    }
    private static double bounded(double base,double raw,boolean critical,double scale) {
        // Native tiny or known progression-critical layers retain their old conditional discovery odds.
        // Others can fall by at most half; no layer is raised more than twice its old odds.
        return Math.min(2*base,Math.max((critical?1:.5)*base,scale*raw));
    }
    private static double sum(double[] base,double[] raw,boolean[] critical,double scale) {
        double total=0;for(int i=0;i<base.length;i++)total+=bounded(base[i],raw[i],critical[i],scale);return total;
    }
    public static boolean progressionCritical(int nativeSize,String[] materials) {
        if(nativeSize<=4)return true;
        for(String m:materials)if("MysteriousCrystal".equals(m)||"Honeaite".equals(m)||"BArTiMaEuSNeK".equals(m))return true;
        return false;
    }
}
