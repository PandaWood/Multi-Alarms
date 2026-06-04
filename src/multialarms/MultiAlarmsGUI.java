package multialarms;

/*
 * Title:        MultiAlarmsGUI
 * Description:  The Graphical User Interface and event handling
 * @author       Peter van der Woude
 */

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.WindowEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

@SuppressWarnings("ConstantConditions")
public class MultiAlarmsGUI extends JFrame {

    private JPanel contentPane;

    // table components
    private final JScrollPane     alarmScrollPane = new JScrollPane();
    private final JTable          alarmTable      = new JTable();
    private final AlarmTableModel alarmTableModel = new AlarmTableModel();

    /** Image (logo) with transparent background to use on About Dialog */
    private ImageIcon iconTransparent;

    /** status bar component (a label) - used to display the time */
    private final JLabel statusBar = new JLabel();

    /** Timer to update the current 'Time:' display on status bar */
    private final Timer clockTimer = new Timer();

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
    private void Init() {

        /* Image (logo) with white background to use as frame icon */
        ImageIcon iconWhite;
        try {

            // ImageIcon will throw error if resource is null (ie. not found)
            // These images will be distributed in a JAR file when released,
            // so this can't really happen, but cater for it, in principle
            iconWhite =
                new ImageIcon(this.getClass().getResource("bell_white.gif"));
            iconTransparent =
                new ImageIcon(this.getClass().getResource("bell.gif"));
                
        } catch (NullPointerException ex) {
            throw new RuntimeException("Unable to load images 'bell_white.gif', 'bell.gif'");
        }

        setIconImage(iconWhite.getImage());

        contentPane = (JPanel) this.getContentPane();

        contentPane.setLayout(new BorderLayout());
        this.setSize(new Dimension(440, 212));
        this.setTitle(MultiAlarms.TITLE);
        registerAboutHandler();
        setupTable();
        setTimeDisplay();
    }

    /**
     * Hook the AboutDialog into the OS-provided application menu's About item
     * (e.g. on macOS, "MultiAlarms > About MultiAlarms"). No-op on platforms
     * without an application About menu.
     */
    private void registerAboutHandler() {
        if (!Desktop.isDesktopSupported()) {
            return;
        }
        Desktop desktop = Desktop.getDesktop();
        if (desktop.isSupported(Desktop.Action.APP_ABOUT)) {
            desktop.setAboutHandler(e -> showAbout());
        }
    }

    /** Show the About dialog */
    private void showAbout() {
        new AboutDialog(this, MultiAlarms.TITLE, MultiAlarms.VERSION,
                        iconTransparent, true).setVisible(true);
    }

	/**
	 * initialise and set up the JTable
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
        boolean alarmActive  = alarm.getActive();
        boolean alarmGoneOff = alarm.getGoneOff();
        Color   colColour    = alarmTable.getBackground();

        if (alarmActive && alarmGoneOff) {
            colColour = Color.lightGray;
        } else if (alarmActive) {
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

            public Component getTableCellRendererComponent(JTable table,
                    Object value, boolean isSelected, boolean hasFocus,
                    int row, int col) {

                Alarm alarm = alarmTableModel.getAlarm(row);

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

            private final JCheckBox checkBox = new JCheckBox();

            public ActiveRenderer() {
                checkBox.setHorizontalAlignment(JCheckBox.CENTER);
            }

            public Component getTableCellRendererComponent(JTable table,
                    Object value, boolean isSelected, boolean hasFocus,
                    int row, int col) {

                setColBackground(checkBox, row);
                checkBox.setSelected((Boolean) value);

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

            private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm.ss");

            public void run() {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        statusBar.setText("Time: " + dateFormat.format(new Date()));
                    }
                });
            }
        }

        // pad so text isn't clipped by the macOS rounded window corners
        statusBar.setBorder(BorderFactory.createEmptyBorder(2, 10, 4, 10));
        contentPane.add(statusBar, BorderLayout.SOUTH);
        clockTimer.schedule(new ClockTask(), 0, CLOCK_UPDATE_INTERVAL);
    }

    // -------------------------------------------------------------------------
    // EVENTS
    //--------------------------------------------------------------------------

    /* Overridden so we can exit cleanly when the window is closed */
    protected void processWindowEvent(WindowEvent e) {

        super.processWindowEvent(e);

        if (e.getID() == WindowEvent.WINDOW_CLOSING) {
            alarmTableModel.stopTimers();
            System.exit(0);
        }
    }
}
