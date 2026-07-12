package app;

import javax.swing.*;
import java.awt.*;

public class TestBG {
    public static void main(String[] args) {
        JFrame f = new JFrame();
        f.setSize(800, 600);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        BackgroundPanel bg = new BackgroundPanel("C:\\Users\\jaima\\Downloads\\construction image.jpg");
        f.setContentPane(bg);

        JLabel lbl = new JLabel("Hello Background!");
        lbl.setForeground(Color.WHITE);
        lbl.setFont(new Font("Arial", Font.BOLD, 30));
        bg.add(lbl, BorderLayout.CENTER);

        f.setVisible(true);
    }
}

class BackgroundPanel extends JPanel {
    private Image backgroundImage;

    public BackgroundPanel(String path) {
        backgroundImage = new ImageIcon(path).getImage();
        setLayout(new BorderLayout());
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        }
    }
}
