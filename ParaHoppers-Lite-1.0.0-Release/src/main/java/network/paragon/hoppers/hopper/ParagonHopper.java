package network.paragon.hoppers.hopper;

import org.bukkit.Location;

import java.util.UUID;

public final class ParagonHopper {
    private final UUID id;
    private final UUID owner;
    private final String world;
    private final int x;
    private final int y;
    private final int z;
    private final HopperType type;
    private final int level;
    private final long createdAt;
    public ParagonHopper(UUID id, UUID owner, String world, int x, int y, int z, HopperType type, int level, long createdAt) {
        this.id=id;
        this.owner=owner;
        this.world=world;
        this.x=x;
        this.y=y;
        this.z=z;
        this.type=type;
        this.level=level;
        this.createdAt=createdAt;
    }
    public UUID id(){return id;}
    public UUID owner(){return owner;}
    public String world(){return world;}
    public int x(){return x;}
    public int y(){return y;}
    public int z(){return z;}
    public HopperType type(){return type;}
    public int level(){return level;}
    public long createdAt(){return createdAt;}
    @Override public boolean equals(Object other) {
        if(this==other)return true;if(!(other instanceof ParagonHopper))return false;
        ParagonHopper h=(ParagonHopper)other;
        return java.util.Objects.equals(id,h.id)&&java.util.Objects.equals(owner,h.owner)&&java.util.Objects.equals(world,h.world)
                &&x==h.x&&y==h.y&&z==h.z&&type==h.type&&level==h.level&&createdAt==h.createdAt;
    }
    @Override public int hashCode(){return java.util.Objects.hash(id,owner,world,x,y,z,type,level,createdAt);}
    @Override public String toString(){return "ParagonHopper[id="+id+", owner="+owner+", world="+world+", x="+x+", y="+y+", z="+z+", type="+type+", level="+level+", createdAt="+createdAt+"]";}
    public static ParagonHopper create(UUID owner, Location location, HopperType type, int level) {
        return new ParagonHopper(
                UUID.randomUUID(),
                owner,
                location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ(),
                type,
                level,
                System.currentTimeMillis()
        );
    }

    public String locationKey() {
        return world + ":" + x + ":" + y + ":" + z;
    }
}
