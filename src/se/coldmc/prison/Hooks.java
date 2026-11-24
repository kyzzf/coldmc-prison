package se.coldmc.prison;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/** Soft hooks. Each plugin's classes are touched only after checking it is enabled. */
final class Hooks {
    private Hooks() {
    }

    static boolean on(String p) {
        return Bukkit.getPluginManager().isPluginEnabled(p);
    }

    static BlockData nexoBlockData(String id) {
        if (!on("Nexo")) return null;
        try {
            return com.nexomc.nexo.api.NexoBlocks.blockData(id);
        } catch (Throwable t) {
            return null;
        }
    }

    /** The Nexo custom-block id at this block, or null. */
    static String nexoId(Block b) {
        if (!on("Nexo")) return null;
        try {
            var mech = com.nexomc.nexo.api.NexoBlocks.customBlockMechanic(b);
            return mech == null ? null : mech.getItemID();
        } catch (Throwable t) {
            return null;
        }
    }

    static Entity mythic(String id, Location at) {
        if (!on("MythicMobs")) return null;
        try {
            var am = io.lumine.mythic.bukkit.MythicBukkit.inst().getMobManager().spawnMob(id, at);
            return am == null ? null : am.getEntity().getBukkitEntity();
        } catch (Throwable t) {
            return null;
        }
    }

    /** The cube's WorldGuard region: the only place in world_prison where blocks can be broken. */
    static boolean cubeRegion(org.bukkit.World w, Mine m) {
        if (!on("WorldGuard")) return false;
        try {
            var rm = com.sk89q.worldguard.WorldGuard.getInstance().getPlatform().getRegionContainer()
                    .get(com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(w));
            if (rm == null) return false;
            String id = "prison_" + m.id;
            if (!m.hasCube()) {
                rm.removeRegion(id);
            } else {
                var r = new com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion(id,
                        com.sk89q.worldedit.math.BlockVector3.at(m.cubeMin[0], m.cubeMin[1], m.cubeMin[2]),
                        com.sk89q.worldedit.math.BlockVector3.at(m.cubeMax[0], m.cubeMax[1], m.cubeMax[2]));
                r.setPriority(10);
                var A = com.sk89q.worldguard.protection.flags.StateFlag.State.ALLOW;
                var D = com.sk89q.worldguard.protection.flags.StateFlag.State.DENY;
                // build must be allow too: WorldGuard combines it with block-break, and the global deny would win
                r.setFlag(com.sk89q.worldguard.protection.flags.Flags.BUILD, A);
                r.setFlag(com.sk89q.worldguard.protection.flags.Flags.BLOCK_BREAK, A);
                r.setFlag(com.sk89q.worldguard.protection.flags.Flags.INTERACT, A);
                r.setFlag(com.sk89q.worldguard.protection.flags.Flags.BLOCK_PLACE, D);
                r.setFlag(com.sk89q.worldguard.protection.flags.Flags.USE, D);
                r.setFlag(com.sk89q.worldguard.protection.flags.Flags.CHEST_ACCESS, D);
                rm.addRegion(r);
            }
            rm.save();
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    static void miningExp(Player p, double amount, Location at) {
        if (amount <= 0 || !on("MMOCore")) return;
        try {
            var prof = net.Indyuce.mmocore.MMOCore.plugin.professionManager.get("mining");
            if (prof == null) return;
            net.Indyuce.mmocore.api.player.PlayerData.get(p).getCollectionSkills()
                    .giveExperience(prof, amount, net.Indyuce.mmocore.experience.EXPSource.SOURCE, at, false);
        } catch (Throwable ignored) {
        }
    }
}
