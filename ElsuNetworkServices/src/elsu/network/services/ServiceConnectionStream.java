package elsu.network.services;

import java.io.*;
import java.net.*;

/**
 * ServiceConnectionAbstractBasic class extends the base abstract class
 AbstractServiceConnection to provide implementation for the thread run
 method.
 * <p>
 * The Thread run() method is overridden and socket streams are used to serve
 * the client connection. Service stream based method provides the core
 * processing (business logic) the the connection.
 *
 * @author Seraj Dhaliwal (seraj.s.dhaliwal@uscg.mil)
 * @see AbstractServiceConnection
 * @see Service
 */
public class ServiceConnectionStream extends AbstractServiceConnection {

    // <editor-fold desc="class constructor destructor">
    /**
     * ServiceConnectionAbstractBasic(...) constructor allows the service
     * listener to pass the client socket and service which are stored for use
     * by the connection during processing.
     *
     * @param client
     * @param service
     */
    public ServiceConnectionStream(Socket client, IService service) {
        super(service);

        setClient(client);
    }
    // </editor-fold>

    // <editor-fold desc="class methods">
    /**
     * run() method implementation is used to ensure if there is an error in the
     * processing the client connection is gracefully closed and removed from
     * the service list.
     */
    @Override
    public void run() {
        // ensure all errors are captured to prevent connection to be orphaned
        try {
            // set client active property
            isActive(true);

            // extract the client socket streams and create references to pass
            // to the service method
            InputStream in = getClient().getInputStream();
            OutputStream out = getClient().getOutputStream();

            // call the service method to perform processing for the client
            getService().serve(in, out);
        } catch (Exception ex){
            // log the error
            getService().logError(getClass().toString() + ", run(), "
                    + getService().getServiceConfig().getServiceName() + ", "
                    + ex.getMessage());
        } finally {
            // clear client active property
            isActive(false);

            // if the service is running, then remove the connection; otherwise,
            // the connection is forced closed and removed as part of the 
            // service shutdown
            if (getService().isRunning()) {
                getService().removeConnection(this);
            }
        }
    }
    // </editor-fold>
}
