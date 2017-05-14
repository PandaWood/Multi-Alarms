package com.mosessoft;


import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
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
 * Company:     MosesSoft
 * @author      Peter van der Woude
 * @version     1.0
 */
public class AboutDialog extends JDialog {

    JPanel            mainPanel = new JPanel();
    private String    title;
    private ImageIcon icon;
    private String    version;

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
     * no-arg constructor
     */
    private AboutDialog() {
        this(null, "", "", null, false);
    }

    /**
     * Testing
     */
    public static void main(String args[]) {
        new AboutDialog(null, "AboutTest", "1.0", null, true).show();
    }

    public void createCentreArea() {

        JPanel centrePanel = new JPanel();

        centrePanel.setLayout(new BoxLayout(centrePanel, BoxLayout.Y_AXIS));

        JLabel authorLabel = new JLabel("by Peter van der Woude");

        authorLabel.setAlignmentX(CENTER_ALIGNMENT);
        centrePanel.add(authorLabel, null);

        JLabel copyrightLabel = new JLabel("version " + version);

        copyrightLabel.setAlignmentX(CENTER_ALIGNMENT);
        centrePanel.add(copyrightLabel, null);
        centrePanel.add(Box.createRigidArea(new Dimension(0, 5)));

        // obtain and position the program icon
        if (icon != null) {
            JLabel iconLabel = new JLabel(icon);

            iconLabel.setAlignmentX(CENTER_ALIGNMENT);
            centrePanel.add(iconLabel, null);
        }

        centrePanel.add(Box.createRigidArea(new Dimension(0, 5)));

        JLabel freewareLabel = new JLabel(title + " is freeware");

        freewareLabel.setFont(new java.awt.Font("Dialog", 2, 14));
        freewareLabel.setAlignmentX(CENTER_ALIGNMENT);
        centrePanel.add(freewareLabel, null);
        mainPanel.add(centrePanel, BorderLayout.CENTER);
    }

    public void createTitleArea() {

        JLabel companyLabel = new JLabel("MosesSoft");
        JLabel titleLabel   = new JLabel(title);

        companyLabel.setFont(new java.awt.Font("Dialog", 2, 14));
        titleLabel.setFont(new java.awt.Font("Dialog", 1, 24));

        JPanel northPanel = new JPanel();

        northPanel.add(companyLabel, null);
        northPanel.add(titleLabel, null);
        mainPanel.add(northPanel, BorderLayout.NORTH);
    }

    public void createSouthArea() {

        JPanel  southPanel  = new JPanel();
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
    void createDialog(Frame parentFrame) throws Exception {

        mainPanel.setLayout(new BorderLayout());
        createTitleArea();
        createCentreArea();
        createSouthArea();
        mainPanel.setMinimumSize(new Dimension(250, 300));
        mainPanel.setPreferredSize(new Dimension(250, 300));
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
