package network.paragon.hoppers.compat;

import org.bukkit.Bukkit;
import java.lang.reflect.Method;

/** Central boundary for APIs that differ between Bukkit/Paper generations. */
public final class CompatibilityManager {
    private final MinecraftVersion version;
    private final CompatibilityFamily family;
    public CompatibilityManager(){
        version = MinecraftVersion.parse(Bukkit.getBukkitVersion());
        family = detect(version);
    }
    private static CompatibilityFamily detect(MinecraftVersion v){
        if(v.calendarVersion()) return CompatibilityFamily.CALENDAR_26_PLUS;
        if(v.major()!=1) return CompatibilityFamily.UNKNOWN;
        if(v.minor()<=8) return CompatibilityFamily.LEGACY_1_7_1_8;
        if(v.minor()<=12) return CompatibilityFamily.LEGACY_1_9_1_12;
        if(v.minor()<=15) return CompatibilityFamily.FLATTENED_1_13_1_15;
        if(v.minor()<=20) return CompatibilityFamily.MODERN_1_16_1_20;
        if(v.minor()>=21) return CompatibilityFamily.CURRENT_1_21;
        return CompatibilityFamily.UNKNOWN;
    }
    public MinecraftVersion version(){return version;}
    public CompatibilityFamily family(){return family;}
    /** Paper exposes getTPS; Bukkit/Spigot legacy does not. Reflection prevents a hard linkage. */
    public double tps(){
        try { Method m=Bukkit.class.getMethod("getTPS"); Object value=m.invoke(null); if(value instanceof double[] && ((double[])value).length>0)return ((double[])value)[0]; }
        catch(ReflectiveOperationException | RuntimeException | LinkageError ignored){}
        return 20.0D;
    }
    public int minHeight(org.bukkit.World world){
        WorldCompat.Bounds bounds=WorldCompat.bounds(world);
        return bounds==null?0:bounds.min;
    }
}
