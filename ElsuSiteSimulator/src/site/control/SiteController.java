/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package site.control;

import elsu.network.application.*;

/**
 *
 * @author ss.dhaliwal
 */
public class SiteController extends AbstractNetworkApplication {

    /**
     * main(Strings[] args) method is the applicaton entry point for
     * ElsuMessageProcessor. The optional parameters are: custom config file and
     * /disableAll option.
     *
     * @param args
     */
    public static void main(String[] args) {
        try {
            // instantiate the main controller class and call its run()
            // method to start service factory
            SiteController dgpsedl = new SiteController();
            dgpsedl.run(args);
        } catch (Exception ex){
            // Display a message if anything goes wrong
            System.err.println("ElsuSiteSimulator, main, " + ex.getMessage());
            System.err.println(
                    "application.main, Usage: java -jar ElsuSiteSimulator.jar "
                    + "[config/app.config] [/disabled]");
            System.exit(1);
        }
    }
}
