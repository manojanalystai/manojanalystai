package app;
import java.sql.*;

public class DBConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/construction_db";
    private static final String USER = "root";
    private static final String PASSWORD = "mmpc2405"; // apna password yahan

    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}
