package app;

import java.sql.*;

public class TestConnection {
    public static void main(String[] args) {
        try {
            Connection con = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/construction_db", "root", "mmpc2405"); // apna password
            System.out.println("Connected Successfully ✅");
            con.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

