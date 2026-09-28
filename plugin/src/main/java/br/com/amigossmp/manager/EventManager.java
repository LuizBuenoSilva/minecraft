package br.com.amigossmp.manager;

import br.com.amigossmp.AmigosSMPPlugin;
import br.com.amigossmp.util.Texts;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class EventManager {
    public enum EventType { CACADA, MINERACAO, AVENTURA }
    private final AmigosSMPPlugin plugin;
    private final Random random = new Random();
    private EventType active;
    private long endsAt;
    private BukkitTask endTask;

    public EventManager(AmigosSMPPlugin plugin) { this.plugin = plugin; }

    public void scheduleRandomEvents() {
        int minutes = Math.max(10, plugin.getConfig().getInt("events.interval-minutes", 45));
        long ticks = minutes * 60L * 20L;
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (active == null) start(List.of(EventType.values()).get(random.nextInt(EventType.values().length)));
        }, ticks, ticks);
    }

    public boolean start(String name) {
        try { start(EventType.valueOf(name.toUpperCase(Locale.ROOT))); return true; }
        catch (IllegalArgumentException ex) { return false; }
    }

    public void start(EventType type) {
        if (endTask != null) endTask.cancel();
        active = type;
        int minutes = Math.max(1, plugin.getConfig().getInt("events.duration-minutes", 10));
        endsAt = System.currentTimeMillis() + minutes * 60_000L;
        String description = switch (type) {
            case CACADA -> "monstros dão moedas em dobro";
            case MINERACAO -> "mineradores recebem XP de profissão em dobro";
            case AVENTURA -> "exploradores recebem XP extra ao usar /rtp";
        };
        Bukkit.broadcastMessage(Texts.color(plugin.prefix() + "&e&lEVENTO: &f" + type.name() + " &8• &7" + description + " &8(" + minutes + " min)"));
        endTask = Bukkit.getScheduler().runTaskLater(plugin, this::stop, minutes * 60L * 20L);
    }

    public void stop() {
        if (active != null) Bukkit.broadcastMessage(Texts.color(plugin.prefix() + "&7O evento &f" + active.name() + " &7terminou."));
        active = null;
        endsAt = 0;
        endTask = null;
    }

    public EventType active() { return active; }
    public boolean is(EventType type) { return active == type; }
    public long remainingSeconds() { return active == null ? 0 : Math.max(0, (endsAt - System.currentTimeMillis()) / 1000L); }
}
