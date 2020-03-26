package dev.itsmeow.activerewards.command.subcommands;

import org.bukkit.command.CommandSender;

import dev.itsmeow.activerewards.command.BaseCommand;

public class ActiveRewardsReloadCommand extends BaseCommand {

    public ActiveRewardsReloadCommand() {
        super("reload", new String[] {});
    }

    @Override
    public boolean onCommand(CommandSender sender, String[] args) {
        plugin.reload();
        sender.sendMessage("Sucessfully reloaded!");
        return true;
    }

    @Override
    public String getInfo() {
        return "Reloads the configuration and re-connects to the SQL database";
    }

    @Override
    public String getUsage() {
        return "/activerewards reload";
    }

    @Override
    public String getPermissionString() {
        return "activerewards.reload";
    }

}
