package de.coolepizza.craftattack;

import de.coolepizza.craftattack.listener.SpawnBoostListener;
import org.bukkit.plugin.java.JavaPlugin;

public class CraftAttackSpawnBoost extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(new SpawnBoostListener(this), this);
    }

}
