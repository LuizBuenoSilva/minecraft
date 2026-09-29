package br.com.amigossmp.listener;

import br.com.amigossmp.AmigosSMPPlugin;
import br.com.amigossmp.manager.EventManager;
import br.com.amigossmp.util.Texts;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.Set;

public class GameListener implements Listener {
    private static final Set<Material> FARM_BLOCKS = Set.of(
            Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS,
            Material.MELON, Material.PUMPKIN, Material.SUGAR_CANE, Material.COCOA
    );
    private final AmigosSMPPlugin plugin;

    public GameListener(AmigosSMPPlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        plugin.profiles().setName(p.getUniqueId(), p.getName());
        p.sendMessage(Texts.color("&8&m-----------------------------------------"));
        p.sendMessage(Texts.color("&b&lAMIGOS SMP &7• &fSurvival+ v1.2"));
        p.sendMessage(Texts.color("&7Use &f/amigos &7para abrir o menu principal."));
        p.sendMessage(Texts.color("&7TP: &f/tpa <jogador> &8• &7Respawn: &f/respawn &8• &7Última morte: &f/backdeath"));
        p.sendMessage(Texts.color("&8&m-----------------------------------------"));
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Material type = event.getBlock().getType();
        plugin.profiles().add(player.getUniqueId(), "blocks-broken", 1);
        plugin.quests().addProgress(player, "minerador", 1);

        if (type.name().endsWith("_ORE") || type == Material.ANCIENT_DEBRIS) {
            int xp = plugin.events().is(EventManager.EventType.MINERACAO) ? 4 : 2;
            plugin.professions().addXp(player, "minerador", xp);
        }
        if (FARM_BLOCKS.contains(type)) plugin.professions().addXp(player, "fazendeiro", 2);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        plugin.profiles().add(player.getUniqueId(), "deaths", 1);
        plugin.setLastDeath(player.getUniqueId(), player.getLocation());
        player.sendMessage(Texts.color(plugin.prefix() + "&cLocal da morte salvo. Use &f/backdeath &cdepois de renascer."));
    }

    @EventHandler
    public void onMobDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();
        if (killer == null) return;

        if (plugin.bosses().isBoss(entity.getUniqueId())) {
            plugin.bosses().defeated(entity.getUniqueId(), killer);
            return;
        }
        if (!(entity instanceof Monster)) return;

        plugin.profiles().add(killer.getUniqueId(), "mob-kills", 1);
        plugin.quests().addProgress(killer, "cacador", 1);
        plugin.professions().addXp(killer, "cacador", 5);
        int coins = plugin.getConfig().getInt("coins.hostile-mob-kill", 2);
        if (plugin.events().is(EventManager.EventType.CACADA)) coins *= 2;
        plugin.profiles().add(killer.getUniqueId(), "coins", coins);
        if (Math.random() < 0.10) killer.sendMessage(Texts.color(plugin.prefix() + "&6+" + coins + " moedas &7por derrotar um inimigo."));
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(Texts.color("&0Amigos SMP"))) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;
        String name = ChatColor.stripColor(meta.getDisplayName());
        player.closeInventory();
        if (name.contains("Perfil")) player.performCommand("perfil");
        else if (name.contains("Explorar")) player.performCommand("rtp");
        else if (name.contains("Casa")) player.performCommand("home");
        else if (name.contains("Missões")) player.performCommand("missoes");
        else if (name.contains("Party")) player.performCommand("party list");
        else if (name.contains("Spawn")) player.performCommand("spawn");
        else if (name.contains("Profissão")) player.performCommand("profissao");
        else if (name.contains("Última Morte")) player.performCommand("backdeath");
        else if (name.contains("Respawn")) player.performCommand("respawn");
    }
}
