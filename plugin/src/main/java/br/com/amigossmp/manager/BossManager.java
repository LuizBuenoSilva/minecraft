package br.com.amigossmp.manager;

import br.com.amigossmp.AmigosSMPPlugin;
import br.com.amigossmp.util.Texts;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.inventory.ItemStack;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class BossManager {
    private final AmigosSMPPlugin plugin;
    private final Set<UUID> activeBosses = new HashSet<>();

    public BossManager(AmigosSMPPlugin plugin) { this.plugin = plugin; }

    public WitherSkeleton spawn(Location location) {
        WitherSkeleton boss = (WitherSkeleton) location.getWorld().spawnEntity(location, EntityType.WITHER_SKELETON);
        boss.setCustomName(Texts.color("&5&lGuardião Corrompido"));
        boss.setCustomNameVisible(true);
        boss.setGlowing(true);
        boss.setRemoveWhenFarAway(false);
        boss.setMaxHealth(plugin.getConfig().getDouble("boss.health", 120.0));
        boss.setHealth(boss.getMaxHealth());
        if (boss.getEquipment() != null) {
            boss.getEquipment().setItemInMainHand(new ItemStack(Material.NETHERITE_SWORD));
            boss.getEquipment().setHelmet(new ItemStack(Material.NETHERITE_HELMET));
        }
        activeBosses.add(boss.getUniqueId());
        Bukkit.broadcastMessage(Texts.color(plugin.prefix() + "&5Um &dGuardião Corrompido &5apareceu em &f" + location.getWorld().getName() + " &7(" + location.getBlockX() + ", " + location.getBlockZ() + ")"));
        return boss;
    }

    public boolean isBoss(UUID uuid) { return activeBosses.contains(uuid); }

    public void defeated(UUID uuid, Player killer) {
        if (!activeBosses.remove(uuid)) return;
        int reward = plugin.getConfig().getInt("coins.boss-kill", 100);
        plugin.profiles().add(killer.getUniqueId(), "coins", reward);
        killer.getInventory().addItem(plugin.items().bossDrop());
        Bukkit.broadcastMessage(Texts.color(plugin.prefix() + "&6" + killer.getName() + " &ederrotou o Guardião Corrompido! &6+" + reward + " moedas &ee recebeu um item especial."));
    }

    public int activeCount() { return activeBosses.size(); }
}
