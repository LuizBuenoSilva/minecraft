package br.com.amigossmp.manager;

import br.com.amigossmp.AmigosSMPPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TpaManager {
    private final AmigosSMPPlugin plugin;
    private final Map<UUID, Request> requests = new HashMap<>();
    public TpaManager(AmigosSMPPlugin plugin) { this.plugin = plugin; }
    public void request(Player from, Player to) {
        long expires = System.currentTimeMillis() + plugin.getConfig().getLong("teleport.request-expire-seconds", 60) * 1000L;
        requests.put(to.getUniqueId(), new Request(from.getUniqueId(), expires));
    }
    public Player getRequester(Player target) {
        Request req = requests.get(target.getUniqueId());
        if (req == null || req.expiresAt() < System.currentTimeMillis()) {
            requests.remove(target.getUniqueId());
            return null;
        }
        return Bukkit.getPlayer(req.from());
    }
    public void clear(Player target) { requests.remove(target.getUniqueId()); }
    private record Request(UUID from, long expiresAt) {}
}
