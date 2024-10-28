package GUI;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.IOException;
import javax.swing.*;

public class WelcomeDialogue extends JFrame implements ActionListener {
    String logo_filepath = "Recommender/resources/LUDII_Icon_transparent.png";
    String title = "Ludii Recommender System";

    public WelcomeDialogue() throws IOException {
        ImageIcon logo = new ImageIcon(logo_filepath);
        setIconImage(logo.getImage());
        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        JButton button = new JButton("Start");
        button.setFont(new Font("Comic Sans", Font.BOLD, 20));
        button.setBackground(Color.CYAN);
        button.setForeground(Color.darkGray);
        button.setEnabled(true);
        button.addActionListener(this);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 40));
        titleLabel.setForeground(Color.BLACK);
        JLabel picLabel = new JLabel(new ImageIcon(logo.getImage().getScaledInstance(100, 100, Image.SCALE_DEFAULT)));
        JPanel button_panel = new JPanel();
        button_panel.add(button);
        JPanel comp_panel = new JPanel();
        GridLayout gridLayout = new GridLayout(2, 1);
        comp_panel.setLayout(gridLayout);
        comp_panel.add(picLabel);
        comp_panel.add(titleLabel);
        setLayout(new GridLayout(2, 1));
        comp_panel.setBackground(Color.lightGray);
        button_panel.setBackground(Color.lightGray);
        add(comp_panel);
        add(button_panel);
        setSize(700, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setVisible(true);

    }
    public static void main(String[] args) throws IOException {
       new WelcomeDialogue();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
        new RecommenderStarter();
    }
}
