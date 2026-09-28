package br.com.amigossmp.manager;

import br.com.amigossmp.util.Texts;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.List;
import java.util.Random;

public class ItemManager {
    private final Random random = new Random();

    public ItemStack bossDrop() {
        return random.nextBoolean() ? guardianBlade() : explorerRelic();
    }

    public ItemStack guardianBlade() {
        ItemStack item = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Texts.color("&5&lLâmina do Guardião"));
        meta.setLore(List.of(
                Texts.color("&dRARIDADE: ÉPICA"),
                Texts.color("&7Forjada com a essência de um chefe do mundo."),
                Texts.color("&8Item exclusivo do Amigos SMP")
        ));
        meta.setUnbreakable(true);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack explorerRelic() {
        ItemStack item = new ItemStack(Material.HEART_OF_THE_SEA);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Texts.color("&b&lRelíquia do Explorador"));
        meta.setLore(List.of(
                Texts.color("&bRARIDADE: RARA"),
                Texts.color("&7Troféu obtido ao derrotar um Guardião Corrompido."),
                Texts.color("&8Guarde para futuras receitas especiais.")
        ));
        item.setItemMeta(meta);
        return item;
    }
}
