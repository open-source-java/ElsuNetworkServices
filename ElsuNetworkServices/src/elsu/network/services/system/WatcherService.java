package elsu.network.services.system;

import elsu.events.*;
import elsu.network.services.core.*;
import elsu.network.services.*;

public class WatcherService extends AbstractService
        implements IService {

    // <editor-fold desc="class private storage">
    // runtime sync object
    private Object _runtimeSync = new Object();

    // local storage for service shutdown string
    private volatile String _serviceShutdown = "#$#";

    // local storage for connection terminator string
    private volatile String _connectionTerminator = ".";

    // local storage for scan period idle
    private volatile int _scanPeriod = 10000;

    // local storage for service port list
    private volatile String _watchList = "";
    // </editor-fold>

    // <editor-fold desc="class constructor destructor">
    public WatcherService(String threadGroup, ServiceConfig serviceConfig) {
        // call the super class constructor
        super(threadGroup, serviceConfig);

        // local config properties for local reference by class method
        // initializeLocalProperties();
    }

    /**
     * initializeProperties() is a generic method to consolidate all initial
     * variable instantiation outside of class constructor. It allows the
     * variables to be reset from another method within a class if required.
     *
     */
    @Override
    protected void initializeLocalProperties() {
        super.initializeLocalProperties();

        this._serviceShutdown = getProperty("service.shutdown").toString();
        this._connectionTerminator
                = getProperty("connection.terminator").toString();

        try {
            this._scanPeriod = Integer.parseInt(
                    getServiceConfig().getAttributes().get(
                            "scanPeriod").toString()) * 1000;
        } catch (Exception ex) {
            logError(getClass().toString() + ", initializeLocalProperties(), "
                    + getServiceConfig().getServiceName() + " on port "
                    + getServiceConfig().getConnectionPort()
                    + ", invalid scanPeriod, "
                    + ex.getMessage());
            this._scanPeriod = 15000;
        }

        this._watchList = getServiceConfig().getAttribute("watchList");
    }
    // </editor-fold>

    // <editor-fold desc="class getter/setters">	
    /**
     * getConnectionTerminator() method returns the connection terminator used
     * to signal the connection to terminate gracefully.
     *
     * @return <code>String</code> returns the connection terminator value.
     */
    private String getConnectionTerminator() {
        String result = "";

        synchronized (this._runtimeSync) {
            result = this._connectionTerminator;
        }

        return result;
    }

    private int getScanPeriod() {
        int result = 0;

        synchronized (this._runtimeSync) {
            result = this._scanPeriod;
        }

        return result;
    }

    /**
     * getServiceAbstractShutdown() method returns the value which when received
     * through the client will shutdown the service.
     *
     * @return <code>String</code> value of the shutdown string
     */
    private String getServiceShutdown() {
        String result = "";

        synchronized (this._runtimeSync) {
            result = this._serviceShutdown;
        }

        return result;
    }

    /**
     * getWatchList() method returns the value which when received from the
     * config for list of ports to be watched.
     *
     * @return <code>String</code> value of the shutdown string
     */
    private String getWatchList() {
        String result = "";

        synchronized (this._runtimeSync) {
            result = this._watchList;
        }

        return result;
    }
    // </editor-fold>

    // <editor-fold desc="class methods">
    @Override
    public void checkConnections() {
        if (isRunning()) {
            try {
                if (getConnections().isEmpty()) {
                    Connection dsConn = new Connection(null, this);
                    addConnection(null, dsConn);
                }
            } catch (Exception ex) {
                // log error for tracking
                logError(getClass().toString() + ", checkConnections(), "
                        + getServiceConfig().getServiceName()
                        + ", error creating connection "
                        + getServiceConfig().getServiceName()
                        + " on port " + getServiceConfig().getConnectionPort()
                        + ", " + ex.getMessage());
            }

            // yield processing to other threads
            Thread.yield();
        }
    }

    /**
     * serve(...) method of the service which processes the client connection
     * which can be non socket based.
     * <p>
     * This method as function, to execute garbage collection to allow orphaned
     * objects to be collected quickly vice waiting for the VM to run. Most of
     * the objects in this API are one time use variables and with high volume
     * transactions from network clients, this can lead to heap out-of-memory or
     * slow memory allocations due to high # of references in stack.
     *
     * @param conn
     * @throws Exception
     */
    @Override
    public void serve(AbstractConnection conn) throws Exception {
        // this is to prevent socket to stay open after error
        try {
            // array of watched ports
            String[] services = getWatchList().split(",");
            Object result = null;

            // loop as long as the service is running, since there is no 
            // connection, this is service threaded method
            while (isRunning()) {
                // yield processing to other threads
                Thread.sleep(getScanPeriod());

                // check the factory for each port 
                // if service is AUTOMATIC or DELAYEDSTART then start the service
                // - if service exists, but is not running then issue start
                // - if service is not present, then create it
                for (String serviceName : services) {
                    result = notifyFactoryListener(this, EventStatusType.statusTypeFor("VALIDATESERVICE"), null, serviceName);

                    if (result instanceof Exception) {
                        logError(getClass().toString() + ", serve(), " + ((Exception) result).getMessage());
                    }
                }
            }
        } catch (Exception ex) {
            // log error for tracking
            logError(getClass().toString() + ", serve(), "
                    + getServiceConfig().getServiceName() + " on port "
                    + getServiceConfig().getConnectionPort() + ", "
                    + ex.getMessage());
        } finally {
            // we have exited the method, but if the service is still running
            // restart the connection
            if (isRunning()) {
                checkConnections();
            }
        }
    }

    /**
     * start() method overloaded from the super class is used to update the
     * local variables for processing. At the end the method calls the
     * checkConnections method to create connection to the
     *
     * @throws java.lang.Exception equipment.
     */
    @Override
    public void start() throws Exception {
        super.start();

        checkConnections();
    }
    // </editor-fold>

    @Override
    public void checkConnection(AbstractConnection connection) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
