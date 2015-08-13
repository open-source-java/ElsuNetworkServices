package elsu.network.services.test;

import elsu.network.services.core.ServiceConfig;
import elsu.network.services.core.IService;
import elsu.network.services.core.AbstractConnection;
import elsu.network.services.core.AbstractService;
import elsu.network.factory.ServiceFactory;
import elsu.network.services.*;
import java.io.*;
import java.util.Map;

/**
 *
 * @author Seraj Dhaliwal (seraj.s.dhaliwal@uscg.mil)
 */
public class ReverseService extends AbstractService implements IService {

    // <editor-fold desc="class private storage">
    // local storage for service shutdown string
    private volatile String _serviceShutdown = "#$#";

    // local storage for connection terminator string
    private volatile String _connectionTerminator = ".";
    // </editor-fold>

    // <editor-fold desc="class constructor destructor">
    public ReverseService(String threadGroup, ServiceConfig serviceConfig) {
        super(threadGroup, serviceConfig);

        // call the super class constructor
        initializeLocalProperties();
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

        this._serviceShutdown = getFactory().getProperty(
                "service.shutdown").toString();
        this._connectionTerminator
                = getFactory().getProperty(
                        "connection.terminator").toString();
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
    /**
     * serve(...) method is the main method of the service which processes the
     * client socket using the streams (in/out). The method is shared by all
     * client sockets.
     * <p>
     * This method returns the reverse of the string passed. The service
     * disconnects the connection after sending the updated string.
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
            // this is the main processing loop, client sends commands, which
            // are parsed, executed, and result returned to the client
            for (;;) {
                // read the client data from the socket
                String line = in.readLine();

                // if the input is null or the value matches connection 
                // terminator then disconnect the client
                if ((line == null) || line.equals(getConnectionTerminator())) {
                    break;
                }

                // increase the total # of incomming messages
                increaseTotalMessagesReceived();

                // loop for the inbound message, reverse it
                for (int j = line.length() - 1; j >= 0; j--) {
                    out.print(line.charAt(j));

                    // yield processing to other threads
                    Thread.yield();
                }

                // send the result of the message reversal back to the client
                out.print(getRecordTerminator());
                out.flush();

                // increase the total # of sent messages
                increaseTotalMessagesSent();

                // yield processing to other threads
                Thread.yield();
            }
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
