package multialarms;

/*
 * Title:        AlarmTableModel
 * Description:  The Alarm JTable's TableModel implementation (handles data)
 * @author       Peter van der Woude
 */

import javax.swing.JProgressBar;
import javax.swing.Timer;
import javax.swing.table.AbstractTableModel;


class AlarmTableModel extends AbstractTableModel {

    public final static int NUM_ALARMS = 5;

    // column order constants
    public final static int ALARM       = 0;
    public final static int DESCRIPTION = 1;
    public final static int TIME        = 2;
    public final static int PROGRESS    = 3;
    public final static int ACTIVE      = 4;

    /** Column name array */
    final String[] columnNames = { "Alarm", "Description", "Set (24h)",
                                   "Progress", "On" };

    /** Alarm array */
    private final Alarm[] alarms = new Alarm[NUM_ALARMS];

    /** Timer to check progress bars */
    private Timer progressTimer;

    /** Interval to check progress bars */
    public static final int PROGRESS_INTERVAL = 2000;

    /** Constructor */
    public AlarmTableModel() {

        for (int x = 0; x < NUM_ALARMS; x++) {
            alarms[x] = new Alarm(x + 1);
        }

        startProgressTimer();
    }

    /** Return alarm given number */
    public Alarm getAlarm(int alarmNum) {
        return alarms[alarmNum];
    }

    /** Stop all timers */
    public void stopTimers() {

        progressTimer.stop();

        for (Alarm alarm : alarms) {
            alarm.stop();
        }
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public int getRowCount() {
        return alarms.length;
    }

    @Override
    public String getColumnName(int col) {
        return columnNames[col];
    }

    /** Alarm table implementation of getValueAt */
    @Override
    public Object getValueAt(int row, int col) {

        Alarm alarm = alarms[row];

        return switch (col) {
            case ALARM       -> alarm.getAlarmNum();
            case DESCRIPTION -> alarm.getDescription();
            case TIME        -> alarm.getTimeString();
            case ACTIVE      -> alarm.isActive();
            default          -> null;    // PROGRESS is painted by its own renderer
        };
    }

    /**
     * Declared explicitly rather than derived from getValueAt(0, col), which
     * returns null for the PROGRESS column.
     */
    @Override
    public Class<?> getColumnClass(int col) {

        return switch (col) {
            case ALARM    -> Integer.class;
            case ACTIVE   -> Boolean.class;
            case PROGRESS -> JProgressBar.class;
            default       -> String.class;
        };
    }

    /** Alarm table implementation of isCellEditable */
    @Override
    public boolean isCellEditable(int row, int col) {

        if ((col == TIME) || (col == DESCRIPTION)) {

            // only allow editing if alarm not set
            return !alarms[row].isActive();
        }

        return col == ACTIVE;
    }

    /** Alarm table implementation of setValueAt */
    @Override
    public void setValueAt(Object value, int row, int col) {

        Alarm alarm = alarms[row];

        switch (col) {
            case DESCRIPTION -> alarm.setDescription((String) value);
            case TIME        -> alarm.setTimeString((String) value);
            case ACTIVE      -> alarm.setActive((Boolean) value);
            default          -> { }
        }

        if (col == ACTIVE) {    // update the highlighted state of row
            fireTableRowsUpdated(row, row);
        } else {
            fireTableCellUpdated(row, col);
        }
    }

    /** Take manual control of updating the progress bar using a timer */
    private void startProgressTimer() {

        progressTimer = new Timer(PROGRESS_INTERVAL, evt -> {

            // for each alarm, check if active.
            for (int row = 0; row < alarms.length; row++) {

                if (!alarms[row].isActive()) {
                    continue;
                }

                // update the progress bar and fire event to repaint it
                alarms[row].updateProgressBar();
                fireTableCellUpdated(row, PROGRESS);

                // if alarm has gone off, update the whole row so it can
                // be re-painted the 'gone off' colour
                if (alarms[row].getGoneOff()) {
                    fireTableRowsUpdated(row, row);
                }
            }
        });

        progressTimer.setCoalesce(false);
        progressTimer.start();
    }
}
