package dev.itsmeow.activerewards.command;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.google.common.collect.Lists;

public abstract class TreeCommand extends BaseCommand {

    private HashMap<String, BaseCommand> subcommands = new HashMap<String, BaseCommand>();
    private List<BaseCommand> subcommandsList = new ArrayList<BaseCommand>();

    public TreeCommand(String name, String[] aliases, BaseCommand... subcommands) {
        super(name, aliases);
        for(BaseCommand command : subcommands) {
            subcommandsList.add(command);
            this.subcommands.put(command.getPrimaryName(), command);
            for(String n : command.getAliases()) {
                this.subcommands.put(n, command);
            }
        }
    }

    public BaseCommand getSubCommand(String command) {
        return subcommands.get(command);
    }

    public List<BaseCommand> getSubCommands() {
        return this.subcommandsList;
    }

    @Override
    public boolean onCommand(CommandSender sender, String[] args) {
        if(args.length == 0) {
            listSubcommands(sender);
        } else {
            String subcommandStr = args[0];
            if(subcommands.containsKey(subcommandStr)) {
                BaseCommand command = subcommands.get(subcommandStr);
                if(sender.hasPermission(command.getPermissionString())) {
                    if(command.mustBePlayer() && !(sender instanceof Player)) {
                        sender.sendMessage(ChatColor.RED + "You must be a player to use this command!");
                    } else {
                        return command.onCommand(sender, shiftArgs(args));
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
                }
            } else {
                listSubcommands(sender);
            }
        }
        return true;
    }

    public static String[] shiftArgs(String[] s) {
        if(s == null || s.length == 0) {
            return new String[0];
        }

        String[] s1 = new String[s.length - 1];
        System.arraycopy(s, 1, s1, 0, s1.length);
        return s1;
    }

    public AbstractMap.Entry<BaseCommand, String[]> getLowestCommandInTree(String[] args, boolean extraArgsNull) {
        BaseCommand cmd = this;
        boolean endTree = false;
        while(!endTree) {
            // This command has subcommands
            if(cmd instanceof TreeCommand) {
                if(args.length > 0) {
                    BaseCommand newCmd = ((TreeCommand) cmd).getSubCommand(args[0]);
                    if(newCmd != null) {
                        // This has subcommands with this name, continue and get completions
                        cmd = newCmd;
                        args = shiftArgs(args);
                    } else {
                        // This has no subcommand with this name, get completions for specific command
                        if(extraArgsNull) {
                            cmd = null;
                        }
                        endTree = true;
                    }
                } else {
                    // This is a tree, but no subcommands were found
                    endTree = true;
                }
            } else {
                // This command has no subcommands, get completions for subcommand
                endTree = true;
            }
        }
        return new AbstractMap.SimpleEntry<BaseCommand, String[]>(cmd, args);
    }

    private void listSubcommands(CommandSender sender) {
        sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "Subcommands: ");
        for(BaseCommand subCmd : this.getSubCommands()) {
            if(sender.hasPermission(subCmd.getPermissionString())) {
                String cmd = this.getUsage().substring(0, this.getUsage().indexOf(this.getPrimaryName() + " ") + this.getPrimaryName().length()) + " " + subCmd.getPrimaryName();
                sender.sendMessage(ChatColor.YELLOW + cmd + ChatColor.AQUA + " - " + ChatColor.WHITE + subCmd.getInfo());
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        AbstractMap.Entry<BaseCommand, String[]> pair = this.getLowestCommandInTree(args, false);
        BaseCommand command = pair.getKey();
        if(command == this) {
            List<String> list = new ArrayList<String>();
            if(args.length > 0) {
                return getListOfStringsMatchingLastWord(args, this.subcommands.keySet());
            }
            if(list.isEmpty()) {
                return Lists.newArrayList(this.subcommands.keySet());
            }
            return list;
        } else {
            return command.onTabComplete(sender, pair.getValue());
        }
    }

}