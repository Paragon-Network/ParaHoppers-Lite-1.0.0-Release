package network.paragon.hoppers.compat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses both legacy 1.xx versions and Mojang's 26.x+ numbering without assuming a fixed scheme. */
public final class MinecraftVersion implements Comparable<MinecraftVersion> {
    private static final Pattern VERSION = Pattern.compile("(?:MC: )?(\\d+)\\.(\\d+)(?:\\.(\\d+))?");
    private final int major, minor, patch;
    private final String raw;

    private MinecraftVersion(int major, int minor, int patch, String raw) {
        this.major = major; this.minor = minor; this.patch = patch; this.raw = raw;
    }

    public static MinecraftVersion parse(String text) {
        Matcher m = VERSION.matcher(text == null ? "" : text);
        MinecraftVersion best = null;
        while (m.find()) {
            int a, b, c;
            try {
                a = Integer.parseInt(m.group(1));
                b = Integer.parseInt(m.group(2));
                c = m.group(3) == null ? 0 : Integer.parseInt(m.group(3));
            } catch (NumberFormatException malformed) { continue; }
            // Prefer Minecraft-looking versions: 1.7+ or calendar-style 26+
            if ((a == 1 && b >= 7) || a >= 26) {
                best = new MinecraftVersion(a,b,c,m.group());
                break;
            }
        }
        return best == null ? new MinecraftVersion(0,0,0,text) : best;
    }

    public int major(){ return major; }
    public int minor(){ return minor; }
    public int patch(){ return patch; }
    public boolean legacyPreFlattening(){ return major == 1 && minor <= 12; }
    public boolean flattened(){ return major > 1 || (major == 1 && minor >= 13); }
    public boolean calendarVersion(){ return major >= 26; }
    public boolean atLeast(int a,int b){ return compareTo(new MinecraftVersion(a,b,0,a+"."+b)) >= 0; }
    public String raw(){ return raw; }
    public String normalized(){ return major + "." + minor + (patch > 0 ? "." + patch : ""); }
    @Override public int compareTo(MinecraftVersion o){ int x=Integer.compare(major,o.major); if(x!=0)return x; x=Integer.compare(minor,o.minor); return x!=0?x:Integer.compare(patch,o.patch); }
}
