package network.paragon.hoppers.compat;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.EnumMap;
import java.util.Map;

/** Optional cosmetics only. Call on the server thread, before replacing a mined block.
 * Capability checks intentionally work without parsing a 1.xx version number.
 * No Particle, BlockData, MaterialData, Sound or Effect types occur in descriptors.
 */
public final class VisualCompat {
    public enum Cue {
        CLICK("UI_BUTTON_CLICK", "CLICK"),
        SUCCESS("ENTITY_PLAYER_LEVELUP", "LEVEL_UP"),
        DENIED("BLOCK_NOTE_BLOCK_BASS", "BLOCK_NOTE_BASS", "NOTE_BASS"),
        PLACE("BLOCK_METAL_PLACE", "BLOCK_STONE_PLACE", "DIG_STONE"),
        BREAK("BLOCK_METAL_BREAK", "BLOCK_STONE_BREAK", "DIG_STONE");

        private final String[] names;
        Cue(String... names) { this.names = names; }
    }

    private static final class SoundCall {
        private final Method method;
        private final Object sound;
        SoundCall(Method method, Object sound) { this.method = method; this.sound = sound; }
    }

    private final Map<Cue, SoundCall> sounds = new EnumMap<Cue, SoundCall>(Cue.class);
    private boolean particleChecked;
    private Method spawnParticle;
    private Method blockData;
    private java.lang.reflect.Constructor<?> materialData;
    private Object blockParticle;
    private Class<?> particleDataType;
    private boolean legacyChecked;
    private Method playEffect;
    private Method materialId;
    private Object stepEffect;

    /** Player-local sound, with cached absent capabilities and no enum assumptions. */
    public void sound(Player player, Cue cue) {
        if (player == null || cue == null) return;
        try {
            if (!sounds.containsKey(cue)) {
                Class<?> type = Class.forName("org.bukkit.Sound");
                Object value = constant(type, cue.names);
                sounds.put(cue, value == null ? null : new SoundCall(
                        Player.class.getMethod("playSound", Location.class, type, float.class, float.class), value));
            }
            SoundCall call = sounds.get(cue);
            if (call != null) call.method.invoke(player, player.getLocation(), call.sound, 0.35F, 1.0F);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            sounds.put(cue, null);
        }
    }

    /** Modern block particles, pre-flattening MaterialData, then legacy STEP_SOUND.
     * No world mutation, inventory access, scheduling or gameplay decisions here.
     */
    public void blockBreak(Block block) {
        if (block == null) return;
        try {
            // Defense in depth: never depict protected bedrock as being mined.
            String name = block.getType().name();
            if ("BEDROCK".equals(name) || "AIR".equals(name) || name.endsWith("_AIR")) return;
            if (particles(block)) return;
            legacyBreak(block);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            // A cosmetic failure must never interrupt drops, block removal or cursor updates.
        }
    }

    private boolean particles(Block block) {
        try {
            if (!particleChecked) {
                particleChecked = true;
                Class<?> type = Class.forName("org.bukkit.Particle");
                blockParticle = constant(type, "BLOCK", "BLOCK_CRACK");
                if (blockParticle == null) return false;
                particleDataType = (Class<?>) type.getMethod("getDataType").invoke(blockParticle);
                // Query the payload contract rather than assuming it from the particle name.
                if ("org.bukkit.block.data.BlockData".equals(particleDataType.getName())) {
                    blockData = Block.class.getMethod("getBlockData");
                } else if ("org.bukkit.material.MaterialData".equals(particleDataType.getName())) {
                    materialData = particleDataType.getConstructor(Material.class, byte.class);
                    blockData = Block.class.getMethod("getData");
                } else return false;
                spawnParticle = World.class.getMethod("spawnParticle", type, Location.class, int.class,
                        double.class, double.class, double.class, double.class, Object.class);
            }
            if (spawnParticle == null) return false;
            Object data = blockData.invoke(block);
            if (materialData != null) {
                data = materialData.newInstance(block.getType(), ((Number) data).byteValue());
            }
            if (!particleDataType.isInstance(data)) return false;
            spawnParticle.invoke(block.getWorld(), blockParticle, block.getLocation().add(0.5, 0.5, 0.5),
                    8, 0.25D, 0.25D, 0.25D, 0.05D, data);
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            // Disable this backend once; avoid repeatedly throwing on every mining tick.
            spawnParticle = null;
            return false;
        }
    }

    private void legacyBreak(Block block) throws ReflectiveOperationException {
        if (!legacyChecked) {
            legacyChecked = true;
            Class<?> effect = Class.forName("org.bukkit.Effect");
            stepEffect = constant(effect, "STEP_SOUND");
            // Numeric material IDs are only meaningful before the Flattening.
            try { Block.class.getMethod("getBlockData"); return; }
            catch (NoSuchMethodException expected) { /* Legacy block API. */ }
            materialId = Material.class.getMethod("getId");
            if (stepEffect != null) playEffect = World.class.getMethod("playEffect", Location.class, effect, int.class);
        }
        if (playEffect == null) return;
        try {
            int id = ((Number) materialId.invoke(block.getType())).intValue();
            if (id > 0) playEffect.invoke(block.getWorld(), block.getLocation(), stepEffect, id);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            playEffect = null;
        }
    }

    private static Object constant(Class<?> type, String... names) {
        for (String name : names) {
            try { return type.getField(name).get(null); }
            catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) { }
        }
        return null;
    }
}
