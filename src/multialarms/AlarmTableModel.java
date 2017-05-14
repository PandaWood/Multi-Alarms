package multialarms;

/*
 * Title:        AlarmTableModel
 * Description:  The Alarm JTable's TableModel implementation (handles data)
 * Company:      MosesSoft
 * @author       Peter van der Woude
 * @version      1.0
 */
 
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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
    private Alarm[] alarms = new Alarm[NUM_ALARMS];

    /** Timer to check progress bars */
    private Timer progressTimer;

    /** Interval to check progress bars */
    public static final int PROGRESS_INTERVAL = 2000;

    /** Constructor */
    public AlarmTableModel() {

        for (int x = 0; x < NUM_ALARMS; x++) {
            alarms[x] = new Alarm(new Integer(x + 1));
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

        for (int x = 0; x < NUM_ALARMS; x++) {
            alarms[x].stop();
        }
    }

    public int getColumnCount() {
        return columnNames.length;
    }

    public int getRowCount() {
        return alarms.length;
    }

    public String getColumnName(int col) {
        return columnNames[col];
    }

    /** Alarm table implementation of getValueAt */
    public Object getValueAt(int row, int col) {

        Object obj   = null;
        Alarm  alarm = (Alarm) alarms[row];

        switch (col) {

        case ALARM :
            obj = alarm.getAlarmNum();
            break;

        case DESCRIPTION :
            obj = alarm.getDescription();
            break;

        case TIME :
            obj = alarm.getTimeString();
            break;

        case ACTIVE :
            obj = alarm.getActive();
            break;
        }

        return obj;
    }

    public Class getColumnClass(int c) {
        return getValueAt(0, c).getClass();
    }

    /** Alarm table implementation of isCellEditable */
    public boolean isCellEditable(int row, int col) {

        if ((col == TIME) || (col == DESCRIPTION)) {

            // only allow editing if alarm not set
            Alarm   alarm       = (Alarm) alarms[row];
            boolean alarmActive =
                ((Boolean) alarm.getActive()).booleanValue();

            if (alarmActive == false) {
                return true;
            }
        }

        if (col == ACTIVE) {
            return true;
        }

        return false;
    }

    /** Alarm table implementation of setValueAt */
    public void setValueAt(Object value, int row, int col) {

        Alarm alarm = (Alarm) alarms[row];

        switch (col) {

        case DESCRIPTION :
            alarm.setDescription((String) value);
            break;

        case TIME :
            alarm.setTimeString((String) value);
            break;

        case ACTIVE :
            alarm.setActive((Boolean) value);
            break;
        }

        if (col == ACTIVE) {    // update the highlighted state of row
            fireTableRowsUpdated(row, row);
        } else {
            fireTableCellUpdated(row, col);
        }
    }

    /** Take manual control of updating the progress bar using a timer */
    private void startProgressTimer() {

        progressTimer = new Timer(PROGRESS_INTERVAL, new ActionListener() {

            public void actionPerformed(ActionEvent evt) {

                // for each alarm, check if active.
                for (int x = 0; x < alarms.length; x++) {

                    if (alarms[x].getActive().equals(Boolean.TRUE)) {

                        // update the progress bar and fire event to repaint it
                        alarms[x].updateProgressBar();
                        fireTableCellUpdated(x, PROGRESS);

                        // if alarm has gone off, update the whole row so it can
                        // can be re-painted the 'gone off' colour
                        if (alarms[x].getGoneOff() == true) {
                            fireTableRowsUpdated(x, x);
                        }
                    }
                }
            }
        });

        progressTimer.setCoalesce(false);
        progressTimer.start();
    }
}