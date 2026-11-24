package se.coldmc.prison;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** One prison: where its schematic goes, its mining cube, its barrier box and its dungeon. */
final class Mine {
    final String id, name, symbol, theme, schematic, newMaterial, elite;
    final int index;
    final int[] paste, boxMin, boxMax;
    int[] cubeMin, cubeMax;
    final double[] spawn;
    final List<String[]> mix = new ArrayList<>();      // [block, weight]
    final List<String[]> tiles = new ArrayList<>();    // [schematic, dx, dz]
    final List<String> mobs, bosses;

    // runtime
    long lastReset, lastBoss, emptySince = -1;
    int broken, resets;
    final Set<UUID> alive = new HashSet<>();
    UUID boss;

    Mine(String id, ConfigurationSection s) {
        this.id = id;
        name = s.getString("name", id);
        symbol = s.getString("symbol", id);
        theme = s.getString("theme", "overworld");
        index = s.getInt("index");
        schematic = s.getString("schematic");
        newMaterial = s.getString("new-material", "STONE");
        elite = s.getString("elite", "");
        paste = ints(s.getIntegerList("paste"));
        cubeMin = ints(s.getIntegerList("cube.min"));
        cubeMax = ints(s.getIntegerList("cube.max"));
        boxMin = ints(s.getIntegerList("box.min"));
        boxMax = ints(s.getIntegerList("box.max"));
        List<Double> sp = s.getDoubleList("spawn");
        spawn = new double[]{sp.get(0), sp.get(1), sp.get(2), sp.size() > 3 ? sp.get(3) : 0};
        for (Object o : s.getList("mix", List.of())) {
            if (o instanceof List<?> l && l.size() == 2) mix.add(new String[]{String.valueOf(l.get(0)), String.valueOf(l.get(1))});
        }
        for (Object o : s.getList("tiles", List.of())) {
            if (o instanceof List<?> l && l.size() == 3)
                tiles.add(new String[]{String.valueOf(l.get(0)), String.valueOf(l.get(1)), String.valueOf(l.get(2))});
        }
        mobs = s.getStringList("mobs");
        bosses = s.getStringList("bosses");
    }

    static int[] ints(List<Integer> l) {
        int[] a = new int[3];
        for (int i = 0; i < 3 && i < l.size(); i++) a[i] = l.get(i);
        return a;
    }

    String title() {
        return symbol.equals(name) ? "Prison " + symbol : "Prison " + symbol + " " + name;
    }

    boolean inCube(Location l) {
        return in(l, cubeMin, cubeMax);
    }

    boolean inBox(Location l) {
        return in(l, boxMin, boxMax);
    }

    private static boolean in(Location l, int[] a, int[] b) {
        int x = l.getBlockX(), y = l.getBlockY(), z = l.getBlockZ();
        return x >= a[0] && x <= b[0] && y >= a[1] && y <= b[1] && z >= a[2] && z <= b[2];
    }

    long cubeVolume() {
        return (long) (cubeMax[0] - cubeMin[0] + 1) * (cubeMax[1] - cubeMin[1] + 1) * (cubeMax[2] - cubeMin[2] + 1);
    }

    Location spawnAt(World w) {
        return new Location(w, spawn[0], spawn[1], spawn[2], (float) spawn[3], 0f);
    }

    List<Player> players(World w) {
        List<Player> out = new ArrayList<>();
        for (Player p : w.getPlayers()) if (inBox(p.getLocation())) out.add(p);
        return out;
    }
}
