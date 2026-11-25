package se.coldmc.prison;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;

import java.util.EnumSet;
import java.util.Set;

/** the owner, 2026-11-25: "all the schematics' flowers on illegal placements get destroyed when loading those
 *  chunks". The builds put flowers, grass and carpets on blocks they cannot stand on; the game pops them
 *  off the first time it checks them. In world_prison that check is cancelled for these decorations, so
 *  they stay as built. Players can still break them where breaking is allowed. */
final class KeepPlants implements Listener {
    private final Prison plugin;
    private final Set<Material> keep = EnumSet.noneOf(Material.class);

    KeepPlants(Prison plugin) {
        this.plugin = plugin;
        for (Tag<Material> t : java.util.List.of(Tag.FLOWERS, Tag.SMALL_FLOWERS, Tag.SAPLINGS, Tag.WOOL_CARPETS,
                Tag.CROPS, Tag.CAVE_VINES, Tag.CORALS, Tag.WALL_CORALS))
            keep.addAll(t.getValues());
        for (String n : new String[]{"SHORT_GRASS", "TALL_GRASS", "FERN", "LARGE_FERN", "DEAD_BUSH", "BUSH",
                "SHORT_DRY_GRASS", "TALL_DRY_GRASS", "FIREFLY_BUSH", "LEAF_LITTER", "WILDFLOWERS",
                "BROWN_MUSHROOM", "RED_MUSHROOM", "CRIMSON_FUNGUS", "WARPED_FUNGUS", "CRIMSON_ROOTS",
                "WARPED_ROOTS", "NETHER_SPROUTS", "SUGAR_CANE", "CACTUS", "BAMBOO", "BAMBOO_SAPLING",
                "SWEET_BERRY_BUSH", "VINE", "WEEPING_VINES", "WEEPING_VINES_PLANT", "TWISTING_VINES",
                "TWISTING_VINES_PLANT", "GLOW_LICHEN", "LILY_PAD", "SEAGRASS", "TALL_SEAGRASS", "KELP",
                "KELP_PLANT", "MOSS_CARPET", "PALE_MOSS_CARPET", "PALE_HANGING_MOSS", "HANGING_ROOTS",
                "SPORE_BLOSSOM", "SMALL_DRIPLEAF", "BIG_DRIPLEAF", "BIG_DRIPLEAF_STEM", "PINK_PETALS",
                "SNOW", "NETHER_WART", "COCOA", "CHORUS_PLANT", "CHORUS_FLOWER", "AZALEA", "FLOWERING_AZALEA",
                "MANGROVE_PROPAGULE", "PITCHER_PLANT", "TORCHFLOWER", "SEA_PICKLE"}) {
            Material m = Material.getMaterial(n);
            if (m != null) keep.add(m);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    void onPhysics(BlockPhysicsEvent e) {
        if (!e.getBlock().getWorld().getName().equals(plugin.worldName)) return;
        if (keep.contains(e.getBlock().getType())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    void onDestroy(com.destroystokyo.paper.event.block.BlockDestroyEvent e) {
        // the game removing a block that can no longer stand (not a player breaking it)
        if (!e.getBlock().getWorld().getName().equals(plugin.worldName)) return;
        if (keep.contains(e.getBlock().getType())) e.setCancelled(true);
    }
}
