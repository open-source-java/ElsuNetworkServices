package elsunetworkservicesunittest;

import elsu.network.application.*;

/**
 *
 * @author ss.dhaliwal
 */
public class ElsuNetworkServicesUnitTest extends AbstractNetworkApplication {

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
            System.err.println("ElsuNetworkServicesUnitTest, main, " + ex.getMessage());
            System.err.println(
                    "application.main, Usage: java -jar ElsuNetworkServicesUnitTest.jar "
                    + "[config/app.config] [/disabled]");
            System.exit(1);
        }
    }
}
