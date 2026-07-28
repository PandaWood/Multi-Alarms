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
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.Timer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

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
    private Timer clockTimer;

    /** Clock refresh interval - in milliseconds */
    private static final int CLOCK_UPDATE_INTERVAL = 2000;

    /** Format of the clock in the status bar */
    private static final DateTimeFormatter CLOCK_FORMAT =
        DateTimeFormatter.ofPattern("HH:mm.ss");

    /** Font for the (bold, oversized) alarm number column */
    private static final Font ALARM_NUM_FONT = new Font("Sans Serif", Font.BOLD, 16);

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

        // These images will be distributed in a JAR file when released,
        // so a missing resource can't really happen, but cater for it in principle
        ImageIcon iconWhite = new ImageIcon(Objects.requireNonNull(
                this.getClass().getResource("bell_white.gif"),
                "Unable to load image 'bell_white.gif'"));

        iconTransparent = new ImageIcon(Objects.requireNonNull(
                this.getClass().getResource("bell.gif"),
                "Unable to load image 'bell.gif'"));

        setIconImage(iconWhite.getImage());

        contentPane = (JPanel) this.getContentPane();

        contentPane.setLayout(new BorderLayout());
        this.setTitle(MultiAlarms.TITLE);
        registerAboutHandler();
        setupTable();
        setTimeDisplay();

        // Snap the window to its content rather than a hard-coded height: pack()
        // sizes it to fit the grid (header + rows) plus the status bar exactly,
        // then we restore the established 440px width. The old fixed 212px height
        // left a leftover band below the last row whose size varied with each
        // platform's font metrics -- larger on Linux than on macOS. Deriving the
        // height from the real row metrics keeps that gap consistent everywhere.
        pack();
        setSize(440, getHeight());
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
        alarmTable.setCellSelectionEnabled(true);
        alarmTable.setModel(alarmTableModel);
        alarmTable.setRowHeight(25);

        // Size the scroll pane's viewport to exactly the table's rows so the
        // frame can be packed snugly around the grid (see Init()). Deriving the
        // height from the real row height keeps it consistent across platforms,
        // rather than a hard-coded pixel height whose leftover gap varies with
        // each platform's font metrics.
        alarmScrollPane.setViewportView(alarmTable);
        alarmTable.setPreferredScrollableViewportSize(new Dimension(200,
                alarmTable.getRowHeight() * alarmTableModel.getRowCount()));
        contentPane.add(alarmScrollPane, BorderLayout.CENTER);

        // setup and initialise the table's column widths and renderers
        TableColumnModel cols = alarmTable.getColumnModel();

        cols.getColumn(AlarmTableModel.ALARM).setPreferredWidth(25);
        cols.getColumn(AlarmTableModel.TIME).setPreferredWidth(45);
        cols.getColumn(AlarmTableModel.PROGRESS).setPreferredWidth(100);
        cols.getColumn(AlarmTableModel.ACTIVE).setPreferredWidth(25);

        cols.getColumn(AlarmTableModel.ALARM).setCellRenderer(new StateRenderer(ALARM_NUM_FONT));
        cols.getColumn(AlarmTableModel.DESCRIPTION).setCellRenderer(new StateRenderer(null));
        cols.getColumn(AlarmTableModel.TIME).setCellRenderer(new StateRenderer(null));
        cols.getColumn(AlarmTableModel.PROGRESS).setCellRenderer(new ProgressRenderer());
        cols.getColumn(AlarmTableModel.ACTIVE).setCellRenderer(new ActiveRenderer());
    }

    /**
     * Convenience method to enable renderers to decide which colour to make
     * the background
     */
    public void setColBackground(Component component, int row) {

        Alarm   alarm        = alarmTableModel.getAlarm(row);
        boolean alarmActive  = alarm.isActive();
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
     * Text renderer that tints the cell background according to the alarm's
     * state. Used by the ALARM, DESCRIPTION and TIME columns, which differ only
     * in whether they override the font.
     */
    private class StateRenderer extends DefaultTableCellRenderer {

        private final Font font;

        StateRenderer(Font font) {
            this.font = font;
            setHorizontalAlignment(CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int col) {

            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);

            setColBackground(this, row);
            if (font != null) {
                setFont(font);
            }

            return this;
        }
    }

    /**
     * The PROGRESS column paints the alarm's own progress bar
     */
    private class ProgressRenderer implements TableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int col) {

            return alarmTableModel.getAlarm(row).getProgressBar();
        }
    }

    /**
     * The ACTIVE column renders as a centred check box
     */
    private class ActiveRenderer implements TableCellRenderer {

        private final JCheckBox checkBox = new JCheckBox();

        ActiveRenderer() {
            checkBox.setHorizontalAlignment(JCheckBox.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int col) {

            setColBackground(checkBox, row);
            checkBox.setSelected((Boolean) value);

            return checkBox;
        }
    }

    /**
     * Setup and initialise the time display in status bar.
     * A javax.swing.Timer fires on the event dispatch thread, so the label can
     * be updated directly.
     */
    private void setTimeDisplay() {

        // pad so text isn't clipped by the macOS rounded window corners
        statusBar.setBorder(BorderFactory.createEmptyBorder(2, 10, 4, 10));

        // Seed the real time before the window is packed (see Init()). The clock
        // Timer below only fires after construction, so without this the status
        // bar would be empty at pack() time and reserve too little height -- the
        // grid would then be squeezed when the time first appears.
        statusBar.setText(clockText());

        contentPane.add(statusBar, BorderLayout.SOUTH);

        clockTimer = new Timer(CLOCK_UPDATE_INTERVAL, e -> statusBar.setText(clockText()));
        clockTimer.setInitialDelay(0);
        clockTimer.start();
    }

    /** Current time, as rendered in the status bar */
    private static String clockText() {
        return "Time: " + LocalTime.now().format(CLOCK_FORMAT);
    }

    // -------------------------------------------------------------------------
    // EVENTS
    //--------------------------------------------------------------------------

    /* Overridden so we can exit cleanly when the window is closed */
    protected void processWindowEvent(WindowEvent e) {

        super.processWindowEvent(e);

        if (e.getID() == WindowEvent.WINDOW_CLOSING) {
            clockTimer.stop();
            alarmTableModel.stopTimers();
            System.exit(0);
        }
    }
}
