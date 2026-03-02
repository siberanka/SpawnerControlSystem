package com.siberanka.spawnercontrolsystem.managers;

import com.siberanka.spawnercontrolsystem.utils.BlockPos;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CacheManager {

    // Using a ConcurrentHashMap to back a thread-safe Set
    private final Set<BlockPos> spawners = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public boolean isSpawner(BlockPos pos) {
        return spawners.contains(pos);
    }

    public void addSpawner(BlockPos pos) {
        spawners.add(pos);
    }

    public void removeSpawner(BlockPos pos) {
        spawners.remove(pos);
    }

    public void clear() {
        spawners.clear();
    }

    public int size() {
        return spawners.size();
    }
}
