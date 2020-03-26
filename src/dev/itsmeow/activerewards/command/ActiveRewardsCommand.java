package dev.itsmeow.activerewards.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import dev.itsmeow.activerewards.command.subcommands.ActiveRewardsInfoCommand;
import dev.itsmeow.activerewards.command.subcommands.ActiveRewardsReloadCommand;

public class ActiveRewardsCommand extends TreeCommand implements CommandExecutor {

    public ActiveRewardsCommand() {
        super("activerewards", new String[] { "ar" }, new ActiveRewardsReloadCommand(), new ActiveRewardsInfoCommand());
    }

    @Override
    public String getInfo() {
        return "Base command for ActiveRewards";
    }

    @Override
    public String getUsage() {
        return "/activerewards [subcommand]";
    }

    @Override
    public String getPermissionString() {
        return "activerewards.use";
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return this.onCommand(sender, args);
    }

}
