package multialarms;

/*
 * Title:        MultiAlarmsGUI
 * Description:  The Graphical User Interface and event handling
 * Company:      MosesSoft
 * @author       Peter van der Woude
 * @version      1.0
 */

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

public class MultiAlarmsGUI extends JFrame {

    private JPanel contentPane;

    // menu components
    private JMenuBar  menuBar        = new JMenuBar();
    private JMenu     jMenuFile      = new JMenu();
    private JMenuItem jMenuFileExit  = new JMenuItem();
    private JMenu     jMenuHelp      = new JMenu();
    private JMenuItem jMenuHelpAbout = new JMenuItem();

    // table components
    private JScrollPane     alarmScrollPane = new JScrollPane();
    private JTable          alarmTable      = new JTable();
    private AlarmTableModel alarmTableModel = new AlarmTableModel();

    /** Image (logo) with white background to use as frame icon */
    private ImageIcon iconWhite;

    /** Image (logo) with transparent background to use on About Dialog */
    private ImageIcon iconTransparent;

    /** status bar component (a label) - used to display the time */
    private JLabel statusBar = new JLabel();

    /** Timer to update the current 'Time:' display on status bar */
    private Timer clockTimer = new Timer();

    /** Clock refresh interval - in milliseconds */
    private static final int CLOCK_UPDATE_INTERVAL = 2000;

    /** Construct the frame */
    public MultiAlarmsGUI() {

        enableEvents(AWTEvent.WINDOW_EVENT_MASK);

        try {
            Init();
        } catch (Exception ex) {
            ex.printStackTrace();

            JOptionPane.showMessageDialog(null, ex.getMessage(),
                                          MultiAlarms.TITLE,
                                          JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
    }

    /** 
     * Image initialization 
     * Extract images/icons objects, ready to display
     */
    private void Init() throws Exception {

        try {

            // ImageIcon will throw error if resource is null (ie. not found)
            // These images will be distributed in a JAR file when released,
            // so this can't really happen, but cater for it, in principle
            iconWhite       =
                new ImageIcon(this.getClass().getResource("bell_white.gif"));
            iconTransparent =
                new ImageIcon(this.getClass().getResource("bell.gif"));
                
        } catch (NullPointerException ex) {
            throw new RuntimeException(
                "Unable to load images 'bell_white.gif', 'bell.gif'");
        }

        setIconImage(iconWhite.getImage());

        contentPane = (JPanel) this.getContentPane();

        contentPane.setLayout(new BorderLayout());
        this.setSize(new Dimension(440, 212));
        this.setTitle(MultiAlarms.TITLE);
        createMenus();
        setupTable();
        setTimeDisplay();
    }

	/**
	 * initialise and setup the JTable 
	 */
    private void setupTable() {

        // alarm table properties
        alarmTable.setPreferredSize(new Dimension(200, 125));
        alarmTable.setCellSelectionEnabled(true);
        alarmTable.setModel(alarmTableModel);
        alarmTable.setRowHeight(25);
        setColumnWidths();

        // alarm table's scrollpane
        alarmScrollPane.setPreferredSize(new Dimension(150, 200));
        alarmScrollPane.setViewportView(alarmTable);
        contentPane.add(alarmScrollPane, BorderLayout.CENTER);

        // setup and initialise table's column renderers/editors
        initAlarmCol();
        initDescriptionCol();
        initTimeCol();
        initProgressCol();
        initActiveCol();
    }

	/**
	 * Create the menus and menu structure
	 */
    private void createMenus() {

        jMenuFile.setText("File");
        jMenuFileExit.setText("Exit");
        jMenuFileExit.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                action_FileExit(e);
            }
        });
        jMenuHelp.setText("Help");
        jMenuHelpAbout.setText("About");
        jMenuHelpAbout.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                action_HelpAbout(e);
            }
        });
        jMenuFile.add(jMenuFileExit);
        jMenuHelp.add(jMenuHelpAbout);
        menuBar.add(jMenuFile);
        menuBar.add(jMenuHelp);
        this.setJMenuBar(menuBar);
    }

	/**
	 * Set the widths for each of the JTable columns
	 */
    private void setColumnWidths() {

        TableColumnModel alarmColModel = alarmTable.getColumnModel();

        alarmColModel.getColumn(AlarmTableModel.ALARM).setPreferredWidth(25);
        alarmColModel.getColumn(AlarmTableModel.TIME).setPreferredWidth(45);
        alarmColModel.getColumn(AlarmTableModel.PROGRESS).setPreferredWidth(100);
        alarmColModel.getColumn(AlarmTableModel.ACTIVE).setPreferredWidth(25);
    }

    /**
     * Convenience method to enable renderers to decide which colour to make
     * the background
     */
    public void setColBackground(Component component, int row) {

        Alarm   alarm        = alarmTableModel.getAlarm(row);
        boolean alarmActive  = alarm.getActive().booleanValue();
        boolean alarmGoneOff = alarm.getGoneOff();
        Color   colColour    = alarmTable.getBackground();

        if (alarmActive && alarmGoneOff) {
            colColour = Color.lightGray;
        } else if (alarmActive && !alarmGoneOff) {
            colColour = Color.yellow;
        }    // else normal background colour (as initialised)

        component.setBackground(colColour);
    }

    /**
     * Setup and initialise the ALARM table column
     */
    public void initAlarmCol() {

        class AlarmNumRenderer extends DefaultTableCellRenderer {

            public AlarmNumRenderer() {
                setHorizontalAlignment(CENTER);
            }

            public Component getTableCellRendererComponent(JTable table,
                    Object value, boolean isSelected, boolean hasFocus,
                    int row, int col) {

                setColBackground(this, row);
                setValue(value);

                return this;
            }

            public void setValue(Object value) {
                super.setValue(value);
                setFont(new Font("Sans Serif", Font.BOLD, 16));
            }
        }

        alarmTable.getColumnModel().getColumn(AlarmTableModel.ALARM)
            .setCellRenderer(new AlarmNumRenderer());
    }

    /**
     * Setup and initialise the TIME table column
     */
    public void initTimeCol() {

        class TimeRenderer extends DefaultTableCellRenderer {

            public TimeRenderer() {
                setHorizontalAlignment(CENTER);
            }

            public Component getTableCellRendererComponent(JTable table,
                    Object value, boolean isSelected, boolean hasFocus,
                    int row, int col) {

                setColBackground(this, row);

                return super.getTableCellRendererComponent(table, value,
                    isSelected, hasFocus, row, col);
            }
        }

        alarmTable.getColumnModel().getColumn(AlarmTableModel.TIME)
            .setCellRenderer(new TimeRenderer());
    }

    /**
     * Setup and initialise the PROGRESS table column
     */
    public void initProgressCol() {

        class ProgressRenderer implements TableCellRenderer {

            private Alarm alarm;

            public Component getTableCellRendererComponent(JTable table,
                    Object value, boolean isSelected, boolean hasFocus,
                    int row, int col) {

                alarm = alarmTableModel.getAlarm(row);

                return alarm.getProgressBar();
            }
        }

        alarmTable.getColumnModel().getColumn(AlarmTableModel.PROGRESS)
            .setCellRenderer(new ProgressRenderer());
    }

    /**
     * Setup and initialise the DESCRIPTION table column
     */
    public void initDescriptionCol() {

        class DescriptionRenderer extends DefaultTableCellRenderer {

            public DescriptionRenderer() {
                setHorizontalAlignment(CENTER);
            }

            public Component getTableCellRendererComponent(JTable table,
                    Object value, boolean isSelected, boolean hasFocus,
                    int row, int col) {

                setColBackground(this, row);

                return super.getTableCellRendererComponent(table, value,
                    isSelected, hasFocus, row, col);
            }
        }

        alarmTable.getColumnModel().getColumn(AlarmTableModel.DESCRIPTION)
            .setCellRenderer(new DescriptionRenderer());
    }

    /**
     * Setup and initialise the ACTIVE table column
     */
    public void initActiveCol() {

        class ActiveRenderer implements TableCellRenderer {

            private JCheckBox checkBox = new JCheckBox();

            public ActiveRenderer() {
                checkBox.setHorizontalAlignment(JCheckBox.CENTER);
            }

            public Component getTableCellRendererComponent(JTable table,
                    Object value, boolean isSelected, boolean hasFocus,
                    int row, int col) {

                setColBackground(checkBox, row);
                checkBox.setSelected(((Boolean) value).booleanValue());

                return checkBox;
            }
        }

        alarmTable.getColumnModel().getColumn(AlarmTableModel.ACTIVE)
            .setCellRenderer(new ActiveRenderer());
    }

    /**
     * Setup and initialise the time display in status bar
     */
    private void setTimeDisplay() {
       
        class ClockTask extends TimerTask {

            private SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm.ss");

            public void run() {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        statusBar.setText("Time: " + dateFormat.format(new Date()));
                    }
                });
            }
        }

        contentPane.add(statusBar, BorderLayout.SOUTH);
        clockTimer.schedule(new ClockTask(), 0, CLOCK_UPDATE_INTERVAL);
    }

    // -------------------------------------------------------------------------
    // EVENTS
    //--------------------------------------------------------------------------

    /** Overridden so we can exit when window is closed */
    protected void processWindowEvent(WindowEvent e) {

        super.processWindowEvent(e);

        if (e.getID() == WindowEvent.WINDOW_CLOSING) {
        	
            action_FileExit(null);
        }
    }

    /** File | Exit action performed */
    public void action_FileExit(ActionEvent e) {
    	
        alarmTableModel.stopTimers();
        System.exit(0);
    }

    /** Help | About action performed */
    public void action_HelpAbout(ActionEvent e) {
    	
        new AboutDialog(this, MultiAlarms.TITLE,
        	MultiAlarms.VERSION,
            	iconTransparent, true).show();
    }
}
