package br.com.amigossmp.manager;

import br.com.amigossmp.AmigosSMPPlugin;
import br.com.amigossmp.util.Texts;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class QuestManager {
    public record Quest(String id, String name, String description, int goal, int reward) {}
    private final AmigosSMPPlugin plugin;
    private final File file;
    private final YamlConfiguration data;
    private final Map<String, Quest> quests = new LinkedHashMap<>();

    public QuestManager(AmigosSMPPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "quests.yml");
        this.data = YamlConfiguration.loadConfiguration(file);
        quests.put("minerador", new Quest("minerador", "Mãos à Obra", "Quebre 64 blocos.", 64, 40));
        quests.put("cacador", new Quest("cacador", "Caçada", "Derrote 12 monstros hostis.", 12, 55));
        quests.put("explorador", new Quest("explorador", "Horizonte Novo", "Use /rtp para explorar uma nova região.", 1, 35));
    }

    public void addProgress(Player player, String questId, int amount) {
        Quest quest = quests.get(questId);
        if (quest == null) return;
        String base = base(player.getUniqueId(), questId);
        if (data.getBoolean(base + ".claimed", false)) return;
        int before = data.getInt(base + ".progress", 0);
        int after = Math.min(quest.goal(), before + amount);
        data.set(base + ".progress", after);
        if (after >= quest.goal() && before < quest.goal()) {
            data.set(base + ".claimed", true);
            plugin.profiles().add(player.getUniqueId(), "coins", quest.reward());
            player.sendMessage(Texts.color(plugin.prefix() + "&aMissão concluída: &f" + quest.name() + " &8• &6+" + quest.reward() + " moedas"));
        }
        save();
    }

    public void sendStatus(Player player) {
        player.sendMessage(Texts.color("&8&m---------------- &bMissões Diárias &8&m----------------"));
        for (Quest quest : quests.values()) {
            String base = base(player.getUniqueId(), quest.id());
            int progress = data.getInt(base + ".progress", 0);
            boolean claimed = data.getBoolean(base + ".claimed", false);
            String state = claimed ? "&aCONCLUÍDA" : "&e" + progress + "/" + quest.goal();
            player.sendMessage(Texts.color("&f" + quest.name() + " &8- &7" + quest.description() + " &8[" + state + "&8] &6" + quest.reward() + " moedas"));
        }
        player.sendMessage(Texts.color("&7As missões reiniciam automaticamente a cada novo dia."));
    }

    private String base(UUID uuid, String questId) {
        return "days." + LocalDate.now() + ".players." + uuid + "." + questId;
    }

    private void save() {
        try { data.save(file); }
        catch (IOException e) { plugin.getLogger().severe("Não foi possível salvar quests.yml: " + e.getMessage()); }
    }
}
