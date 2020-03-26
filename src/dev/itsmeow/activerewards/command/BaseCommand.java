package dev.itsmeow.activerewards.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import com.google.common.base.Functions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import dev.itsmeow.activerewards.ActiveRewards;
import net.md_5.bungee.api.ChatColor;

public abstract class BaseCommand {

    protected static ActiveRewards plugin;

    public static void setPlugin(ActiveRewards instance) {
        plugin = instance;
    }

    private String primaryName;
    private String[] aliases;

    public BaseCommand(String primaryName, String... aliases) {
        this.primaryName = primaryName;
        this.aliases = aliases;
    }

    public abstract boolean onCommand(CommandSender sender, String[] args);

    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return new ArrayList<String>();
    }

    public String getPrimaryName() {
        return primaryName;
    }

    public String[] getAliases() {
        return aliases;
    }

    public abstract String getInfo();

    public abstract String getUsage();

    public abstract String getPermissionString();

    public boolean mustBePlayer() {
        return false;
    }

    public void syncMessage(CommandSender sender, String... messages) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            for(String message : messages) {
                sender.sendMessage(message);
            }
        });
    }

    public void sync(Runnable run) {
        Bukkit.getScheduler().runTask(plugin, run);
    }

    public void printInvalidUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.RED + "Incorrect usage! Usage: " + ChatColor.WHITE + this.getUsage());
    }

    public static List<String> getListOfStringsMatchingLastWord(String[] args, String... possibilities) {
        return getListOfStringsMatchingLastWord(args, Arrays.asList(possibilities));
    }

    public static List<String> getListOfStringsMatchingLastWord(String[] inputArgs, Collection<?> possibleCompletions) {
        String s = inputArgs[inputArgs.length - 1];
        List<String> list = Lists.<String>newArrayList();

        if(!possibleCompletions.isEmpty()) {
            for(String s1 : Iterables.transform(possibleCompletions, Functions.toStringFunction())) {
                if(doesStringStartWith(s, s1)) {
                    list.add(s1);
                }
            }
        }

        return list;
    }

    public static boolean doesStringStartWith(String original, String region) {
        return region.regionMatches(true, 0, original, 0, original.length());
    }

}