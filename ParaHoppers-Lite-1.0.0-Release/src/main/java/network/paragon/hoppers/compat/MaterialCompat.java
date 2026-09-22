package network.paragon.hoppers.compat;

import org.bukkit.Material;
import java.lang.reflect.Method;
import java.util.*;

/** Version tolerant material resolver. Never references optional Material enum constants directly. */
public final class MaterialCompat {
    private static final Map<String,String[]> ALIASES = new HashMap<>();
    static {
        alias("GRASS_BLOCK", "GRASS");
        alias("NETHER_QUARTZ_ORE", "QUARTZ_ORE");
        alias("END_STONE", "ENDER_STONE");
        alias("MAGMA_BLOCK", "MAGMA");
        alias("OAK_LOG", "LOG"); alias("SPRUCE_LOG", "LOG"); alias("BIRCH_LOG", "LOG"); alias("JUNGLE_LOG", "LOG");
        alias("OAK_PLANKS", "WOOD");
        alias("WATER", "STATIONARY_WATER"); alias("LAVA", "STATIONARY_LAVA");
        alias("COMPARATOR", "REDSTONE_COMPARATOR");
        alias("WHITE_STAINED_GLASS_PANE", "STAINED_GLASS_PANE", "THIN_GLASS");
        alias("IRON_PICKAXE", "IRON_PICKAXE");
        alias("DIAMOND_PICKAXE", "DIAMOND_PICKAXE");
    }
    private static void alias(String modern,String... legacy){ALIASES.put(modern,legacy);}
    private MaterialCompat(){}

    public static Material resolve(String name){
        if(name==null||name.trim().isEmpty())return null;
        String n=name.trim().toUpperCase(Locale.ROOT).replace(' ','_').replace('-','_');
        Material direct=match(n); if(direct!=null)return direct;
        String[] aliases=ALIASES.get(n); if(aliases!=null)for(String a:aliases){Material m=match(a);if(m!=null)return m;}
        // Deepslate variants map to their base ore on pre-1.17 servers.
        if(n.startsWith("DEEPSLATE_")&&n.endsWith("_ORE")){Material m=match(n.substring("DEEPSLATE_".length()));if(m!=null)return m;}
        return null;
    }
    private static Material match(String n){
        try { Method m=Material.class.getMethod("matchMaterial",String.class); Object v=m.invoke(null,n); if(v instanceof Material)return (Material)v; }
        catch(Exception ignored){}
        try{return Material.valueOf(n);}catch(IllegalArgumentException ignored){return null;}
    }
    public static boolean is(Material material,String... names){
        if(material==null)return false; String actual=material.name();
        for(String n:names){Material resolved=resolve(n);if(resolved!=null&&resolved==material)return true;if(actual.equalsIgnoreCase(n))return true;}
        return false;
    }
    public static boolean isBedrock(Material m){return is(m,"BEDROCK");}
    public static boolean isHopper(Material m){return is(m,"HOPPER");}
    public static boolean isFluid(Material m){return is(m,"WATER","STATIONARY_WATER","LAVA","STATIONARY_LAVA");}
    public static boolean isProtectedContainer(Material m){
        if(m==null)return false;String n=m.name();
        return is(m,"HOPPER","CHEST","TRAPPED_CHEST","ENDER_CHEST","BARREL","SHULKER_BOX")||n.endsWith("_SHULKER_BOX");
    }
    public static Set<Material> resolveAll(Collection<String> names){Set<Material>s=new LinkedHashSet<>();if(names!=null)for(String n:names){Material m=resolve(n);if(m!=null)s.add(m);}return s;}
}
