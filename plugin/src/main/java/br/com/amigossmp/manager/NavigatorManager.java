package br.com.amigossmp.manager;

import br.com.amigossmp.AmigosSMPPlugin;
import br.com.amigossmp.util.Texts;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class NavigatorManager {
    private final AmigosSMPPlugin plugin;
    private final File file;
    private final YamlConfiguration data;
    private final Map<UUID, RouteTarget> routes = new HashMap<>();

    public NavigatorManager(AmigosSMPPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "waypoints.yml");
        this.data = YamlConfiguration.loadConfiguration(file);
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void routeToLocation(Player player, String name, Location location) {
        if (location == null || location.getWorld() == null) {
            player.sendMessage(Texts.color(plugin.prefix() + "&cDestino indisponível."));
            return;
        }
        routes.put(player.getUniqueId(), RouteTarget.location(name, location.clone()));
        player.sendMessage(Texts.color(plugin.prefix() + "&6Rota iniciada: &f" + name + "&6."));
        updateCompass(player, location);
    }

    public void routeToPlayer(Player player, Player target) {
        if (player.equals(target)) {
            player.sendMessage(Texts.color(plugin.prefix() + "&cVocê já está no próprio destino."));
            return;
        }
        routes.put(player.getUniqueId(), RouteTarget.player(target.getName(), target.getUniqueId()));
        player.sendMessage(Texts.color(plugin.prefix() + "&6Rastreando jogador: &f" + target.getName() + "&6."));
        updateCompass(player, target.getLocation());
    }

    public boolean stop(Player player) {
        boolean existed = routes.remove(player.getUniqueId()) != null;
        player.setCompassTarget(player.getWorld().getSpawnLocation());
        return existed;
    }

    public boolean hasRoute(Player player) {
        return routes.containsKey(player.getUniqueId());
    }

    public String activeRouteName(Player player) {
        RouteTarget route = routes.get(player.getUniqueId());
        return route == null ? null : route.name;
    }

    public boolean saveWaypoint(Player player, String rawName) {
        String name = normalize(rawName);
        if (name.isBlank()) return false;
        String path = "players." + player.getUniqueId() + "." + name;
        writeLocation(path, player.getLocation());
        return true;
    }

    public boolean deleteWaypoint(Player player, String rawName) {
        String name = normalize(rawName);
        String path = "players." + player.getUniqueId() + "." + name;
        if (!data.contains(path)) return false;
        data.set(path, null);
        save();
        return true;
    }

    public Location getWaypoint(UUID uuid, String rawName) {
        return readLocation("players." + uuid + "." + normalize(rawName));
    }

    public List<String> getWaypoints(UUID uuid) {
        String path = "players." + uuid;
        if (data.getConfigurationSection(path) == null) return new ArrayList<>();
        return new ArrayList<>(data.getConfigurationSection(path).getKeys(false));
    }

    public void openMap(Player player) {
        Inventory inv = Bukkit.createInventory(null, 45, Texts.color("&8✦ &6Mapa do Explorador &8✦"));
        inv.setItem(4, item(Material.FILLED_MAP, "&6&lAMIGOS SMP", "&7Mapa de navegação estilo Skyrim", "&7Região: &f" + biomeName(player.getLocation()), "&7Dimensão: &f" + dimensionName(player.getWorld())));

        inv.setItem(10, item(Material.NETHER_STAR, "&eRota: Spawn", "&7Clique para navegar até o spawn."));
        inv.setItem(11, item(Material.RED_BED, "&aRota: Casa", "&7Usa sua home &fcasa&7."));
        inv.setItem(12, item(Material.RESPAWN_ANCHOR, "&bRota: Respawn", "&7Cama ou âncora de respawn."));
        inv.setItem(13, item(Material.RECOVERY_COMPASS, "&cRota: Última Morte", "&7Volte ao local da última morte."));
        inv.setItem(14, item(Material.PLAYER_HEAD, "&dRastrear Amigo", "&7Use &f/rota <jogador>&7."));
        inv.setItem(15, item(Material.COMPASS, "&6Rota Atual", hasRoute(player) ? "&f" + activeRouteName(player) : "&7Nenhuma rota ativa."));
        inv.setItem(16, item(Material.BARRIER, "&cParar Navegação", "&7Cancela a rota atual."));

        int slot = 18;
        for (String name : getWaypoints(player.getUniqueId())) {
            if (slot >= 36) break;
            Location loc = getWaypoint(player.getUniqueId(), name);
            if (loc == null) continue;
            inv.setItem(slot++, item(Material.LODESTONE, "&e✦ " + name,
                    "&7Clique para navegar.",
                    "&8" + loc.getWorld().getName() + "  " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ()));
        }

        inv.setItem(40, item(Material.NAME_TAG, "&fCriar Marcador", "&7Use &e/marcar <nome>&7 onde estiver."));
        inv.setItem(41, item(Material.PAPER, "&fLista de Marcadores", "&7Use &e/rota local <nome>&7."));
        player.openInventory(inv);
    }

    public void handleMapClick(Player player, ItemStack clicked) {
        if (clicked == null || clicked.getType() == Material.AIR || !clicked.hasItemMeta()) return;
        ItemMeta meta = clicked.getItemMeta();
        if (!meta.hasDisplayName()) return;
        String name = org.bukkit.ChatColor.stripColor(meta.getDisplayName());
        player.closeInventory();

        if (name.equals("Rota: Spawn")) player.performCommand("rota spawn");
        else if (name.equals("Rota: Casa")) player.performCommand("rota home");
        else if (name.equals("Rota: Respawn")) player.performCommand("rota respawn");
        else if (name.equals("Rota: Última Morte")) player.performCommand("rota death");
        else if (name.equals("Parar Navegação")) player.performCommand("rota off");
        else if (name.startsWith("✦ ")) player.performCommand("rota local " + name.substring(2));
    }

    public String biomeName(Location location) {
        String key = location.getBlock().getBiome().getKey().getKey();
        String[] parts = key.split("_");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (!out.isEmpty()) out.append(' ');
            out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return out.toString();
    }

    public String dimensionName(World world) {
        return switch (world.getEnvironment()) {
            case NETHER -> "Nether";
            case THE_END -> "The End";
            default -> "Mundo";
        };
    }

    private void tick() {
        for (Iterator<Map.Entry<UUID, RouteTarget>> it = routes.entrySet().iterator(); it.hasNext();) {
            Map.Entry<UUID, RouteTarget> entry = it.next();
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !player.isOnline()) {
                it.remove();
                continue;
            }

            RouteTarget route = entry.getValue();
            Location target;
            if (route.playerUuid != null) {
                Player tracked = Bukkit.getPlayer(route.playerUuid);
                if (tracked == null || !tracked.isOnline()) {
                    sendBar(player, "&c" + route.name + " está offline. Rota encerrada.");
                    it.remove();
                    continue;
                }
                target = tracked.getLocation();
            } else {
                target = route.location;
            }

            if (target == null || target.getWorld() == null) {
                it.remove();
                continue;
            }

            if (!player.getWorld().equals(target.getWorld())) {
                sendBar(player, "&6✦ " + route.name + " &8• &d" + dimensionName(target.getWorld()) + " &8• &7outra dimensão");
                continue;
            }

            double distance = player.getLocation().distance(target);
            if (distance <= 6.0) {
                sendBar(player, "&a✓ Destino alcançado: &f" + route.name);
                player.sendMessage(Texts.color(plugin.prefix() + "&aVocê chegou em &f" + route.name + "&a."));
                it.remove();
                continue;
            }

            updateCompass(player, target);
            String arrow = relativeArrow(player.getLocation(), target);
            String cardinal = cardinal(player.getLocation(), target);
            sendBar(player, "&8[&6" + arrow + "&8] &f" + route.name + " &8• &e" + Math.round(distance) + " blocos &8• &7" + cardinal);
        }
    }

    private void updateCompass(Player player, Location target) {
        if (player.getWorld().equals(target.getWorld())) player.setCompassTarget(target);
    }

    private String relativeArrow(Location from, Location to) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
        double diff = normalizeAngle(targetYaw - from.getYaw());
        if (diff >= -22.5 && diff < 22.5) return "↑";
        if (diff >= 22.5 && diff < 67.5) return "↖";
        if (diff >= 67.5 && diff < 112.5) return "←";
        if (diff >= 112.5 && diff < 157.5) return "↙";
        if (diff >= 157.5 || diff < -157.5) return "↓";
        if (diff >= -157.5 && diff < -112.5) return "↘";
        if (diff >= -112.5 && diff < -67.5) return "→";
        return "↗";
    }

    private String cardinal(Location from, Location to) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double angle = Math.toDegrees(Math.atan2(-dx, dz));
        angle = (angle + 360.0) % 360.0;
        if (angle < 22.5 || angle >= 337.5) return "Sul";
        if (angle < 67.5) return "Sudoeste";
        if (angle < 112.5) return "Oeste";
        if (angle < 157.5) return "Noroeste";
        if (angle < 202.5) return "Norte";
        if (angle < 247.5) return "Nordeste";
        if (angle < 292.5) return "Leste";
        return "Sudeste";
    }

    private double normalizeAngle(double angle) {
        while (angle <= -180.0) angle += 360.0;
        while (angle > 180.0) angle -= 360.0;
        return angle;
    }

    private void sendBar(Player player, String text) {
        player.sendActionBar(LegacyComponentSerializer.legacyAmpersand().deserialize(text));
    }

    private ItemStack item(Material type, String name, String... lore) {
        ItemStack item = new ItemStack(type);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Texts.color(name));
        meta.setLore(Arrays.stream(lore).map(Texts::color).toList());
        item.setItemMeta(meta);
        return item;
    }

    private void writeLocation(String path, Location loc) {
        data.set(path + ".world", loc.getWorld().getName());
        data.set(path + ".x", loc.getX());
        data.set(path + ".y", loc.getY());
        data.set(path + ".z", loc.getZ());
        data.set(path + ".yaw", loc.getYaw());
        data.set(path + ".pitch", loc.getPitch());
        save();
    }

    private Location readLocation(String path) {
        String worldName = data.getString(path + ".world");
        if (worldName == null) return null;
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        return new Location(world, data.getDouble(path + ".x"), data.getDouble(path + ".y"), data.getDouble(path + ".z"),
                (float) data.getDouble(path + ".yaw"), (float) data.getDouble(path + ".pitch"));
    }

    private String normalize(String raw) {
        if (raw == null) return "";
        String normalized = raw.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "");
        return normalized.length() > 24 ? normalized.substring(0, 24) : normalized;
    }

    private void save() {
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Não foi possível salvar waypoints.yml: " + e.getMessage());
        }
    }

    private static class RouteTarget {
        private final String name;
        private final Location location;
        private final UUID playerUuid;

        private RouteTarget(String name, Location location, UUID playerUuid) {
            this.name = name;
            this.location = location;
            this.playerUuid = playerUuid;
        }

        static RouteTarget location(String name, Location location) {
            return new RouteTarget(name, location, null);
        }

        static RouteTarget player(String name, UUID uuid) {
            return new RouteTarget(name, null, uuid);
        }
    }
}
