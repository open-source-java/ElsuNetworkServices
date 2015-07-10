package elsu.network.services.support;

import elsu.common.FileStack;
import elsu.network.service.factory.ConfigLoader;
import elsu.network.services.*;
import elsu.network.service.factory.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;

/**
 * ControlServiceAbstract class provides capability for controlling of the
 * running services and allows dynamic instantiation capability of services
 * which are configured in manual mode.
 * <p>
 * Following commands can be executed through remote connection:
 *
 * add serviceName port; add config.xml; add serviceName newServiceAbstractName
 * port [key=value ....]; help; max intValue; password stringValue; quit; remove
 * port [...]; stop port [...]; start port [...]; status
 * <p>
 * All commands except informational require password authentication prior to
 * execution. config.xml file needs to reside on the server; file transfer
 * option can be used to upload config dynamically.
 *
 * 20141128 SSD updated for reflection generics warning on
 * getDeclaredConstructor
 *
 * @author Seraj Dhaliwal (seraj.s.dhaliwal@uscg.mil)
 */
public class ControlService extends ServiceAbstract implements IService {

    // <editor-fold desc="class private storage">
    // local storage for service shutdown string
    private volatile String _serviceShutdown = "#$#";

    // local storage for connection terminator string
    private volatile String _connectionTerminator = ".";

    // local storage for password string
    private volatile String _password = null;

    // local storage for local storage directory
    private volatile String _localStorage = null;
    // </editor-fold>

    // <editor-fold desc="class constructor destructor">
    /**
     * ControlServiceAbstract(...) constructor calls the super class constructor
     * to complete class initialization. Local properties are retrieved from
     * application and service configuration to allow fast access to connection
     * methods.
     *
     * @param factory
     * @param serviceConfig
     * @see ServiceFactory
     * @see ServiceAbstract
     * @see ServiceProperties
     * @see ServiceRuntimeProperties
     *
     */
    public ControlService(ServiceFactory factory, ServiceConfig serviceConfig) {
        // call the super class constructor
        super(factory, null, serviceConfig);

        // local config properties for local reference by class method
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
        this._password = getServiceConfig().getAttributes().get(
                "service.password").toString();
        this._localStorage = getFactory().getApplicationProperties().get(
                "localStore.directory").toString();
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
     * getPassword() method which returns the current password for the control
     * service which is required when connection needs to update application
     * wide or service specific parameters.
     *
     * @return <code>String</code> value of the password.
     */
    private synchronized String getPassword() {
        return this._password;
    }

    /**
     * getLocalStorage() method returns the current local directory which can be
     * used to process data.
     *
     * @return <code>String</code> value of the local store.
     */
    private synchronized String getLocalStorage() {
        return this._localStorage;
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

    // <editor-fold desc="command execution methods">
    /**
     * commandPassword(...) method takes the passed tokens, extracts the
     * password value provided, compares it to the stored value from config; if
     * matches, then returns true, else false;
     *
     * @param tokens
     * @param out
     * @return <code>boolean</code> value of the comparison
     */
    private synchronized boolean commandPassword(StringTokenizer tokens,
            PrintWriter out) {
        // declare local evaluation for return
        boolean result = false;

        // get the next argument for the command
        String p = tokens.nextToken();

        // does the info match password from the configuration
        if (p.equals(getPassword())) {
            // yes, it matches, notify client, and return true
            out.print(getStatusOk() + getRecordTerminator());
            out.flush();

            result = true;
        } else {
            // return failure notification
            out.print(getStatusUnAuthorized() + getRecordTerminator());
            out.flush();
        }

        // return result to the processor
        return result;
    }

    /**
     * commandAddXMLFile(...) method reads the XML configuration file specified
     * applies the attributes changes to the loaded service configuration. The
     * new service is then instantiated, activated, and monitored.
     *
     * @param configFilename
     * @param tokens
     * @param out
     * @throws Exception
     */
    private synchronized void commandAddXMLFile(String configFilename,
            PrintWriter out) throws Exception {
        // local list to track the services which were created through this
        // method
        ArrayList<IService> serviceList = new ArrayList<>();

        // load the config into memory using the configuration loader
        ConfigLoader configTemp = new ConfigLoader(configFilename);

        // load all configuration items loaded from the config file, do not
        // use iterator because the list is being modified while processing and
        // will result in invalid condition
        ArrayList<String> spIterator;
        spIterator = new ArrayList<>(configTemp.getServiceProperties().keySet());

        // for service config each item in the iterator, process the service
        for (Object spObject : spIterator) {
            // convert the object as config item
            ServiceConfig configObject;
            configObject = (ServiceConfig) configTemp.getServiceProperties().get(
                    spObject);

            // if the service startup is not configured as disabled, process it
            if (configObject.getStartupType() != ServiceStartupType.DISABLED) {
                // add the configObject to the core service properties
                getServiceProperties().put(configObject.getServiceName(),
                        configObject);

                // activate the service and add it to the local list
                serviceList.add(activateService(configObject, out));

                // send the configuration update to the client
                out.print(configObject.toString());
                out.flush();
            }

            // yield processing to other threads
            Thread.yield();
        }

        // scan all the services which were processed, if any are configured
        // delayed start, start the services
        for (IService service : serviceList) {
            // if the service is configured as delayed start, start the service
            if (service.getServiceConfig().getStartupType()
                    == ServiceStartupType.DELAYEDSTART) {
                // update the delayed start to false to allow child services to start
                service.getServiceConfig().setStartupType(
                        ServiceStartupType.AUTOMATIC);

                // start the service
                service.start();

                // set it back to delayed start
                service.getServiceConfig().setStartupType(
                        ServiceStartupType.DELAYEDSTART);
            }

            // yield processing to other threads
            Thread.yield();
        }

        // return status back to the client
        out.print(getStatusOk() + getRecordTerminator());
        out.flush();
    }

    /**
     * commandAddXMLString(...) method reads the XML configuration from string
     * passed and adds the changes to the loaded service configuration. The new
     * service is then instantiated, activated, and monitored.
     *
     * @param configFilename
     * @param tokens
     * @param out
     * @throws Exception
     *
     * 20141119 ssd added to support direct xml configuration
     */
    private synchronized void commandAddXMLString(String config,
            PrintWriter out) throws Exception {
        // get local storage and make sure it exists
        String lStore = getLocalStorage();
        new File(lStore).mkdirs();

        String fName = lStore + FileStack.tempFile(8, ".xml");

        // save the package data to xml file; igore XML in the front
        String data = config.substring(8);
        FileStack.writeFile(fName, data, true);

        // load the saved file
        commandAddXMLFile(fName, out);

        // delete the file
        new File(fName).delete();

        // return result
        out.print(getStatusOk() + getRecordTerminator());
        out.flush();
    }

    /**
     * commandAddConfig(...) method reads the tokens passed and configures
     * existing service configuration with new attributes. The new service is
     * then instantiated, activated, and monitored.
     *
     * @param serviceName
     * @param tokens
     * @param out
     * @throws Exception
     */
    private synchronized void commandAddConfig(String serviceName,
            StringTokenizer tokens,
            PrintWriter out) throws Exception {
        // local list to track the services which were created through this
        // method and copies the existing properties of the service configurations
        ArrayList<String> spIterator;
        spIterator = new ArrayList<>(getServiceProperties().keySet());

        // for service config each item in the iterator, process the service
        for (String spObject : spIterator) {
            // copy config properties to new object and add the object to the list
            ServiceConfig configObject
                    = ((ServiceConfig) getServiceProperties().get(spObject)).clone();

            // if the service startup is not configured as disabled, process it
            if (configObject.getStartupType() != ServiceStartupType.DISABLED) {
                // if the service name matches the config properties
                if (configObject.getServiceName().equals(serviceName)) {
                    // get the new name
                    String newName = tokens.nextToken();

                    // get the new port
                    int port = Integer.parseInt(tokens.nextToken());

                    // update the service config with the new properties
                    configObject.setServiceName(newName);
                    configObject.setConnectionPort(port);

                    // if there are more tokens, then update the new service
                    // attributes with new values
                    while (tokens.hasMoreTokens()) {
                        // extract the new attribute key/value pair
                        String[] newAttr = tokens.nextToken().split("=");

                        // if the key/value pair is valid
                        if (newAttr.length == 2) {
                            // 20141119 ssd added to allow child service
                            // publishers and subscribers to be configured
                            if (newAttr[0].contains(":")) {
                                // get the child type and port #
                                String[] keys = newAttr[0].split(":");

                                // check the child type and update the attributes
                                ServiceConfig childConfigObject;

                                switch (keys[0]) {
                                    case "sub": {
                                        int childPort = Integer.parseInt(keys[1]);
                                        childConfigObject
                                                = configObject.getSubscriber(
                                                        childPort);
                                        // remove the old value
                                        childConfigObject.getAttributes().remove(
                                                keys[2]);
                                        // store the new value
                                        childConfigObject.getAttributes().put(
                                                keys[2], newAttr[1]);
                                        break;
                                    }
                                    case "pub": {
                                        int childPort = Integer.parseInt(keys[1]);
                                        childConfigObject
                                                = configObject.getPublisher(
                                                        childPort);
                                        // remove the old value
                                        childConfigObject.getAttributes().remove(
                                                keys[2]);
                                        // store the new value
                                        childConfigObject.getAttributes().put(
                                                keys[2], newAttr[1]);
                                        break;
                                    }
                                    default:
                                        // notify client value ignored
                                        out.print(getStatusInvalidContent() + ", "
                                                + newAttr[0]
                                                + getRecordTerminator());
                                        out.flush();
                                        break;
                                }
                            } else {
                                // remove the old value
                                configObject.getAttributes().remove(newAttr[0]);

                                // store the new value
                                configObject.getAttributes().put(newAttr[0],
                                        newAttr[1]);
                            }
                        }

                        // yield processing to other threads
                        Thread.yield();
                    }

                    // send the configuration update to the client
                    out.print(configObject.toString());
                    out.flush();

                    // instantiate and activate the new service
                    IService service = activateService(configObject, out);

                    // if the service is configured as delayed start, start the service
                    if (service.getServiceConfig().getStartupType()
                            == ServiceStartupType.DELAYEDSTART) {
                        // update the delayed start to false to allow child services to start
                        service.getServiceConfig().setStartupType(
                                ServiceStartupType.AUTOMATIC);

                        // start the service
                        service.start();

                        // set it back to delayed start
                        service.getServiceConfig().setStartupType(
                                ServiceStartupType.DELAYEDSTART);
                    }

                    // since only one service can be processed, exit once done
                    break;
                }
            }

            // yield processing to other threads
            Thread.yield();
        }

        // return status back to the client
        out.print(getStatusOk() + getRecordTerminator());
        out.flush();
    }

    /**
     * commandRemove(...) method remove all services identified by the ports
     * passed from the client. The service state is changed to non-running which
     * also signals all connected clients to disconnect and service listener is
     * active is stopped.
     * <p>
     * The service configuration is also removed from memory and once removed
     * the service cannot be restarted - this is permanent. Only way to restart
     * the service is to reload the config by performing application start or
     * via secondary XML.
     *
     * @param tokens
     * @param out
     */
    public synchronized void commandRemove(StringTokenizer tokens,
            PrintWriter out) {
        // as long as there are tokens, process them
        while (tokens.hasMoreTokens()) {
            // convert the token to int
            int port = Integer.parseInt(tokens.nextToken());

            // remove port port port ...
            if (getFactory().removeService(port, true)) {
                // acknowledge
                out.print(getStatusOk() + ", " + port + getRecordTerminator());
                out.flush();
            } else {
                // acknowledge
                out.print(getStatusInvalidContent() + ", " + port
                        + getRecordTerminator());
                out.flush();
            }

            // yield processing to other threads
            Thread.yield();
        }

        // return status back to the client
        out.print(getStatusOk() + getRecordTerminator());
        out.flush();
    }

    /**
     * commandStop(...) method stops all services identified by the ports passed
     * from the client. The service state is changed to non-running which also
     * signals all connected clients to disconnect and service listener is
     * active is stopped.
     *
     * @param tokens
     * @param out
     */
    public synchronized void commandStop(StringTokenizer tokens, PrintWriter out) {
        // as long as there are tokens, process them
        while (tokens.hasMoreTokens()) {
            // convert the token to int
            int port;
            port = Integer.parseInt(tokens.nextToken());;

            // remove port port port ...
            if (getFactory().removeService(port, false)) {
                // acknowledge
                out.print(getStatusOk() + ", " + port + getRecordTerminator());
                out.flush();
            } else {
                // acknowledge
                out.print(getStatusInvalidContent() + ", " + port
                        + getRecordTerminator());
                out.flush();
            }

            // yield processing to other threads
            Thread.yield();
        }

        // return status back to the client
        out.print(getStatusOk() + getRecordTerminator());
        out.flush();
    }

    /**
     * commandStart(...) method starts all services identified by the ports
     * passed from the client. If service is configured to have listener, the
     * listener is enabled.
     *
     * @param tokens
     * @param out
     * @throws java.lang.Exception
     */
    public synchronized void commandStart(StringTokenizer tokens,
            PrintWriter out)
            throws Exception {
        // as long as there are tokens, process them
        while (tokens.hasMoreTokens()) {
            // convert the token to int
            int port;
            port = Integer.parseInt(tokens.nextToken());;

            // remove port port port ...
            if (getFactory().startService(port)) {
                // acknowledge
                out.print(getStatusOk() + ", " + port + getRecordTerminator());
                out.flush();
            } else {
                // acknowledge
                out.print(getStatusInvalidContent() + ", " + port
                        + getRecordTerminator());
                out.flush();
            }

            // yield processing to other threads
            Thread.yield();
        }

        // return status back to the client
        out.print(getStatusOk() + getRecordTerminator());
        out.flush();
    }

    /**
     * commandAttribute(...) method allows client to update application global
     * parameters; currently only max connection parameters can be updated.
     *
     * @param tokens
     * @param out
     */
    public synchronized void commandAttribute(StringTokenizer tokens,
            PrintWriter out) {
        // convert the token to int
        int max = Integer.parseInt(tokens.nextToken());

        // update the maximum connections limit
        setMaximumConnections(max);

        // return status back to the client
        out.print(getStatusOk() + getRecordTerminator());
        out.flush();
    }

    /**
     * commandStatus(...) method returns the factory status which includes all
     * running services and their configuration properties.
     *
     * @param tokens
     * @param out
     */
    public synchronized void commandStatus(StringTokenizer tokens,
            PrintWriter out) {
        // display status of the service
        getFactory().toString(out);

        // return status back to the client
        out.print(getStatusOk() + getRecordTerminator());
        out.flush();
    }

    /**
     * commandHelp(...) method returns to client the usage menu items which are
     * supported by the control service.
     *
     * @param tokens
     * @param out
     */
    public synchronized void commandHelp(StringTokenizer tokens, PrintWriter out) {
        // Display command syntax. Password not required
        out.print("COMMANDS:" + getRecordTerminator()
                + "\tadd <service> <port>" + getRecordTerminator()
                + "\tadd <config.xml>" + getRecordTerminator()
                + "\tadd <xml> <xml_package>" + getRecordTerminator()
                + "\tadd <service> <newService> <port> [<key=value> "
                + "<sub|pub:port:key=value> ...]"
                + getRecordTerminator()
                + "\thelp" + getRecordTerminator()
                + "\tmax <intValue>" + getRecordTerminator()
                + "\tpassword <stringValue>" + getRecordTerminator()
                + "\tquit" + getRecordTerminator()
                + "\tremove <port> [...]" + getRecordTerminator()
                + "\tstop <port> [...]" + getRecordTerminator()
                + "\tstart <port> [...]" + getRecordTerminator()
                + "\tstatus" + getRecordTerminator());
        out.flush();

        // return status back to the client
        out.print(getStatusOk() + getRecordTerminator());
        out.flush();
    }
    // </editor-fold>

    // <editor-fold desc="class methods">
    /**
     * activateServiceAbstract(...) method is used to instantiate a service
     * using passed config properties which includes the service port #.
     * <p>
     * If the service port # already exists, then the service is started and
     * instantiation is garbage collected.
     *
     * @param config
     * @param out
     * @return <code>IService</code> object created or null value
     * @throws Exception
     */
    public synchronized IService activateService(ServiceConfig config,
            PrintWriter out) throws Exception {
        out.print(".. activating service (" + config.getServiceName() + ")"
                + getRecordTerminator());
        out.flush();

        Class<?> serviceClass = Class.forName(config.getServiceClass());

        //service = (IService) serviceClass.newInstance();
        Class<?>[] argTypes = {ServiceFactory.class, String.class,
            ServiceConfig.class};

        Constructor<?> cons = serviceClass.getDeclaredConstructor(argTypes);

        Object[] arguments = {getFactory(), config.getServiceClass(), config};
        IService service = (IService) cons.newInstance(arguments);

        getFactory().addService(service, config.getConnectionPort());
        return service;
    }

    /**
     * serve(...) method is the main method of the service which processes the
     * client socket using the streams (in/out). The method is shared by all
     * client sockets.
     * <p>
     * This method is used by the client to send commands which are executed on
     * the server.
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

            // flag to monitor if the client authenticated
            boolean authorized = false;

            // 20141128 SSD local function to consolidation authorization check
            class LocalAuthorizationCheck {

                public boolean IsAuthorized(boolean value, PrintWriter out) {
                    boolean result = false;

                    if (!value) {
                        out.print(getStatusUnAuthorized()
                                + getRecordTerminator());
                        out.flush();
                    } else {
                        result = true;
                    }

                    return result;
                }
            }
            LocalAuthorizationCheck lValidation = new LocalAuthorizationCheck();

            // Control ServiceAbstract implements a singleton pattern, only one client
            // can be connected at a time and ignores any config settings.
            // If there is already a client connected to this service, display
            // a message to this client and close the connection. We use a
            // synchronized block to prevent a race condition.
            synchronized (this) {
                // check if there is already a connection
                if (getActiveConnections() > 1) {
                    // yes, there is already a connection, notify client and 
                    // exit
                    out.print("ONLY ONE CONTROL CONNECTION ALLOWED"
                            + getRecordTerminator());
                    out.flush();

                    out.close();
                    return;
                }
            }

            // optimized by IDE; changed from if statements to case with branch
            OUTER:
            for (;;) {
                String line = in.readLine();
                if ((line == null) || line.equals(getConnectionTerminator())) {
                    // Quit if we get EOF.
                    break;
                }
                try {
                    StringTokenizer tokens = new StringTokenizer(line);
                    if (!tokens.hasMoreTokens()) {
                        continue;
                    }

                    String command = tokens.nextToken().toLowerCase();

                    // based on command passed, execute the command using
                    // supplied arguments
                    switch (command) {
                        case "password":
                            authorized = commandPassword(tokens, out);
                            break;
                        case "add":
                            // validate the authorization
                            if (lValidation.IsAuthorized(authorized, out)) {
                                // extract the service name as first argument
                                String serviceName = tokens.nextToken();

                                // if this is a control service, then ignore it
                                if (serviceName.equals("controlService")) {
                                    out.print(getStatusInvalidContent()
                                            + getRecordTerminator());
                                    out.flush();
                                } else {
                                    // check if the command ends as an xml file
                                    if (line.endsWith(".xml")) {
                                        // load the XML for execution
                                        commandAddXMLFile(line.substring(4), out);
                                    } else if (serviceName.equals("xml")) {
                                        // 20141118 ssd add to pass XML package as
                                        // string to the config
                                        commandAddXMLString(line, out);
                                    } else {
                                        // reconfigure existing service with new
                                        // arguments (port, attributes...)
                                        commandAddConfig(serviceName, tokens, out);
                                    }
                                }
                            }
                            break;
                        case "remove":
                            // validate the authorization
                            if (lValidation.IsAuthorized(authorized, out)) {
                                // call the command method to execute
                                commandRemove(tokens, out);
                            }
                            break;
                        case "stop":
                            // validate the authorization
                            if (lValidation.IsAuthorized(authorized, out)) {
                                // call the command method to execute
                                commandStop(tokens, out);
                            }
                            break;
                        case "start":
                            // validate the authorization
                            if (lValidation.IsAuthorized(authorized, out)) {
                                // call the command method to execute
                                commandStart(tokens, out);
                            }
                            break;
                        case "max":
                            // validate the authorization
                            if (lValidation.IsAuthorized(authorized, out)) {
                                // call the command method to execute
                                commandAttribute(tokens, out);
                            }
                            break;
                        case "status":
                            // validate the authorization
                            if (lValidation.IsAuthorized(authorized, out)) {
                                // call the command method to execute
                                commandStatus(null, out);
                            }
                            break;
                        case "help":
                            commandHelp(null, out);
                            break;
                        case "quit":
                            break OUTER;
                        default:
                            // return error to the client
                            out.print(getStatusInvalidContent()
                                    + getRecordTerminator());
                            out.flush();
                            break;
                    }
                } catch (Exception ex) {
                    // exception is processing, try to return the info back to
                    // the client, but ignore if client has already closed the
                    // connection
                    try {
                        out.print(getClass().toString() + ", serve(), "
                                + getServiceConfig().getServiceName() + ", "
                                + ex.getMessage() + getRecordTerminator());
                        out.flush();
                    } catch (Exception exi) {
                    }

                    // log error for tracking
                    logError(getClass().toString() + ", serve(), "
                            + getServiceConfig().getServiceName() + ", "
                            + ex.getMessage());
                }
                Thread.yield();
            }
        } catch (Exception ex) {
            // log error for tracking
            logError(getServiceConfig().getServiceName() + ", serve(), "
                    + getServiceConfig().getConnectionPort() + ", "
                    + ex.getMessage());
        } finally {
            // close out all open in/out streams.
            try {
                out.flush();
            } catch (Exception exi) {
            }

            try {
                out.close();
            } catch (Exception exi) {
            }
            try {
                in.close();
            } catch (Exception exi) {
            }

            // Finally, when the loop command loop ends, close the streams
            // and set our connected flag to false so that other clients can
            // now connect.
            // -- updated, the active connections is cleared when connection 
            // -- is closed.
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
