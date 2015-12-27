package elsu.network.application;

import elsu.network.factory.*;

/**
 * ServiceManager is a framework for a flexible multi-threaded services. The
 * services managed by the framework can support listeners for specific ports or
 * initiate connections to other services.
 * <p>
 * There are two main types of main services: Server or Client and two child
 * services: Subscriber or Publisher. Server service creates a listener and
 * after accepting the connection and uses the connection input/output streams
 * to perform service actions. Client service is used to support child services
 * and does not create a listener by default. Child services are used to group
 * services under one parent service when multiple actions have common
 * configuration properties.
 *
 * 20141128 SSD updated for reflection generics warning on
 * getDeclaredConstructor
 *
 * @see ConfigLoader
 * @see AbstractService
 * @see IService
 * @author Seraj Dhaliwal (seraj.s.dhaliwal@uscg.mil)
 * @version .51
 */
public class ServiceManager extends AbstractServiceManager {

    // <editor-fold desc="class private storage">
    // </editor-fold>

    // <editor-fold desc="class constructor destructor">
    /**
     * ServiceAbstractFactory(...) does not support a no-argument constructor.
     * The argument specified is used to allow logging which factory loads the
     * initial configuration parameters in the super class ConfigLoader.
     *
     * @param logStream is the console output stream passed from the main
     * program (normally System.out) to allow user to see exception or info
     * messages.
     * @throws java.lang.Exception
     * @see ConfigLoader
     */
    public ServiceManager() throws Exception {
        super();
    }

    public ServiceManager(String config) throws Exception {
        super(config);
    }

    public ServiceManager(String config, String[] suppresspath) throws Exception {
        super(config, suppresspath);
    }
    // </editor-fold>

    // <editor-fold desc="class getter/setters">
    /**
     * run() is entry point for the AbstractApplication to try to instantiate
     * the ServiceManager with default or user defined configuration.
     * <p>
     * if args are provided, then default value of config property (app.config)
     * is changed to provided value. Note, this should be full path since it is
     * external to the config packaged within the jar file
     *
     * @param args
     * @see ServiceManager
     * @see ServiceFactory
     * @see ConfigLoader
     */
    public void run() {
        try {
            ServiceFactory.initializeServices(this);
        } catch (Exception ex) {
            logInfo(getClass().getName() + ", run(), "
                    + ex.getMessage());
        }
    }
    // </editor-fold>
}
