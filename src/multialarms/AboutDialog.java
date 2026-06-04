package multialarms;

import java.awt.*;
import java.awt.event.ActionEvent;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

/*
 * Title:       AboutDialog
 * Copyright:   Copyright (c) 2002
 * @author      Peter van der Woude
 */
public class AboutDialog extends JDialog {

    JPanel            mainPanel = new JPanel();
    private final String    title;
    private final ImageIcon icon;
    private final String    version;

    public AboutDialog(Frame frame, String title, String version,
                       ImageIcon icon, boolean modal) {

        super(frame, "About " + title, modal);

        this.title   = title;
        this.icon    = icon;
        this.version = version;

        try {
            createDialog(frame);
            pack();
            setResizable(false);    // set after to cater for linux KDE JVM bug
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Testing
     */
    public static void main(String[] args) {
        new AboutDialog(null, "AboutTest", "1.0", null, true).setVisible(true);
    }

    public void createCentreArea() {

        JPanel centrePanel = new JPanel();

        centrePanel.setLayout(new BoxLayout(centrePanel, BoxLayout.Y_AXIS));

        JLabel authorLabel = new JLabel("by Peter van der Woude");

        authorLabel.setAlignmentX(CENTER_ALIGNMENT);
        centrePanel.add(authorLabel, null);

        JLabel versionLabel = new JLabel("version " + version);
        versionLabel.setFont(new Font("Dialog", Font.PLAIN, 11));
        versionLabel.setForeground(Color.GRAY);
        versionLabel.setAlignmentX(CENTER_ALIGNMENT);
        centrePanel.add(versionLabel, null);
        centrePanel.add(Box.createRigidArea(new Dimension(0, 5)));

        // obtain and position the program icon
        if (icon != null) {
            // source bell.gif is 436x364 — scale to fit the dialog, preserving aspect ratio
            Image scaled = icon.getImage().getScaledInstance(120, -1, Image.SCALE_SMOOTH);
            JLabel iconLabel = new JLabel(new ImageIcon(scaled));

            iconLabel.setAlignmentX(CENTER_ALIGNMENT);
            centrePanel.add(iconLabel, null);
        }

        mainPanel.add(centrePanel, BorderLayout.CENTER);
    }

    public void createTitleArea() {

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new java.awt.Font("Dialog", Font.BOLD, 24));

        JPanel northPanel = new JPanel();
        northPanel.add(titleLabel, null);
        mainPanel.add(northPanel, BorderLayout.NORTH);
    }

    public void createSouthArea() {

        JPanel  southPanel  = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton closeButton = new JButton("Close");

        closeButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                AboutDialog.this.dispose();
            }
        });
        southPanel.add(closeButton, null);
        mainPanel.add(southPanel, BorderLayout.SOUTH);
    }

    /**
     * init method
     */
    void createDialog(Frame parentFrame) {

        mainPanel.setLayout(new BorderLayout());
        createTitleArea();
        createCentreArea();
        createSouthArea();
        // pack() will size to content; pad up to a minimum width so the
        // dialog doesn't look cramped when the content is narrow.
        Dimension natural = mainPanel.getPreferredSize();
        mainPanel.setPreferredSize(
                new Dimension(Math.max(natural.width, 250), natural.height));
        getContentPane().add(mainPanel);

        int xPos = 0;
        int yPos = 0;

        if (parentFrame != null) {

            // centre the Window on screen
            xPos = (int) (parentFrame.getX() + (parentFrame.getWidth() / 2)
                          - (getPreferredSize().getWidth() / 2));
            yPos = (int) (parentFrame.getY() + (parentFrame.getHeight() / 2)
                          - (getPreferredSize().getHeight() / 2));
        }

        setLocation(xPos, yPos);
    }
}
