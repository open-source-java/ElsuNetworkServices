package elsu.network.services.support;

import elsu.network.services.core.ServiceConfig;
import elsu.network.services.core.IService;
import elsu.network.services.core.AbstractService;
import elsu.network.services.AbstractConnection;
import elsu.network.factory.ServiceFactory;
import elsu.network.services.*;
import java.io.*;
import java.security.*;
import java.math.*;
import java.util.Map;

/**
 *
 * @author Seraj Dhaliwal (seraj.s.dhaliwal@uscg.mil)
 */
public class StateService extends AbstractService implements IService {

    // <editor-fold desc="class private storage">
    // runtime sync object
    private Object _runtimeSync = new Object();

    // local storage for service shutdown string
    private volatile String _serviceShutdown = "#$#";

    // local storage for connection terminator string
    private volatile String _connectionTerminator = ".";

    // public static to keep track of the last value
    public static volatile int id = 0;

    // public static for generating random numbers
    public static SecureRandom random = new SecureRandom();
    // </editor-fold>

    // <editor-fold desc="class constructor destructor">
    public StateService(String threadGroup, ServiceConfig serviceConfig) {
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
     * getNextId() method returns the next value of the shared static number
     * after incrementing it.
     *
     * @return <code>int</code> value
     */
    private int getNextId() {
        int result = 0;
        
        synchronized (this._runtimeSync) {
            result = StateService.id++;
        }
        
        return result;
    }
    // </editor-fold>

    // <editor-fold desc="class methods">
    /**
     * serve(...) method is the main method of the service which processes the
     * client socket using the streams (in/out). The method is shared by all
     * client sockets.
     * <p>
     * This method returns two values: (1) static int value which is sequential
     * and provides a way to create a shared counter and (2) random generated 32
     * byte string to create a global GUID.
     *
     * @param conn
     * @throws Exception
     */
    @Override
    public void serve(AbstractConnection conn) throws Exception {
        // local parameter for reader thread access, passes the connection 
        // object
        final Connection cConn = (Connection) conn;

        // local parameter for reader thread access, passes the socket in stream
        final BufferedReader in = new BufferedReader(new InputStreamReader(
                cConn.getClient().getInputStream()));

        // local parameter for reader thread access, passes the socket out 
        // stream
        final PrintWriter out = new PrintWriter(new BufferedWriter(
                new OutputStreamWriter(cConn.getClient().getOutputStream())));

        // this is to prevent socket to stay open after error
        try {
            // increase the total # of incomming messages
            increaseTotalMessagesReceived();

            // format and return the id and the session guid as XML
            out.print("<state>");
            out.print("<id>" + getNextId() + "</id>");
            out.print("<guid>"
                    + new BigInteger(130, StateService.random).toString(32)
                    + "</guid>");
            out.print("</state>" + getRecordTerminator());
            out.flush();

            // increase the total # of incomming messages
            increaseTotalMessagesSent();
        } catch (Exception ex) {
            // log error for tracking
            logError(getClass().toString() + ", serve(), "
                    + getServiceConfig().getServiceName() + " on port "
                    + getServiceConfig().getConnectionPort() + ", "
                    + ex.getMessage());
        } finally {
            // close out all open in/out streams.
            try {
                try {
                    out.flush();
                } catch (Exception exi) {
                }
                out.close();
            } catch (Exception exi) {
            }
            try {
                in.close();
            } catch (Exception exi) {
            }
        }
    }
    // </editor-fold>

    @Override
    public void checkConnection(AbstractConnection connection) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void checkConnections() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
