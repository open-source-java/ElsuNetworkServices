package elsu.network.services.core;

import elsu.network.core.*;
import elsu.support.*;
import java.util.*;

/**
 * ServiceConfig class is used to store the configuration parameters loaded by
 * the ConfigLoader class from the app.config. Each service has its own
 * ServiceConfig object and all the attributes.
 * <p>
 * Attributes is a basic hashMap (key/value) pair.
 *
 * @author Seraj Dhaliwal (seraj.s.dhaliwal@uscg.mil)
 * @see ConfigLoader
 */
public class ServiceConfig {

    // <editor-fold desc="class private storage">
    // runtime sync object
    private Object _runtimeSync = new Object();

    // store the name of the service, this is unique across the application
    private volatile String _serviceName = null;

    // service port, unique across the application
    private volatile int _connectionPort = 0;

    // service class for instantiation through reflection
    private volatile String _serviceClass = null;

    // service type: SERVER, CLIENT
    // SERVER type is always configured to have a listener
    // CLIENT type does not have a listener but it can create a custom listener
    // if required through attribute properties
    private volatile ServiceType _serviceType = ServiceType.SERVER;

    // service startup type allows control over when the services are started.
    // AUTOMATIC services are started as soon as instantiation is complete
    // DELAYEDSTART services are started after all AUTOMATIC service have been
    // signaled to start
    // DISABLED services are never started and cannot be started through 
    // control module at a later time
    // MANUAL services are not started when application starts but can be 
    // started through control module.
    private volatile ServiceStartupType _startupType
            = ServiceStartupType.AUTOMATIC;

    // ignore maximum connection limit property when true bypasses the client
    // count validation when new client tries to connect
    private volatile boolean _isIgnoreConnectionLimit = false;

    // maximum connection is the # of maximum clients which can connect to the
    // service at one time
    private volatile int _maximumConnections = 10;

    // custom attributes defined for the service
    private volatile Map<String, String> _attributes = null;
    // </editor-fold>

    // <editor-fold desc="class constructor destructor">
    /**
     * ServiceConfig() constructor creates an empty config object and
     * initializes the subscribers, publishers, and attributes list.
     *
     */
    public ServiceConfig() {
        this._attributes = new HashMap<>();
    }
    // </editor-fold>

    // <editor-fold desc="class getter/setters">
    public Map<String, String> getAttributes() {
        Map<String, String> result = null;

        synchronized (this._runtimeSync) {
            result = this._attributes;
        }

        return result;
    }

    public String getAttribute(String key) {
        String result = "";

        synchronized (this._runtimeSync) {
            result = this._attributes.get(key);
        }

        return result;
    }

    public int getConnectionPort() {
        int result = 0;

        synchronized (this._runtimeSync) {
            result = this._connectionPort;
        }

        return result;
    }

    public void setConnectionPort(int port) {
        synchronized (this._runtimeSync) {
            this._connectionPort = port;
        }
    }

    public boolean isIgnoreConnectionLimit() {
        boolean result = false;

        synchronized (this._runtimeSync) {
            result = this._isIgnoreConnectionLimit;
        }

        return result;
    }

    public boolean isIgnoreConnectionLimit(boolean ignore) {
        synchronized (this._runtimeSync) {
            this._isIgnoreConnectionLimit = ignore;
        }

        return isIgnoreConnectionLimit();
    }

    public int getMaximumConnections() {
        int result = 0;

        synchronized (this._runtimeSync) {
            result = this._maximumConnections;
        }

        return result;
    }

    public void setMaximumConnections(int allowedMax) {
        synchronized (this._runtimeSync) {
            this._maximumConnections = allowedMax;
        }
    }

    public String getServiceClass() {
        String result = "";

        synchronized (this._runtimeSync) {
            result = this._serviceClass;
        }

        return result;
    }

    public void setServiceClass(String classRef) {
        synchronized (this._runtimeSync) {
            this._serviceClass = classRef;
        }
    }

    public String getServiceName() {
        String result = "";
        
        synchronized (this._runtimeSync) {
            result = this._serviceName;
        }
        
        return result;
    }

    public void setServiceName(String name) {
        synchronized (this._runtimeSync) {
            this._serviceName = name;
        }
    }

    public ServiceType getServiceType() {
        ServiceType result = _serviceType.SERVER;
        
        synchronized (this._runtimeSync) {
            result = this._serviceType;
        }
        
        return result;
    }

    public ServiceStartupType getStartupType() {
        ServiceStartupType result = ServiceStartupType.DISABLED;
        
        synchronized (this._runtimeSync) {
            result = this._startupType;
        }
        
        return result;
    }

    public void setStartupType(ServiceStartupType type) {
        synchronized (this._runtimeSync) {
            this._startupType = type;
        }
    }

    public void setServiceType(ServiceType type) {
        synchronized (this._runtimeSync) {
            this._serviceType = type;
        }
    }
    // </editor-fold>

    // <editor-fold desc="class methods">
    /**
     * clone() method is override of the object method to perform a deep copy of
     * the ServiceConfig object. All the collectors, distributors, and
     * attributes are copied to the new object.
     * <p>
     * The purpose of custom clone is to ensure all local properties are copied
     * returning new addresses to ensure the copy object references are
     * independent of the original object.
     *
     * @return <code>ServiceConfig</code> copy object
     * @throws java.lang.CloneNotSupportedException
     */
    @Override
    public ServiceConfig clone() throws CloneNotSupportedException {
        // create new object to return
        ServiceConfig copyConfig = new ServiceConfig();

        // copy the orignal objects local properties
        copyConfig.setServiceName(getServiceName());
        copyConfig.setConnectionPort(getConnectionPort());
        copyConfig.setServiceClass(getServiceClass());
        copyConfig.setServiceType(getServiceType());
        copyConfig.setStartupType(getStartupType());
        copyConfig.isIgnoreConnectionLimit(isIgnoreConnectionLimit());
        copyConfig.setMaximumConnections(getMaximumConnections());

        // copy the original objects attributes to new object
        if (!getAttributes().isEmpty()) {
            ArrayList<String> keyList = new ArrayList<>(
                    getAttributes().keySet());

            String key;
            String value;
            for (Object attrKey : keyList) {
                key = attrKey.toString();
                value = getAttributes().get(key);

                copyConfig.getAttributes().put(key, value);

                // yield processing to other threads
                Thread.yield();
            }
        }

        // return the new object
        return copyConfig;
    }

    public static ServiceConfig LoadConfig(ConfigLoader config, String serviceName)
            throws Exception {
        // create new object to return
        String childServiceName;
        ServiceConfig childConfig;

        ServiceConfig sc = new ServiceConfig();

        // copy the orignal objects local properties
        sc.setServiceName(serviceName);
        sc.setConnectionPort(Integer.valueOf(config.getProperty(serviceName + ".port").toString()));
        sc.setServiceClass(config.getProperty(serviceName + ".class").toString());
        sc.setServiceType(ServiceType.valueOf(config.getProperty(serviceName + ".serviceType").toString()));
        sc.setStartupType(ServiceStartupType.valueOf(config.getProperty(serviceName + ".startupType").toString()));
        sc.isIgnoreConnectionLimit(Boolean.valueOf(config.getProperty(serviceName + ".ignoreConnectionLimit").toString()));
        sc.setMaximumConnections(Integer.valueOf(config.getProperty(serviceName + ".maxConnections").toString()));

        // copy the original objects attributes to new object
        for (String attrKey : config.getKeySet()) {
            if (attrKey.startsWith(serviceName + ".attributes.")) {
                sc.getAttributes().put(attrKey.replace(serviceName + ".attributes.", ""),
                        config.getProperty(attrKey).toString());
            }
        }

        return sc;
    }
    // </editor-fold>

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();

        result.append("<object attr='").append(getClass().getName()).append("'>");
        result.append("<name>").append(getServiceName()).append("</name>");
        result.append("<connectionPort>").append(getConnectionPort()).append("</connectionPort>");
        result.append("<serviceClass>").append(getServiceClass()).append("</serviceClass>");
        result.append("<serviceType>").append(getServiceType()).append("</serviceType>");
        result.append("<startupType>").append(getStartupType()).append("</startupType>");
        result.append("<isIgnoreMaximumConnection>").append(isIgnoreConnectionLimit()).append("</isIgnoreMaximumConnection>");
        result.append("<maximumConnections>").append(getMaximumConnections()).append("</maximumConnections>");

        result.append("<attributes>")
                .append("<size>").append(getAttributes().size()).append("</size>");
        if (!getAttributes().isEmpty()) {
            ArrayList<String> keyList;
            keyList = new ArrayList<>(getAttributes().keySet());

            String key;
            String value;
            for (Object attrKey : keyList) {
                key = attrKey.toString();
                value = getAttributes().get(key);
                result.append("<" + key + ">").append(value).append("</" + key + ">");

                // yield processing to other threads
                Thread.yield();
            }
        }
        result.append("</attributes>");

        result.append("</object>");

        return result.toString();
    }
}
