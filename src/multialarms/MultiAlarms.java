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
            String message = "The file 'multialarms.properties' was not found" +
                             " in archive." +
                             "\nPlease obtain a valid version of " + TITLE + ".jar";
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

    /** Main method */
    public static void main(String[] args) {

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
