package site.service;

import elsu.network.services.core.*;
import elsu.network.services.*;
import elsu.network.factory.*;
import elsu.database.*;
import elsu.database.DatabaseUtils.*;
import elsu.network.application.*;
import java.io.*;
import java.net.*;
import java.util.*;

/**
 * CommandForwarderServiceAbstract class provide delivery of notification
 * messages stored in the database to the equipment service for delivery to
 * equipment.
 * <p>
 * The class scans the database after expiration of idle time, checks if there
 * are any pending outgoing messages, if yes = connects to the equipment service
 * and delivers the message.
 * <p>
 * This service creates connections on demand to the equipment service.
 *
 * 20141128 SSD updated database calls to separate return variable
 * initialization to null
 *
 * @author Seraj Dhaliwal (seraj.s.dhaliwal@uscg.mil)
 */
public class CommandForwarderService extends AbstractService implements IService {

    // <editor-fold desc="class private storage">
    // storage for service shutdown string when received terminates the service
    private volatile String _serviceShutdown = "#$#";

    // storage for connection terminator when received closes the connection
    private volatile String _connectionTerminator = ".";

    // storage for database driver class
    private volatile String _dbDriver = null;

    // storage for database connection string
    private volatile String _dbConnectionString = null;

    // storage for database user
    private volatile String _dbUser = null;

    // storage for database password
    private volatile String _dbPassword = null;

    // storage for database manager which monitors the shared pool between
    // all connections
    private volatile DatabaseManager _dbManager = null;

    // storage for database pool limit, default is 5
    private volatile int _maxPool = 5;

    // storage for client port which will receive the commands sent from the
    // database
    private volatile int _clientPort = 5000;

    // storage for client timeout, this is used to pause when no data is 
    // available to allow other threads to continue
    private volatile int _idleTimeout = 5000;

    // storage to track if the monitor is running
    private volatile boolean _isMonitorRunning = false;
    // </editor-fold>

    // <editor-fold desc="class constructor destructor">
    /**
     * CommandForwarderServiceAbstract(...) constructor creates the object based
     * on the factory, thread group, and service config.
     *
     * @param factory
     * @param threadGroup
     * @param serviceConfig
     * @see ServiceFactory
     * @see ServiceAbstract
     * @see ServiceProperties
     * @see ServiceConnectionAbstract
     * @see ServiceConnectionBasic
     * @see Connection
     */
    public CommandForwarderService(String threadGroup, ServiceManager serviceManager,
            ServiceConfig serviceConfig) {
        // call the super class constructor
        super(threadGroup, serviceManager, serviceConfig);

        // local config properties for local reference by class method
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
        this._serviceShutdown = getProperty("service.shutdown").toString();
        this._connectionTerminator
                = getProperty("connection.terminator").toString();
        this._dbDriver = getServiceConfig().getAttribute(
                "service.database.driver").toString();
        this._dbConnectionString = getServiceConfig().getAttribute(
                "service.database.connectionString").toString();
        this._dbUser = getServiceConfig().getAttribute(
                "service.database.user").toString();
        this._dbPassword = getServiceConfig().getAttribute(
                "service.database.password").toString();

        try {
            this._maxPool = Integer.parseInt(
                    getServiceConfig().getAttribute(
                            "service.database.max.pool").toString());
        } catch (Exception ex) {
            logError(getClass().toString() + ", initializeLocalProperties(), "
                    + getServiceConfig().getServiceName() + " on port "
                    + getServiceConfig().getConnectionPort()
                    + ", invalid service.database.max.pool, " + ex.getMessage());
            this._maxPool = 5;
        }

        try {
            this._clientPort = Integer.parseInt(
                    getServiceConfig().getAttribute("service.client.port").toString());
        } catch (Exception ex) {
            logError(getClass().toString() + ", initializeLocalProperties(), "
                    + getServiceConfig().getServiceName() + " on port "
                    + getServiceConfig().getConnectionPort()
                    + ", invalid service.client.port property, "
                    + ex.getMessage());
            this._clientPort = 0;
        }

        try {
            this._idleTimeout = Integer.parseInt(
                    getServiceConfig().getAttribute(
                            "service.monitor.idleTimeout").toString());
        } catch (Exception ex) {
            logError(getClass().toString() + ", initializeLocalProperties(), "
                    + getServiceConfig().getServiceName() + " on port "
                    + getServiceConfig().getConnectionPort()
                    + ", invalid service.monitor.idleTimeout, "
                    + ex.getMessage());
            this._idleTimeout = 5000;
        }
    }
    // </editor-fold>

    // <editor-fold desc="class getter/setters">	
    /**
     * getClientPort() method returns the port of the equipment service which
     * will be used to connect to. The host name is retrieved from the database
     * with the pending command.
     *
     * @return <code>int</code> value of the port
     */
    private synchronized int getClientPort() {
        return this._clientPort;
    }

    /**
     * getConnectionTerminator() method returns the string value of the
     * connection terminator which when received closes the connection to the
     * equipment.
     *
     * @return <code>String</code> value of the connection terminator
     */
    private synchronized String getConnectionTerminator() {
        return this._connectionTerminator;
    }

    /**
     * getDbConnectionString() method returns the database connection string
     * used by the service to connect and monitor.
     *
     * @return <code>String</code> value of the connection string
     */
    private synchronized String getDbConnectionString() {
        return this._dbConnectionString;
    }

    /**
     * getDbDriver() method returns the database driver which will be used by
     * database manager to instantiate.
     *
     * @return <code>String<code> value of the string
     */
    private synchronized String getDbDriver() {
        return this._dbDriver;
    }

    /**
     * getDatabaseManager() method returns the database manager object used to
     * instantiate the database connections, pool, and execute commands through
     *
     * @return <code>DatabaseManager</code> object
     */
    private synchronized DatabaseManager getDBManager() {
        return this._dbManager;
    }

    /**
     * getDbPassword() method returns the password of the user.
     *
     * @return <code>String</code> value of the password
     */
    private synchronized String getDbPassword() {
        return this._dbPassword;
    }

    /**
     * getDbUser() method returns the user name for the database connection.
     *
     * @return <code>String</code> value of the user name
     */
    private synchronized String getDbUser() {
        return this._dbUser;
    }

    /**
     * getIdleTimeout() method returns the timeout value used to pause the
     * reader. It is also used to pause the loop when trying to connect to the
     * equipment and it is not responding.
     *
     * @return <code>int</code> value of the timeout
     */
    private synchronized int getIdleTimeout() {
        return _idleTimeout;
    }

    /**
     * getMaxPool() method returns the maximum connections which will be created
     * by the database manager upon initialization. The pool of connections
     * allows the service to provide fast response when servicing multiple
     * connections simultaneously.
     *
     * @return <code>int</code> value of the max pool
     */
    private synchronized int getMaxPool() {
        return this._maxPool;
    }

    /**
     * getServiceAbstractShutdown() method returns the shutdown value which is
     * matched with the incoming string. If the string matches, then the
     * connection is closed and service is set to shutdown.
     *
     * @return <code>String</code> value of the service shutdown
     */
    private synchronized String getServiceShutdown() {
        return this._serviceShutdown;
    }

    /**
     * isMonitorRunning() method returns true if the service is active.
     *
     * @return <code>boolean</code> value of the sender running state
     */
    private synchronized boolean isMonitorRunning() {
        return this._isMonitorRunning;
    }

    /**
     * isMonitorRunning(..) method allows the value of the service running state
     * to be changed and returns the current value.
     *
     * @return <code>boolean</code> value of the sender running state
     */
    private synchronized boolean isMonitorRunning(boolean running) {
        this._isMonitorRunning = running;
        return isMonitorRunning();
    }
    // </editor-fold>

    // <editor-fold desc="class methods">
    /**
     * clearPendingNotifier(...) method calls the stored procedure to clear any
     * pending messages for the site or all sites (if siteid is set to ZERO).
     * <p>
     * Executed on startup with siteId = 0 to clear all pending messages to
     * prevent old messages from being sent to the equipment
     * <p>
     * Executed on exception while sending message to site with siteId > 0 to
     * clear message and set them to ERROR.
     *
     * @param siteId
     */
    private void clearPendingNotifier(int siteId) {
        // capture any exceptions to prevent resource leaks
        java.sql.Connection dbConn = null;

        try {
            // allocate list to manage params
            ArrayList<DatabaseParameter> params;
            params = new ArrayList<>();

            // store the siteId parameter value
            params.add(new DatabaseParameter("siteid", java.sql.Types.BIGINT,
                    siteId));

            // using database manager, execute the procedure with parameters
            dbConn = getDBManager().getConnection();
            DatabaseUtils.executeProcedure(dbConn,
                    "{call pClearPendingNotifier(?)}",
                    params);
            getDBManager().releaseConnection(dbConn);
        } catch (Exception ex) {
            try {
            	getDBManager().releaseConnection(dbConn);
            } catch (Exception exr) {
                // log error for tracking
                logError(getClass().toString() + ", clearPendingNotifier(), "
                        + getServiceConfig().getServiceName() + " on port "
                        + getServiceConfig().getConnectionPort() + ", "
                        + "releaseConnection()/ "
                        + exr.getMessage());
            }

            // log error for tracking
            logError(getClass().toString() + ", clearPendingNotifier(), "
                    + getServiceConfig().getServiceName() + " on port "
                    + getServiceConfig().getConnectionPort() + ", "
                    + ex.getMessage());
        }
    }

    /**
     * getPendingNotifierCount(...) method returns the count of all pending
     * outgoing messages when siteId = 0 or count of messages for a site when
     * siteId > 0.
     * <p>
     * Used by the monitor loop to check if there are any pending messages and
     * spawn connections to sites to deliver the messages.
     *
     * @param siteId
     * @return <code>int</code> value of the siteId
     */
    private int getPendingNotifierCount(int siteId) {
        // storage for return value
        Map<String, Object> result = null;
        java.sql.Connection dbConn = null;

        // capture any exceptions to prevent resource leaks
        try {
            // allocate list to manage params
            ArrayList<DatabaseParameter> params;
            params = new ArrayList<>();

            // store the siteId parameter value
            params.add(new DatabaseParameter("siteid", java.sql.Types.BIGINT,
                    siteId));
            params.add(new DatabaseParameter("count", java.sql.Types.BIGINT,
                    true));

            // using database manager, execute the procedure with parameters
            dbConn = getDBManager().getConnection();
            result = DatabaseUtils.executeProcedure(
                    dbConn, "{call pGetPendingNotifierCnt(?,?)}", params);
            getDBManager().releaseConnection(dbConn);
        } catch (Exception ex) {
            try {
            	getDBManager().releaseConnection(dbConn);
            } catch (Exception exr) {
                // log error for tracking
                logError(getClass().toString() + ", getPendingNotifierCount(), "
                        + getServiceConfig().getServiceName() + " on port "
                        + getServiceConfig().getConnectionPort() + ", "
                        + "releaseConnection()/ "
                        + exr.getMessage());
            }

            // log error for tracking
            logError(getClass().toString() + ", getPendingNotifierCount(), "
                    + getServiceConfig().getServiceName() + " on port "
                    + getServiceConfig().getConnectionPort() + ", "
                    + ex.getMessage());
        }

        // return the value of the count field
        return Integer.parseInt(result.get("count").toString());
    }

    /**
     * updatePendingNotifier(...) method updates the status of the pending
     * message processed by the sender loop and if all messages have been
     * processed, clears the pending notifier for the site.
     *
     * @param siteId
     * @param messageId
     */
    private void updatePendingNotifier(int siteId, int messageId) {
        // capture any exceptions to prevent resource leaks
        java.sql.Connection dbConn = null;

        try {
            // allocate list to manage params
            ArrayList<DatabaseParameter> params;
            params = new ArrayList<>();

            // store the siteId parameter value
            params.add(new DatabaseParameter("siteid", java.sql.Types.BIGINT,
                    siteId));
            params.add(new DatabaseParameter("messageid", java.sql.Types.BIGINT,
                    messageId));

            // using database manager, execute the procedure with parameters
            dbConn = getDBManager().getConnection();
            DatabaseUtils.executeProcedure(
                    dbConn, "{call pUpdatePendingNotifier(?, ?)}", params);
            getDBManager().releaseConnection(dbConn);
        } catch (Exception ex) {
            try {
            	getDBManager().releaseConnection(dbConn);
            } catch (Exception exr) {
                // log error for tracking
                logError(getClass().toString() + ", updatePendingNotifier(), "
                        + getServiceConfig().getServiceName() + " on port "
                        + getServiceConfig().getConnectionPort() + ", "
                        + "releaseConnection()/ "
                        + exr.getMessage());
            }

            // log error for tracking
            logError(getClass().toString() + ", updatePendingNotifier(), "
                    + getServiceConfig().getServiceName() + " on port "
                    + getServiceConfig().getConnectionPort() + ", "
                    + ex.getMessage());
        }
    }

    /**
     * checkConnections() method tries to open a non socket connection which
     * will be used to monitor the database for pending messages to delivery.
     * <p>
     * Creation of non socket connection does not block, therefore, does not
     * require separate thread to process
     */
    @Override
    public synchronized void checkConnections() {
        // check is service is running
        if (isRunning()) {
            // capture any exceptions to prevent resource leaks
            try {
                // if monitor loop is not running, then create the non socket
                // connection to monitor database for outbound messages
                if (!isMonitorRunning()) {
                    // create the service connection non socket
                    Connection dsConn = new Connection(null, this);

                    // add the connection to the service list
                    addConnection(null, dsConn);

                    // update the connection indicator
                    isMonitorRunning(true);
                }
            } catch (Exception ex) {
                // log error for tracking
                logError(getClass().toString() + ", checkConnections(), "
                        + getServiceConfig().getServiceName()
                        + ", error creating connection "
                        + ", to database change notification server"
                        + ", " + ex.getMessage());
            }
        }
    }

    /**
     * server(...) method provides support for processing custom connections.
     * ServiceAbstract manages two types of actions: monitor and sender.
     * <p>
     * Monitor action is performed by non socket connection. Here the connection
     * continues to loop until the service terminates and keeps monitoring the
     * database for outbound messages.
     *
     * If there are pending outbound messages, the message delivery information
     * is retrieved and socket connection is established for delivery.
     * <p>
     * Sender action is performed by socket connection. Here the connection
     * scans the siteId and ipAddress, reads all pending messages, sends the
     * messages to the site, updates the message to complete or error.
     *
     * @param conn
     * @throws Exception
     */
    @Override
    public void serve(AbstractConnection conn) throws Exception {
        // unbox the connection to custom service connection
        Connection cConn = (Connection) conn;
        java.sql.Connection dbConn = null;

        // this is a non socket connection, monitor action will be performed
        if (cConn.getClient() == null) {
            // capture any exceptions to prevent resource leaks
            try {
                // loop as long as the service is running
                while (isRunning()) {
                    // are there any pending messages to send, if none, then
                    // yield processing to other thread and continue
                    if (getPendingNotifierCount(0) == 0) {
                        Thread.sleep(getIdleTimeout());
                        continue;
                    }

                    // local variables for processing of messages
                    int siteId = 0;
                    String siteIp;

                    // allocate list to manage params
                    ArrayList<DatabaseParameter> params;
                    params = new ArrayList<>();

                    // store the siteId parameter value
                    params.add(new DatabaseParameter("siteid",
                            java.sql.Types.BIGINT, true));
                    params.add(new DatabaseParameter("siteip",
                            java.sql.Types.VARCHAR, true));

                    // using database manager, execute the procedure with parameters
                    Map<String, Object> result = null;
                    dbConn = getDBManager().getConnection();
                    result = DatabaseUtils.executeProcedure(
                            dbConn, "{call pGetPendingNotifier(?,?)}", params);
                    getDBManager().releaseConnection(dbConn);

                    // if there is data available, result contains key/value pairs
                    if ((result != null) && (!result.isEmpty())) {
                        // capture any exceptions to prevent resource leaks
                        try {
                            // extract the siteId from the results
                            siteId = Integer.parseInt(
                                    result.get("siteid").toString());

                            // extract the siteIp from the results
                            siteIp = result.get("siteip").toString();

                            // if siteId is valid, then create socket to send
                            // the messages to the site equipment
                            if (siteId != 0) {
                                // create connection to the host address / port
                                Socket client = new Socket(siteIp,
                                        getClientPort());

                                // assign the socket to the service connection
                                Connection dsConn
                                        = new Connection(client, this);

                                // store the connection properties that will be
                                // used for retrieving and connection properties
                                dsConn.addProperty("siteid", siteId);
                                dsConn.addProperty("siteip", siteIp);

                                // add the connection to the service list
                                addConnection(client, dsConn);
                            }
                        } catch (Exception ex) {
                            // if siteId is not zero, clear the pending messages
                            // due to error, old messages are set to ignore
                            if (siteId != 0) {
                                clearPendingNotifier(siteId);
                            }

                            // log error for tracking
                            logError(getClass().toString() + ", serve(), "
                                    + getServiceConfig().getServiceName()
                                    + ", error creating connection "
                                    + ", to database change notification "
                                    + ", " + ex.getMessage());
                        }
                    } else {
                        // the database is not responding correctly, exit
                        throw new Exception("invalid site configuration!!");
                    }

                    // yield processing to other threads
                    Thread.yield();
                }
            } catch (Exception ex) {
                getDBManager().releaseConnection(dbConn);

                // log error for tracking
                logError(getClass().toString() + ", serve(), "
                        + getServiceConfig().getServiceName() + ", "
                        + ex.getMessage());
            } finally {
                // update monitoring indicator to false
                isMonitorRunning(false);

                // update connection state to false
                cConn.isActive(false);

                // if the service is still running, check connections to restart
                if (isRunning()) {
                    checkConnections();
                }
            }
        } else {
            // local parameter for reader thread access, passes the socket in 
            // stream
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    cConn.getClient().getInputStream()));

            // local parameter for reader thread access, passes the socket out 
            // stream
            PrintWriter out = new PrintWriter(new BufferedWriter(
                    new OutputStreamWriter(cConn.getClient().getOutputStream())));

            // extract the siteId from the parameters stored from monitor
            int siteId = Integer.parseInt(
                    cConn.getProperty("siteid").toString());

            // capture any exceptions to prevent resource leaks
            try {
                // loop as long as the service is running and the connection is
                // active
                while (isRunning() && cConn.isActive()) {
                    // allocate list to manage params
                    ArrayList<DatabaseParameter> params;
                    params = new ArrayList<>();

                    // store the siteId parameter value
                    params.add(new DatabaseParameter("siteid",
                            java.sql.Types.BIGINT, siteId));
                    params.add(new DatabaseParameter("messageid",
                            java.sql.Types.BIGINT, true));
                    params.add(new DatabaseParameter("equipmentid",
                            java.sql.Types.BIGINT, true));
                    params.add(new DatabaseParameter("message",
                            java.sql.Types.VARCHAR, true));

                    // using database manager, execute the procedure with parameters
                    Map<String, Object> result = null;
                    dbConn = getDBManager().getConnection();
                    result = DatabaseUtils.executeProcedure(
                            dbConn, "{call pRetrievePendingNotifier(?,?,?,?)}",
                            params);
                    getDBManager().releaseConnection(dbConn);

                    // if the result from the stored procedure is valid
                    if ((result != null) && (!result.isEmpty())) {
                        // check if the messageid is valid, if not exit the loop
                        if (result.get("messageid") == null) {
                            break;
                        }

                        // capture any exceptions to prevent resource leaks
                        try {
                            // log info for tracking
                            logDebug("DB -> CS, "
                                    + getServiceConfig().getConnectionPort());

                            // extract the message from the params
                            int messageId = Integer.parseInt(result.get(
                                    "messageid").toString());
                            int equipmentId = Integer.parseInt(result.get(
                                    "equipmentid").toString());
                            String message = result.get("message").toString();

                            // log info for tracking
                            logDebug("DB -> CS, MSG, " + equipmentId + ", "
                                    + messageId + ", " + message);

                            // format the message object using the results from
                            // the query
                            SiteMessage bMessage = new SiteMessage(siteId,
                                    equipmentId, message);

                            // send the message to the client service
                            out.write(
                                    bMessage.getBcsMessage(getDatetimeFormat(),
                                            getFieldDelimiter(),
                                            getRecordTerminator()));
                            out.flush();

                            // read the input from the client service
                            String line = in.readLine();

                            // clear the site messages which have been sent
                            updatePendingNotifier(0, messageId);
                        } catch (Exception ex) {
                            // throw exception if there is error with the data
                            throw new Exception("invalid data/message retrieval");
                        }
                    } else {
                        // no pending messages for site
                        break;
                    }

                    // yield processing to other threads
                    Thread.yield();
                }
            } catch (Exception ex) {
                getDBManager().releaseConnection(dbConn);

                // clear the pending messages due to error, old messages are 
                // set to ignore
                clearPendingNotifier(siteId);

                // log error for tracking
                logError(getClass().toString() + ", serve(), "
                        + getServiceConfig().getServiceName() + ", "
                        + ex.getMessage());
            } finally {
                // update connection state to false
                cConn.isActive(false);

                // close the in/out streams
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

            // clear all messages for the site, if there was error in 
            // processing, then the pending messages are set to error
            updatePendingNotifier(siteId, 0);
        }
    }

    /**
     * start() method overloaded from the super class is used to instantiate
     * child services.
     *
     * @throws java.lang.Exception
     */
    @Override
    public synchronized void start() throws Exception {
        // call the super method to perform initialization
        super.start();

        // capture any exceptions to prevent resource leaks
        try {
            // reload the local properties
            initializeLocalProperties();

            // create the database manager
            this._dbManager = new DatabaseManager(
                    getDbDriver(),
                    getDbConnectionString(), getMaxPool(),
                    getDbUser(),
                    getDbPassword());
        } catch (Exception ex) {
            // log error for tracking
            logError(getClass().toString() + ", start, "
                    + getServiceConfig().getServiceName() + " on port "
                    + getServiceConfig().getConnectionPort() + ", "
                    + ex.getMessage());

            // shutdown the service and exit
            shutdown();
            return;
        }

        // clear all old notifications for all sites
        clearPendingNotifier(0);

        // start the monitoring connection
        checkConnections();
    }
    // </editor-fold>
}
