package elsu.network.services.support;

import elsu.network.services.*;
import elsu.network.service.factory.*;
import java.io.*;
import java.util.*;

/**
 *
 * @author Seraj Dhaliwal (seraj.s.dhaliwal@uscg.mil)
 */
public class TimeService extends ServiceAbstract implements IService {

    // <editor-fold desc="class private storage">
    // local storage for service shutdown string
    private volatile String _serviceShutdown = "#$#";

    // local storage for connection terminator string
    private volatile String _connectionTerminator = ".";
    // </editor-fold>

    // <editor-fold desc="class constructor destructor">
    public TimeService(ServiceFactory factory, String threadGroup,
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
     * This method returns the current system time at the local system the
     * service is running and servicing clients. The service disconnects the
     * connection after sending the time string.
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

            // increase the total # of incomming messages
            increaseTotalMessagesReceived();

            // send the date/time from the local application to the client
            out.print(new Date() + getRecordTerminator());
            out.flush();

            // increase the total # of sent messages
            increaseTotalMessagesSent();
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