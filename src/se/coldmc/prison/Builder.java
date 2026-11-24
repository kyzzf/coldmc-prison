package se.coldmc.prison;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.mask.BlockTypeMask;
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

/** Every WorldEdit job: paste a prison, box it in barriers, line and fill its mining cube. */
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

    /** Paste the schematic so its lowest corner lands on paste[], then the barrier box. */
    void build(World w, Mine m, boolean paste) throws Exception {
        try (EditSession es = session(w)) {
            if (paste && m.tiles.isEmpty()) pasteOne(es, m.schematic, v(m.paste));
            if (paste) for (String[] t : m.tiles) {
                // each tile has its origin at its own min corner
                pasteOne(es, t[0], v(m.paste).add(Integer.parseInt(t[1]), 0, Integer.parseInt(t[2])));
                es.flushQueue();
            }
            es.makeFaces(new CuboidRegion(BukkitAdapter.adapt(w), v(m.boxMin), v(m.boxMax)),
                    BlockTypes.BARRIER.getDefaultState());
        }
        line(w, m);
        fill(w, m);
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

    /** Bedrock under and around the cube wherever the build left it open, so ore never hangs over the void. */
    void line(World w, Mine m) throws Exception {
        int[] a = m.cubeMin, b = m.cubeMax;
        com.sk89q.worldedit.world.World ww = BukkitAdapter.adapt(w);
        try (EditSession es = session(w)) {
            BlockTypeMask air = new BlockTypeMask(es, BlockTypes.AIR, BlockTypes.CAVE_AIR, BlockTypes.VOID_AIR,
                    BlockTypes.WATER, BlockTypes.LAVA);
            BlockState rock = BlockTypes.BEDROCK.getDefaultState();
            int x0 = a[0] - 1, x1 = b[0] + 1, z0 = a[2] - 1, z1 = b[2] + 1, y0 = a[1] - 1, y1 = b[1];
            CuboidRegion[] faces = {
                    new CuboidRegion(ww, BlockVector3.at(x0, y0, z0), BlockVector3.at(x1, y0, z1)),   // floor
                    new CuboidRegion(ww, BlockVector3.at(x0, y0, z0), BlockVector3.at(x0, y1, z1)),
                    new CuboidRegion(ww, BlockVector3.at(x1, y0, z0), BlockVector3.at(x1, y1, z1)),
                    new CuboidRegion(ww, BlockVector3.at(x0, y0, z0), BlockVector3.at(x1, y1, z0)),
                    new CuboidRegion(ww, BlockVector3.at(x0, y0, z1), BlockVector3.at(x1, y1, z1))};
            for (CuboidRegion r : faces) es.replaceBlocks(r, air, rock);
        }
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
            es.setBlocks((com.sk89q.worldedit.regions.Region) new CuboidRegion(BukkitAdapter.adapt(w), v(m.cubeMin), v(m.cubeMax)), (com.sk89q.worldedit.function.pattern.Pattern) rp);
        }
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
