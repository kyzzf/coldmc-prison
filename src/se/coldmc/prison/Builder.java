package se.coldmc.prison;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.mask.RegionMask;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.pattern.RandomPattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

/** Every WorldEdit job: paste a prison, box it in barriers, fill its mining cube, undo an old automatic cube. */
final class Builder {
    private final Prison plugin;

    Builder(Prison plugin) {
        this.plugin = plugin;
    }

    private EditSession session(World w) {
        // No undo history and no neighbour updates: a prison is millions of blocks, and keeping their
        // history on a 2 GB heap stalled the first build for hours (2026-11-24).
        return WorldEdit.getInstance().newEditSessionBuilder().world(BukkitAdapter.adapt(w))
                .changeSetNull().fastMode(true).limitUnlimited().build();
    }

    private static BlockVector3 v(int[] a) {
        return BlockVector3.at(a[0], a[1], a[2]);
    }

    /** Paste the schematic so its lowest corner lands on paste[], then the barrier box, then fill the
     *  cube if the owner has marked one. */
    void build(World w, Mine m, boolean paste) throws Exception {
        try (EditSession es = session(w)) {
            if (paste) pasteSchematic(es, m, null);
        }
        if (paste) floors(w, m);
        try (EditSession es = session(w)) {
            es.makeFaces(new CuboidRegion(BukkitAdapter.adapt(w), v(m.boxMin), v(m.boxMax)),
                    BlockTypes.BARRIER.getDefaultState());
        }
        if (m.hasCube()) fill(w, m);
    }

    /** The whole schematic, or only the tiles that reach into `only` (with a mask, nothing outside it changes). */
    private void pasteSchematic(EditSession es, Mine m, CuboidRegion only) throws Exception {
        if (only != null) es.setMask(new RegionMask(only));
        if (m.tiles.isEmpty()) {
            pasteOne(es, m.schematic, v(m.paste));
            return;
        }
        for (String[] t : m.tiles) {
            // each tile has its origin at its own min corner and is TILE wide
            int dx = Integer.parseInt(t[1]), dz = Integer.parseInt(t[2]);
            int x0 = m.paste[0] + dx, z0 = m.paste[2] + dz;
            if (only != null && (x0 > only.getMaximumPoint().x() || x0 + TILE - 1 < only.getMinimumPoint().x()
                    || z0 > only.getMaximumPoint().z() || z0 + TILE - 1 < only.getMinimumPoint().z())) continue;
            pasteOne(es, t[0], v(m.paste).add(dx, 0, dz));
            es.flushQueue();
        }
    }

    static final int TILE = 128;

    /** The ground the original export lacks (floors.yml, find_prison_floors.py): under plants, water and
     *  sand left floating, and a floor in the old mine pits. the owner, 2026-11-25. */
    void floors(World w, Mine m) throws Exception {
        List<String> runs = plugin.floors.get(m.schematic.substring(m.schematic.lastIndexOf('/') + 1));
        if (runs == null || runs.isEmpty()) return;
        java.util.Map<String, BlockState> states = new java.util.HashMap<>();
        com.sk89q.worldedit.world.World ww = BukkitAdapter.adapt(w);
        int bad = 0;
        try (EditSession es = session(w)) {
            for (String r : runs) {
                String[] p = r.split(" ");
                BlockState st = states.computeIfAbsent(p[4], Builder::state);
                if (st == null) {
                    bad++;
                    continue;
                }
                int dy = Integer.parseInt(p[0]), dz = Integer.parseInt(p[1]);
                BlockVector3 a = BlockVector3.at(m.paste[0] + Integer.parseInt(p[2]), m.paste[1] + dy, m.paste[2] + dz);
                BlockVector3 b = BlockVector3.at(m.paste[0] + Integer.parseInt(p[3]), m.paste[1] + dy, m.paste[2] + dz);
                es.setBlocks((com.sk89q.worldedit.regions.Region) new CuboidRegion(ww, a, b), (com.sk89q.worldedit.function.pattern.Pattern) st);
            }
        }
        if (bad > 0) plugin.getLogger().warning(m.id + ": " + bad + " floor runs with an unknown block");
    }

    /** Undo the automatic cube of 2026-11-24 (the owner: "wrong placed mineable blocks surrounded by bedrock"):
     *  empty the cube and its one-block bedrock shell, then paste the original build back into exactly
     *  that space. Everything outside it is untouched. */
    void clearAuto(World w, Mine m) throws Exception {
        if (m.autoMin == null) return;
        int[] a = m.autoMin, b = m.autoMax;
        CuboidRegion r = new CuboidRegion(BukkitAdapter.adapt(w), BlockVector3.at(a[0] - 1, a[1] - 1, a[2] - 1),
                BlockVector3.at(b[0] + 1, b[1], b[2] + 1));
        try (EditSession es = session(w)) {
            es.setBlocks((com.sk89q.worldedit.regions.Region) r, BlockTypes.AIR.getDefaultState());
        }
        try (EditSession es = session(w)) {
            pasteSchematic(es, m, r);
        }
    }

    private void pasteOne(EditSession es, String schematic, BlockVector3 minCorner) throws Exception {
        File f = new File(Bukkit.getPluginManager().getPlugin("FastAsyncWorldEdit").getDataFolder(), "schematics/" + schematic);
        ClipboardFormat fmt = ClipboardFormats.findByFile(f);
        if (fmt == null) throw new IllegalStateException("unknown schematic format: " + f);
        Clipboard cb;
        try (ClipboardReader r = fmt.getReader(new FileInputStream(f))) {
            cb = r.read();
        }
        BlockVector3 to = minCorner.add(cb.getOrigin().subtract(cb.getRegion().getMinimumPoint()));
        Operations.complete(new ClipboardHolder(cb).createPaste(es).to(to).ignoreAirBlocks(true).build());
        cb.close();
    }

    /** A fresh random mix in the whole cube. Nexo ores go in as the note-block state Nexo reads them by. */
    void fill(World w, Mine m) throws Exception {
        RandomPattern rp = new RandomPattern();
        int n = 0;
        for (String[] e : m.mix) {
            BlockState s = state(e[0]);
            if (s == null) {
                plugin.getLogger().warning(m.id + ": unknown block in mix: " + e[0]);
                continue;
            }
            rp.add(s, Double.parseDouble(e[1]));
            n++;
        }
        if (n == 0) rp.add(BlockTypes.STONE.getDefaultState(), 1);
        try (EditSession es = session(w)) {
            // the owner, 2026-11-25: the mine is only the minable part - bedrock (and barrier) inside the cube stays
            es.setMask(com.sk89q.worldedit.function.mask.Masks.negate(
                    new com.sk89q.worldedit.function.mask.BlockTypeMask(es, BlockTypes.BEDROCK, BlockTypes.BARRIER)));
            es.setBlocks((com.sk89q.worldedit.regions.Region) new CuboidRegion(BukkitAdapter.adapt(w), v(m.cubeMin), v(m.cubeMax)), (com.sk89q.worldedit.function.pattern.Pattern) rp);
        }
    }

    /** Shave off every outer face of a selection that is (almost) all bedrock or barrier, so the cube is
     *  only the minable inside (the owner, 2026-11-25). Returns null if nothing minable is left. */
    static int[][] trimBedrock(World w, int[] a0, int[] b0) {
        int[] a = a0.clone(), b = b0.clone();
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int axis = 0; axis < 3; axis++) {
                for (int side = 0; side < 2; side++) {
                    if (a[axis] > b[axis]) return null;
                    int at = side == 0 ? a[axis] : b[axis];
                    if (hard(w, a, b, axis, at) >= 0.9) {
                        if (side == 0) a[axis]++;
                        else b[axis]--;
                        changed = true;
                    }
                }
            }
        }
        return a[0] <= b[0] && a[1] <= b[1] && a[2] <= b[2] ? new int[][]{a, b} : null;
    }

    private static double hard(World w, int[] a, int[] b, int axis, int at) {
        int n = 0, hard = 0;
        int[] lo = a.clone(), hi = b.clone();
        lo[axis] = hi[axis] = at;
        for (int x = lo[0]; x <= hi[0]; x++)
            for (int y = lo[1]; y <= hi[1]; y++)
                for (int z = lo[2]; z <= hi[2]; z++) {
                    Material t = w.getBlockAt(x, y, z).getType();
                    n++;
                    if (t == Material.BEDROCK || t == Material.BARRIER) hard++;
                }
        return n == 0 ? 1 : (double) hard / n;
    }

    static BlockState state(String id) {
        if (id.startsWith("nexo:")) {
            org.bukkit.block.data.BlockData d = Hooks.nexoBlockData(id.substring(5));
            return d == null ? null : BukkitAdapter.adapt(d);
        }
        Material mat = Material.matchMaterial(id);
        if (mat == null || !mat.isBlock()) return null;
        return BukkitAdapter.adapt(mat.createBlockData());
    }
}
