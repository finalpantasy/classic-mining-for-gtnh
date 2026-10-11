package classicmining;

/** Experimental, Overworld-only bulk supply correction. Native smelting output defines resource families. */
public final class BulkProfile {
    public static final String PROFILE="bulk-20261010-v2";
    private BulkProfile() {}
    public static double factor(String smeltingOutput) {
        if("Iron".equals(smeltingOutput))return 1.8;
        if("Copper".equals(smeltingOutput))return 2.4;
        if("Tin".equals(smeltingOutput))return .45;
        return 1;
    }
    public static boolean compact(String primary,String dimension,boolean mica) {
        return "Overworld".equals(dimension) && !mica && ("Tin".equals(primary)||"Cassiterite".equals(primary)||"CassiteriteSand".equals(primary));
    }
    public static double industrialBonus(double compactProbability) {
        if(compactProbability<0 || compactProbability>1)throw new IllegalArgumentException("Probability");
        // E ordinary size 11 -> 7, p=.94. Industrial probability 1/3000.
        return compactProbability*4*.94*3000;
    }
    public static double compactOdds(double tinRoleProbability) {
        if(tinRoleProbability<0 || tinRoleProbability>1)throw new IllegalArgumentException("Role probability");
        return .5+.5*tinRoleProbability;
    }
}
