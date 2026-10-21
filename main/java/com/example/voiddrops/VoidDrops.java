package com.example.voiddrops;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class VoidDrops extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);

        long interval = Math.max(1, getConfig().getLong("check-interval", 10));
        Bukkit.getScheduler().runTaskTimer(this, this::checkItems, interval, interval);
    }

    // Make sure the thrower is recorded for player drops
    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        event.getItemDrop().setThrower(event.getPlayer().getUniqueId());
    }

    private void checkItems() {
        double voidY = getConfig().getDouble("void-y", -10);
        boolean removeUnowned = getConfig().getBoolean("remove-unowned", false);
        boolean requireOnline = getConfig().getBoolean("require-online", true);
        List<String> ignored = getConfig().getStringList("ignored-worlds");
        String msg = ChatColor.translateAlternateColorCodes('&',
                getConfig().getString("message", ""));

        for (World world : Bukkit.getWorlds()) {
            if (ignored.contains(world.getName())) continue;

            for (Item item : world.getEntitiesByClass(Item.class)) {
                if (item.getLocation().getY() > voidY) continue;

                UUID thrower = item.getThrower();
                Player player = thrower == null ? null : Bukkit.getPlayer(thrower);

                if (player == null) {
                    if (thrower == null) {
                        if (removeUnowned) item.remove();   // no owner: optionally delete
                    }
                    // Owner offline: leave the item. It will keep falling and eventually
                    // be removed by the server at the world's void limit.
                    continue;
                }

                ItemStack stack = item.getItemStack();
                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(stack);
                item.remove();

                // Inventory full: drop the rest at the player's feet
                for (ItemStack rest : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), rest);
                }
                if (!msg.isEmpty()) player.sendMessage(msg);
            }
        }
    }
}
