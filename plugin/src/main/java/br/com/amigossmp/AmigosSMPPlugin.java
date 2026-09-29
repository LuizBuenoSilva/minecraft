package br.com.amigossmp;

import br.com.amigossmp.command.MainCommand;
import br.com.amigossmp.listener.GameListener;
import br.com.amigossmp.manager.*;
import br.com.amigossmp.util.Texts;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class AmigosSMPPlugin extends JavaPlugin {
    private HomeManager homes;
    private ProfileManager profiles;
    private TpaManager tpa;
    private PartyManager parties;
    private QuestManager quests;
    private ProfessionManager professions;
    private ItemManager items;
    private BossManager bosses;
    private EventManager events;
    private NavigatorManager navigator;
    private File locationsFile;
    private YamlConfiguration locations;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (!getDataFolder().exists()) getDataFolder().mkdirs();
        homes = new HomeManager(this);
        profiles = new ProfileManager(this);
        tpa = new TpaManager(this);
        parties = new PartyManager();
        quests = new QuestManager(this);
        professions = new ProfessionManager(this);
        items = new ItemManager();
        bosses = new BossManager(this);
        events = new EventManager(this);
        navigator = new NavigatorManager(this);
        locationsFile = new File(getDataFolder(), "locations.yml");
        locations = YamlConfiguration.loadConfiguration(locationsFile);

        MainCommand executor = new MainCommand(this);
        for (String command : List.of("amigos","mapa","marcar","desmarcar","rota","tpa","tpaccept","tpdeny","sethome","home","delhome","spawn","setspawn","respawn","backdeath","rtp","perfil","moedas","party","missoes","profissao","boss","evento","itens")) {
            Objects.requireNonNull(getCommand(command)).setExecutor(executor);
        }
        Bukkit.getPluginManager().registerEvents(new GameListener(this), this);
        if (getConfig().getBoolean("survival.keep-inventory", true)) {
            for (World world : Bukkit.getWorlds()) {
                world.setGameRule(org.bukkit.GameRule.KEEP_INVENTORY, true);
            }
        }
        events.scheduleRandomEvents();
        getLogger().info("AmigosSMP 1.3.0 ativado. Navegação Survival+ pronta!");
    }

    public String prefix() { return getConfig().getString("messages.prefix", "&8[&bAmigosSMP&8] &r"); }
    public HomeManager homes() { return homes; }
    public ProfileManager profiles() { return profiles; }
    public TpaManager tpa() { return tpa; }
    public PartyManager parties() { return parties; }
    public QuestManager quests() { return quests; }
    public ProfessionManager professions() { return professions; }
    public ItemManager items() { return items; }
    public BossManager bosses() { return bosses; }
    public EventManager events() { return events; }
    public NavigatorManager navigator() { return navigator; }

    public void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, Texts.color("&0Amigos SMP"));
        inv.setItem(10, item(Material.PLAYER_HEAD, "&bSeu Perfil", "&7Moedas, estatísticas e profissão."));
        inv.setItem(11, item(Material.COMPASS, "&aExplorar (/rtp)", "&7Encontre uma nova região."));
        inv.setItem(12, item(Material.RED_BED, "&eCasa (/home)", "&7Volte para sua home principal."));
        inv.setItem(13, item(Material.WRITABLE_BOOK, "&6Missões Diárias", "&7Objetivos com recompensas."));
        inv.setItem(14, item(Material.TOTEM_OF_UNDYING, "&dParty", "&7Veja seu grupo de amigos."));
        inv.setItem(15, item(Material.NETHER_STAR, "&6Spawn", "&7Volte ao ponto central."));
        inv.setItem(16, item(Material.IRON_PICKAXE, "&bProfissão", "&7Evolua jogando do seu jeito."));
        inv.setItem(19, item(Material.RECOVERY_COMPASS, "&cÚltima Morte (/backdeath)", "&7Volte ao último local onde morreu."));
        inv.setItem(20, item(Material.RESPAWN_ANCHOR, "&aRespawn (/respawn)", "&7Volte para sua cama ou âncora."));
        inv.setItem(22, item(Material.FILLED_MAP, "&6Mapa do Explorador (/mapa)", "&7Navegação, rotas e marcadores."));
        player.openInventory(inv);
    }

    private ItemStack item(Material type, String name, String... lore) {
        ItemStack item = new ItemStack(type);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Texts.color(name));
        meta.setLore(Arrays.stream(lore).map(Texts::color).toList());
        item.setItemMeta(meta);
        return item;
    }

    public void setSpawn(Location loc) {
        writeLocation("spawn", loc);
    }

    public Location getSpawn() {
        return readLocation("spawn");
    }

    public void setLastDeath(UUID uuid, Location loc) {
        writeLocation("last-death." + uuid, loc);
    }

    public Location getLastDeath(UUID uuid) {
        return readLocation("last-death." + uuid);
    }

    private void writeLocation(String path, Location loc) {
        locations.set(path + ".world", loc.getWorld().getName());
        locations.set(path + ".x", loc.getX());
        locations.set(path + ".y", loc.getY());
        locations.set(path + ".z", loc.getZ());
        locations.set(path + ".yaw", loc.getYaw());
        locations.set(path + ".pitch", loc.getPitch());
        try {
            locations.save(locationsFile);
        } catch (IOException e) {
            getLogger().severe("Não foi possível salvar locations.yml: " + e.getMessage());
        }
    }

    private Location readLocation(String path) {
        String name = locations.getString(path + ".world");
        if (name == null) return null;
        World world = Bukkit.getWorld(name);
        if (world == null) return null;
        return new Location(
                world,
                locations.getDouble(path + ".x"),
                locations.getDouble(path + ".y"),
                locations.getDouble(path + ".z"),
                (float) locations.getDouble(path + ".yaw"),
                (float) locations.getDouble(path + ".pitch")
        );
    }
}
