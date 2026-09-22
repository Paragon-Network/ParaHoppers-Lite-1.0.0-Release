package network.paragon.hoppers.compat;

import org.bukkit.World;
import java.lang.reflect.Method;

/** Actual per-world limits, including custom dimensions. Maximum is exclusive. */
public final class WorldCompat {
    private static final Method MIN_HEIGHT = findMinHeight();
    private WorldCompat() {}

    public static final class Bounds {
        public final int min;
        public final int max;
        public Bounds(int min, int max) { this.min = min; this.max = max; }
        public boolean contains(int y) { return y >= min && y < max; }
    }

    private static Method findMinHeight() {
        try { return World.class.getMethod("getMinHeight"); }
        catch (NoSuchMethodException | RuntimeException | LinkageError ignored) { return null; }
    }

    /** Legacy Bukkit starts at zero. A failing present API does not imply legacy. */
    public static Bounds bounds(World world) {
        if (world == null) return null;
        try {
            int min = MIN_HEIGHT == null ? 0 : ((Number) MIN_HEIGHT.invoke(world)).intValue();
            int max = world.getMaxHeight();
            if (min == Integer.MIN_VALUE || max <= min) return null;
            return new Bounds(min, max);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return null;
        }
    }
}
