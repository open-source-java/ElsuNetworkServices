package elsu.network.services;

import elsu.network.services.core.*;
import java.util.*;
import java.net.*;

/**
 * ServiceConnectionAbstractCustom class extends the base abstract class
 AbstractConnection to provide implementation for the thread run
 method.
 * <p>
 * The Thread run() method is overridden and connection object is used to serve
 * the client connection.
 *
 * @author Seraj Dhaliwal (seraj.s.dhaliwal@uscg.mil)
 * @see AbstractConnection
 * @see Service
 */
public class Connection extends AbstractConnection {

    // <editor-fold desc="class private storage">
    // runtime sync object
    private Object _runtimeSync = new Object();

    // local hashMap to store service or application properties by the 
    // connection for direct access
    private Map<String, Object> _properties = new HashMap<>();
    // </editor-fold>

    // <editor-fold desc="class constructor destructor">
    /**
     * ServiceConnectionAbstractCustom(...) constructor allows the service to
     * manually create connection object for custom processing without a socket.
     * These types of connections can be used for heartbeat, garbage collection,
     * or other internal processing.
     *
     * @param service
     */
    public Connection(IService service) {
        super(service);
    }

    /**
     * ServiceConnectionAbstractCustom(...) constructor allows the service to
     * manually create connection object for custom processing with a socket.
     * These types of connections can be used by child services which create
     * connections for specific tasks.
     *
     * @param client
     * @param service
     */
    public Connection(Socket client, IService service) {
        super(service);

        setClient(client);
    }
    // </editor-fold>

    // <editor-fold desc="class getter/setters">
    public Object addProperty(String key, Object value) {
        return this._properties.put(key, value);
    }

    public Object getProperty(String key) {
        return this._properties.get(key);
    }

    public Set<String> getKeySet() {
        Set<String> result = null;
        
        synchronized (this._runtimeSync) {
            result = this._properties.keySet();
        }
        
        return result;
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

            // call the service method to perform processing for the client,
            // since this is a custom client, socket may not exist so we just
            // pass the entire connection object to the service to process
            getService().serve((AbstractConnection) this);
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
