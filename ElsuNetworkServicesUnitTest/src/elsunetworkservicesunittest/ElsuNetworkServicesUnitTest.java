/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package elsunetworkservicesunittest;

import elsu.network.application.*;

/**
 *
 * @author ss.dhaliwal
 */
public class ElsuNetworkServicesUnitTest extends AbstractNetworkServices {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            // instantiate the main controller class and call its run()
            // method to start service factory
            ElsuNetworkServicesUnitTest dgpsedl = new ElsuNetworkServicesUnitTest();
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
