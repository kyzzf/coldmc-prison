package se.coldmc.prison;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

/** One Raid Dungeon (the owner, 2026-11-25): a dungeon build pasted in world_prison, where players mainly
 *  loot treasure. Only pasted and boxed in by Prison; no cube, no refills, no mobs of its own. */
final class Raid {
    final String id, name, schematic;
    final int[] paste, boxMin, boxMax;
    final double[] spawn;

    Raid(String id, ConfigurationSection s) {
        this.id = id;
        name = s.getString("name", id);
        schematic = s.getString("schematic");
        paste = Mine.ints(s.getIntegerList("paste"));
        boxMin = Mine.ints(s.getIntegerList("box.min"));
        boxMax = Mine.ints(s.getIntegerList("box.max"));
        var sp = s.getDoubleList("spawn");
        spawn = new double[]{sp.get(0), sp.get(1), sp.get(2), sp.size() > 3 ? sp.get(3) : 0};
    }

    Location spawnAt(World w) {
        return new Location(w, spawn[0], spawn[1], spawn[2], (float) spawn[3], 0f);
    }
}
