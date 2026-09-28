package br.com.amigossmp.manager;

import br.com.amigossmp.AmigosSMPPlugin;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class ProfileManager {
    private final AmigosSMPPlugin plugin;
    private final File file;
    private final YamlConfiguration data;
    public ProfileManager(AmigosSMPPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "profiles.yml");
        this.data = YamlConfiguration.loadConfiguration(file);
    }
    public int get(UUID uuid, String stat) { return data.getInt("players." + uuid + "." + stat, 0); }
    public void add(UUID uuid, String stat, int amount) {
        String path = "players." + uuid + "." + stat;
        data.set(path, data.getInt(path, 0) + amount);
        save();
    }
    public void setName(UUID uuid, String name) {
        data.set("players." + uuid + ".name", name);
        save();
    }
    private void save() {
        try { data.save(file); }
        catch (IOException e) { plugin.getLogger().severe("Não foi possível salvar profiles.yml: " + e.getMessage()); }
    }
}
