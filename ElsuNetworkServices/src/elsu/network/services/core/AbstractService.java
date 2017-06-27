package elsu.network.services.core;

import elsu.events.*;
import elsu.network.core.*;
import elsu.network.factory.*;
import elsu.common.*;
import elsu.network.application.*;
import elsu.network.services.*;
import elsu.support.*;
import java.io.*;
import java.net.*;
import java.util.*;

/**
 * AbstractService is an abstract class providing base functions and storage for
 * properties. All core functions which control adding, removing, or shutting
 * down service objects like connections, child services, and listeners are
 * defined under AbstractService base.
 * <p>
 * All concrete implementations of this class should override start() and
 * shutdown() functions to ensure local objects are closed properly.
 *
 * @author Seraj Dhaliwal (seraj.s.dhaliwal@uscg.mil)
 */
public abstract class AbstractService extends AbstractServiceProperties
        implements IService, IEventPublisher, IEventSubscriber {

    // <editor-fold desc="class private storage">
    // runtime sync object
    private Object _runtimeSync = new Object();
    // storage for all child services; normally null.  if child service then
    // this is null since child services cannot have child services
    private volatile Map<Integer, IService> _childServices;
    // used by child service to back reference the parent service
    private volatile IService _parentService = null;
    // used to store child service config
    private volatile ServiceConfig _childConfig = null;
    // reflects if the listener is defined by the service
    private volatile boolean _isListener = false;
    // reference to listener object for the service
    private volatile ServiceListener _listener = null;
    // system logger if configured
    private volatile Log4JManager _log4JManager = null;
    // </editor-fold>

    // <editor-fold desc="class constructor destructor">
    /**
     * Service(...) class constructor creates an instance of the service using
     * configuration properties loaded from app.config. The configuration is
     * stored in the AbstractServiceProperties and runtime properties are stored
     * in ServiceRuntimeProperties super classes.
     * <p>
     * Since all connections are created per service, the thread groups for the
     * service connections is same as that of the service listener.
     *
     * @param factory
     * @param threadGroup
     * @param serviceConfig
     * @see ServiceFactory
     * @see ServiceListener
     * @see AbstractServiceProperties
     * @see ServiceRuntimeProperties
     */
    public AbstractService(String threadGroup, ServiceManager serviceManager,
            ServiceConfig serviceConfig) {
        // call the super class constructor
        super(serviceManager, serviceConfig);

        // store the thread group for use by other objects
        setThreadGroup(new ThreadGroup(threadGroup));

        // create a hashMap to store the child service objects
        this._childServices = new HashMap<>();

        // local config properties for local reference by class method
        // initializeLocalProperties();
        // initialize logger if serviceConfig defines log.filename
    }

    /**
     * initializeProperties() is a generic method to consolidate all initial
     * variable instantiation outside of class constructor. It allows the
     * variables to be reset from another method within a class if required.
     *
     */
    protected void initializeLocalProperties() {
        // initialize shared/global service properties 
        setDateTimeFormat(getProperty("application.framework.attributes.key.message.datetimeFormat").toString());
        setFieldDelimiter(getProperty("application.framework.attributes.key.record.field.delimiter").toString());
        setRecordTerminator(getProperty("application.framework.attributes.key.record.terminator").toString());
        setStatusOk(getProperty("application.framework.attributes.key.connection.status.ok").toString());
        setStatusInvalidContent(getProperty("application.framework.attributes.key.connection.status.invalidContent").toString());
        setStatusUnAuthorized(getProperty("application.framework.attributes.key.connection.status.unauthorized").toString());
        setStatusRequestTimeout(getProperty("application.framework.attributes.key.connection.status.requestTimeout").toString());
        setStatusDatabaseError(getProperty("application.framework.attributes.key.connection.status.databaseError").toString());
        setStatusSystemError(getProperty("application.framework.attributes.key.connection.status.systemError").toString());

        // initialize local service properties
        if (getServiceConfig().getServiceType() == ServiceType.SERVER) {
            this._isListener = true;
        } else {
            if (getChildConfig() != null) {
                try {
                    this._isListener = Boolean.valueOf(
                            getServiceConfig().getAttributes().get(
                                    "key.service.listener").toString());
                } catch (Exception ex) {
                    logError(getClass().toString()
                            + ", initializeLocalProperties(), "
                            + getServiceConfig().getServiceName() + " on port "
                            + getServiceConfig().getConnectionPort()
                            + ", invalid service.listener, " + ex.getMessage());
                    this._isListener = false;
                }
            } else {
                this._isListener = false;
            }
        }

        // initialize log for the service
        try {
        	String logPath = getProperty("application.framework.attributes.key.log.path").toString();
        	String logClass = getProperty("application.framework.attributes.key.log.class").toString();
        	String logFile = getProperty("application.framework.attributes.key.log.filename").toString();
            this._log4JManager = ConfigLoader.initializeLogger(logPath, logClass, logFile);
        } catch (Exception ex) {
            getServiceManager().logError(getClass().toString() + ", initializeLocalProperties(), "
                    + ex.getMessage());
        }
    }

    /**
     * finalize() is the class destructor, normally not required, but since we
     * are managing connections which may open files or database connections it
     * is a good idea to force a shutdown to clear memory for garbage collection
     *
     * @throws Throwable
     */
    @Override
    protected void finalize() throws Throwable {
        try {
            shutdown();
        } finally {
            super.finalize();
        }
    }
    // </editor-fold>

    // <editor-fold desc="class factory getter/setters">
    public int getMaximumConnections() {
        return getServiceManager().getMaximumConnections();
    }

    public void setMaximumConnections(int count) {
        getServiceManager().setMaximumConnections(count);
    }

    public int getServiceConnections() {
        return getServiceManager().getServiceConnections();
    }

    public Object getProperty(String key) {
        return getServiceManager().getProperty(key);
    }

    public Set<String> getKeySet() {
        return getServiceManager().getKeySet();
    }

    public void addService(IService service) throws Exception {
        getServiceManager().addService(service);
    }

    public void decreaseServiceConnections() {
        getServiceManager().decreaseServiceConnections();
    }

    public void increaseServiceConnections() {
        getServiceManager().increaseServiceConnections();
    }

    /**
     * logDebug(...) method is an interface method to the central factory
     * logDebug method to support multi-threaded logging
     *
     * @param obj
     */
    public void logDebug(Object obj) {
        if (_log4JManager != null) {
            synchronized (this._runtimeSync) {
                _log4JManager.debug(obj.toString());
            }
        } else {
            getServiceManager().logDebug(obj.toString());
        }
    }

    /**
     * logError(...) method is an interface method to the central factory
     * logDebug method to support multi-threaded logging
     *
     * @param obj
     */
    public void logError(Object obj) {
        if (_log4JManager != null) {
            synchronized (this._runtimeSync) {
                _log4JManager.error(obj.toString());
            }
        } else {
            getServiceManager().logError(obj.toString());
        }
    }

    /**
     * logInfo(...) method is an interface method to the central factory
     * logDebug method to support multi-threaded logging
     *
     * @param obj
     */
    public void logInfo(Object obj) {
        if (_log4JManager != null) {
            synchronized (this._runtimeSync) {
                _log4JManager.info(obj.toString());
            }
        } else {
            getServiceManager().logInfo(obj.toString());
        }
    }

    // </editor-fold>
    // <editor-fold desc="class getter/setters">
    /**
     * getChildConfig() method returns the child configuration object. If this
     * is not a child service, the value is null.
     *
     * @return <code>ServiceAbstractConfig</code> is the configuration object
     * for the child service.
     */
    @Override
    public ServiceConfig getChildConfig() {
        ServiceConfig result = null;

        synchronized (this._runtimeSync) {
            result = this._childConfig;
        }

        return result;
    }

    /**
     * setChildConfig() method stores the config object for the child service.
     * The method is used by child service constructors to store the their
     * configuration object.
     *
     * @param config
     */
    protected void setChildConfig(ServiceConfig config) {
        synchronized (this._runtimeSync) {
            this._childConfig = config;
        }
    }

    /**
     * getChildServiceAbstracts() method returns the hashMap of all the child
     * services for the parent service. This map will be empty if this service
     * is a child service.
     *
     * @return <code>Map</code> returns the collection of the child services.
     */
    public Map<Integer, IService> getChildServices() {
        return _childServices;
    }

    /**
     * getListener() method returns the service listener object (if created),
     * else null reference is returned.
     *
     * @return
     */
    @Override
    public ServiceListener getListener() {
        ServiceListener result = null;

        synchronized (this._runtimeSync) {
            result = this._listener;
        }

        return result;
    }

    /**
     * isListener() method returns the value true/false if the service is
     * configured to instantiate a listener. AbstractService Type of SERVER
     * always shows the value = true. Other services can manually set this value
     * based on their needs by using the app.config attribute for the service.
     *
     * @return <code>boolean</code> status of the listener
     */
    public boolean isListener() {
        boolean result = false;

        synchronized (this._runtimeSync) {
            result = this._isListener;
        }

        return result;
    }

    /**
     * isListener(...) method allows the value of the listener status to be
     * changed at runtime and returns the value true/false if the service is
     * configured to instantiate a listener.
     *
     * @param active
     * @return <code>boolean</code> status of the listener
     */
    public boolean isListener(boolean active) {
        synchronized (this._runtimeSync) {
            this._isListener = active;
        }

        return isListener();
    }

    /**
     * getParentServiceAbstract() method returns the parent service object for
     * the child service. This will return null if this is not a child service.
     *
     * @return <code>IService</code> returns the service object
     */
    public IService getParentService() {
        IService result = null;

        synchronized (this._runtimeSync) {
            result = this._parentService;
        }

        return result;
    }

    /**
     * setParentServiceAbstract(...) method sets the parent service object for
     * the child service.
     *
     * @param service is the object reference of the parent service
     */
    protected void setParentService(IService service) {
        synchronized (this._runtimeSync) {
            this._parentService = service;
        }
    }
    // </editor-fold>

    // <editor-fold desc="class methods">
    /**
     * addConnection(...) method allows service to add a custom connection which
     * is not socket based. The connection is a threaded class therefore
     * allowing service to create custom threaded objects specific for its needs
     * <p>
     * There is no limit on the # of non socket based connections.
     *
     * @param connection
     */
    @Override
    public void addConnection(AbstractConnection connection)
            throws Exception {
        addConnection(null, connection);
    }

    /**
     * addConnection(...) method adds a socket based connection for a service.
     * <p>
     * New connection is validated against the max connections and service
     * ignore maximum connections settings.
     *
     * @param socket
     * @param connection
     */
    @Override
    public void addConnection(Socket socket,
            AbstractConnection connection) throws Exception {
        // if the max connection limit for the service is not zero and the 
        // total application connection count is greater than or equal to the 
        // max connections then raise exception
        // if the socket is null, then the connection limit is not validated
        if (!(getMaximumConnections() == 0)
                && (getServiceConnections() >= getMaximumConnections())
                && (!getServiceConfig().isIgnoreConnectionLimit())
                && (socket != null)) {
            try {
                // create a writer for the socket ouput stream to send error to
                PrintWriter out = new PrintWriter(socket.getOutputStream());

                // send the error to the client
                out.print("Connection refused; "
                        + "the server is busy; please try again later"
                        + getRecordTerminator());
                out.flush();

                // force close the client connection
                socket.close();

                // log the info
                logInfo(getClass().toString()
                        + ", addConnection(), connection refused to "
                        + socket.getInetAddress().getHostAddress()
                        + ":" + socket.getPort() + ": max connections reached");
            } catch (Exception ex) {
                // if there is an error during processing, log the error and
                // close the connection
                logError(getClass().toString() + ", addConnection(), "
                        + getServiceConfig().getServiceName() + ", "
                        + ex.getMessage());

                // close the socket, ignore the exception
                try {
                    socket.close();
                } catch (Exception exi) {
                }
            }
        } else {
            // if the max connection limit for the service is not zero and the 
            // total connection count for the service is greater than or equal 
            // to the max connections then raise exception
            // if the socket is null, then the connection limit is not validated
            if (!(getServiceConfig().getMaximumConnections() == 0)
                    && (getActiveConnections()
                    >= getServiceConfig().getMaximumConnections())
                    && (socket != null)) {
                try {
                    // create a writer for the socket ouput stream to send error to
                    PrintWriter out = new PrintWriter(socket.getOutputStream());

                    // send the error to the client
                    out.print("Connection refused; "
                            + "the server is busy; please try again later"
                            + getRecordTerminator());
                    out.flush();

                    // force close the client connection
                    socket.close();

                    // log the info
                    logInfo(getClass().toString()
                            + ", addConnection(), connection refused to "
                            + socket.getInetAddress().getHostAddress()
                            + ":" + socket.getPort()
                            + ": max connections reached");
                } catch (Exception ex) {
                    // if there is an error during processing, log the error and
                    // close the connection
                    logError(getClass().toString() + ", addConnection(), "
                            + getServiceConfig().getServiceName() + ", "
                            + ex.getMessage());

                    // close the socket, ignore the exception
                    try {
                        socket.close();
                    } catch (Exception exi) {
                    }
                }
            } else {
                // increase the application connection count
                increaseServiceConnections();

                // if connection is null, then we will create a new connection
                // for the service.  this is normally used by the listener to
                // pass a socket for the service which will be wrapped into the
                // connection class
                try {
                    // if connection is null, create a basic wrapper for the
                    // socket - stream based connection
                    if (connection == null) {
                        connection = new Connection(socket, this);
                    }

                    // if the socket is not null, make sure the linger option
                    // is set to false and time to ZERO.  linger is whether the
                    // socket keeps the connection open for reuse when force
                    // disconnected by server (not client)
                    if (connection.getClient() != null) {
                        connection.getClient().setSoLinger(false, 0);
                    }

                    // add the new connection to the service connections list
                    synchronized (this._runtimeSync) {
                        getConnections().add(connection);
                    }

                    // increase the # of service active connections
                    increaseActiveConnections();

                    // log if socket or non socket connection
                    if (socket != null) {
                        logInfo(getClass().toString()
                                + ", addConnection(), connected to "
                                + socket.getInetAddress().getHostAddress()
                                + ":" + socket.getPort() + " on port "
                                + socket.getLocalPort()
                                + " for service "
                                + getServiceConfig().getServiceName());
                    } else {
                        logInfo(getClass().toString() + ", addConnection(), "
                                + " for service "
                                + getServiceConfig().getServiceName());
                    }

                    // start the connection thread
                    connection.start();
                } catch (Exception ex) {
                    // if exception, log the error, close the socket, and 
                    // release the connection
                    logError(getClass().toString() + ", addConnection(), "
                            + getServiceConfig().getServiceName() + ", "
                            + ex.getMessage());

                    // close the socket, ignore the exception
                    try {
                        socket.close();
                    } catch (Exception exi) {
                    }

                    // signal the connection to close
                    try {
                        connection.isActive(false);
                    } catch (Exception exi) {
                    }
                }
            }
        }
    }

    /**
     * removeConnection(...) method removes the service connection from the
     * collection of connections. The connection, if socket based, closes the in
     * and out streams of the socket.
     *
     * @param connection
     */
    @Override
    public void removeConnection(AbstractConnection connection) {
        // check to make sure the connections streams are closed
        // they should be if this was called from a service
        try {
            if (connection.getClient() != null) {
                try {
                    connection.getClient().getInputStream().close();
                } catch (Exception exi) {
                }
                try {
                    connection.getClient().getOutputStream().close();
                } catch (Exception exi) {
                }
            }
        } catch (Exception exi) {
        }

        try {
            // remove connection from list
            synchronized (this._runtimeSync) {
                getConnections().remove(connection);
            }

            // log info for debugging
            logInfo(getClass().toString()
                    + ", removeConnection(), connection to "
                    + connection.getClass().toString() + ", "
                    + connection.getName() + " removed");
        } catch (Exception ex) {
            // log error based on the client status, catch (Exception ex)the
            // client goes into invalid state during disconnection
            try {
                if (connection.getClient() != null) {
                    logError(getClass().toString() + ", removeConnection(), "
                            + getServiceConfig().getServiceName()
                            + ", connection to "
                            + connection.getClient().getInetAddress().getHostAddress()
                            + " on port " + connection.getClient().getPort()
                            + " aborted, " + ex.getMessage());
                } else {
                    logError(getClass().toString() + ", removeConnection(), "
                            + getServiceConfig().getServiceName()
                            + ", connection to " + connection.getName()
                            + " on port " + connection.getClient().getPort()
                            + " removed");
                }
            } catch (Exception exi) {
            }
        } finally {
            // update the master counter used to track # of service and
            // application wide active connections.
            decreaseServiceConnections();
            decreaseActiveConnections();
        }
    }

    /**
     * addChildServiceAbstract(...) method adds the child service to the child
     * service collection list. The service mapping is based on the port of the
     * service, so only one service can be associated to the port. The service
     * is started upon completion of the method.
     *
     * @param service
     * @param port
     * @throws IOException
     */
    public void addChildService(IService service, int port)
            throws Exception {
        // key for hash table lookup
        Integer key = new Integer(port);

        // check if the service for the port already exists, if yes, then
        // return error
        if (getChildServices().get(key) != null) {
            throw new IllegalArgumentException(getClass().toString()
                    + "//addChildService//Port " + port
                    + " already in use.");
        }

        // store the service with the port as the key for lookup
        synchronized (this._runtimeSync) {
            getChildServices().put(key, service);
        }

        // signal the service to start; there is no delayed start for child
        // services
        service.start();
    }

    /**
     * removeChildServiceAbstract(...) method removes the child service based on
     * the port passed. If valid service port, the service associated is also
     * shutdown.
     *
     * @param port
     * @return
     */
    public boolean removeChildService(int port) {
        // key for hash table lookup
        Integer key = new Integer(port);

        // check if the service for the port exists, if no, then
        // return, cannot remove, return false signalling error
        final IService service
                = (IService) getChildServices().get(key);
        if (service == null) {
            return false;
        }

        // remove the key from the hash table
        synchronized (this._runtimeSync) {
            getChildServices().remove(key);
        }

        // signal the service to shutdown
        service.shutdown();

        // log for debugging
        logInfo(getClass().toString()
                + ", removeChildService(), stopping service "
                + service.getClass().getName()
                + " on port " + port);

        // return true, no error in removing the service
        return true;
    }

    /**
     * start() method is used to provide all services with a method to allow for
     * last minute initialization, validation, and then initiation of
     * processing.
     * <p>
     * If overloaded by the service, the the super method should be called to
     * ensure proper initialization takes place.
     *
     * @throws java.lang.Exception
     */
    @Override
    public void start() throws Exception {
        // set the service running state to true
        isRunning(true);

        // if the service requires a listener, this is set in the service
        // constructor or through super class AbstractServiceProperties when service
        // is created
        if (isListener()) {
            // capture any exception during listener creation, duplication of
            // port is most common and display it for user to correct the
            // app.config
            try {
                // create the listener object for the service
                this._listener = new ServiceListener((IService) this);

                // start the listener to accept connections
                getListener().start();

                // log for debugging
                logInfo(getClass().toString() + ", start(), "
                        + getServiceConfig().getServiceName()
                        + " listener activated ("
                        + getServiceConfig().getConnectionPort() + ")");
            } catch (Exception ex) {
                // if error, log error
                logError(getClass().toString() + ", start(), "
                        + getServiceConfig().getServiceName() + " on port "
                        + getServiceConfig().getConnectionPort() + ", "
                        + ex.getMessage());
            }
        }
    }

    /**
     * shutdown() method is used to provide all services with a method to
     * cleanly shutdown and close any open connections. If services override the
     * method, then they should call the super method to cleanly shutdown the
     * listener and connections (if they exist). *
     */
    @Override
    public void shutdown() {
        // if service is not running, then exit
        if (!isRunning()) {
            return;
        } else {
            // set the service to not-running state, this prevents new 
            // connections to be accepted while processing and signals to the
            // existing connections to terminate
            isRunning(false);

            // if the listener exists for the service, shut it down.  this is
            // a forced shutdown via interruption of the listener wait state
            if (getListener() != null) {
                getListener().shutdown();
            }

            // loop through the connecions and remove all connections, do not
            // use iterator for the connections list because removeConnection
            // method updates the list when removing connection and will cause
            // exception in the iterator
            ArrayList<AbstractConnection> al;
            al = new ArrayList<>(getConnections());

            // for each connection in the list
            for (Object o : al) {
                // remove the connection, this also updates the master list
                removeConnection((AbstractConnection) o);

                // yield processing to other threads
                Thread.yield();
            }

            // clear the connections list to prevent gc cycles
            al.clear();
        }

        // loop through all the child services, do not use iterator for the 
        // services list because removeChildService method updates the list 
        // when removing service and will cause exception in the iterator
        ArrayList<Integer> aKeys;
        aKeys = new ArrayList<>(getChildServices().keySet());

        // for each service in the list
        for (Integer key : aKeys) {
            // extract the port # which is the service map key
            // Integer port = Integer.parseInt(key.toString());

            // remove the child serivce, key = port for the service
            removeChildService(key);

            // yield processing to other threads
            Thread.yield();
        }
    }
    // </editor-fold>

    // <editor-fold desc="class methods not implemented">
    @Override
    public void checkConnection(AbstractConnection connection) {
        logError(getClass().toString()
                + ", checkConnection(), base class implementation - needs to be overriden");
    }

    @Override
    public void checkConnections() {
        logError(getClass().toString()
                + ", checkConnections(), base class implementation - needs to be overriden");
    }

    @Override
    public void serve(AbstractConnection conn) throws Exception {
        logError(getClass().toString()
                + ", serve(), base class implementation - needs to be overriden");
    }

    @Override
    public void validateService() throws Exception {
        // mostly do nothing, this is service specific
    }
    // </editor-fold>

    // <editor-fold desc="class event listener">
    @Override
    public Object EventHandler(Object sender, IEventStatusType status, String message, Object o) {
        Object result = null;

        if (sender instanceof ServiceManager) {
            switch (EventStatusType.valueOf(status.getName())) {
                case INITIALIZE:
                    if (this == o) {
                        this.initializeLocalProperties();
                    }
                    break;
                case START:
                    if (this == o) {
                        try {
                            start();
                        } catch (Exception ex) {
                            result = ex;
                        }
                    }
                    break;
                case SHUTDOWN:
                    if (this == o) {
                        try {
                            shutdown();
                        } catch (Exception ex) {
                            result = ex;
                        }
                    }
                    break;
                default:
                    break;
            }
        }

        return result;
    }
    // </editor-fold>

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();

        result.append("<object attr='").append(getClass().getName()).append("'>");
        result.append("<name>").append(getServiceConfig().getServiceName()).append("</name>");
        result.append("<port>").append(getServiceConfig().getConnectionPort()).append("</port>");
        result.append("<isListener>").append(isListener()).append("</isListener>");
        result.append("<isRunning>").append(isRunning()).append("</isRunning>");
        result.append("<maxConnections>").append(getServiceConfig().getMaximumConnections()).append("</maxConnections>");

        if (getChildConfig() != null) {
            result.append("<childConfig>").append(getChildConfig().toString()).append("</childConfig>");
        } else {
            result.append("<serviceConfig>").append(getServiceConfig().toString()).append("</serviceConfig>");
        }

        result.append("<childServices>")
                .append("<size>").append(getChildServices().size()).append("</size>");
        if (!getChildServices().isEmpty()) {
            ArrayList<Integer> aKeys;
            aKeys = new ArrayList<>(getChildServices().keySet());

            for (Integer key : aKeys) {
                // key = port for the service
                IService service
                        = (IService) getChildServices().get(key);

                result.append(service.toString());

                Thread.yield();
            }
        }
        result.append("</childServices>");

        result.append(super.toString());
        result.append("</object>");

        return result.toString();
    }

    @Override
    public void toString(PrintWriter out) {
        // retrieve the toString() representation of this object and write
        // it to the output stream provided
        out.print(toString() + GlobalStack.LINESEPARATOR);
        out.flush();
    }
}
