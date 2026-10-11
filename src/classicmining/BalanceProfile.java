package classicmining;

/** Bounded, sample-calibrated availability changes. Does not alter recipes or eligible dimensions. */
public final class BalanceProfile {
    private BalanceProfile() {}
    public static double factor(String dimension,String material) {
        // Calibrate only the Overworld, where two paired seeds establish a baseline.
        // Unmeasured planets/materials retain factor 1. No resource is disabled or heavily suppressed.
        if(!"Overworld".equals(dimension))return 1.0;
        if("Tin".equals(material)||"Cassiterite".equals(material)||"CassiteriteSand".equals(material))return 1.20;
        if("Iron".equals(material)||"Magnetite".equals(material)||"VanadiumMagnetite".equals(material)||"BrownLimonite".equals(material)||"YellowLimonite".equals(material)||"Pyrite".equals(material))return 1.10;
        if("Mica".equals(material))return 1.20;
        if("Lapis".equals(material))return 1.40;
        if("Graphite".equals(material))return .80;
        if("Diamond".equals(material)||"Nickel".equals(material))return .85;
        if("Redstone".equals(material))return .90;
        return 1.0;
    }
}
