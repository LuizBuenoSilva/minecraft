package br.com.amigossmp.command;

import br.com.amigossmp.AmigosSMPPlugin;
import br.com.amigossmp.manager.EventManager;
import br.com.amigossmp.manager.PartyManager;
import br.com.amigossmp.util.Texts;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

public class MainCommand implements CommandExecutor {
    private final AmigosSMPPlugin plugin;
    private final Map<UUID, Long> rtpCooldown = new HashMap<>();

    public MainCommand(AmigosSMPPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Texts.color(plugin.prefix() + plugin.getConfig().getString("messages.player-only")));
            return true;
        }
        return switch (command.getName().toLowerCase(Locale.ROOT)) {
            case "amigos" -> { plugin.openMainMenu(player); yield true; }
            case "tpa" -> tpa(player, args);
            case "tpaccept" -> tpAccept(player);
            case "tpdeny" -> tpDeny(player);
            case "sethome" -> setHome(player, args);
            case "home" -> home(player, args);
            case "delhome" -> delHome(player, args);
            case "spawn" -> spawn(player);
            case "setspawn" -> setSpawn(player);
            case "respawn" -> respawn(player);
            case "backdeath" -> backDeath(player);
            case "rtp" -> rtp(player);
            case "perfil" -> profile(player, args);
            case "moedas" -> coins(player);
            case "party" -> party(player, args);
            case "missoes" -> missions(player);
            case "profissao" -> profession(player, args);
            case "boss" -> boss(player, args);
            case "evento" -> event(player, args);
            case "itens" -> items(player);
            default -> false;
        };
    }

    private boolean tpa(Player player, String[] args) {
        if (args.length < 1) { player.sendMessage(c("&eUse: /tpa <jogador>")); return true; }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || !target.isOnline()) { player.sendMessage(c("&cJogador não encontrado.")); return true; }
        if (target.equals(player)) { player.sendMessage(c("&cVocê já está com você mesmo 😄")); return true; }
        plugin.tpa().request(player, target);
        player.sendMessage(c("&aPedido de teleporte enviado para &f" + target.getName() + "&a."));
        target.sendMessage(c("&b" + player.getName() + " &equer teleportar até você. &a/tpaccept &7ou &c/tpdeny"));
        return true;
    }

    private boolean tpAccept(Player player) {
        Player requester = plugin.tpa().getRequester(player);
        if (requester == null) { player.sendMessage(c("&cVocê não tem pedidos pendentes.")); return true; }
        plugin.tpa().clear(player);
        int seconds = plugin.getConfig().getInt("teleport.warmup-seconds", 3);
        Location start = requester.getLocation().clone();
        requester.sendMessage(c("&aPedido aceito! Teleportando em &f" + seconds + "s&a. Não se mova."));
        player.sendMessage(c("&aVocê aceitou o pedido de &f" + requester.getName() + "&a."));
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!requester.isOnline() || !player.isOnline()) return;
            if (!requester.getWorld().equals(start.getWorld()) || requester.getLocation().distanceSquared(start) > 4) {
                requester.sendMessage(c("&cTeleporte cancelado porque você se moveu."));
                return;
            }
            requester.teleportAsync(player.getLocation());
            requester.sendMessage(c("&aTeleportado até &f" + player.getName() + "&a."));
        }, seconds * 20L);
        return true;
    }

    private boolean tpDeny(Player player) {
        Player requester = plugin.tpa().getRequester(player);
        if (requester == null) { player.sendMessage(c("&cVocê não tem pedidos pendentes.")); return true; }
        plugin.tpa().clear(player);
        requester.sendMessage(c("&c" + player.getName() + " recusou seu pedido de teleporte."));
        player.sendMessage(c("&7Pedido recusado."));
        return true;
    }

    private boolean setHome(Player player, String[] args) {
        String name = args.length > 0 ? args[0] : "casa";
        if (!plugin.homes().setHome(player, name)) player.sendMessage(c("&cLimite de homes atingido. Limite: &f" + plugin.homes().limit(player)));
        else player.sendMessage(c("&aHome &f" + name + " &asalva."));
        return true;
    }

    private boolean home(Player player, String[] args) {
        String name = args.length > 0 ? args[0] : "casa";
        Location loc = plugin.homes().getHome(player.getUniqueId(), name);
        if (loc == null) {
            player.sendMessage(c("&cHome não encontrada. Suas homes: &f" + String.join(", ", plugin.homes().getHomes(player.getUniqueId()))));
            return true;
        }
        player.teleportAsync(loc);
        player.sendMessage(c("&aBem-vindo de volta à home &f" + name + "&a."));
        return true;
    }

    private boolean delHome(Player player, String[] args) {
        String name = args.length > 0 ? args[0] : "casa";
        player.sendMessage(c(plugin.homes().deleteHome(player.getUniqueId(), name) ? "&aHome removida." : "&cHome não encontrada."));
        return true;
    }

    private boolean spawn(Player player) {
        Location loc = plugin.getSpawn();
        if (loc == null) loc = player.getWorld().getSpawnLocation();
        player.teleportAsync(loc);
        player.sendMessage(c("&aTeleportado para o spawn."));
        return true;
    }

    private boolean setSpawn(Player player) {
        if (!player.hasPermission("amigossmp.admin")) { player.sendMessage(c("&cApenas administradores.")); return true; }
        plugin.setSpawn(player.getLocation());
        player.sendMessage(c("&aSpawn definido aqui."));
        return true;
    }

    private boolean respawn(Player player) {
        Location loc = player.getRespawnLocation();
        if (loc == null) {
            loc = player.getWorld().getSpawnLocation();
            player.sendMessage(c("&eVocê não tem cama/âncora definida. Indo para o spawn do mundo."));
        } else {
            player.sendMessage(c("&aTeleportando para seu ponto de respawn."));
        }
        player.teleportAsync(loc);
        return true;
    }

    private boolean backDeath(Player player) {
        Location loc = plugin.getLastDeath(player.getUniqueId());
        if (loc == null) {
            player.sendMessage(c("&cAinda não há uma última morte salva."));
            return true;
        }
        player.teleportAsync(loc);
        player.sendMessage(c("&cVocê voltou ao local da sua última morte."));
        return true;
    }

    private boolean rtp(Player player) {
        long now = System.currentTimeMillis();
        long cooldownMs = plugin.getConfig().getLong("rtp.cooldown-seconds", 120) * 1000L;
        long last = rtpCooldown.getOrDefault(player.getUniqueId(), 0L);
        if (!player.hasPermission("amigossmp.admin") && now - last < cooldownMs) {
            long remaining = Math.max(1, (cooldownMs - (now - last)) / 1000L);
            player.sendMessage(c("&cAguarde &f" + remaining + "s &cpara usar /rtp novamente."));
            return true;
        }

        World world = player.getWorld();
        int min = plugin.getConfig().getInt("rtp.min-radius", 500);
        int max = plugin.getConfig().getInt("rtp.max-radius", 5000);
        int attempts = plugin.getConfig().getInt("rtp.attempts", 40);
        Random random = new Random();

        for (int i = 0; i < attempts; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            int radius = min + random.nextInt(Math.max(1, max - min));
            int x = (int)Math.round(Math.cos(angle) * radius);
            int z = (int)Math.round(Math.sin(angle) * radius);
            Block top = world.getHighestBlockAt(x, z);
            Material ground = top.getType();
            if (ground.isSolid() && ground != Material.LAVA && ground != Material.MAGMA_BLOCK && ground != Material.CACTUS) {
                Location target = top.getLocation().add(0.5, 1, 0.5);
                rtpCooldown.put(player.getUniqueId(), now);
                player.teleportAsync(target);
                plugin.quests().addProgress(player, "explorador", 1);
                int xp = plugin.events().is(EventManager.EventType.AVENTURA) ? 20 : 10;
                plugin.professions().addXp(player, "explorador", xp);
                player.sendMessage(c("&aExploração iniciada! Você foi enviado para uma região aleatória."));
                return true;
            }
        }
        player.sendMessage(c("&cNão encontrei um local seguro. Tente novamente."));
        return true;
    }

    private boolean profile(Player viewer, String[] args) {
        Player target = args.length > 0 ? Bukkit.getPlayerExact(args[0]) : viewer;
        if (target == null) { viewer.sendMessage(c("&cJogador não encontrado.")); return true; }
        UUID id = target.getUniqueId();
        viewer.sendMessage(c("&8&m---------------- &bPerfil &8&m----------------"));
        viewer.sendMessage(c("&fJogador: &b" + target.getName()));
        viewer.sendMessage(c("&fMoedas: &6" + plugin.profiles().get(id, "coins")));
        viewer.sendMessage(c("&fMobs derrotados: &c" + plugin.profiles().get(id, "mob-kills")));
        viewer.sendMessage(c("&fMortes: &7" + plugin.profiles().get(id, "deaths")));
        viewer.sendMessage(c("&fBlocos quebrados: &a" + plugin.profiles().get(id, "blocks-broken")));
        viewer.sendMessage(c("&fProfissão: &d" + plugin.professions().get(id) + " &7nível " + plugin.professions().level(id)));
        viewer.sendMessage(c("&8&m-----------------------------------------"));
        return true;
    }

    private boolean coins(Player player) {
        player.sendMessage(c("&6Você possui &e" + plugin.profiles().get(player.getUniqueId(), "coins") + " moedas&6."));
        return true;
    }

    private boolean party(Player player, String[] args) {
        if (args.length == 0) {
            player.sendMessage(c("&e/party create | invite <jogador> | accept | leave | list | chat <mensagem>"));
            return true;
        }
        PartyManager pm = plugin.parties();
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "create" -> {
                pm.create(player.getUniqueId());
                player.sendMessage(c("&aParty criada. Use &f/party invite <jogador>&a."));
            }
            case "invite" -> {
                if (args.length < 2) { player.sendMessage(c("&eUse: /party invite <jogador>")); break; }
                PartyManager.Party party = pm.get(player.getUniqueId());
                if (party == null) party = pm.create(player.getUniqueId());
                if (!party.leader().equals(player.getUniqueId())) { player.sendMessage(c("&cSomente o líder pode convidar.")); break; }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) { player.sendMessage(c("&cJogador não encontrado.")); break; }
                pm.invite(player.getUniqueId(), target.getUniqueId());
                target.sendMessage(c("&b" + player.getName() + " &econvidou você para uma party. Use &a/party accept"));
                player.sendMessage(c("&aConvite enviado."));
            }
            case "accept" -> player.sendMessage(c(pm.accept(player.getUniqueId()) ? "&aVocê entrou na party!" : "&cNenhum convite pendente."));
            case "leave" -> {
                pm.leave(player.getUniqueId());
                player.sendMessage(c("&7Você saiu da party."));
            }
            case "list" -> {
                PartyManager.Party party = pm.get(player.getUniqueId());
                if (party == null) { player.sendMessage(c("&cVocê não está em uma party.")); break; }
                List<String> names = party.members().stream()
                        .map(id -> Optional.ofNullable(Bukkit.getOfflinePlayer(id).getName()).orElse(id.toString().substring(0, 8)))
                        .toList();
                player.sendMessage(c("&bParty: &f" + String.join(", ", names)));
            }
            case "chat" -> {
                PartyManager.Party party = pm.get(player.getUniqueId());
                if (party == null) { player.sendMessage(c("&cVocê não está em uma party.")); break; }
                if (args.length < 2) { player.sendMessage(c("&eUse: /party chat <mensagem>")); break; }
                String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                for (UUID member : party.members()) {
                    Player online = Bukkit.getPlayer(member);
                    if (online != null) online.sendMessage(c("&8[&bPARTY&8] &f" + player.getName() + "&7: &f" + message));
                }
            }
            default -> player.sendMessage(c("&e/party create | invite <jogador> | accept | leave | list | chat <mensagem>"));
        }
        return true;
    }

    private boolean missions(Player player) {
        plugin.quests().sendStatus(player);
        return true;
    }

    private boolean profession(Player player, String[] args) {
        if (args.length == 0) {
            player.sendMessage(c("&dProfissão atual: &f" + plugin.professions().get(player.getUniqueId())
                    + " &8• &7nível " + plugin.professions().level(player.getUniqueId())
                    + " &8• &7XP " + plugin.professions().xp(player.getUniqueId())));
            player.sendMessage(c("&eUse: /profissao escolher <minerador|cacador|fazendeiro|explorador>"));
            return true;
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("escolher")) {
            if (plugin.professions().choose(player, args[1])) player.sendMessage(c("&aProfissão escolhida: &f" + args[1].toLowerCase(Locale.ROOT) + "&a."));
            else player.sendMessage(c("&cProfissão inválida. Use minerador, cacador, fazendeiro ou explorador."));
            return true;
        }
        player.sendMessage(c("&eUse: /profissao escolher <minerador|cacador|fazendeiro|explorador>"));
        return true;
    }

    private boolean boss(Player player, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("summon")) {
            if (!player.hasPermission("amigossmp.admin")) { player.sendMessage(c("&cApenas administradores.")); return true; }
            plugin.bosses().spawn(player.getLocation().add(player.getLocation().getDirection().multiply(5)));
            return true;
        }
        player.sendMessage(c("&5Bosses ativos: &f" + plugin.bosses().activeCount()));
        return true;
    }

    private boolean event(Player player, String[] args) {
        if (args.length >= 2 && args[0].equalsIgnoreCase("iniciar")) {
            if (!player.hasPermission("amigossmp.admin")) { player.sendMessage(c("&cApenas administradores.")); return true; }
            if (!plugin.events().start(args[1])) player.sendMessage(c("&cEvento inválido. Use cacada, mineracao ou aventura."));
            return true;
        }
        if (plugin.events().active() == null) player.sendMessage(c("&7Nenhum evento está ativo agora."));
        else player.sendMessage(c("&eEvento ativo: &f" + plugin.events().active().name() + " &8• &7" + plugin.events().remainingSeconds() + "s restantes"));
        return true;
    }

    private boolean items(Player player) {
        player.sendMessage(c("&8&m---------------- &dItens Especiais &8&m----------------"));
        player.sendMessage(c("&5Lâmina do Guardião &8- &7raridade ÉPICA, drop de boss."));
        player.sendMessage(c("&bRelíquia do Explorador &8- &7raridade RARA, usada em futuras receitas."));
        return true;
    }

    private String c(String msg) { return Texts.color(plugin.prefix() + msg); }
}
