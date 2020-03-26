package dev.itsmeow.activerewards.command.subcommands;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dev.itsmeow.activerewards.command.BaseCommand;
import net.md_5.bungee.api.ChatColor;

public class ActiveRewardsInfoCommand extends BaseCommand {

    public ActiveRewardsInfoCommand() {
        super("info", "amount", "playtime");
    }

    @Override
    public boolean onCommand(CommandSender sender, String[] args) {
        if(args.length == 0) {
            if(sender instanceof Player) {
                plugin.getSQL().queueAction(() -> {
                    try {
                        if(plugin.showMultipleServers) {
                            display(sender, sender.getName(), plugin.getSQL().execQueryBlocking("SELECT server,COUNT(CASE WHEN multiplier>0 THEN 1 ELSE NULL END),SUM(CASE WHEN NOW()<expires THEN multiplier ELSE 0 END),SUM(multiplier) FROM " + plugin.userTable + " WHERE uuid='" + ((Player) sender).getUniqueId() + "' GROUP BY server;"));
                        } else {
                            display(sender, sender.getName(), plugin.getSQL().execQueryBlocking("SELECT COUNT(CASE WHEN multiplier>0 THEN 1 ELSE NULL END),SUM(CASE WHEN NOW()<expires THEN multiplier ELSE 0 END),SUM(multiplier) FROM " + plugin.userTable + " WHERE uuid='" + ((Player) sender).getUniqueId() + "';"));
                        }
                    }
                    catch(SQLException e) {
                        e.printStackTrace();
                    }
                });
            } else {
                sender.sendMessage("Non-players must specify a playername or uuid!");
            }
        } else if(args.length == 1) {
            if(args[0].equals(sender.getName()) || sender.hasPermission("activerewards.info.others")) {
                plugin.getSQL().queueAction(() -> {
                    try {
                        if(plugin.showMultipleServers) {
                            display(sender, args[0], plugin.getSQL().execQueryBlocking("SELECT server,COUNT(CASE WHEN multiplier>0 THEN 1 ELSE NULL END),SUM(CASE WHEN NOW()<expires THEN multiplier ELSE 0 END),SUM(multiplier) FROM " + plugin.userTable + " WHERE uuid=(SELECT uuid FROM " + plugin.uuidTable + " WHERE name='" + args[0] + "') GROUP BY server;"));
                        } else {
                            display(sender, args[0], plugin.getSQL().execQueryBlocking("SELECT COUNT(CASE WHEN multiplier>0 THEN 1 ELSE NULL END),SUM(CASE WHEN NOW()<expires THEN multiplier ELSE 0 END),SUM(multiplier) FROM " + plugin.userTable + " WHERE uuid=(SELECT uuid FROM " + plugin.uuidTable + " WHERE name='" + args[0] + "');"));
                        }
                    } catch(SQLException e) {
                        e.printStackTrace();
                    }
                });
            } else {
                sender.sendMessage(ChatColor.RED + "You do not have permission to get the Active Rewards info for other users!");
            }
        } else {
            printInvalidUsage(sender);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if(args.length == 1) {
            return getListOfStringsMatchingLastWord(args, Bukkit.getOnlinePlayers().stream().map(p -> p.getName()).collect(Collectors.toCollection(ArrayList::new)));
        } else {
            return super.onTabComplete(sender, args);
        }
    }

    private void display(CommandSender sender, String name, ResultSet result) throws SQLException {
        if(plugin.showMultipleServers) {
            Map<String, Integer> playtime = new HashMap<String, Integer>();
            Map<String, Integer> points = new HashMap<String, Integer>();
            Map<String, Integer> historicPoints = new HashMap<String, Integer>();
            while(result.next()) {
                playtime.put(result.getString(1), result.getInt(2));
                points.put(result.getString(1), result.getInt(3));
                historicPoints.put(result.getString(1), result.getInt(4));
            }
            result.close();
            int totalPlaytime = playtime.values().stream().reduce(0, Integer::sum);
            int currentTotalPoints = points.values().stream().reduce(0, Integer::sum);
            int historicTotalPoints = historicPoints.values().stream().reduce(0, Integer::sum);
            List<String> messages = new ArrayList<String>();
            BiConsumer<String, Integer> ptConsumer = (server, p) -> messages.add(ChatColor.YELLOW + "  from " + ChatColor.GREEN + server + ChatColor.YELLOW + ": " + ChatColor.AQUA + p + " points");
            messages.add(ChatColor.YELLOW + "" + ChatColor.BOLD + "Active Rewards info for " + ChatColor.AQUA + "" + ChatColor.BOLD + name + ChatColor.YELLOW + "" + ChatColor.BOLD + ":");
            messages.add(ChatColor.YELLOW + "Total Playtime: " + ChatColor.AQUA + minutesFormat(totalPlaytime));
            playtime.forEach((server, p) -> messages.add(ChatColor.YELLOW + "  from " + ChatColor.GREEN + server + ChatColor.YELLOW + ": " + ChatColor.AQUA + minutesFormat(p)));
            messages.add(ChatColor.YELLOW + "Current Total Points: " + ChatColor.AQUA + currentTotalPoints + " points");
            points.forEach(ptConsumer);
            messages.add(ChatColor.YELLOW + "Historic Total Points: " + ChatColor.AQUA + historicTotalPoints + " points");
            historicPoints.forEach(ptConsumer);
            syncMessage(sender, messages.toArray(new String[0]));
        } else {
            result.next();
            int playtime = result.getInt(1);
            int points = result.getInt(2);
            int historicPoints = result.getInt(3);
            result.close();
            syncMessage(sender,
            ChatColor.YELLOW + "" + ChatColor.BOLD + "Active Rewards info for " + ChatColor.AQUA + "" + ChatColor.BOLD + name + ChatColor.YELLOW + "" + ChatColor.BOLD + ":",
            ChatColor.YELLOW + "Playtime: " + ChatColor.AQUA + playtime + " minutes", 
            ChatColor.YELLOW + "Points: " + ChatColor.AQUA + points + " points",
            ChatColor.YELLOW + "Historic Points: " + ChatColor.AQUA + historicPoints + " points"
            );
        }
    }

    @Override
    public String getInfo() {
        return "Displays the amount of time and the amount of points of a player";
    }

    @Override
    public String getUsage() {
        return "/activerewards info [player]";
    }

    @Override
    public String getPermissionString() {
        return "activerewards.info";
    }

    public String minutesFormat(int minutes) {
        int days = minutes / (24 * 60);
        int hours = (minutes % (24 * 60)) / 60;
        int minutesRemainder = (minutes % (24 * 60)) % 60;
        return (days > 0 ? days + "d " : "") + (hours > 0 ? hours + "h " : "") + minutesRemainder + "m";
    }

}
