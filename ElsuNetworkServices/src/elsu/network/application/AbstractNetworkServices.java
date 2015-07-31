package elsu.network.application;

import elsu.network.service.factory.*;
import java.io.*;
import java.util.Collections.*;

/**
 * AbstractNetworkServices is the main class which is used to instantiate
 user request for different services as defined in the app.config file.
 * <p>
 * Standard invocation is "java -jar NCSElsuNetworkServicesAbstract.jar". User
 * can specify following additional arguments:
 *
 * <ul>
 * <li> custom config file by specifying the config file as argument for
 * example: java -jar NCSElsuNetworkServicesAbstract.jar c:\custom\app.config
 * <li> disable all services upon startup by specifying /disableAll argument.
 * Note: /disableAll does not disable services which have attribute
 * service.no.disable set to true.
 * </ul>
 *
 * @author Seraj Dhaliwal (seraj.s.dhaliwal@uscg.mil)
 * @version .51
 */
public abstract class AbstractNetworkServices {

    // <editor-fold desc="class private storage">
    // storage for factory instance; singleton pattern, only one factory can
    // be running at a time per application
    private volatile ServiceFactory _factory = null;
    // </editor-fold>

    // <editor-fold desc="class inline">
    // private class ShutdownHook extends Thread {
    //
    //     public void run() {
    //         doAppCleanup();
    //     }
    // }
    //
    // private void doAppCleanup() {
    //     // delete oracle change monitor registration
    //     if (getFactory() != null) {
    //         getFactory().shutdownServices();
    //         setFactory(null);
    //     }
    // }
    // </editor-fold>
    // <editor-fold desc="class getter/setters">
    /**
     * getFactory() returns the current active factory instance. There is no
     * public set method as this is a singleton and should only be created once
     * by the main controller or the application.
     *
     * @return      <code>ServiceFactory</code> if the factory is created with no
     * errors.
     * @see ServiceFactory
     */
    public synchronized ServiceFactory getFactory() {
        return this._factory;
    }

    /**
     * setFactory() is a private method for the controller or application to
     * update the global variable _factory.
     *
     * @param factory instance of the ServiceFactory.
     * @see ServiceFactory
     */
    private synchronized void setFactory(ServiceFactory factory) {
        this._factory = factory;
    }
    // </editor-fold>

    // <editor-fold desc="class methods">
    /**
     * run() is entry point for the MessageProcesor to try to instantiate the
     * ServiceFactory with default or user defined configuration.
     * <p>
     * Since there is no configuration loaded, the errors are displayed to the
     * default system error stream. If there is error during execution, the
     * method will display the error, usage help, and exit to system prompt.
     * <p>
     * if args are provided, then default value of config property (app.config)
     * is changed to provided value. Note, this should be full path since it is
     * external to the config packaged within the jar file
     *
     * @param args
     * @see ServiceFactory
     * @see ConfigLoader
     */
    public void run(String[] args) {
        try {
            //ShutdownHook sh = new ShutdownHook();
            //Runtime.getRuntime().addShutdownHook(sh);

            // if user as provided command line arguments, parse them
            if (args.length > 0) {
                // set the global static APPCONFIG variable so the 
                // ServiceFactory when instantiated will use it
                ServiceFactory.APPCONFIG = args[0];
            }

            // create ServiceFactory and store it in class variable.  since 
            // logging is not yet configured, ServiceFactory, uses System.out
            // to display any notifications.
            setFactory(new ServiceFactory(System.out));

            // allow the ServiceFactory to initialize services stored in the
            // default app.config or user specified custom config
            getFactory().initializeServices();
        } catch (Exception ex) {
            System.err.println("getClass().toString(), run(), "
                    + ex.getMessage());
            System.err.println(getClass().toString()
                    + ", Usage: java -jar <classname>.jar "
                    + "[./app.config] [/disabled]");
            System.exit(1);
        }
    }
    // </editor-fold>
}
