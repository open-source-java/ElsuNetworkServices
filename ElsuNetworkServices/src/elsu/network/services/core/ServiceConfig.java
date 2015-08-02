package elsu.network.services.core;

import elsu.network.core.ServiceType;
import elsu.network.core.ServiceStartupType;
import elsu.support.*;
import java.util.*;

/**
 * ServiceConfig class is used to store the configuration parameters loaded by
 * the ConfigLoader class from the app.config. Each service has its own
 * ServiceConfig object and all the attributes to include any child service
 * definitions loaded into subcribers or publishers.
 * <p>
 * Attributes is a basic hashMap (key/value) pair.
 *
 * @author Seraj Dhaliwal (seraj.s.dhaliwal@uscg.mil)
 * @see ConfigLoader
 */
public class ServiceConfig {

    // <editor-fold desc="class private storage">
    // store the name of the service, this is unique across the application
    private volatile String _serviceName = null;

    // service port, unique across the application
    private volatile int _connectionPort = 0;

    // service class for instantiation through reflection
    private volatile String _serviceClass = null;

    // service type: SERVER, CLIENT, SUBSCRIBER, PUBLISHER
    // SERVER type is always configured to have a listener
    // CLIENT type does not have a listener but it can create a custom listener
    // if required through attribute properties
    // SUBSCRIBER and PUBLISHER types are for child services
    // - SUBSCRIBER is service which connects to another service to retrieve
    // data and store it locally
    // - PUBLISHER is service which reads local data collected by COLLECTOR
    // and forwarding it to other services
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

    // list of subcribers defined for the service
    private volatile ArrayList<ServiceConfig> _subscribers = null;

    // list of publishers defined for the service
    private volatile ArrayList<ServiceConfig> _publishers = null;

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
        this._subscribers = new ArrayList<>();
        this._publishers = new ArrayList<>();
        this._attributes = new HashMap<>();
    }
    // </editor-fold>

    // <editor-fold desc="class getter/setters">
    public synchronized Map<String, String> getAttributes() {
        return _attributes;
    }

    public synchronized int getConnectionPort() {
        return this._connectionPort;
    }

    public synchronized void setConnectionPort(int port) {
        this._connectionPort = port;
    }

    public synchronized boolean isIgnoreConnectionLimit() {
        return this._isIgnoreConnectionLimit;
    }

    public synchronized boolean isIgnoreConnectionLimit(boolean ignore) {
        this._isIgnoreConnectionLimit = ignore;
        return isIgnoreConnectionLimit();
    }

    public synchronized int getMaximumConnections() {
        return this._maximumConnections;
    }

    public synchronized void setMaximumConnections(int allowedMax) {
        this._maximumConnections = allowedMax;
    }

    public synchronized ArrayList<ServiceConfig> getPublishers() {
        return this._publishers;
    }

    public synchronized String getServiceClass() {
        return this._serviceClass;
    }

    public synchronized void setServiceClass(String classRef) {
        this._serviceClass = classRef;
    }

    public synchronized String getServiceName() {
        return this._serviceName;
    }

    public synchronized void setServiceName(String name) {
        this._serviceName = name;
    }

    public synchronized ServiceType getServiceType() {
        return _serviceType;
    }

    public synchronized ServiceStartupType getStartupType() {
        return this._startupType;
    }

    public synchronized void setStartupType(ServiceStartupType type) {
        this._startupType = type;
    }

    public synchronized void setServiceType(ServiceType type) {
        this._serviceType = type;
    }

    public synchronized ArrayList<ServiceConfig> getSubscribers() {
        return this._subscribers;
    }

    public synchronized ServiceConfig getSubscriber(int port) {
        ServiceConfig result = null;

        // loop through and create subscriber child services if defined
        for (ServiceConfig subscriber : getSubscribers()) {
            // if the child service is set to auto-start, configure the service
            // otherwise the child service is not processed
            if (subscriber.getStartupType() == ServiceStartupType.AUTOMATIC) {
                if (subscriber.getConnectionPort() == port) {
                    result = subscriber;
                    break;
                }
            }
        }

        return result;
    }

    public synchronized ServiceConfig getSubscriber(String name) {
        ServiceConfig result = null;

        // loop through and create subscriber child services if defined
        for (ServiceConfig subscriber : getSubscribers()) {
            // if the child service is set to auto-start, configure the service
            // otherwise the child service is not processed
            if (subscriber.getStartupType() == ServiceStartupType.AUTOMATIC) {
                if (subscriber.getServiceName().equals(name)) {
                    result = subscriber;
                    break;
                }
            }
        }

        return result;
    }

    public synchronized ServiceConfig getPublisher(int port) {
        ServiceConfig result = null;

        // loop through and create subscriber child services if defined
        for (ServiceConfig publisher : getPublishers()) {
            // if the child service is set to auto-start, configure the service
            // otherwise the child service is not processed
            if (publisher.getStartupType() == ServiceStartupType.AUTOMATIC) {
                if (publisher.getConnectionPort() == port) {
                    result = publisher;
                    break;
                }
            }
        }

        return result;
    }

    public synchronized ServiceConfig getPublisher(String name) {
        ServiceConfig result = null;

        // loop through and create subscriber child services if defined
        for (ServiceConfig publisher : getPublishers()) {
            // if the child service is set to auto-start, configure the service
            // otherwise the child service is not processed
            if (publisher.getStartupType() == ServiceStartupType.AUTOMATIC) {
                if (publisher.getServiceName().equals(name)) {
                    result = publisher;
                    break;
                }
            }
        }

        return result;
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

        // copy of collector configurations from the original object
        for (ServiceConfig collector : getSubscribers()) {
            copyConfig.getSubscribers().add(collector.clone());

            // yield processing to other threads
            Thread.yield();
        }

        // copy of distributor configurations from the original object
        for (ServiceConfig publisher : getPublishers()) {
            copyConfig.getPublishers().add(publisher.clone());

            // yield processing to other threads
            Thread.yield();
        }

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

        // copy of collector configurations from the original object
        for (String attrKey : config.getClassSet(serviceName + ".")) {
            childServiceName = attrKey.replace(".class", "");
            if (config.getProperty(childServiceName + ".serviceType").toString().equals("SUBSCRIBER")) {
                childConfig = ServiceConfig.LoadConfig(config, childServiceName);
                sc.getSubscribers().add(childConfig);
            }
        }

        // copy of distributor configurations from the original object
        for (String attrKey : config.getClassSet(serviceName + ".")) {
            childServiceName = attrKey.replace(".class", "");
            if (config.getProperty(childServiceName + ".serviceType").toString().equals("PUBLISHER")) {
                childConfig = ServiceConfig.LoadConfig(config, childServiceName);
                sc.getPublishers().add(childConfig);
            }
        }

        // copy the original objects attributes to new object
        for (String attrKey : config.getProperties().keySet()) {
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

        result.append("<subcribers>")
                .append("<size>").append(getSubscribers().size()).append("</size>");
        if (getSubscribers().size() > 0) {
            for (ServiceConfig conn : getSubscribers()) {
                result.append(conn.toString());

                // yield processing to other threads
                Thread.yield();
            }
        }
        result.append("</subcribers>");

        result.append("<publishers>")
                .append("<size>").append(getPublishers().size()).append("</size>");
        if (getPublishers().size() > 0) {
            for (ServiceConfig conn : getPublishers()) {
                result.append(conn.toString());

                // yield processing to other threads
                Thread.yield();
            }
        }
        result.append("</publishers>");

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
