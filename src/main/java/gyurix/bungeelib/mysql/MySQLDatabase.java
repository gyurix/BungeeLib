package gyurix.bungeelib.mysql;

import com.mysql.jdbc.Connection;
import gyurix.bungeelib.configfile.ConfigSerialization.ConfigOptions;
import gyurix.bungeelib.utils.BU;
import gyurix.bungeelib.utils.StringUtils;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static gyurix.bungeelib.utils.BU.bl;

/**
 * Class representing a MySQL storage connection and containing the required methods and utils
 * for executing MySQL querries
 */
public class MySQLDatabase {
    public String table;
    @ConfigOptions(serialize = false)
    private Connection con;
    private String database;
    private String host;
    private String password;
    private int timeout = 10000;
    private String username;

    public MySQLDatabase() {

    }

    public MySQLDatabase(String host, String database, String username, String password) {
        this.host = host;
        this.username = username;
        this.password = password;
        this.database = database;
        openConnection();
    }

    public boolean command(String cmd, Object... args) {
        try {
            return prepare(cmd, args).execute();
        } catch (Throwable e) {
            BU.log(bl, "MySQL - Command", cmd, StringUtils.join(args, ", "));
            BU.error(BU.cs, e, "SpigotLib", "gyurix");
        }
        return false;
    }

    private Connection getConnection() {
        try {
            if (con == null || !con.isValid(timeout))
                openConnection();
        } catch (Throwable e) {
            BU.error(BU.cs, e, "SpigotLib", "gyurix");
        }
        return con;
    }

    public boolean openConnection() {
        try {
            con = (Connection) DriverManager.getConnection("jdbc:mysql://" + host + "/" + database + "?autoReconnect=true&useSSL=true", username, password);
            con.setAutoReconnect(true);
            con.setConnectTimeout(timeout);
        } catch (Throwable e) {
            BU.error(BU.cs, e, "SpigotLib", "gyurix");
            return false;
        }
        return true;
    }

    private PreparedStatement prepare(String cmd, Object... args) throws Throwable {
        PreparedStatement st = getConnection().prepareStatement(cmd);
        for (int i = 0; i < args.length; ++i)
            st.setObject(i + 1, args[i]);
        return st;
    }

    public ResultSet querry(String cmd, Object... args) {
        try {
            return prepare(cmd, args).executeQuery();
        } catch (Throwable e) {
            BU.log(bl, "MySQL - Querry", cmd, StringUtils.join(args, ", "));
            BU.error(BU.cs, e, "SpigotLib", "gyurix");
            return null;
        }
    }

    public int update(String cmd, Object... args) {
        try {
            return prepare(cmd, args).executeUpdate();
        } catch (Throwable e) {
            BU.log(bl, "MySQL - Update", cmd, StringUtils.join(args, ", "));
            BU.error(BU.cs, e, "SpigotLib", "gyurix");
            return -1;
        }
    }
}

