package dev.itsmeow.activerewards;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import dev.itsmeow.activerewards.RewardAction.RewardActionCondition;
import dev.itsmeow.activerewards.command.ActiveRewardsCommand;
import dev.itsmeow.activerewards.command.BaseCommand;

public class ActiveRewards extends JavaPlugin {

    private static boolean debug;
    public String userTable;
    public String uuidTable;
    public String dataCacheTable;

    private String db_host, db_database, db_username, db_password, db_prefix;
    private int db_port;
    private boolean db_useSSL;
    private String server_name;
    public boolean showMultipleServers;
    private Map<String, Short> modifiers = new HashMap<String, Short>();

    private SQLConnection sql;

    private ActiveRewardsCommand command;

    private Set<Player> lastPlayerList = Collections.newSetFromMap(new WeakHashMap<Player, Boolean>());

    private Set<RewardAction> actions = new HashSet<RewardAction>();

    @Override
    public void onEnable() {
        command = new ActiveRewardsCommand();
        this.getCommand("activerewards").setExecutor(command);
        this.getCommand("ar").setExecutor(command);
        Configuration cfg = this.getConfig();
        cfg.addDefault("server", "default");
        cfg.addDefault("show_multiple_servers", false);
        cfg.addDefault("sql.host", "127.0.0.1");
        cfg.addDefault("sql.port", 3306);
        cfg.addDefault("sql.database", "activerewards");
        cfg.addDefault("sql.username", "");
        cfg.addDefault("sql.password", "");
        cfg.addDefault("sql.prefix", "ar_");
        cfg.addDefault("sql.use_ssl", true);
        cfg.addDefault("modifiers.default.permission", "activerewards.use");
        cfg.addDefault("modifiers.default.multiplier", 1);
        cfg.addDefault("actions.action1.if", new String[] { "[newpoints:default]>=1", "[lastpoints:default]<1" });
        cfg.addDefault("actions.action1.do", new String[] { "lp user {username} parent settrack playtime lvl1" });
        cfg.addDefault("actions.action2.if", new String[] { "[newpoints:default,]<1", "[lastpoints:default]>=1" });
        cfg.addDefault("actions.action2.do", new String[] { "lp user {username} parent settrack playtime default" });
        cfg.addDefault("debug", false);
        cfg.options().copyDefaults(true);
        this.saveConfig();
        this.loadConfig();
        loadSQL();
        BaseCommand.setPlugin(this);
        this.scheduleMinuteTask();
    }

    @Override
    public void onDisable() {
        if(sql != null) {
            sql.closeConnection();
        }
    }

    private void loadSQL() {
        try {
            sql = new SQLConnection(this, "ActiveRewards_SQL", db_host, db_database, db_username, db_password, db_port, db_useSSL);
            sql.execUpdate("CREATE TABLE IF NOT EXISTS " + userTable + "( id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, uuid VARCHAR(36) NOT NULL, added DATETIME NOT NULL, expires DATETIME NOT NULL, multiplier SMALLINT NOT NULL, server VARCHAR(255) NOT NULL);");
            sql.execUpdate("CREATE TABLE IF NOT EXISTS " + uuidTable + "( id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, uuid VARCHAR(36) NOT NULL, name VARCHAR(16) NOT NULL, UNIQUE(uuid,name));");
            sql.execUpdate("CREATE TABLE IF NOT EXISTS " + dataCacheTable + "( id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, uuid VARCHAR(36) NOT NULL, server varchar(255) NOT NULL, playtime INT NOT NULL, points INT NOT NULL, historicpoints INT NOT NULL, UNIQUE(uuid,server));");
        } catch(ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadConfig() {
        Configuration cfg = this.getConfig();
        server_name = cfg.getString("server");
        showMultipleServers = cfg.getBoolean("show_multiple_servers");
        db_database = cfg.getString("sql.database");
        db_host = cfg.getString("sql.host");
        db_username = cfg.getString("sql.username");
        db_password = cfg.getString("sql.password");
        db_prefix = cfg.getString("sql.prefix");
        db_port = cfg.getInt("sql.port");
        db_useSSL = cfg.getBoolean("sql.use_ssl");
        modifiers.clear();
        actions.clear();
        ConfigurationSection modifiersSection = cfg.getConfigurationSection("modifiers");
        for(String modifierKey : modifiersSection.getKeys(false)) {
            ConfigurationSection modifierSection = modifiersSection.getConfigurationSection(modifierKey);
            modifiers.put(modifierSection.getString("permission"), (short) modifierSection.getInt("multiplier"));
        }

        ConfigurationSection actionsSection = cfg.getConfigurationSection("actions");
        for(String actionKey : actionsSection.getKeys(false)) {
            ConfigurationSection actionSection = actionsSection.getConfigurationSection(actionKey);
            List<String> ifs = actionSection.getStringList("if");
            List<String> dos = actionSection.getStringList("do");
            actions.add(new RewardAction(ifs.stream().map(s -> RewardActionCondition.fromString(s)).collect(Collectors.toCollection(ArrayList::new)).toArray(new RewardActionCondition[0]), dos.toArray(new String[0])));
        }
        debug = cfg.getBoolean("debug");
        userTable = db_prefix + "users";
        uuidTable = db_prefix + "uuid_cache";
        dataCacheTable = db_prefix + server_name + "_data_cache";
    }

    private void scheduleMinuteTask() {
        lastPlayerList.clear();
        lastPlayerList.addAll(Bukkit.getOnlinePlayers());
        getServer().getScheduler().runTaskLater(this, () -> {
            for(Player player : Bukkit.getOnlinePlayers()) {
                boolean hasPerm = player.hasPermission("activerewards.use");
                if(hasPerm) {
                    if(lastPlayerList.contains(player)) {
                        String uuid = getUUID(player);
                        sql.execUpdate("INSERT INTO " + uuidTable + " (uuid, name) VALUES('" + uuid + "', '" + player.getName() + "') ON DUPLICATE KEY UPDATE uuid='" + uuid + "', name='" + player.getName() + "'");
                        sql.execUpdate("INSERT INTO " + userTable + " (uuid, added, expires, multiplier, server) VALUES ('" + uuid + "', NOW(), DATE_ADD(NOW(), INTERVAL 1 MONTH), " + +getMultiplier(player) + ", '" + server_name + "');");
                    }

                    sql.queueAction(() -> {
                        try {
                            // get cache for this server
                            ResultSet result = sql.execQueryBlocking("SELECT server,playtime,points,historicpoints FROM " + dataCacheTable + " WHERE uuid='" + player.getUniqueId() + "' GROUP BY server;");
                            final Map<String, ServerTuple> serversLast = new HashMap<String, ServerTuple>();
                            while(result.next()) {
                                serversLast.put(result.getString(1), new ServerTuple(result.getInt(2), result.getInt(3), result.getInt(4)));
                            }
                            result.close();
                            // get actual data
                            ResultSet curResult = sql.execQueryBlocking("SELECT server,COUNT(CASE WHEN multiplier>0 THEN 1 ELSE NULL END),SUM(CASE WHEN NOW()<expires THEN multiplier ELSE 0 END),SUM(multiplier) FROM " + userTable + " WHERE uuid='" + player.getUniqueId() + "' GROUP BY server;");
                            final Map<String, ServerTuple> serversCurrent = new HashMap<String, ServerTuple>();
                            while(curResult.next()) {
                                serversCurrent.put(curResult.getString(1), new ServerTuple(curResult.getInt(2), curResult.getInt(3), curResult.getInt(4)));
                            }
                            curResult.close();
                            serversLast.forEach((server, tuple) -> debug("LAST-" + server + " - " + tuple.playtime + " - " + tuple.points + " - " + tuple.historicalPoints));
                            serversCurrent.forEach((server, tuple) -> debug("CURR-" + server + " - " + tuple.playtime + " - " + tuple.points + " - " + tuple.historicalPoints));
                            Bukkit.getScheduler().runTask(this, () -> {
                                serversLast.forEach((server, tuple) -> debug("LAST-" + server + " - " + tuple.playtime + " - " + tuple.points + " - " + tuple.historicalPoints));
                                serversCurrent.forEach((server, tuple) -> debug("CURR-" + server + " - " + tuple.playtime + " - " + tuple.points + " - " + tuple.historicalPoints));
                                for(RewardAction action : actions) {
                                    action.checkExecute(this, player.getName(), serversLast, serversCurrent);
                                }
                            });
                        } catch(SQLException e) {
                            e.printStackTrace();
                        }
                    });
                    sql.execUpdate("INSERT INTO " + dataCacheTable + "(uuid, server, playtime, points, historicpoints) ( SELECT uuid, server, COUNT(CASE WHEN multiplier > 0 THEN 1 ELSE NULL END), SUM( CASE WHEN NOW() < expires THEN multiplier ELSE 0 END), SUM(multiplier) FROM " + userTable + " WHERE uuid='" + player.getUniqueId() + "' GROUP BY uuid, server ) ON DUPLICATE KEY UPDATE playtime = VALUES(playtime), points = VALUES(points), historicpoints = VALUES(historicpoints); ");
                }
            }
            lastPlayerList.clear();
            lastPlayerList.addAll(Bukkit.getOnlinePlayers());
            // re-schedule
            scheduleMinuteTask();
        }, 1200);
    }

    public void reload() {
        this.loadConfig();
        if(sql != null) {
            sql.closeConnection();
        }
        this.loadSQL();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return this.command.onTabComplete(sender, args);
    }

    public SQLConnection getSQL() {
        return sql;
    }

    private static String getUUID(Player player) {
        return player.getUniqueId().toString();
    }

    /*
     * API ACCESSORS
     */

    /**
     * @return The (current) points multiplier for a given player
     */
    public short getMultiplier(Player player) {
        short multiplier = 1;
        for(String permission : modifiers.keySet()) {
            if(player.hasPermission(permission)) {
                short newMultiplier = modifiers.get(permission);
                if(multiplier < newMultiplier) {
                    multiplier = newMultiplier;
                }
            }
        }
        return multiplier;
    }

    /*
     * PLAYTIME ASYNC
     */

    public void getPlaytimeAsync(UUID uuid, Consumer<Integer> callback) {
        sql.execQuery("SELECT COUNT(*) FROM " + userTable + " WHERE uuid='" + uuid + "';", result -> {
            result.next();
            callback.accept(result.getInt(1));
        });
    }

    public void getPlaytimeAsync(String username, Consumer<Integer> callback) {
        sql.execQuery("SELECT COUNT(*) FROM " + userTable + " WHERE uuid=(SELECT uuid FROM " + uuidTable + " WHERE name='" + username + "');", result -> {
            result.next();
            callback.accept(result.getInt(1));
        });
    }

    /*
     * POINTS ASYNC
     */

    public void getPointsAsync(UUID uuid, Consumer<Integer> callback) {
        sql.execQuery("SELECT SUM(multiplier) FROM " + userTable + " WHERE uuid='" + uuid + "';", result -> {
            result.next();
            callback.accept(result.getInt(1));
        });
    }

    public void getPointsAsync(String username, Consumer<Integer> callback) {
        sql.execQuery("SELECT SUM(multiplier) FROM " + userTable + " WHERE uuid=(SELECT uuid FROM " + uuidTable + " WHERE name='" + username + "');", result -> {
            result.next();
            callback.accept(result.getInt(1));
        });
    }

    /* 
     * PLAYTIME
     */

    public int getPlaytime(UUID uuid) throws SQLException {
        ResultSet result = sql.execQueryBlocking("SELECT COUNT(*) FROM " + userTable + " WHERE uuid='" + uuid + "';");
        result.next();
        int r = result.getInt(1);
        result.close();
        return r;
    }

    public int getPlaytime(String username) throws SQLException {
        ResultSet result = sql.execQueryBlocking("SELECT COUNT(*) FROM " + userTable + " WHERE uuid=(SELECT uuid FROM " + uuidTable + " WHERE name='" + username + "');");
        result.next();
        int r = result.getInt(1);
        result.close();
        return r;
    }

    /*
     * POINTS
     */

    public int getPoints(UUID uuid) throws SQLException {
        ResultSet result = sql.execQueryBlocking("SELECT SUM(multiplier) FROM " + userTable + " WHERE uuid='" + uuid + "';");
        result.next();
        int r = result.getInt(1);
        result.close();
        return r;
    }

    public int getPoints(String username) throws SQLException {
        ResultSet result = sql.execQueryBlocking("SELECT SUM(multiplier) FROM " + userTable + " WHERE uuid=(SELECT uuid FROM " + uuidTable + " WHERE name='" + username + "');");
        result.next();
        int r = result.getInt(1);
        result.close();
        return r;
    }

    public static void debug(String string) {
        if(debug) {
            System.out.println("[ARDEBUG] " + string);
        }
    }

}
