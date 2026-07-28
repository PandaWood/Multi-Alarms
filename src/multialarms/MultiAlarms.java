package multialarms;

/*
 * Title:        MultiAlarms
 * Description:  Main program module
 * @author       Peter van der Woude
 */

import com.incors.plaf.kunststoff.KunststoffLookAndFeel;
import com.incors.plaf.kunststoff.KunststoffTheme;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;



public class MultiAlarms {

    /** Program title */
    public static String TITLE = "MultiAlarms";
    
    /** Program version number/string - to be extracted from properties file */
    public static String VERSION = "unknown";

    public static void initResourceBundle() {
    	
        try {
            // ResourceBundle object used to extract the properties file from .jar
            ResourceBundle resBundle = ResourceBundle.getBundle("multialarms.multialarms");
            VERSION = resBundle.getString("version");
            
        } catch (java.util.MissingResourceException ex) {
            String message = """
                    The file 'multialarms.properties' was not found in archive.
                    Please obtain a valid version of %s.jar""".formatted(TITLE);
            JOptionPane.showMessageDialog(null, message, TITLE,
                                          JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Construct the application */
    public MultiAlarms() {

		initResourceBundle();
		
        MultiAlarmsGUI frame = new MultiAlarmsGUI();
        frame.validate();            

        // centre the window
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frameSize  = frame.getSize();

        if (frameSize.height > screenSize.height) {
            frameSize.height = screenSize.height;
        }

        if (frameSize.width > screenSize.width) {
            frameSize.width = screenSize.width;
        }

        frame.setLocation((screenSize.width - frameSize.width) / 2,
                          (screenSize.height - frameSize.height) / 2);
        frame.setVisible(true);
    }

    /**
     * Work around the JVM's HiDPI detection gaps on Linux.
     *
     * On macOS and Windows the JVM reads the display's scale factor directly,
     * so the UI is sized correctly with no help from us. On Linux/X11 the JVM
     * only auto-detects "integer window scaling" (the GDK_SCALE mechanism).
     * Many desktops (Zorin/GNOME fractional, XFCE, ...) instead express e.g.
     * 200% by doubling the X server DPI (Xft.dpi=192) while leaving the window
     * scale at 1 -- which the JVM ignores, so the window opens at half size.
     *
     * We detect that case from Xft.dpi and set sun.java2d.uiScale ourselves.
     * This MUST run before any AWT/Swing/Java2D class is loaded, so it is the
     * very first thing main() does.
     *
     * Safe by construction: it only acts on Linux, never overrides an explicit
     * -Dsun.java2d.uiScale or a desktop that already sets GDK_SCALE, and on any
     * error or non-HiDPI display it does nothing (i.e. the previous behaviour).
     */
    private static void configureHiDpiScaling() {

        if (!System.getProperty("os.name", "").toLowerCase().contains("linux")) {
            return;                       // macOS/Windows detect scale natively
        }
        if (System.getProperty("sun.java2d.uiScale") != null) {
            return;                       // explicit override always wins
        }
        if (System.getenv("GDK_SCALE") != null) {
            return;                       // JVM's own detection already handles it
        }

        int scale = detectXftScale();
        if (scale >= 2) {                 // JDK 11/X11 only honours integer scales
            System.setProperty("sun.java2d.uiScale", Integer.toString(scale));
        }
    }

    /**
     * Read the X server's Xft.dpi (the value the desktop uses to express its
     * scaling) and convert it to an integer UI scale, where 96 dpi = 100%.
     * Returns 1 (i.e. "leave it alone") if it can't be determined.
     */
    private static int detectXftScale() {

        Process p = null;
        try {
            p = new ProcessBuilder("xrdb", "-query")
                    .redirectErrorStream(true).start();

            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(p.getInputStream()))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("Xft.dpi:")) {
                        double dpi = Double.parseDouble(line.substring(8).trim());
                        return (int) Math.round(dpi / 96.0);
                    }
                }
            }
        } catch (Exception ignore) {
            // xrdb missing, not an X11 session, unparseable value, etc.
            // -> fall through and leave scaling untouched.
        } finally {
            if (p != null) {
                p.destroy();
            }
        }
        return 1;
    }

    /** Main method */
    public static void main(String[] args) {

        // Fix Linux HiDPI sizing. Must run before any Swing/Java2D class loads.
        configureHiDpiScaling();

        // Use the macOS global menu bar instead of an in-window menu bar.
        // No effect on other platforms. Must be set before any Swing class loads.
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("apple.awt.application.name", TITLE);

        try {

            KunststoffLookAndFeel.setCurrentTheme(new KunststoffTheme());
            UIManager.setLookAndFeel(new KunststoffLookAndFeel());
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        new MultiAlarms();
    }
}
