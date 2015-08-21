package elsu.network.services;

import elsu.network.services.core.IService;
import java.io.*;
import java.net.*;

/**
 * AbstractConnection() class is used to define the base / abstract
 properties and methods for all connections. The class provides storage for
 * all common properties for the service connections.
 * <p>
 * This class extends Thread class but does not provide its implementation, that
 * is done by the concrete classes ServiceConnectionAbstractBasic and
 * ServiceConnectionAbstractCustom.
 *
 * @author Seraj Dhaliwal (seraj.s.dhaliwal@uscg.mil)
 * @see Thread
 * @see ServiceConnectionBasic
 * @see ServiceConnectionCustom
 * @see Service
 */
public abstract class AbstractConnection extends Thread {

    // <editor-fold desc="class private storage">
    // runtime sync object
    private Object _runtimeSync = new Object();

    // service object which owns this connection
    private volatile IService _service = null;

    // client socket created through the service listener or manually by the
    // service.  this is the core communication object between the service
    // and the client
    private volatile Socket _client = null;

    // property which notifies client methods if the client is active or not
    private volatile boolean _isActive = false;
    // </editor-fold>

    // <editor-fold desc="class constructor destructor">
    /**
     * ServiceConnection constructor uses the service thread group to link the
     * connection threads. It also stores the service which the client is
     * serving for use by the connection.
     *
     * @param service
     */
    public AbstractConnection(IService service) {
        super(service.getThreadGroup(), "Connection:"
                + service.getServiceConfig().getServiceName());

        this._service = service;
    }
    // </editor-fold>

    // <editor-fold desc="class getter/setters">
    /**
     * isActive() method returns if the connection is active or not. If the
     * connection is not-socket connection then this property is manually
     * managed by the connection. If the connection is closed for socket based
     * connection, then return is false.
     *
     * @return <code>boolean</code> value of the property
     */
    public boolean isActive() {
        if ((getClient() != null) && (getClient().isClosed())) {
            this._isActive = false;
        }

        return this._isActive;
    }

    /**
     * isActive(...) method allows the connection to change the value of the
     * active property manually. This is used to signal loops within the
     * connection code that the connection is terminating, exit gracefully.
     *
     * @param active
     * @return <code>boolean</code> value of the property
     */
    public boolean isActive(boolean active) {
        this._isActive = active;
        return isActive();
    }

    /**
     * getClient() method returns the current client for the connection.
     *
     * @return <code>Socket</code> object for the connection.
     */
    public Socket getClient() {
        Socket result = null;
        
        synchronized (this._runtimeSync) {
            result = this._client;
        }
        
        return result;
    }

    /**
     * setClient(...) stores the socket which is serving the connection.
     *
     * @param socket
     */
    protected void setClient(Socket socket) {
        synchronized (this._runtimeSync) {
            this._client = socket;
        }
    }

    /**
     * getService() returns the service which created the connection
     *
     * @return <code>IService</code> object which the connection is serving.
     */
    public IService getService() {
        IService result = null;
        
        synchronized (this._runtimeSync) {
            result = this._service;
        }
        
        return result;
    }
    // </editor-fold>
}
