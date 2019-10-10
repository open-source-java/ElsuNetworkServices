package elsu.network.application;

public class ServiceApplication extends AbstractNetworkApplication {
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
        	ServiceApplication app = new ServiceApplication();
        	app.run(args);
        } catch (Exception ex){
            // Display a message if anything goes wrong
            System.err.println("ServiceApplication, main, " + ex.getMessage());
            System.err.println(
                    "application.main, Usage: java -jar ServiceApplication.jar "
                    + "[config/app.config] [/disabled]");
            System.exit(1);
        }
    }
}
