package classicmining;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import java.io.*;
import java.util.Properties;

@Mod(modid="classicmining", name="Classic Mining for GTNH", version="0.1.0", dependencies="required-after:gregtech@[5.09.54.205]", acceptableRemoteVersions="*")
public final class ClassicMining {
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) throws Exception {
        File config = new File(event.getModConfigurationDirectory(), "classic-mining.properties");
        Properties props = new Properties();
        if (config.isFile()) try (InputStream in = new FileInputStream(config)) { props.load(in); }
        MiningHooks.enabled = Boolean.parseBoolean(props.getProperty("enabled", "true"));
        MiningHooks.deposits = Integer.parseInt(props.getProperty("depositsPerChunk", "32"));
        if (MiningHooks.deposits < 1 || MiningHooks.deposits > 128) throw new IllegalArgumentException("depositsPerChunk must be 1..128");
        if (!config.isFile()) {
            props.setProperty("enabled", "true");
            props.setProperty("depositsPerChunk", "32");
            try (OutputStream out = new FileOutputStream(config)) {
                props.store(out, "Classic Mining 0.1.0 - use a FRESH world. Full GT ore processing, original dimensions and depth bands. Small ores/stones/asteroids unchanged. Keep settings fixed after world creation.");
            }
        }
        System.out.println("[ClassicMining] enabled=" + MiningHooks.enabled + " depositsPerChunk=" + MiningHooks.deposits);
    }
}
