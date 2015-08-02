package elsu.network.factory;

import elsu.network.services.core.ServiceConfig;
import elsu.network.services.core.IService;
import elsu.network.services.core.AbstractService;
import elsu.network.core.ServiceStartupType;
import elsu.network.services.support.*;
import elsu.network.services.*;
import elsu.common.*;
import elsu.support.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;

/**
 * ServiceAbstractFactory is a framework for a flexible multi-threaded services.
 * The services managed by the framework can support listeners for specific
 * ports or initiate connections to other services.
 * <p>
 * ServiceAbstractFactory extends ConfigLoader
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
public class ServiceFactory extends AbstractEventPublisher implements IEventPublisher, IEventSubscriber {

    // <editor-fold desc="class private storage">
    // configuration reference object
    private ConfigLoader _config = null;
    // hashMap to track all services created 
    private volatile Map<Integer, IService> _services;
    // master connection limit
    private volatile int _maximumConnections;
    // total active connections
    private volatile int _serviceConnections = 0;
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
    public ServiceFactory() throws Exception {
        setConfig();

        // load configuration properties
        initializeLocalProperties();
    }

    public ServiceFactory(String config) throws Exception {
        setConfig(config);

        // load configuration properties
        initializeLocalProperties();
    }

    public ServiceFactory(String config, String[] suppresspath) throws Exception {
        setConfig(config, suppresspath);

        // load configuration properties
        initializeLocalProperties();
    }

    /**
     * initializeProperties() is a generic method to consolidate all initial
     * variable instantiation outside of class constructor. It allows the
     * variables to be reset from another method within a class if required.
     *
     */
    private void initializeLocalProperties() {
        try {
            this._maximumConnections = Integer.parseInt(
                    getConfig().getProperty("connection.maximum").toString());
        } catch (Exception ex) {
            this._maximumConnections = 10;
            logError(getClass().toString()
                    + ", initializeLocalProperties(), invalid connection.maximum, "
                    + ex.getMessage());
        }

        this._services = new HashMap<>();
    }
    // </editor-fold>

    // <editor-fold desc="class getter/setters">
    public ConfigLoader getConfig() {
        return this._config;
    }

    private void setConfig() {
        try {
            this._config = new ConfigLoader("", new String[]{
                "application.framework.attributes.key.",
                "application.services.service.", "application.childServices.forService.",
                ".services.childService", ".key"});
        } catch (Exception ex) {

        }
    }

    private void setConfig(String config) {
        try {
            this._config = new ConfigLoader(config,
                    new String[]{
                        "application.framework.attributes.key.",
                        "application.services.service.", "application.childServices.forService.",
                        ".services.childService", ".key"});
        } catch (Exception ex) {

        }
    }

    private void setConfig(String config, String[] suppressPath) {
        try {
            this._config = new ConfigLoader(config, suppressPath);
        } catch (Exception ex) {

        }
    }

    /**
     * getMaximumConnections() returns the value of the maximum connnections.
     * This is the maximum limit of connections from all services, including
     * child services. The connection limit can be exceeded only if the service
     * is configured with ignoreMaximumConnections = true in the service config.
     *
     * @return      <code>int</code> value of the maximum connections
     */
    public synchronized int getMaximumConnections() {
        return this._maximumConnections;
    }

    /**
     * setMaximumConnections(...) updates the value of the maximum connections.
     * The value can be reduced or increased. If reduced, the active connections
     * are not terminated but new connections are limited.
     *
     * @param allowedMax value to set the maximum connections to
     */
    public synchronized void setMaximumConnections(int allowedMax) {
        this._maximumConnections = allowedMax;
    }

    /**
     * getServiceAbstract(...) returns the service object by searching the
     * services list by the name of the service.
     *
     * @param serviceName is the name of the service being searched and is
     * derived from the name property of the connection in the config file.
     *
     * @return  <code>AbstractService</code> object
     */
    public synchronized IService getService(String serviceName) {
        IService result = null;

        // do not use iterator as changes to the hashMap can cause exceptions
        // when modified while looping
        ArrayList<Integer> svcList;
        svcList = new ArrayList<>(getServices().keySet());

        // loop through the list of services and see if any match the name
        // provided
        for (Integer key : svcList) {
            // key = port for the service
            IService service = (AbstractService) getServices().get(key);

            if (service.getServiceConfig().getServiceName().equals(serviceName)) {
                result = service;
                break;
            }

            // yield processing to other threads
            Thread.yield();
        }

        return result;
    }

    /**
     * getServiceAbstracts() returns the hashMap containing the services.
     *
     * @return <code>Map</code> of the services
     */
    public synchronized Map<Integer, IService> getServices() {
        return this._services;
    }

    /**
     * getServiceAbstractConnections() returns the count of all service
     * connections across the entire framework. The count can exceed maximum
     * connection value since services can choose to ignore the maximum
     * connections using config parameter.
     *
     * @return <code>int</code> total value stored
     */
    public synchronized int getServiceConnections() {
        return this._serviceConnections;
    }

    /**
     * decreaseServiceAbstractConnections() allows services to update the value
     * when there is a change in the state of the connection.
     */
    public synchronized void decreaseServiceConnections() {
        this._serviceConnections--;
    }

    /**
     * increaseServiceAbstractConnections() allows services to update the value
     * when there is a change in the state of the connection.
     */
    public synchronized void increaseServiceConnections() {
        this._serviceConnections++;
    }
    // </editor-fold>

    // <editor-fold desc="class methods">
    /**
     * addServiceAbstract(...) adds a new service into the service map based on
     * the port of the service. Each service uses two items to identify it's
     * uniqueness - port and service name.
     * <p>
     * ServiceAbstracts which are not configured to Automatic for startup type
     * are just added to the list, while, services configured for Automatic
     * startup are started by calling their start() method.
     *
     * @param service is the service object which needs to be managed
     * @param port is the unique service id and also the listener port if
     * service is configured as a SERVER service.
     * @throws java.lang.Exception
     */
    public synchronized void addService(IService service, int port)
            throws Exception {
        // convert to object for comparison in the hashMap
        Integer key = new Integer(port);

        // check if the port exists in the services hashMap
        // if port already exists, log error and return
        if (getServices().get(key) != null) {
            logError(getClass().toString() + "//addService//Port " + port
                    + " already in use");
            return;
        }

        // check if the service exists by its user name in the services hashMap
        // if service name is duplicated, log error and return
        if (getService(service.getServiceConfig().getServiceName()) != null) {
            logError(getClass().toString() + "//addService//Name "
                    + service.getServiceConfig().getServiceName()
                    + " already in use");
            return;
        }

        // new service, store the service object with port as its key
        getServices().put(key, service);

        // check the service startup type, if Automatic, notify service to 
        // start.  start() is a overloaded method from base service and allows
        // services to perform pre-setup before connections are active
        if (service.getServiceConfig().getStartupType()
                == ServiceStartupType.AUTOMATIC) {
            service.start();
            logInfo(getClass().toString() + ", addService(), starting service "
                    + service.getClass().getName()
                    + " on port " + port);
        }
    }

    /**
     * removeServiceAbstract(...) removes the service from the services list
     * hashMap based on the service unique id (port). ServiceAbstracts can be
     * permanently removed from the list by specifying true to delete parameter.
     * <p>
     * If delete options is false, the service is signaled to be in non-running
     * state, which turns off the listener is the service type is SERVER and
     * signals all connections to terminate gracefully or by force by closing
     * the connection in and out streams.
     *
     * @param port is the service unique id
     * @param delete option if set to true ensure the service is removed from
     * the service list (hashMap) and cannot be restarted.
     * @return <code>boolean</code> if the service was successfully removed.
     */
    public synchronized boolean removeService(int port, boolean delete) {
        // convert to object for comparison in the hashMap
        Integer key = new Integer(port);

        // retrive the service object from the service list
        // if port does not exist, log error and return
        AbstractService service = (AbstractService) getServices().get(key);
        if (service == null) {
            return false;
        }

        // send signal to the service to shutdown before we remove it from 
        // the service list to prevent orphanded services
        service.shutdown();

        // if delete = true, remove the service from the service list
        // permanently.  This remove the service config and therefore, once
        // removed, the service cannot be restarted.
        if (delete) {
            getServices().remove(key);
        }

        // log the action
        logInfo(getClass().toString() + ", removeService(), stopping service "
                + service.getClass().getName()
                + " on port " + port);

        return true;
    }

    /**
     * startServiceAbstract(...) is custom method to allow a service to be
     * started or restarted. This method is normally used by control service or
     * a heartbeat service which restarts stopped services to recover from a
     * failure.
     *
     * @param port is the service unique id
     * @return <code>boolean</code> if the service was successfully started or
     * was already running.
     * @throws java.lang.Exception
     */
    public synchronized boolean startService(int port)
            throws Exception {
        // convert to object for comparison in the hashMap
        Integer key = new Integer(port);

        // retrive the service object from the service list
        // if port does not exist, log error and return
        AbstractService svc = (AbstractService) getServices().get(key);
        if (svc == null) {
            logError(getClass().toString() + "//startService//Port " + port
                    + " not found.");

            return false;
        }

        // check if the service is running.  if the service is not running
        // start the service else skip and continue.
        if (!svc.isRunning()) {
            svc.start();
            logInfo(getClass().toString()
                    + ", startService(), starting service "
                    + svc.getClass().getName()
                    + " on port " + port);
        }

        return true;
    }

    /**
     * initializeServiceAbstracts() method is used to iterate through the list
     * of services and start activating them one by one. Only services which are
     * not marked as Disabled are created through reflection using the service
     * class and constructor parameters.
     * <p>
     * ServiceAbstracts marked Automatic are started once they have their
     * instance was successfully created. ServiceAbstracts marked DelayedStart
     * are started after the Automatic services have been started.
     * <p>
     * DelayedStart services are always started last because they may have
     * dependencies on other services.
     *
     * @throws Exception
     */
    public void initializeServices() throws Exception {
        try {
            ServiceConfig config;
            String serviceName;

            // collect the list of all services into array list for processing
            // do not use iterator since control service can change the scope
            // of the iterate and will result in exceptions.
            ArrayList<String> spIterator;
            spIterator = new ArrayList<>(getConfig().getClassSet());

            // loop through all the services in the service list
            for (String spObject : spIterator) {
                serviceName = spObject.replace(".class", "");

                // make sure this is not a child service type: PUBLISHER or SUBSCRIBER
                if (getConfig().getProperty(serviceName + ".serviceType").toString().equals("SUBSCRIBER")
                        || getConfig().getProperty(serviceName + ".serviceType").toString().equals("PUBLISHER")) {
                    // ignore this class type
                } else {
                    // extract the service properties for parsing
                    config = ServiceConfig.LoadConfig(getConfig(), serviceName);

                    // control service is a custom service and there does not use
                    // reflection but direct instantiation.
                    if (config.getServiceClass().equals(
                            "elsu.network.services.support.ControlService")) {
                        // is the service disabled, if not create an instance of
                        // the service
                        if (config.getStartupType() != ServiceStartupType.DISABLED) {
                            // log the action
                            notifyListeners(new EventObject(this), StatusType.INFORMATION,
                                    ".. service activated (" + spObject.toString() + ")",
                                    config);

                            // create the service instance
                            IService service = new ControlService(this, config);

                            // add the service to the service list in the factory
                            addService(service, config.getConnectionPort());
                        }
                    } else if (config.getStartupType() != ServiceStartupType.DISABLED) {
                         // service is not control service, so if it is not 
                        // disabled process the service properties

                        // log the action
                        notifyListeners(new EventObject(this), StatusType.INFORMATION,
                                ".. service activated (" + spObject.toString() + ")",
                                config);

                        // using reflection, load the class for the service
                        Class<?> serviceClass = Class.forName(config.getServiceClass());

                        // create service constructor discovery type parameter array
                        // populate it with the required class types
                        Class<?>[] argTypes = {ServiceFactory.class, String.class,
                            ServiceConfig.class};

                        // retrieve the matching constructor for the service using
                        // reflection
                        Constructor<?> cons = serviceClass.getDeclaredConstructor(
                                argTypes);

                        // create parameter array and populate it with values to 
                        // pass to the service constructor
                        Object[] arguments
                                = {this, config.getServiceClass(), config};

                        // create new instance of the service using the discovered
                        // constructor and parameters
                        IService service = (IService) cons.newInstance(arguments);

                        // add the service to the service list in the factory
                        addService(service, config.getConnectionPort());
                    }

                    // yield processing to other threads
                    Thread.yield();
                }

                // since all the services which were not disabled were already 
                // loaded and services which were Automatic were started previously
                // we need to now start the services marked DelayedStart.
                // collect the list of all services into array list for processing
                // do not use iterator since control service can change the scope
                // of the iterate and will result in exceptions.
                List<IService> serviceList;
                serviceList = new ArrayList<>(getServices().values());

                // loop through all the services in the service list
                for (IService svc : serviceList) {
                    // if the service is marked delayed start, process the service
                    if (svc.getServiceConfig().getStartupType()
                            == ServiceStartupType.DELAYEDSTART) {
                        // temporarily update the service startup type to Automatic
                        // so we can use the common start method
                        svc.getServiceConfig().setStartupType(
                                ServiceStartupType.AUTOMATIC);

                        // start the service.  we do not need to add the service,
                        // just start it.
                        svc.start();

                        // reset the service startup type back to DelayedStart
                        svc.getServiceConfig().setStartupType(
                                ServiceStartupType.DELAYEDSTART);
                    }

                    // yield processing to other threads
                    Thread.yield();
                }

                // clear the service list to allow garbage collection to recover
                // the memory
                serviceList = null;
            }
        } catch (Exception ex) {
            // log error if there was any exception in processing during
            // reflection or parameter discovery and throw it to allow calling
            // function to handle it
            logError(getClass().getName() + ", initializeServices(), "
                    + ex.getMessage());
            throw new Exception(ex.getMessage());
        }
    }

    /**
     * shutdownServiceAbstracts() method is used to allow application to
     * gracefully signal shutdown to all running services.
     */
    public synchronized void shutdownServices() {
        // collect the list of all services into array list for processing
        // do not use iterator since control service can change the scope
        // of the iterate and will result in exceptions.
        List<IService> serviceList;
        serviceList = new ArrayList<>(getServices().values());

        // loop through all the services in the service list
        for (IService svc : serviceList) {
            // if the service is running, call the shutdown() method for the
            // service.s
            if (svc.isRunning()) {
                svc.shutdown();
            }

            // yield processing to other threads
            Thread.yield();
        }
    }
    // </editor-fold>

    // <editor-fold desc="class logging">
    /**
     * logDebug(...) method is an interface method to Log4JManager logging
     * capability. This method is provided to allow multiple threads to log to
     * one file.
     * <p>
     * Debug messages are will only be processed if log4j.properties are set to
     * log debug or info or warn or error or fatal messages
     *
     * @param info is the object whose string representation will be stored in
     * the log file
     */
    public synchronized void logDebug(Object info) {
        getConfig().logDebug(info.toString());
    }

    /**
     * logError(...) method is an interface method to Log4JManager logging
     * capability. This method is provided to allow multiple threads to log to
     * one file.
     * <p>
     * Error messages are will only be processed if log4j.properties are set to
     * log error or fatal messages
     *
     * @param info is the object whose string representation will be stored in
     * the log file
     */
    public synchronized void logError(Object info) {
        getConfig().logError(info.toString());
    }

    /**
     * logInfo(...) method is an interface method to Log4JManager logging
     * capability. This method is provided to allow multiple threads to log to
     * one file.
     * <p>
     * Debug messages are will only be processed if log4j.properties are set to
     * log info or warn or error or fatal messages
     *
     * @param info is the object whose string representation will be stored in
     * the log file
     */
    public synchronized void logInfo(Object info) {
        getConfig().logInfo(info.toString());
    }
    // </editor-fold>

    /**
     * toString() method is overridden from default Object toString() to display
     * custom information of the factory object.
     *
     * @return
     */
    @Override
    public synchronized String toString() {
        // create string builder to store the results
        StringBuilder result = new StringBuilder();

        // start the object representation
        result.append("<object attr='").append(getClass().getName()).append("'>");

        // store the # of services configured and loaded
        result.append("<services>")
                .append("<size>").append(getServices().size()).append("</size>");

        // collect the list of all services into array list for processing
        // do not use iterator since control service can change the scope
        // of the iterate and will result in exceptions.
        ArrayList<Integer> svcList;
        svcList = new ArrayList<>(getServices().keySet());

        // loop through all the services in the service list
        for (Integer key : svcList) {
            // get the service port and service object for the port
            // Integer key = Integer.parseInt(svc.toString());

            // key = port for the service
            AbstractService service = (AbstractService) getServices().get(key);

            // call the service toString() method to get the service
            // string representation
            result.append(service.toString());

            // yield processing to other threads
            Thread.yield();
        }

        // store the local factory configuration properties
        result.append("</services>");

        result.append("<maxConnections>").append(getMaximumConnections()).append("</maxConnections>");
        result.append("<active>").append(getServiceConnections()).append("</active>");

        // close the object representation
        result.append("</object>");

        // return the final string
        return result.toString();
    }

    /**
     * toString(...) method is overridden from default Object toString() to
     * display custom information of the factory object.
     *
     * @param out is the output stream where the data will sent.
     */
    public synchronized void toString(PrintWriter out) {
        // retrieve the toString() representation of this object and write
        // it to the output stream provided
        out.print(toString() + GlobalStack.LINESEPARATOR);
        out.flush();
    }

    @Override
    public synchronized void EventHandler(EventObject e, StatusType s, String message, Object o) {
        switch (s) {
            case DEBUG:
                getConfig().logDebug(message);
                break;
            case ERROR:
                getConfig().logError(message);
                break;
            case INFORMATION:
                getConfig().logInfo(message);
                break;
            default:
                break;
        }
    }
}
