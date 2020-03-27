package dev.itsmeow.activerewards;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Stack;
import java.util.function.Consumer;

import org.bukkit.plugin.Plugin;

public class SQLConnection {

    private static Runnable NONE = () -> {
    };

    private Thread sql_thread;
    private Stack<Runnable> queue = new Stack<Runnable>();

    private Connection db;
    private String db_host, db_database, db_username, db_password;
    private int db_port;
    private boolean db_useSSL;

    private Statement statement;

    private Plugin plugin;

    public SQLConnection(Plugin plugin, String threadName, String host, String database, String username, String password, int port, boolean useSSL) throws ClassNotFoundException, SQLException {
        this.db_host = host;
        this.db_database = database;
        this.db_username = username;
        this.db_password = password;
        this.db_port = port;
        this.db_useSSL = useSSL;
        this.plugin = plugin;
        this.sql_thread = new Thread(threadName) {
            @Override
            public void run() {
                while(!this.isInterrupted()) {
                    if(!queue.empty()) {
                        ActiveRewards.debug("RUN TASK START");
                        queue.pop().run();
                        ActiveRewards.debug("RUN TASK END");
                    }
                }
            }
        };
        this.openConnection();
    }

    public void execQuery(String query, SqlConsumer<ResultSet> callback) {
        queueAction(() -> {
            try {
                ActiveRewards.debug(query);
                checkConnection();
                callback.accept(statement.executeQuery(query));
            } catch(SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public ResultSet execQueryBlocking(String query) throws SQLException {
        ActiveRewards.debug(query);
        checkConnection();
        return statement.executeQuery(query);
    }

    public void execUpdateBlocking(String update) throws SQLException {
        ActiveRewards.debug(update);
        checkConnection();
        statement.executeUpdate(update);
    }

    public void execUpdate(String update) {
        execUpdate(update, NONE);
    }

    public void execUpdate(String update, Runnable callback) {
        queueAction(() -> {
            try {
                ActiveRewards.debug(update);
                checkConnection();
                statement.executeUpdate(update);
                callback.run();
            } catch(SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public void checkConnection() {
        try {
            if(statement == null || statement.isClosed() && db != null && !db.isClosed()) {
                statement = db.createStatement();
            } else if(db == null || db.isClosed()) {
                openConnection();
            }
        } catch(SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void openConnection() throws SQLException, ClassNotFoundException {
        if(db != null && !db.isClosed()) {
            return;
        }

        synchronized(plugin) {
            if(db != null && !db.isClosed()) {
                return;
            }
            Class.forName("com.mysql.jdbc.Driver");
            ActiveRewards.debug("CONNECT");
            db = DriverManager.getConnection("jdbc:mysql://" + db_host + ":" + db_port + "/" + db_database + "?useSSL=" + db_useSSL, db_username, db_password);
            statement = db.createStatement();
            ActiveRewards.debug("THREAD START");
            if(!this.sql_thread.isAlive()) {
                this.sql_thread.start();
            }
        }
    }

    public void closeConnection() {
        try {
            if(statement != null) {
                statement.close();
                statement = null;
            }
            if(db != null) {
                db.close();
                db = null;
            }
        } catch(SQLException e) {
            e.printStackTrace();
        }
        this.sql_thread.interrupt();
    }

    public void queueAction(Runnable run) {
        queue.add(0, run);
    }

    @FunctionalInterface
    public interface SqlConsumer<T> extends Consumer<T> {

        @Override
        default void accept(final T elem) {
            try {
                acceptThrows(elem);
            } catch(SQLException e) {
                e.printStackTrace();
            }
        }

        public void acceptThrows(final T elem) throws SQLException;
    }

}
