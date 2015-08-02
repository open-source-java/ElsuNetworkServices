package elsu.network.services;

import elsu.network.services.core.IService;
import java.io.*;
import java.net.*;

/**
 * ServiceListener class creates a threaded implementation of the ServerSocket
 * to listen on the service port. Each Service has-a ServiceListener to manage
 * the incoming connections. Each connection is created in its own thread using
 * ServiceConnectionBasic or ServiceConnectionCustom.
 * <p>
 * ServiceListener accepts a connection, checks to make sure the connection
 * limit has not been exceeded, adds the connection the service connection list,
 * and starts the connection. Most of these methods are part of the factory
 * class to allow general access from any class managed by factory.
 *
 * @author Seraj Dhaliwal (seraj.s.dhaliwal@uscg.mil)
 * @see ServiceFactory
 * @see Service
 * @see ServiceConnection
 * @see ServiceConnectionBasic
 * @see ServiceConnectionCustom
 */
public class ServiceListener extends Thread {

    // <editor-fold desc="class private storage">
    // storage for the listener socket
    private volatile ServerSocket _listen_socket = null;
    // socket port which the listener is monitoring
    private volatile int _port;
    // service which is bound to the port to provide processing oversight
    private volatile IService _service;
    // status of the socket listener
    private volatile boolean _isActive = false;
    // </editor-fold>

    // <editor-fold desc="class constructor destructor">
    /**
     * ServiceListener(...) constructor takes the service which is the
     * controller for all classes which support it.
     * <p>
     * The port is defined in the service config.
     *
     * @param service
     * @throws java.io.IOException
     * @see Service
     */
    public ServiceListener(IService service) throws IOException {
        super(service.getThreadGroup(), "Listener:"
                + service.getServiceConfig().getConnectionPort());

        // store local properties derived from service object
        this._port = service.getServiceConfig().getConnectionPort();

        // create listener socket for the port
        this._listen_socket = new ServerSocket(
                service.getServiceConfig().getConnectionPort());

        // give it a non-zero timeout so accept() can be interrupted
        this._listen_socket.setSoTimeout(600000);

        // store the service
        this._service = service;

        // set the listener status/running to true
        this._isActive = true;

        // load configuration properties
        initializeLocalProperties();
    }

    /**
     * ServiceListener(...) constructor takes the service which is the
     * controller for all classes which support it.
     * <p>
     * The port is defined as a parameter allowing multiple listeners to be
     * created for one service. This constructor is not the default service
     * constructor but normally used by ControlService to load custom configs
     *
     * @param port
     * @param service
     * @throws java.io.IOException
     * @see Service
     */
    public ServiceListener(int port, IService service) throws IOException {
        super(service.getThreadGroup(), "Listener:" + port);

        // store local properties derived from service object
        this._port = port;

        // create listener socket for the port
        this._listen_socket = new ServerSocket(port);

        // give it a non-zero timeout so accept() can be interrupted
        this._listen_socket.setSoTimeout(600000);

        // store the service
        this._service = service;

        // set the listener status/running to true
        this._isActive = true;

        // load configuration properties
        initializeLocalProperties();
    }

    /**
     * initializeProperties() is a generic method to consolidate all initial
     * variable instantiation outside of class constructor. It allows the
     * variables to be reset from another method within a class if required.
     *
     */
    private void initializeLocalProperties() {
    }
    // </editor-fold>

    // <editor-fold desc="class getter/setters">
    /**
     * getListen_socket() method returns the listener socket component which is
     * monitoring the port for the service.
     *
     * @return <code>ServerSocket</code> is the listener socket
     */
    public synchronized ServerSocket getListener() {
        return this._listen_socket;
    }

    /**
     * getPort() method returns the current port the listener is monitoring for
     * inbound connections.
     *
     * @return <code>int</code> is the value of the port.
     */
    public synchronized int getPort() {
        return this._port;
    }

    /**
     * isActive() method returns the listener state (true/false). Call to the
     * method will validate if the listener is closed, if true, the state will
     * be changed to false.
     *
     * @return <code>boolean</code> is the current state of the listener
     */
    public synchronized boolean isActive() {
        // check if the listener socket is closed, it yes, then override
        // the status and set it to false, this is direct access otherwise
        // calling the setter will result in loop
        if (getListener().isClosed()) {
            this._isActive = false;
        }

        return this._isActive;
    }

    /**
     * isActive(...) method is used to set the state of the listener object and
     * returns the current state. Method validates the current state to the
     * socket listener to ensure true state is reflected.
     *
     * @param running is the new desired state of the object.
     * @return <code>boolean</code> is the current state of the listener
     *
     */
    private synchronized boolean isActive(boolean running) {
        // set the status to the user option
        this._isActive = running;

        // return the value of the current state; note, the default getter
        // dose validation of the listener status
        return isActive();
    }

    /**
     * getService() method returns the service which owns the listener object
     *
     * @return <code>IService</code> is the service object
     */
    public synchronized IService getService() {
        return this._service;
    }
    // </editor-fold>

    // <editor-fold desc="class methods">
    /**
     * shutdown() method is general method used to perform a graceful shutdown
     * of the listener once notified through the service. When completed the
     * Server Socket is closed and running status of the listener object is set
     * to false.
     *
     */
    public synchronized void shutdown() {
        // update the status to false so the listener will not acivate itself
        this.isActive(false);

        // send interrupt to the listener to exit the blocking wait
        this.interrupt();

        // force close on the listener, exception is captured, logged, and 
        // ignored
        try {
            getListener().close();
        } catch (Exception ex){
            getService().logError(getClass().toString() + ", shutdown(), "
                    + getService().getServiceConfig().getServiceName() + ", "
                    + ex.getMessage());
        }
    }

    /**
     * run() method is the method which perform the work once the socket server
     * thread is started by the service. The method will continue to loop until
     * either the service is shutdown or the listener is shutdown.
     * <p>
     * The method keeps looping even through exception.
     *
     */
    @Override
    public void run() {
        // the listener will keep looping until the service or the listener is
        // stopped.  this loop has to be quick, so the connection thread is 
        // created to service the client and listener returns to listening.
        while (isActive()) {
            // any exception are captured logged, and ignored to allow the 
            // listener to keep running.
            try {
                // if the service is still running, continue to listen for the
                // next connection
                if (getService().isRunning()) {
                    // wait for the client to connect, when client connects, 
                    // the listener creates a socket for the new client and
                    // returns it.
                    Socket client = getListener().accept();

                    // using the client socket, if valid, create a new connection
                    // for the service
                    if (client != null) {
                        getService().addConnection(client, null);
                    } else {
                        // if we are getting null value for client, then the
                        // service was interrupted, exit
                        getService().logError(getClass().toString()
                                + ", run(), "
                                + getService().getServiceConfig().getServiceName()
                                + ", client returned as null");
                        break;
                    }
                } else {
                    // service is no longer running, exit
                    getService().logError(getClass().toString() + ", run(), "
                            + getService().getServiceConfig().getServiceName()
                            + ", service stopped");
                    break;
                }
            } catch (InterruptedException iexi){
                // this exception is ignored, because the listener wait state
                // can interrupted by many external factors (like O/S, VM...)
                // which is normal behaviour and does not require monitoring
            } catch (Exception ex){
                getService().logError(getClass().toString() + ", run(), "
                        + getService().getServiceConfig().getServiceName()
                        + ", " + ex.getMessage());
            }

            // this is good for normal processing, but during high volumne
            // connection requests we do not want to miss a client, so yielding 
            // is not used
            //Thread.yield();
        }

        // either the service or the listener had an issue, shutdown the 
        // listener
        shutdown();
    }
    // </editor-fold>
}
