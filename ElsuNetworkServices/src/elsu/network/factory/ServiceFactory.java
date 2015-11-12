package elsu.network.factory;

import elsu.events.*;
import elsu.network.services.system.*;
import elsu.network.services.core.*;
import elsu.network.core.*;
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
public class ServiceFactory extends AbstractEventManager implements IEventPublisher, IEventSubscriber {

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
        // update the service status types, this is a throw away initialization
        // since the class initializer calls static method to update enum 
        // properties for Events
        ServiceStatusType sst = new ServiceStatusType();

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
        ConfigLoader result = null;

        synchronized (this._runtimeSync) {
            result = this._config;
        }

        return result;
    }

    private void setConfig() {
        try {
            this._config = new ConfigLoader("", null);
        } catch (Exception ex) {

        }
    }

    private void setConfig(String config) {
        try {
            this._config = new ConfigLoader(config, null);
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
     * getProperties() returns the hashmap <String, Object> which is the main
     * storage for all the global properties.
     *
     * @return <code>Map<String, Object></code> with the global properties
     */
    public Map<String, Object> getProperties() {
        Map<String, Object> result = null;

        synchronized (this._runtimeSync) {
            result = getConfig().getProperties();
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
        String serviceName = "";
        IService service = null;

        try {
            // collect the list of all services into array list for processing
            // do not use iterator since control service can change the scope
            // of the iterate and will result in exceptions.
            ArrayList<String> spIterator;
            spIterator = new ArrayList<>(getConfig().getClassSet());

            // loop through all the services in the service list
            for (String spObject : spIterator) {
                serviceName = spObject.replace(".class", "");

                initializeServices(serviceName);
            }

            // since all the services which were not disabled were already 
            // loaded and services which were Automatic were started previously
            // we need to now start the services marked DelayedStart.
            // collect the list of all services into array list for processing
            // do not use iterator since control service can change the scope
            // of the iterate and will result in exceptions.
            List<IService> serviceList;
            serviceList = new ArrayList<>(getServices().values());

            for (Iterator<IService> it = serviceList.iterator(); it.hasNext();) {
                service = it.next();
                if (service.getServiceConfig().getStartupType()
                        == ServiceStartupType.DELAYEDSTART) {
                    // temporarily update the service startup type to Automatic
                    // so we can use the common start method
                    service.getServiceConfig().setStartupType(
                            ServiceStartupType.AUTOMATIC);

                    // start the service.  we do not need to add the service,
                    // just start it.
                    //service.start();
                    Object status = notifyListeners(this, EventStatusType.START, null, service);
                    if (status instanceof Exception) {
                        logError(getClass().getName() + ", initializeServices(), "
                                + service.getClass().getName() + ", service load error, "
                                + ((Exception) status).getMessage());
                    }

                    // reset the service startup type back to DelayedStart
                    service.getServiceConfig().setStartupType(
                            ServiceStartupType.DELAYEDSTART);
                }

                Thread.yield();
            }

            // clear the service list to allow garbage collection to recover
            // the memory
            serviceList = null;
        } catch (Exception ex) {
            // log error if there was any exception in processing during
            // reflection or parameter discovery and throw it to allow calling
            // function to handle it
            logError(getClass().getName() + ", initializeServices(), "
                    + ex.getMessage());
            throw new Exception(ex.getMessage());
        }
    }

    private void initializeServices(String serviceName) {
        ServiceConfig config = null;
        IService service = null;

        try {
            // make sure this is not a child service type: PUBLISHER or SUBSCRIBER
            if (getConfig().getProperty(serviceName + ".serviceType").toString().equals("SUBSCRIBER")
                    || getConfig().getProperty(serviceName + ".serviceType").toString().equals("PUBLISHER")) {
                // ignore this class type
            } else {
                // extract the service properties for parsing
                config = ServiceConfig.LoadConfig(getConfig(), serviceName);

                // control service is a custom service and there does not use
                // reflection but direct instantiation.
                if (serviceName.equals("controlService")) {
                    // is the service disabled, if not create an instance of
                    // the service
                    if (config.getStartupType() != ServiceStartupType.DISABLED) {
                        // log the action
                        notifyListeners(this, EventStatusType.INFORMATION,
                                ".. service activated (" + serviceName + ")",
                                config);

                        // create the service instance
                        service = new ControlService(config);

                        // connect the factory event listeners
                        ((IEventPublisher) service).addEventListener(this);
                        addEventListener((IEventSubscriber) service);

                        // add the service to the service list in the factory
                        addService(service);
                    }
                } else if (config.getStartupType() != ServiceStartupType.DISABLED) {
                         // service is not control service, so if it is not 
                    // disabled process the service properties

                    // log the action
                    notifyListeners(this, EventStatusType.INFORMATION,
                            ".. service activated (" + serviceName + ")",
                            config);

                    // using reflection, load the class for the service
                    Class<?> serviceClass = Class.forName(config.getServiceClass());

                    // create service constructor discovery type parameter array
                    // populate it with the required class types
                    Class<?>[] argTypes = {String.class, ServiceConfig.class};

                    // retrieve the matching constructor for the service using
                    // reflection
                    Constructor<?> cons = serviceClass.getDeclaredConstructor(
                            argTypes);

                    // create parameter array and populate it with values to 
                    // pass to the service constructor
                    Object[] arguments
                            = {config.getServiceClass(), config};

                    // create new instance of the service using the discovered
                    // constructor and parameters
                    service = (IService) cons.newInstance(arguments);

                    // connect the factory event listeners
                    ((IEventPublisher) service).addEventListener(this);
                    addEventListener((IEventSubscriber) service);

                    // add the service to the service list in the factory
                    addService(service);
                }

                // yield processing to other threads
                Thread.yield();
            }
        } catch (Exception ex) {
            // log error if there was any exception in processing during
            // reflection or parameter discovery and throw it to allow calling
            // function to handle it
            logError(getClass().getName() + ", initializeServices(), "
                    + serviceName + " service load error, " + ex.getMessage());

            if (service != null) {
                try {
                    removeService(service.getServiceConfig().getConnectionPort(), false);
                } catch (Exception exi) {
                }
            }
        }
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
        boolean result = true;
        IService service = getService(serviceName);

        // if service is running, exit
        if (service != null) {
            result = isRunning(service.getServiceConfig().getConnectionPort());
            if (!result) {
                startService(service.getServiceConfig().getConnectionPort());
            }
        } else {
            throw new Exception(getClass().getName() + ", validateService(), "
                    + serviceName + " is not configured.");
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

        if (sender instanceof ServiceFactory) {
            switch (EventStatusType.valueOf(status.getName())) {
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

            return null;
        } else if (sender instanceof IService) {
            switch (status.getId()) {
                case 7001:  // GETMAXIMUMCONNECTIONS
                    result = getMaximumConnections();
                    break;
                case 7002:  // SETMAXIMUMCONNECTIONS
                    setMaximumConnections(Integer.valueOf(o.toString()));
                    break;
                case 7003:  // GETSERVICECONNECTIONS
                    result = getServiceConnections();
                    break;
                case 7004:  // GETPROPERTY
                    result = getProperty(o.toString());
                    break;
                case 7005:  // GETPROPERTIES
                    result = getProperties();
                    break;
                case 7006:  // ADDSERVICE
                    try {
                        addService((IService) o);
                    } catch (Exception ex) {
                        result = new Exception(getClass().getName() + ", EventHandler(), "
                                + "addService failed.");
                    }
                    break;
                case 7007:  // DECREASESERVICECONNECTIONS
                    decreaseServiceConnections();
                    break;
                case 7008:  // INCREASESERVICECONNECTIONS
                    increaseServiceConnections();
                    break;
                case 7009:  // LOGERROR
                    logError(message);
                    break;
                case 7010:  // LOGDEBUG
                    logDebug(message);
                    break;
                case 7011:  // LOGINFO
                    logInfo(message);
                    break;
                case 7012:  // GETCONFIG
                    result = getConfig();
                    break;
                case 7013:  // REMOVESERVICE
                    Object[] to = (Object[]) o;
                    result = removeService(Integer.valueOf(to[0].toString()), (boolean) to[1]);
                    break;
                case 7014:  // STARTSERVICE
                    try {
                        result = startService((int) o);
                    } catch (Exception ex) {
                        result = new Exception(getClass().getName() + ", EventHandler(), "
                                + "addService failed.");
                    }
                    break;
                case 7015:  // TOSTRING
                    if (o != null) {
                        toString((PrintWriter) o);
                    } else {
                        result = toString();
                    }
                    break;
                case 7016:  // GETSERVICE
                    result = getService(Integer.valueOf(o.toString()));
                    break;
                case 7017:  // VALIDATESERVICE
                    try {
                        result = validateService(o.toString());
                    } catch (Exception ex) {
                        result = new Exception(getClass().getName() + ", EventHandler(), "
                                + "validateService failed.");
                    }
                    break;
                case 7018:  // ISSERVICERUNNING
                    if (o instanceof Integer) {
                        result = isRunning(Integer.valueOf(o.toString()));
                    } else {
                        result = isRunning(o.toString());
                    }
                    break;

                default:
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
