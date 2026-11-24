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
