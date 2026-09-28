package br.com.amigossmp.util;

import org.bukkit.ChatColor;

public final class Texts {
    private Texts() {}
    public static String color(String value) {
        return ChatColor.translateAlternateColorCodes('&', value == null ? "" : value);
    }
}
