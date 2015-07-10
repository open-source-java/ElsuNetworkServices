package elsu.network.services.test;

import elsu.network.services.*;
import elsu.network.service.factory.*;
import java.io.*;

/**
 *
 * @author Seraj Dhaliwal (seraj.s.dhaliwal@uscg.mil)
 */
public class ReverseService extends ServiceAbstract implements IService {

    // <editor-fold desc="class private storage">
    // local storage for service shutdown string
    private volatile String _serviceShutdown = "#$#";

    // local storage for connection terminator string
    private volatile String _connectionTerminator = ".";
    // </editor-fold>

    // <editor-fold desc="class constructor destructor">
    public ReverseService(ServiceFactory factory, String threadGroup,
            ServiceConfig serviceConfig) {
        super(factory, threadGroup, serviceConfig);

        // call the super class constructor
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
     * This method returns the string sent by the client by reversing and
     * echoing it back to the client. It was designed to simulate a working load
     * where CPU performs the work and then returns the data back.
     *
     * @param iStream
     * @param oStream
     * @throws Exception
     */
    @Override
    public void serve(InputStream iStream, OutputStream oStream) throws
            Exception {
        // create bufferred reader reference for the input stream. the 
        // reference is created outside the try...catch (Exception ex)the
        // finally to perform cleanup correctly
        BufferedReader in = null;

        // create print writer reference for the output stream. the 
        // reference is created outside the try...catch (Exception ex)the
        // finally to perform cleanup correctly
        PrintWriter out = null;

        // this is to prevent socket to stay open after error
        try {
            in = new BufferedReader(new InputStreamReader(iStream));
            out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                    oStream)));

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
        } catch (Exception ex){
            // log error for tracking
            logError(getClass().toString() + ", serve(), "
                    + getServiceConfig().getServiceName() + " on port "
                    + getServiceConfig().getConnectionPort() + ", "
                    + ex.getMessage());
        } finally {
            // close out all open in/out streams.
            try {
                out.close();
            } catch (Exception exi){
            }
            try {
                in.close();
            } catch (Exception exi){
            }
        }
    }

    /**
     * serve(...) method is the optional method of the service which processes
     * the client connection which can be not socket based.
     * <p>
     * Not used for this service, Not supported exception is thrown if executed.
     *
     * @param conn
     * @throws Exception
     */
    @Override
    public void serve(ServiceConnectionAbstract conn) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    // </editor-fold>
}
