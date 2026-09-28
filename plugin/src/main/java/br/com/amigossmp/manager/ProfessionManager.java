package br.com.amigossmp.manager;

import br.com.amigossmp.AmigosSMPPlugin;
import br.com.amigossmp.util.Texts;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class ProfessionManager {
    private static final Set<String> VALID = Set.of("minerador", "cacador", "fazendeiro", "explorador");
    private final AmigosSMPPlugin plugin;
    private final File file;
    private final YamlConfiguration data;

    public ProfessionManager(AmigosSMPPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "professions.yml");
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    public boolean choose(Player player, String profession) {
        String normalized = profession.toLowerCase(Locale.ROOT);
        if (!VALID.contains(normalized)) return false;
        data.set(path(player.getUniqueId(), "name"), normalized);
        data.set(path(player.getUniqueId(), "xp"), 0);
        save();
        return true;
    }

    public String get(UUID uuid) { return data.getString(path(uuid, "name"), "nenhuma"); }
    public int xp(UUID uuid) { return data.getInt(path(uuid, "xp"), 0); }
    public int level(UUID uuid) { return 1 + (xp(uuid) / 100); }

    public void addXp(Player player, String expectedProfession, int amount) {
        if (!get(player.getUniqueId()).equalsIgnoreCase(expectedProfession)) return;
        int oldLevel = level(player.getUniqueId());
        String path = path(player.getUniqueId(), "xp");
        data.set(path, data.getInt(path, 0) + amount);
        save();
        int newLevel = level(player.getUniqueId());
        if (newLevel > oldLevel) {
            int reward = 20 + (newLevel * 5);
            plugin.profiles().add(player.getUniqueId(), "coins", reward);
            player.sendMessage(Texts.color(plugin.prefix() + "&dSua profissão subiu para o nível &f" + newLevel + "&d! &6+" + reward + " moedas"));
        }
    }

    private String path(UUID uuid, String field) { return "players." + uuid + "." + field; }
    private void save() {
        try { data.save(file); }
        catch (IOException e) { plugin.getLogger().severe("Não foi possível salvar professions.yml: " + e.getMessage()); }
    }
}
