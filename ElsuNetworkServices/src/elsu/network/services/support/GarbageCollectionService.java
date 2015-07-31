package elsu.network.services.support;

import elsu.network.services.*;
import elsu.network.service.factory.*;
import java.io.*;

/**
 *
 * @author Seraj Dhaliwal (seraj.s.dhaliwal@uscg.mil)
 */
public class GarbageCollectionService extends AbstractService
        implements IService {

    // <editor-fold desc="class private storage">
    // local storage for service shutdown string
    private volatile String _serviceShutdown = "#$#";

    // local storage for connection terminator string
    private volatile String _connectionTerminator = ".";

    // local storage for gc idle timeout
    private volatile int _gcDelay = 10000;
    // </editor-fold>

    // <editor-fold desc="class constructor destructor">
    public GarbageCollectionService(ServiceFactory factory, String threadGroup,
            ServiceConfig serviceConfig) {
        // call the super class constructor
        super(factory, threadGroup, serviceConfig);

        initializeLocalProperties();
    }

    /**
     * initializeProperties() is a generic method to consolidate all initial
     * variable instantiation outside of class constructor. It allows the
     * variables to be reset from another method within a class if required.
     *
     */
    private void initializeLocalProperties() {
        this._serviceShutdown = getFactory().getApplicationProperties().get(
                "service.shutdown").toString();
        this._connectionTerminator
                = getFactory().getApplicationProperties().get(
                        "connection.terminator").toString();

        try {
            this._gcDelay = Integer.parseInt(
                    getServiceConfig().getAttributes().get(
                            "service.garbage.collection.timer").toString());
        } catch (Exception ex) {
            logError(getClass().toString() + ", initializeLocalProperties(), "
                    + getServiceConfig().getServiceName() + " on port "
                    + getServiceConfig().getConnectionPort()
                    + ", invalid service.garbage.collection.timer, "
                    + ex.getMessage());
            this._gcDelay = 10000;
        }
    }
    // </editor-fold>

    // <editor-fold desc="class getter/setters">	
    /**
     * getConnectionTerminator() method returns the connection terminator used
     * to signal the connection to terminate gracefully.
     *
     * @return <code>String</code> returns the connection terminator value.
     */
    private synchronized String getConnectionTerminator() {
        return this._connectionTerminator;
    }

    private synchronized int getGCDelay() {
        return this._gcDelay;
    }

    /**
     * getServiceAbstractShutdown() method returns the value which when received
     * through the client will shutdown the service.
     *
     * @return <code>String</code> value of the shutdown string
     */
    private synchronized String getServiceShutdown() {
        return this._serviceShutdown;
    }
    // </editor-fold>

    // <editor-fold desc="class methods">
    @Override
    public synchronized void checkConnections() {
        if (isRunning()) {
            try {
                if (getConnections().isEmpty()) {
                    ServiceConnection dsConn = new ServiceConnection(null, this);
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
     * serve(...) method is the optional method of the service which processes
     * the client connection using the socket in and out streams.
     * <p>
     * Not used for this service, Not supported exception is thrown if executed.
     *
     * @param iStream
     * @param oStream
     * @throws Exception
     */
    @Override
    public void serve(InputStream iStream, OutputStream oStream) throws
            Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * serve(...) method of the service which processes the client connection
     * which can be non socket based.
     * <p>
     * This method as one function, to execute garbage collection to allow
     * orphaned objects to be collected quickly vice waiting for the VM to run.
     * Most of the objects in this API are one time use variables and with high
     * volumne transactions from network clients, this can lead to heap
     * out-of-memory or slow memory allocations due to high # of references in
     * stack.
     *
     * @param conn
     * @throws Exception
     */
    @Override
    public void serve(AbstractServiceConnection conn) throws Exception {
        final ServiceConnection cConn = (ServiceConnection) conn;

        // this is to prevent socket to stay open after error
        try {
            // loop as long as the service is running, since there is no 
            // connection, this is service threaded method
            while (isRunning()) {
                // yield processing to other threads
                Thread.sleep(getGCDelay());

                // force garbage collectio
                System.gc();
            }
        } catch (Exception ex) {
            // log error for tracking
            logError(getClass().toString() + ", serve(), "
                    + getServiceConfig().getServiceName() + " on port "
                    + getChildConfig().getConnectionPort() + ", "
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
    public synchronized void start() throws Exception {
        super.start();

        checkConnections();
    }
    // </editor-fold>

    @Override
    public void checkConnection(AbstractServiceConnection connection) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
