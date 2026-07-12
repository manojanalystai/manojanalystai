package app;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class LoginFrame extends JFrame {

    JTextField txtUser;
    JPasswordField txtPass;

    public LoginFrame() {

        setTitle("Login");
        setSize(300,200);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel p = new JPanel(new GridLayout(3,2,5,5));

        p.add(new JLabel("Username"));
        txtUser = new JTextField();
        p.add(txtUser);

        p.add(new JLabel("Password"));
        txtPass = new JPasswordField();
        p.add(txtPass);

        JButton btnLogin = new JButton("Login");
        p.add(new JLabel());
        p.add(btnLogin);

        add(p);

        btnLogin.addActionListener(e -> login());

        setVisible(true);
    }

    void login() {
        try {
        	//System.out.println("Trying login: " + txtUser.getText() + " / " + new String(txtPass.getPassword()));

            Connection con = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/construction_db",
                "root","mmpc2405"
            );

            String sql = "SELECT role FROM users WHERE username=? AND password=?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, txtUser.getText());
            ps.setString(2, new String(txtPass.getPassword()));

            ResultSet rs = ps.executeQuery();

            if(rs.next()) {
                String role = rs.getString("role");
                dispose(); // login band
                String loggedUsername = txtUser.getText(); // jo login username hai
                new ConstructionDataManagementSystem(role);
 // main app
            } else {
                JOptionPane.showMessageDialog(this,"Wrong Username / Password");
            }

        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }
}
