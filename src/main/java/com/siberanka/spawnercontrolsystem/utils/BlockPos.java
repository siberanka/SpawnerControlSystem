package com.siberanka.spawnercontrolsystem.utils;

import org.bukkit.Location;
import java.util.Objects;

public class BlockPos {
    public final String world;
    public final int x;
    public final int y;
    public final int z;

    public BlockPos(String world, int x, int y, int z) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public BlockPos(Location loc) {
        this(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        BlockPos blockPos = (BlockPos) o;
        return x == blockPos.x && y == blockPos.y && z == blockPos.z && world.equals(blockPos.world);
    }

    @Override
    public int hashCode() {
        return Objects.hash(world, x, y, z);
    }

    @Override
    public String toString() {
        return world + ";" + x + ";" + y + ";" + z;
    }
}
