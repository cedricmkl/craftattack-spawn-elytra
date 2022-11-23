package de.coolepizza.craftattack.listener;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.KeybindComponent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SpawnBoostListener extends BukkitRunnable implements Listener {

    private final Plugin plugin;
    private final int multiplyValue;
    private final int spawnRadius;
    private final boolean boostEnabled;
    private final World world;
    private final List<Player> flying = new ArrayList<>();
    private final List<Player> boosted = new ArrayList<>();
    private final String message;

    public static SpawnBoostListener create(Plugin plugin) {
        var config = plugin.getConfig();
        if (!config.contains("multiplyValue") || !config.contains("spawnRadius") || !config.contains("boostEnabled") || !config.contains("world") ||  !config.contains("message")) {
            plugin.saveResource("config.yml", true);
            plugin.reloadConfig();
        }
        return new SpawnBoostListener(
                plugin,
                config.getInt("multiplyValue"),
                config.getInt("spawnRadius"),
                config.getBoolean("boostEnabled"),
                Objects.requireNonNull(Bukkit.getWorld(config.getString("world"))
                        , "Invalid world " + config.getString("world")),
                config.getString("message"));
    }

    private SpawnBoostListener(Plugin plugin, int multiplyValue, int spawnRadius, boolean boostEnabled, World world, String message) {
        this.plugin = plugin;
        this.multiplyValue = multiplyValue;
        this.spawnRadius = spawnRadius;
        this.boostEnabled = boostEnabled;
        this.world = world;
        this.message = message;

        this.runTaskTimer(this.plugin, 0, 3);
    }


    @Override
    public void run() {
        world.getPlayers().forEach(player -> {
            if (player.getGameMode() != GameMode.SURVIVAL) return;
            player.setAllowFlight(isInSpawnRadius(player));
            if (flying.contains(player) && !player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType().isAir()) {
                player.setAllowFlight(false);
                player.setGliding(false);
                boosted.remove(player);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    flying.remove(player);
                }, 5);
            }
        });
    }


    @EventHandler
    public void onDoubleJump(PlayerToggleFlightEvent event) {
        if (event.getPlayer().getGameMode() != GameMode.SURVIVAL && event.getPlayer().getGameMode() != GameMode.ADVENTURE) return;
        if (!isInSpawnRadius(event.getPlayer())) return;
        event.setCancelled(true);
        event.getPlayer().setGliding(true);
        flying.add(event.getPlayer());
        if (!boostEnabled) return;
        String[] messageParts = message.split("%key%");
        event.getPlayer().spigot().sendMessage(ChatMessageType.ACTION_BAR,
                new ComponentBuilder(messageParts[0])
                        .append(new KeybindComponent("key.swapOffhand"))
                        .append(messageParts[1])
                        .create());
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntityType() == EntityType.PLAYER
                && (event.getCause() == EntityDamageEvent.DamageCause.FALL
                || event.getCause() == EntityDamageEvent.DamageCause.FLY_INTO_WALL)
                && flying.contains(event.getEntity())) event.setCancelled(true);
    }


    @EventHandler
    public void onSwapItem(PlayerSwapHandItemsEvent event) {
        if (!boostEnabled || !flying.contains(event.getPlayer()) || boosted.contains(event.getPlayer())) return;
        event.setCancelled(true);
        boosted.add(event.getPlayer());
        event.getPlayer().setVelocity(event.getPlayer().getLocation().getDirection().multiply(multiplyValue));
    }

    @EventHandler
    public void onToggleGlide(EntityToggleGlideEvent event) {
        if (event.getEntityType() == EntityType.PLAYER && flying.contains(event.getEntity())) event.setCancelled(true);
    }

    private boolean isInSpawnRadius(Player player) {
        if (!player.getWorld().equals(world)) return false;
        return world.getSpawnLocation().distance(player.getLocation()) <= spawnRadius;
    }
}
