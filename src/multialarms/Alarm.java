package multialarms;

/*
 * Title:        Alarm
 * Description:  Class representing an Alarm
 * @author       Peter van der Woude
 */

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.BorderFactory;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;


public class Alarm {

    /**
     * 24-hour time format for displaying and editing the alarm go-off time.
     * The day name is an optional section, so both "Tue 14:30" and a bare
     * "14:30" (user deleted the day) parse. Locale is pinned so the format
     * doesn't shift with the machine's regional settings.
     */
    private static final DateTimeFormatter TIME_FORMAT =
        DateTimeFormatter.ofPattern("[E ]HH:mm", Locale.ENGLISH);

    /** The delay in milliseconds for the alarm sound */
    private static final int SOUND_LOOP_DELAY = 900;

    /** Alarm progress bar */
    private final JProgressBar progressBar = new JProgressBar();

    /** The main timer that determines when the alarm goes off */
    private Timer alarmTimer = new Timer();

    /** Date/Time the alarm is to go off */
    private LocalDateTime alarmGoOffTime;

    /** Date/Time the alarm was started/initiated */
    private LocalDateTime alarmStartTime;

    /** Alarm number reference */
    private final Integer alarmNum;

    /** Indicates active status of the alarm */
    private boolean active;

    /** Audio clip to play */
    private Clip audioClip;

    /** Description of the alarm (supplied by user) */
    private String description;

    /** The timer used to loop sound when alarm goes off */
    private Timer ringSoundTimer;

    /** Determines alarm goneOff status */
    private boolean goneOff = false;

    /** 1-arg constructor */
    public Alarm(Integer alarmNum) {

        this.alarmNum       = alarmNum;
        this.alarmGoOffTime = LocalDateTime.now();   // default goOff time to now
        active              = false;
        description         = "alarm " + alarmNum;

        progressBar.setBorder(BorderFactory.createLoweredBevelBorder());
        progressBar.setStringPainted(true);
        progressBar.setString("");

        /* Sound file to play */
        URL soundFileURL = this.getClass().getResource("alarm.au");
        if (soundFileURL != null) {
            // alarm.au is ULAW-encoded; convert to 16-bit PCM so the audio
            // system can open a playback line on any platform.
            try (AudioInputStream rawStream = AudioSystem.getAudioInputStream(soundFileURL)) {
                AudioFormat src = rawStream.getFormat();
                AudioFormat pcm = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        src.getSampleRate(), 16, src.getChannels(),
                        src.getChannels() * 2, src.getSampleRate(), false);
                try (AudioInputStream pcmStream = AudioSystem.getAudioInputStream(pcm, rawStream)) {
                    audioClip = AudioSystem.getClip();
                    audioClip.open(pcmStream);
                }
            } catch (UnsupportedAudioFileException | IOException | LineUnavailableException ex) {
                ex.printStackTrace();
            }
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

        alarmStartTime = LocalDateTime.now();
        System.out.println("start() - " + this);

        // (48*60*60*1000=86,400,000 is maximum possible milliseconds
        Duration total = Duration.between(alarmStartTime, alarmGoOffTime);
        progressBar.setMaximum((int) total.toMillis());
        progressBar.setValue(0);

        goneOff = false;
        try {
            alarmTimer = new Timer();
            alarmTimer.schedule(new TimerTask() {
                                    public void run() {
                                        goOff();
                                    }
                                }, Math.max(0, total.toMillis()));

        } catch (IllegalStateException ex) {
            System.out.println(ex.getMessage());
            active = false;
        }
    }

    /** the alarm 'ring' event */
    private synchronized void goOff() {

        goneOff = true;
        System.out.println("goOff() - " + this);

        // this runs on the Timer thread, so bounce the Swing update to the EDT
        SwingUtilities.invokeLater(() -> progressBar.setString("Ring, ring..."));

        if (audioClip == null) {
            return;
        }

        ringSoundTimer = new Timer();
        ringSoundTimer.schedule(new TimerTask() {
                                    public void run() {
                                        audioClip.setFramePosition(0);
                                        audioClip.start();
                                    }
                                }, 0, SOUND_LOOP_DELAY);
    }

    /** return number of this alarm */
    public Integer getAlarmNum() {
        return alarmNum;
    }

    /** return the alarm goOff time formatted as String */
    public String getTimeString() {
        return alarmGoOffTime.format(TIME_FORMAT);
    }

    /**
     * Return active status
     * This method is synchronized since UpdateProgressTask thread may call at
     * same time as thread on the even dispatch queue
     */
    public synchronized boolean isActive() {
        return active;
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
    public synchronized void setActive(boolean active) {

        this.active = active;

        if (active) {
            start();
        } else {
            stop();
        }
    }

    /**
     * Parse String in format "E HH:mm", where the day name is optional.
     * The date is today, unless that time has already passed - in which case
     * the alarm is for the same time tomorrow.
     */
    public void setTimeString(String alarmString) {

        try {
            LocalDateTime goOff = LocalDate.now().atTime(
                    LocalTime.parse(alarmString.trim(), TIME_FORMAT));

            if (!goOff.isAfter(LocalDateTime.now())) {
                goOff = goOff.plusDays(1);
            }

            alarmGoOffTime = goOff;
        } catch (DateTimeParseException ex) {
            System.out.println("setTimeString() - " + ex.getMessage());
        }
    }

    /** External access to the progress bar */
    public JProgressBar getProgressBar() {
        return progressBar;
    }

    /** Update progress bar */
    public synchronized void updateProgressBar() {

        if (!active) {
            return;
        }

        // the int won't overflow because we only use time within one 48hr
        // period (48*60*60*1000=86,400,000 is maximum possible milliseconds)
        progressBar.setValue(
                (int) Duration.between(alarmStartTime, LocalDateTime.now()).toMillis());

        Duration left = Duration.between(LocalDateTime.now(), alarmGoOffTime);
        if (left.isNegative() || left.isZero()) {
            return;     // this will occur if the alarm is completed (gone off)
        }

        progressBar.setString(left.toHours() > 0    // only print hours if necessary
                ? "%dh %dm %ds left".formatted(left.toHours(), left.toMinutesPart(), left.toSecondsPart())
                : "%dm %ds left".formatted(left.toMinutesPart(), left.toSecondsPart()));
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

        StringBuilder alarmString = new StringBuilder("[" + alarmNum + "]->");

        if (alarmStartTime != null) {
            alarmString.append(" started[").append(alarmStartTime.format(TIME_FORMAT)).append("]");
        }

        if (alarmGoOffTime != null) {
            alarmString.append(" set[").append(alarmGoOffTime.format(TIME_FORMAT)).append("]");
        }

        alarmString.append(" goneOff=").append(goneOff);

        return alarmString.toString();
    }
}
