package com.mosessoft.multialarms;

/*
 * Title:        Alarm
 * Description:  Class representing an Alarm
 * Company:      MosesSoft
 * @author       Peter van der Woude
 * @version      1.1
 */
 
import java.applet.Applet;
import java.applet.AudioClip;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.BorderFactory;
import javax.swing.JProgressBar;


public class Alarm {

    /** 24-hour time format for editing alarm go-off time */
    private static final SimpleDateFormat timeFormat =
        new SimpleDateFormat("E HH:mm");

    /** The delay in milliseconds for the alarm sound */
    private static final int SOUND_LOOP_DELAY = 900;

    /** Alarm progress bar */
    private JProgressBar progressBar = new JProgressBar();

    /** The main timer that determines when the alarm goes off */
    private Timer alarmTimer = new Timer();

    /** Date/Time the alarm is to go off */
    private Date alarmGoOffTime;

    /** Date/Time the alarm was started/initiated */
    private Date alarmStartTime;

    /** Alarm number reference */
    private Integer alarmNum;

    /** Boolean to indicate active status of the alarm */
    private Boolean active;

    /** Audio clip to play */
    private AudioClip audioClip;

    /** Sound file to play */
    private URL soundFileURL;

    /** Description of the alarm (supplied by user) */
    private String description;

    /** The timer used to loop sound when alarm goes off */
    private Timer ringSoundTimer;

    /** Boolean to determine alarm goneOff status */
    private boolean goneOff = false;

    /** String to formulate time - re-used for efficiency */
    StringBuffer timeToGo = new StringBuffer();

    /** Private constructor to prevent object creation other than 1-arg */
    private Alarm() {}

    /** 1-arg constructor */
    public Alarm(Integer alarmNum) {

        this.alarmNum       = alarmNum;
        this.alarmGoOffTime = new Date();   // default goOff time to now
        active              = Boolean.FALSE;
        description         = "alarm " + alarmNum;

        progressBar.setBorder(BorderFactory.createLoweredBevelBorder());
        progressBar.setStringPainted(true);
        progressBar.setString("");

        soundFileURL = this.getClass().getResource("alarm.au");
        if (soundFileURL != null) {
            audioClip = Applet.newAudioClip(soundFileURL);
        }
    }

    /**
     * Stop the alarm
     */
    public synchronized void stop() {
        System.out.println("stop() - " + this);

        // cancel the alarm sound loop, if running
        if (ringSoundTimer != null) {
            ringSoundTimer.cancel();
        }

        // cancel main timer (cater for interrupted timer as well as completed)
        alarmTimer.cancel();

        // reset the progress bar
        progressBar.setString("");
        progressBar.setValue(0);
    }

    /**
     * Start the alarm
     */
    private synchronized void start() {

        alarmStartTime = new Date();
        System.out.println("start() - " + this);

        // (48*60*60*1000=86,400,000 is maximum possible milliseconds
        progressBar.setMaximum((int) (alarmGoOffTime.getTime()
                                      - alarmStartTime.getTime()));
        progressBar.setValue(0);

        goneOff = false;
        try {
            alarmTimer = new Timer();
            alarmTimer.schedule(new TimerTask() {
                                    public void run() {
                                        goOff();
                                    }
                                }, alarmGoOffTime);

        } catch (IllegalStateException ex) {
            System.out.println(ex.getMessage());
            active = Boolean.FALSE;
        }
    }

    /** the alarm 'ring' event */
    private synchronized void goOff() {
    	
        goneOff = true;
        System.out.println("goOff() - " + this);

        progressBar.setString("Ring, ring...");

        if (audioClip == null) {
            return;
        }

        ringSoundTimer = new Timer();

        class AlarmRingTask extends TimerTask {
             public void run() {
                audioClip.play();
            }
        }
        ringSoundTimer.schedule(new AlarmRingTask(), 0, SOUND_LOOP_DELAY);
    }

    /** return number of this alarm */
    public Integer getAlarmNum() {    	
        return alarmNum;
    }

    /** return the alarm goOff time formatted as String */
    public String getTimeString() {    	
        return timeFormat.format(alarmGoOffTime);
    }

    /**
     * Return active status
     * This method is synchronized since UpdateProgressTask thread may call at
     * same time as thread on the even dispatch queue
     */
    public synchronized Boolean getActive() {    	
        return active;
    }

    /**
     * Determine progress of alarm in milliseconds from time started.
     * This is used by the progress bar (after alarmGoOffTime is used as maximum)
     * to determine percentage complete.
     */
    private int getTimeElapsed() {

        // the int won't overflow because we only use time within one 48hr
        // period (48*60*60*1000=86,400,000 is maximum possible milliseconds)
        return (int)(System.currentTimeMillis() - alarmStartTime.getTime());
    }

    /** Return description of the alarm */
    public String getDescription() {    	
        return description;
    }

    /** Set description of the alarm */
    public void setDescription(String description) {
    	
        if (description != null) {
            this.description = description;
        }
    }

    /** Set the active status of the alarm and react accordingly */
    public synchronized void setActive(Boolean active) {

        this.active = active;

        if (this.active.equals(Boolean.TRUE)) {
            start();
        } else {
            stop();
        }
    }

    /**
     * Parse String in format HH:mm
     * First, work out today's date (otherwise it defaults to epoch 1970)
     * Then, set time given (typed in by user)
     */
    public void setTimeString(String alarmString) {

        try {
            Calendar dayCal   = new GregorianCalendar();
            Calendar timesetCal = new GregorianCalendar();
            Calendar calFinal   = new GregorianCalendar();

            // get today's date
            dayCal.setTime(new Date());
            
            // set to today or tomorrow?
            SimpleDateFormat dayFormat = new SimpleDateFormat("E");
            String dayString = dayFormat.format(new Date());
            
            boolean tomorrow = (alarmString.indexOf(dayString) == -1);
            
            if (alarmString.length() <= 6) {
                // in this case, the day string has been deleted - assume is today
                tomorrow = false;
            }
            
            if (tomorrow) {
                dayCal.add(Calendar.DATE, 1);
            }

            // get the time that was given (albeit with 1970 year)
            Date timesetDate = timeFormat.parse(alarmString);

            timesetCal.setTime(timesetDate);

            // now we have a new time and the current date, join them both
            calFinal.set(dayCal.get(Calendar.YEAR),    // set to today
                         dayCal.get(Calendar.MONTH),
                         dayCal.get(Calendar.DATE),
                         timesetCal.get(Calendar.HOUR_OF_DAY),  // set new time
                         timesetCal.get(Calendar.MINUTE),
                         0);    // assume beginng of the minute (ie. no seconds)

            alarmGoOffTime = calFinal.getTime();
        } catch (ParseException ex) {
            System.out.println("setTimeString() - " + ex.getMessage());
        }
    }

    /** External access to the progress bar */
    public JProgressBar getProgressBar() {    	
        return progressBar;
    }

    /** Update progress bar */
    public synchronized void updateProgressBar() {

        if (active.equals(Boolean.FALSE)) {
            return;
        }

        progressBar.setValue(getTimeElapsed());

        long timeLeft = alarmGoOffTime.getTime() - System.currentTimeMillis();
        if (timeLeft <= 0 ) {
            return;     // this will occur if the alarm is completed (gone off)
        }

        int timeInSeconds = (int)(timeLeft / 1000);

        // calculate time to go in hours, minutes and seconds
        int hours = timeInSeconds / 3600;
        timeInSeconds = timeInSeconds - (hours * 3600);
        int minutes = timeInSeconds / 60;
        timeInSeconds = timeInSeconds - (minutes * 60);
        int seconds = timeInSeconds;

        timeToGo.setLength(0);
        if (hours > 0) {    // only print hours if necessary
            timeToGo.append(hours).append("h ");
        }
        timeToGo.append(minutes).append("m ").append(seconds).append("s left");

        progressBar.setString(timeToGo.toString());
    }

    /**
     * Return 'true' if the alarm has gone off
     * This method is synchronized because the ProgressTimer thread may call
     * it at same time as an event dispatch thread
     */
    public synchronized boolean getGoneOff() {
        return goneOff;
    }

    /** 
     * String representation of the alarm 
     * Used for debugging and System.out output 
     */
    public String toString() {

        StringBuffer alarmString = new StringBuffer("[" + alarmNum + "]->");

        if (alarmStartTime != null) {
            alarmString.append(" started[" + timeFormat.format(alarmStartTime) + "]");
        }

        if (alarmGoOffTime != null) {
            alarmString.append(" set[" + timeFormat.format(alarmGoOffTime) + "]");
        }

        alarmString.append(" goneOff=" + goneOff);

        return alarmString.toString();
    }
}
