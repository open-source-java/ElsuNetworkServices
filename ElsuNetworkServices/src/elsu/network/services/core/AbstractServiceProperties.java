package elsu.network.services.core;

import elsu.network.factory.ServiceFactory;
import elsu.common.*;
import java.util.*;

/**
 * AbstractServiceProperties class is used to store the local properties used by
 the service or it's connections or it's listener. This class provides a
 * direct access to the properties vice having the service to parse the
 * application attribute list or service attribute list which can degrade the
 * performance when high volume of data is processed.
 * <p>
 This class is declared abstract to prevent concrete implementations which may
 lead to security issue if the information is shared with other classes. The
 class extends AbstractServiceRuntimeProperties class which stores the current
 state of the service or its connections.
 *
 * @author Seraj Dhaliwal (seraj.s.dhaliwal@uscg.mil)
 * @see AbstractServiceRuntimeProperties
 */
public abstract class AbstractServiceProperties extends AbstractServiceRuntimeProperties {

    // <editor-fold desc="class private storage">
    // factory object which created the serice for back reference for support
    // functions like logging
    private volatile ServiceFactory _factory = null;

    // service configuration object created from app.config properties.
    private volatile ServiceConfig _serviceConfig = null;

    // set/list of client connections active for the service
    private volatile Set<AbstractConnection> _connections = null;

    // default date format to use by the service
    private volatile String _datetimeFormat = "yyyyMMddHHmmssS";

    // default field delimiter for parsing
    private volatile String _fieldDelimiter = "|";

    // default record terminator used for input parsing
    private volatile String _recordTerminator = GlobalStack.LINESEPARATOR;

    // monitor object for locking access for variables when shared across
    // multiple threads
    private volatile Object _monitor = new Object();

    // status variables for communication notifications
    private volatile String _statusOk = "100";
    private volatile String _statusInvalidContent = "110";
    private volatile String _statusUnAuthorized = "120";
    private volatile String _statusRequestTimeout = "130";
    private volatile String _statusDatabaseError = "140";
    private volatile String _statusSystemError = "150";
    // </editor-fold>

    // <editor-fold desc="class constructor destructor">
    /**
     * ServiceProperties(...) constructor creates the service propertie storage
     * object and initializes the default values from application properties or
     * local service properties
     *
     * @param factory is the factory which created the service
     * @param serviceConfig is the configuration object loaded from app.config
     */
    public AbstractServiceProperties(ServiceFactory factory,
            ServiceConfig serviceConfig) {
        // store the factory for back-reference
        this._factory = factory;

        // store the service configuration
        this._serviceConfig = serviceConfig;

        // create a set for storing connections
        this._connections = new HashSet<>(
                serviceConfig.getMaximumConnections());

        // local config properties for local reference by class method
        initializeLocalProperties();
    }

    /**
     * initializeProperties() is a generic method to consolidate all initial
     * variable instantiation outside of class constructor. It allows the
     * variables to be reset from another method within a class if required.
     *
     */
    private void initializeLocalProperties() {
        this._datetimeFormat = getProperties().get(
                "message.datetimeFormat").toString();
        this._fieldDelimiter = getProperties().get(
                "record.field.delimiter").toString();
        this._recordTerminator = getProperties().get(
                "record.terminator").toString();
        this._statusOk
                = getProperties().get("connection.status.ok").toString();
        this._statusInvalidContent = getProperties().get(
                "connection.status.invalidContent").toString();
        this._statusUnAuthorized = getProperties().get(
                "connection.status.unauthorized").toString();
        this._statusRequestTimeout = getProperties().get(
                "connection.status.requestTimeout").toString();
        this._statusDatabaseError = getProperties().get(
                "connection.status.databaseError").toString();
        this._statusSystemError = getProperties().get(
                "connection.status.systemError").toString();
    }
    // </editor-fold>

    // <editor-fold desc="class getter/setters">
    public synchronized Set<AbstractConnection> getConnections() {
        return this._connections;
    }

    public synchronized String getDatetimeFormat() {
        return this._datetimeFormat;
    }

    public synchronized ServiceFactory getFactory() {
        return this._factory;
    }

    public synchronized String getFieldDelimiter() {
        return this._fieldDelimiter;
    }

    public synchronized int getMaximumConnections() {
        return getFactory().getMaximumConnections();
    }

    public synchronized void setMaximumConnections(int count) {
        getFactory().setMaximumConnections(count);
    }

    public synchronized Object getMonitor() {
        return this._monitor;
    }

    public synchronized String getRecordTerminator() {
        return this._recordTerminator;
    }

    public synchronized char getRecordTerminatorChar() {
        return getRecordTerminator().charAt(0);
    }

    public synchronized ServiceConfig getServiceConfig() {
        return this._serviceConfig;
    }

    public synchronized int getServiceConnections() {
        return getFactory().getServiceConnections();
    }

    public synchronized Map<String, Object> getProperties() {
        return getFactory().getConfig().getProperties();
    }

    public synchronized String getStatusDatabaseError() {
        return this._statusDatabaseError;
    }

    public synchronized String getStatusInvalidContent() {
        return this._statusInvalidContent;
    }

    public synchronized String getStatusOk() {
        return this._statusOk;
    }

    public synchronized String getStatusRequestTimeout() {
        return this._statusRequestTimeout;
    }

    public synchronized String getStatusSystemError() {
        return this._statusSystemError;
    }

    public synchronized String getStatusUnAuthorized() {
        return this._statusUnAuthorized;
    }
    // </editor-fold>

    // <editor-fold desc="class methods">
    public synchronized void addService(IService service, int port) throws
            Exception {
        getFactory().addService(service, port);
    }

    public synchronized void decreaseServiceConnections() {
        getFactory().decreaseServiceConnections();
    }

    public synchronized void increaseServiceConnections() {
        getFactory().increaseServiceConnections();
    }
    // </editor-fold>

    // <editor-fold desc="class logging">
    /**
     * logDebug(...) method is an interface method to the central factory
     * logDebug method to support multi-threaded logging
     *
     * @param obj
     */
    public synchronized void logDebug(Object obj) {
        getFactory().logDebug(obj.toString());
    }

    /**
     * logError(...) method is an interface method to the central factory
     * logDebug method to support multi-threaded logging
     *
     * @param obj
     */
    public synchronized void logError(Object obj) {
        getFactory().logError(obj.toString());
    }

    /**
     * logInfo(...) method is an interface method to the central factory
     * logDebug method to support multi-threaded logging
     *
     * @param obj
     */
    public synchronized void logInfo(Object obj) {
        getFactory().logInfo(obj.toString());
    }
    // </editor-fold>

    @Override
    public synchronized String toString() {
        StringBuilder result = new StringBuilder();

        result.append("<object attr='").append(getClass().getName()).append("'>");
        result.append("<connections>").append(getConnections().size()).append("</connections>");
        result.append("<datetimeFormat>").append(getDatetimeFormat()).append("</datetimeFormat>");
        result.append("<fieldDelimiter>").append(getFieldDelimiter()).append("</fieldDelimiter>");
        result.append("<recordTerminator>").append(getRecordTerminator()).append("</recordTerminator>");
        result.append("<statusOk>").append(getStatusOk()).append("</statusOk>");
        result.append("<statusInvalidContent>").append(getStatusInvalidContent()).append("</statusInvalidContent>");
        result.append("<statusUnAuthorized>").append(getStatusUnAuthorized()).append("</statusUnAuthorized>");
        result.append("<statusRequestTimeout>").append(getStatusRequestTimeout()).append("</statusRequestTimeout>");
        result.append("<statusDatabaseError>").append(getStatusDatabaseError()).append("</statusDatabaseError>");
        result.append("<statusSystemError>").append(getStatusSystemError()).append("</statusSystemError>");

        // append runtime properties
        result.append(super.toString());
        result.append("</object>");
        
        return result.toString();
    }
}
