package elsu.network.application;

import elsu.common.*;
import elsu.events.*;
import elsu.network.core.*;
import elsu.network.factory.*;
import elsu.network.services.core.*;
import elsu.support.*;
import java.io.*;
import java.util.*;

public class AbstractServiceManager extends AbstractEventManager implements IEventPublisher, IEventSubscriber {

    // <editor-fold desc="class private storage">
    // runtime sync object
    private Object _runtimeSync = new Object();
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
    public AbstractServiceManager() throws Exception {
        setConfig();

        // load configuration properties
        initializeLocalProperties();
    }

    public AbstractServiceManager(String config) throws Exception {
        setConfig(config);

        // load configuration properties
        initializeLocalProperties();
    }

    public AbstractServiceManager(ConfigLoader config) throws Exception {
        setConfig(config);

        // load configuration properties
        initializeLocalProperties();
    }

    public AbstractServiceManager(String config, String[] filterPath)
            throws Exception {
        setConfig(config, filterPath);

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
        // update the service status types, this is a throw away initialization
        // since the class initializer calls static method to update enum 
        // properties for Events
        try {
            this._maximumConnections = Integer.parseInt(
                    getConfig().getProperty("application.framework.attributes.key.connection.maximum").toString());
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
        ConfigLoader result = null;

        synchronized (this._runtimeSync) {
            result = this._config;
        }

        return result;
    }

    private void setConfig() throws Exception {
        this._config = new ConfigLoader("", null);
    }

    private void setConfig(String config) throws Exception {
        this._config = new ConfigLoader(config, null);
    }

    private void setConfig(ConfigLoader config) throws Exception {
        this._config = config;
    }

    private void setConfig(String config, String[] filterPath)
            throws Exception {
        this._config = new ConfigLoader(config, filterPath);
    }

    /**
     * getProperty(...) returns the string value for the key provided from the
     * global factory properties.
     *
     * @param key is the property which is being searched
     * @return <code>String</code> value of the key
     */
    public Object getProperty(String key) {
        Object result = null;

        synchronized (this._runtimeSync) {
            result = getConfig().getProperty(key);
        }

        return result;
    }

    /**
     * getKeySet() returns the Set <String> which is the main storage for all
     * the global properties.
     *
     * @return <code>Set<String></code> with the global properties
     */
    public Set<String> getKeySet() {
        Set<String> result = null;

        synchronized (this._runtimeSync) {
            result = getConfig().getKeySet();
        }

        return result;
    }

    /**
     * getMaximumConnections() returns the value of the maximum connnections.
     * This is the maximum limit of connections from all services, including
     * child services. The connection limit can be exceeded only if the service
     * is configured with ignoreMaximumConnections = true in the service config.
     *
     * @return      <code>int</code> value of the maximum connections
     */
    public int getMaximumConnections() {
        int result = 0;

        synchronized (this._runtimeSync) {
            result = this._maximumConnections;
        }

        return result;
    }

    /**
     * setMaximumConnections(...) updates the value of the maximum connections.
     * The value can be reduced or increased. If reduced, the active connections
     * are not terminated but new connections are limited.
     *
     * @param allowedMax value to set the maximum connections to
     */
    public void setMaximumConnections(int allowedMax) {
        synchronized (this._runtimeSync) {
            this._maximumConnections = allowedMax;
        }
    }

    /**
     * getService(...) returns the service object by searching the services list
     * by the name of the service.
     *
     * @param serviceName is the name of the service being searched and is
     * derived from the name property of the connection in the config file.
     *
     * @return  <code>AbstractService</code> object
     */
    public IService getService(String serviceName) {
        IService result = null;

        // do not use iterator as changes to the hashMap can cause exceptions
        // when modified while looping
        ArrayList<Integer> svcList;
        synchronized (this._runtimeSync) {
            svcList = new ArrayList<>(getServices().keySet());
        }

        // loop through the list of services and see if any match the name
        // provided
        for (Integer key : svcList) {
            synchronized (this._runtimeSync) {
                // key = port for the service
                IService service = (IService) getServices().get(key);

                if (service.getServiceConfig().getServiceName().equals(serviceName)) {
                    result = service;
                    break;
                }
            }

            // yield processing to other threads
            Thread.yield();
        }

        return result;
    }

    /**
     * getService(...) returns the service object by searching the services list
     * by the name of the service.
     *
     * @param port is the name of the service being searched and is derived from
     * the name property of the connection in the config file.
     *
     * @return  <code>AbstractService</code> object
     */
    public IService getService(int port) {
        IService result = null;

        // do not use iterator as changes to the hashMap can cause exceptions
        // when modified while looping
        synchronized (this._runtimeSync) {
            result = (IService) getServices().get(port);
        }

        return result;
    }

    /**
     * getServiceAbstracts() returns the hashMap containing the services.
     *
     * @return <code>Map</code> of the services
     */
    public Map<Integer, IService> getServices() {
        Map<Integer, IService> result = null;

        synchronized (this._runtimeSync) {
            result = this._services;
        }

        return result;
    }

    /**
     * getServiceAbstractConnections() returns the count of all service
     * connections across the entire framework. The count can exceed maximum
     * connection value since services can choose to ignore the maximum
     * connections using config parameter.
     *
     * @return <code>int</code> total value stored
     */
    public int getServiceConnections() {
        int result = 0;

        synchronized (this._runtimeSync) {
            result = this._serviceConnections;
        }

        return result;
    }

    /**
     * decreaseServiceAbstractConnections() allows services to update the value
     * when there is a change in the state of the connection.
     */
    public void decreaseServiceConnections() {
        synchronized (this._runtimeSync) {
            this._serviceConnections--;
        }
    }

    /**
     * increaseServiceAbstractConnections() allows services to update the value
     * when there is a change in the state of the connection.
     */
    public void increaseServiceConnections() {
        synchronized (this._runtimeSync) {
            this._serviceConnections++;
        }
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
     * @param service is the service object which needs to be managed service is
     * configured as a SERVER service.
     * @throws java.lang.Exception
     */
    public void addService(IService service)
            throws Exception {
        // convert to object for comparison in the hashMap
        Integer port = new Integer(service.getServiceConfig().getConnectionPort());

        // check if the port exists in the services hashMap
        // if port already exists, log error and return
        if (getServices().get(port) != null) {
            logError(getClass().toString() + "//addService//Port " + port
                    + " already in use");
            return;
        }

        // check if the service exists by its user name in the services hashMap
        // if service name is duplicated, log error and return
        String serviceName = service.getServiceConfig().getServiceName();
        if (getService(serviceName) != null) {
            logError(getClass().toString() + "//addService//Name "
                    + service.getServiceConfig().getServiceName()
                    + " already in use");
            return;
        }

        // new service, store the service object with port as its key
        synchronized (this._runtimeSync) {
            getServices().put(port, service);
        }

        // set notification to services with factory reference
        notifyListeners(this, EventStatusType.INITIALIZE, null, service);

        // check the service startup type, if Automatic, notify service to 
        // start.  start() is a overloaded method from base service and allows
        // services to perform pre-setup before connections are active
        if ((service.getServiceConfig().getStartupType()
                == ServiceStartupType.AUTOMATIC)
                || (service.getServiceConfig().getStartupType()
                == ServiceStartupType.SYSTEM)) {
            //service.start();
            Object status = notifyListeners(this, EventStatusType.START, null, service);
            if (status instanceof Exception) {
                throw new Exception((Exception) status);
            }

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
    public boolean removeService(int port, boolean delete) {
        // convert to object for comparison in the hashMap
        Integer key = new Integer(port);

        // retrive the service object from the service list
        // if port does not exist, log error and return
        IService service = (IService) getServices().get(key);
        if (service == null) {
            return false;
        }

        // send signal to the service to shutdown before we remove it from 
        // the service list to prevent orphanded services
        //service.shutdown();
        notifyListeners(this, EventStatusType.SHUTDOWN, null, service);

        // if delete = true, remove the service from the service list
        // permanently.  This remove the service config and therefore, once
        // removed, the service cannot be restarted.
        if (delete) {
            synchronized (this._runtimeSync) {
                getServices().remove(key);
            }
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
    public boolean startService(int port)
            throws Exception {
        // convert to object for comparison in the hashMap
        Integer key = new Integer(port);

        // retrive the service object from the service list
        // if port does not exist, log error and return
        IService service = (IService) getServices().get(key);
        if (service == null) {
            logError(getClass().toString() + "//startService//Port " + port
                    + " not found.");

            return false;
        }

        // check if the service is running.  if the service is not running
        // start the service else skip and continue.
        if (!service.isRunning()) {
            //service.start();
            Object status = notifyListeners(this, EventStatusType.START, null, service);
            if (status instanceof Exception) {
                throw new Exception((Exception) status);
            }

            logInfo(getClass().toString()
                    + ", startService(), starting service "
                    + service.getClass().getName()
                    + " on port " + port);
        }

        return true;
    }

    /**
     * validateService(...) method is used to check if the configured service is
     * running or not. If the service is not running and is a system service,
     * then it will be restarted.
     *
     * @param serviceName
     * @return <code>boolean</code> false if service could not be started, true
     * if service is running or was restarted
     */
    public boolean validateService(String serviceName) throws Exception {
        IService service = getService(serviceName);
        return validateService(service);
    }

    /**
     * validateService(...) method is used to check if the configured service is
     * running or not. If the service is not running and is a system service,
     * then it will be restarted.
     *
     * @param serviceName
     * @return <code>boolean</code> false if service could not be started, true
     * if service is running or was restarted
     */
    public boolean validateService(int port) throws Exception {
        IService service = getService(port);
        return validateService(service);
    }

    private boolean validateService(IService service) throws Exception {
        boolean result = true;

        // if service is running, exit
        if (service != null) {
            result = isRunning(service.getServiceConfig().getConnectionPort());
            if (!result) {
                startService(service.getServiceConfig().getConnectionPort());
            } else {
                service.validateService();
            }
        } else {
            throw new Exception(getClass().getName() + ", validateService(), "
                    + service.getServiceConfig().getServiceName()
                    + "@" + service.getServiceConfig().getConnectionPort()
                    + " is not configured.");
        }

        return result;

    }

    public boolean isRunning(int port) {
        boolean result = false;

        synchronized (this._runtimeSync) {
            result = ((IService) getServices().get(port)).isRunning();
        }

        return result;
    }

    public boolean isRunning(String serviceName) {
        boolean result = false;
        IService service = getService(serviceName);

        if (service != null) {
            result = isRunning(service.getServiceConfig().getConnectionPort());
        }

        return result;
    }

    /**
     * shutdownServiceAbstracts() method is used to allow application to
     * gracefully signal shutdown to all running services.
     */
    public void shutdownServices() {
        // collect the list of all services into array list for processing
        // do not use iterator since control service can change the scope
        // of the iterate and will result in exceptions.
        List<IService> serviceList;
        serviceList = new ArrayList<>(getServices().values());

        // loop through all the services in the service list
        for (IService service : serviceList) {
            // if the service is running, call the shutdown() method for the
            // service.s
            if (service.isRunning()) {
                //service.shutdown();
                notifyListeners(this, EventStatusType.SHUTDOWN, null, service);
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
    public void logDebug(Object info) {
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
    public void logError(Object info) {
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
    public void logInfo(Object info) {
        getConfig().logInfo(info.toString());
    }
    // </editor-fold>

    // <editor-fold desc="class event listener">
    @Override
    public Object EventHandler(Object sender, IEventStatusType status, String message, Object o) {
        Object result = null;

        if (sender instanceof IService) {
            switch (status.getId()) {
                default:
                    logInfo(sender.getClass() + ", " + status.getName() + ", " + message);
                    break;
            }
        }

        return result;
    }
// </editor-fold>

    /**
     * toString() method is overridden from default Object toString() to display
     * custom information of the factory object.
     *
     * @return
     */
    @Override
    public String toString() {
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
            IService service = (IService) getServices().get(key);

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
    public void toString(PrintWriter out) {
        // retrieve the toString() representation of this object and write
        // it to the output stream provided
        out.print(toString() + GlobalStack.LINESEPARATOR);
        out.flush();
    }
}
