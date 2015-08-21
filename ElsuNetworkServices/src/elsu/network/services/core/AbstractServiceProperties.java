package elsu.network.services.core;

import elsu.network.services.AbstractConnection;
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
    // runtime sync object
    private Object _runtimeSync = new Object();

    // factory object which created the serice for back reference for support
    // functions like logging
    //private volatile ServiceFactory _factory = null;

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
    public AbstractServiceProperties(ServiceConfig serviceConfig) {
        System.out.println("- AbstractServiceProperties()");
        // store the service configuration
        this._serviceConfig = serviceConfig;

        // create a set for storing connections
        this._connections = new HashSet<>(
                serviceConfig.getMaximumConnections());
    }
    // </editor-fold>

    // <editor-fold desc="class getter/setters">
    public Set<AbstractConnection> getConnections() {
        System.out.println("- AbstractServiceProperties(), getConnections()");
        
        Set<AbstractConnection> result = null;

        synchronized(this._runtimeSync) {
            result = this._connections;
        }
        
        return result;
    }

    public String getDatetimeFormat() {
        System.out.println("- AbstractServiceProperties(), getDatetimeFormat()");
        
        String result = "";
        
        synchronized(this._runtimeSync) {
            result = this._datetimeFormat;
        }
        
        return result;
    }
    protected void setDateTimeFormat(String format) {
        System.out.println("- AbstractServiceProperties(), setDateTimeFormat(format)");
        
        synchronized(this._runtimeSync) {
            this._datetimeFormat = format;
        }
    }

    //public synchronized ServiceFactory getFactory() {
    //    return this._factory;
    //}

    //protected void setFactory(ServiceFactory factory) {
    //    this._factory = factory;
    //}

    public String getFieldDelimiter() {
        System.out.println("- AbstractServiceProperties(), getFieldDelimiter()");
        
        String result = "";
        
        synchronized(this._runtimeSync) {
            result = this._fieldDelimiter;
        }
        
        return result;
    }
    protected void setFieldDelimiter(String delimiter) {
        System.out.println("- AbstractServiceProperties(), setFieldDelimiter(delimiter)");
        this._fieldDelimiter = delimiter;
    }

    public Object getMonitor() {
        System.out.println("- AbstractServiceProperties(), getMonitor()");
        
        Object result = null;
        
        synchronized(this._runtimeSync) {
            result = this._monitor;
        }
        
        return result;
    }

    public String getRecordTerminator() {
        System.out.println("- AbstractServiceProperties(), getRecordTerminator()");
        
        String result = "";
        
        synchronized(this._runtimeSync) {
            result = this._recordTerminator;
        }
        
        return result;
    }
    protected void setRecordTerminator(String terminator) {
        System.out.println("- AbstractServiceProperties(), setRecordTerminator(terminator)");
        this._recordTerminator = terminator;
    }

    public char getRecordTerminatorChar() {
        System.out.println("- AbstractServiceProperties(), getRecordTerminatorChar()");
        
        char result = '\0';
        
        synchronized(this._runtimeSync) {
            result = getRecordTerminator().charAt(0);
        }
        
        return result;
    }

    public ServiceConfig getServiceConfig() {
        System.out.println("- AbstractServiceProperties(), getServiceConfig()");
        
        ServiceConfig result = null;
        
        synchronized(this._runtimeSync) {
            result = this._serviceConfig;
        }
        
        return result;
    }

    public String getStatusDatabaseError() {
        System.out.println("- AbstractServiceProperties(), getStatusDatabaseError()");
        
        String result = "";
        
        synchronized(this._runtimeSync) {
            result = this._statusDatabaseError;
        }
        
        return result;
    }
    protected void setStatusDatabaseError(String status) {
        System.out.println("- AbstractServiceProperties(), setStatusDatabaseError(status)");
        this._statusDatabaseError = status;
    }

    public String getStatusInvalidContent() {
        System.out.println("- AbstractServiceProperties(), getStatusInvalidContent()");
        
        String result = "";
        
        synchronized(this._runtimeSync) {
            result = this._statusInvalidContent;
        }
        
        return result;
    }
    protected void setStatusInvalidContent(String status) {
        System.out.println("- AbstractServiceProperties(), setStatusInvalidContent(status)");
        this._statusInvalidContent = status;
    }

    public String getStatusOk() {
        System.out.println("- AbstractServiceProperties(), getStatusOk()");
        
        String result = "";
        
        synchronized(this._runtimeSync) {
            result = this._statusOk;
        }
        
        return result;
    }
    protected void setStatusOk(String status) {
        System.out.println("- AbstractServiceProperties(), setStatusOk(status)");
        this._statusOk = status;
    }

    public String getStatusRequestTimeout() {
        System.out.println("- AbstractServiceProperties(), getStatusRequestTimeout()");
        
        String result = "";
        
        synchronized(this._runtimeSync) {
            result = this._statusRequestTimeout;
        }
        
        return result;
    }
    protected void setStatusRequestTimeout(String status) {
        System.out.println("- AbstractServiceProperties(), setStatusRequestTimeout(status)");
        this._statusRequestTimeout = status;
    }

    public String getStatusSystemError() {
        System.out.println("- AbstractServiceProperties(), getStatusSystemError()");
        
        String result = "";
        
        synchronized(this._runtimeSync) {
            result = this._statusSystemError;
        }
        
        return result;
    }
    protected void setStatusSystemError(String status) {
        System.out.println("- AbstractServiceProperties(), setStatusSystemError(status)");
        this._statusSystemError = status;
    }

    public String getStatusUnAuthorized() {
        System.out.println("- AbstractServiceProperties(), getStatusUnAuthorized()");
        
        String result = "";
        
        synchronized(this._runtimeSync) {
            result = this._statusUnAuthorized;
        }
        
        return result;
    }
    protected void setStatusUnAuthorized(String status) {
        System.out.println("- AbstractServiceProperties(), setStatusUnAuthorized(status)");
        this._statusUnAuthorized = status;
    }
    // </editor-fold>

    @Override
    public String toString() {
        System.out.println("- AbstractServiceProperties(), toString()");
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
