package com.mosessoft.multialarms;

/*
 * Title:        MultiAlarms
 * Description:  Main program module
 * Company:      MosesSoft
 * @author       Peter van der Woude
 * @version      1.11
 */
 
import java.awt.Dimension;
import java.awt.Toolkit;
import java.util.ResourceBundle;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

import com.incors.plaf.kunststoff.KunststoffLookAndFeel;
import com.incors.plaf.kunststoff.KunststoffTheme;



public class MultiAlarms {

    /** Program title */
    public static String TITLE = "MultiAlarms";
    
    /** Program version number/string - to be extracted from properties file */
    public static String VERSION = "unknown";

	/** ResourceBundle object used to extract the properties file from .jar */ 
	private static ResourceBundle resBundle;

    public static void initResourceBundle() {
    	
        try {
            resBundle = ResourceBundle.getBundle("multialarms.multialarms");
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
    
        try {
            new com.mosessoft.Splash(
                new ImageIcon(this.getClass().getResource("splash.png")));                    
			
        } catch (Exception ex) {
            ex.printStackTrace();
        }

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

        try {

            KunststoffLookAndFeel.setCurrentTheme(new KunststoffTheme());
            UIManager.setLookAndFeel(new KunststoffLookAndFeel());        
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        new MultiAlarms();
    }
}
