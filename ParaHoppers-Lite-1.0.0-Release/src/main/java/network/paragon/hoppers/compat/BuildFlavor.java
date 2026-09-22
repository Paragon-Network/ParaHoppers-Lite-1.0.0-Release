package network.paragon.hoppers.compat;

import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** Refuse the wrong descriptor before touching stored hoppers/materials. */
public final class BuildFlavor {
    private BuildFlavor() {}

    public static boolean check(JavaPlugin plugin) {
        Properties metadata = new Properties();
        try (InputStream input = plugin.getResource("compat-build.properties")) {
            if (input == null) throw new IOException("Missing build metadata; use a packaged legacy or modern JAR");
            metadata.load(input);
        } catch (IOException | RuntimeException failure) {
            plugin.getLogger().severe("ParaHoppers cannot validate its build: " + failure.getMessage());
            return false;
        }
        boolean flattened;
        try { Block.class.getMethod("getBlockData"); flattened = true; }
        catch (NoSuchMethodException legacy) { flattened = false; }
        catch (RuntimeException | LinkageError unavailable) {
            plugin.getLogger().severe("Cannot determine the server block API; refusing to modify hopper data.");
            return false;
        }
        String flavor = metadata.getProperty("flavor", "");
        String required = flattened ? "modern" : "legacy";
        if (!required.equals(flavor)) {
            plugin.getLogger().severe("Install only the Para-Hoppers-Lite modern JAR for 1.13+, or legacy JAR for 1.7.10-1.12.2. This server requires: " + required);
            return false;
        }
        plugin.getLogger().info("ParaHoppers Lite 1.0.0: " + flavor + " artifact, Java " + System.getProperty("java.version")
                + "; Java 8 bytecode.");
        return true;
    }
}

