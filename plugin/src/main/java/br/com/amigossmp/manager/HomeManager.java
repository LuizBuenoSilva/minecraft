package br.com.amigossmp.manager;

import br.com.amigossmp.AmigosSMPPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class HomeManager {
    private final AmigosSMPPlugin plugin;
    private final File file;
    private final YamlConfiguration data;
    public HomeManager(AmigosSMPPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "homes.yml");
        this.data = YamlConfiguration.loadConfiguration(file);
    }
    public int limit(Player player) { return player.hasPermission("amigossmp.admin") ? 20 : plugin.getConfig().getInt("homes.default-limit", 3); }
    public boolean setHome(Player player, String rawName) {
        String name = normalize(rawName);
        List<String> homes = getHomes(player.getUniqueId());
        if (!homes.contains(name) && homes.size() >= limit(player)) return false;
        String path = base(player.getUniqueId(), name);
        Location loc = player.getLocation();
        data.set(path + ".world", loc.getWorld().getName());
        data.set(path + ".x", loc.getX()); data.set(path + ".y", loc.getY()); data.set(path + ".z", loc.getZ());
        data.set(path + ".yaw", loc.getYaw()); data.set(path + ".pitch", loc.getPitch());
        save(); return true;
    }
    public Location getHome(UUID uuid, String rawName) {
        String path = base(uuid, normalize(rawName));
        String worldName = data.getString(path + ".world");
        if (worldName == null) return null;
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        return new Location(world, data.getDouble(path + ".x"), data.getDouble(path + ".y"), data.getDouble(path + ".z"), (float)data.getDouble(path + ".yaw"), (float)data.getDouble(path + ".pitch"));
    }
    public boolean deleteHome(UUID uuid, String rawName) {
        String path = base(uuid, normalize(rawName));
        if (!data.contains(path)) return false;
        data.set(path, null); save(); return true;
    }
    public List<String> getHomes(UUID uuid) {
        String path = "players." + uuid + ".homes";
        if (data.getConfigurationSection(path) == null) return new ArrayList<>();
        return new ArrayList<>(data.getConfigurationSection(path).getKeys(false));
    }
    private String base(UUID uuid, String name) { return "players." + uuid + ".homes." + name; }
    private String normalize(String name) {
        if (name == null || name.isBlank()) return "casa";
        String normalized = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "");
        if (normalized.isBlank()) return "casa";
        return normalized.length() > 20 ? normalized.substring(0, 20) : normalized;
    }
    private void save() {
        try { data.save(file); }
        catch (IOException e) { plugin.getLogger().severe("Não foi possível salvar homes.yml: " + e.getMessage()); }
    }
}
