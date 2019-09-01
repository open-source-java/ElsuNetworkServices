package site.service;

import elsu.network.services.core.*;
import elsu.network.services.*;
import elsu.network.factory.*;
import elsu.database.*;
import elsu.database.DatabaseUtils.*;
import elsu.common.*;
import elsu.network.application.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;

/**
 * MessageStorageServiceAbstract class is used to listen to a port and allow all
 * publisher services to deliver message to store in the database. The service
 * is designed to receive message, parse it for verification, send it to
 * database for storage.
 * <p>
 * The service uses independent database manager with connection pool to ensure
 * high through-put for multiple connections.
 *
 * 20141128 SSD updated database calls to separate return variable
 * initialization to null
 *
 * @author Seraj Dhaliwal (seraj.s.dhaliwal@uscg.mil)
 */
public class MessageStorageService extends AbstractService implements IService {

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

    // storage for messageType used to identify if the service is processing
    // alarm or normal messages
    private volatile MessageStorageType _messageStorageType
            = MessageStorageType.MESSAGE;

    // storage for messageMode used to identify if the service is processing
    // live or recovery messages
    private volatile MessageStorageProcessingType _messageStorageMode
            = MessageStorageProcessingType.RECOVERY;
    // </editor-fold>

    // <editor-fold desc="class constructor destructor">
    /**
     * MessageStorageServiceAbstract(...) constructor creates the object based
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
     * @see ServiceConnectionCustom
     */
    public MessageStorageService(String threadGroup, ServiceManager serviceManager,
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
        this._dbDriver = getServiceConfig().getAttributes().get(
                "service.database.driver").toString();
        this._dbConnectionString = getServiceConfig().getAttributes().get(
                "service.database.connectionString").toString();
        this._dbUser = getServiceConfig().getAttributes().get(
                "service.database.user").toString();
        this._dbPassword = getServiceConfig().getAttributes().get(
                "service.database.password").toString();

        try {
            this._maxPool = Integer.parseInt(
                    getServiceConfig().getAttributes().get(
                            "service.database.max.pool").toString());
        } catch (Exception ex) {
            logError(getClass().toString() + ", initializeLocalProperties(), "
                    + getServiceConfig().getServiceName() + " on port "
                    + getServiceConfig().getConnectionPort()
                    + ", invalid service.database.max.pool, " + ex.getMessage());
            this._maxPool = 5;
        }

        String messageStorageType = getServiceConfig().getAttributes().get(
                "service.message.storage.type").toString();
        if (messageStorageType.equals(MessageStorageType.ALARM.name())) {
            this._messageStorageType = MessageStorageType.ALARM;
        } else {
            this._messageStorageType = MessageStorageType.MESSAGE;
        }

        String messageStorageMode = getServiceConfig().getAttributes().get(
                "service.message.mode").toString();
        if (messageStorageMode.equals(MessageStorageProcessingType.LIVE.name())) {
            this._messageStorageMode = MessageStorageProcessingType.LIVE;
        } else {
            this._messageStorageMode = MessageStorageProcessingType.RECOVERY;
        }
    }
    // </editor-fold>

    // <editor-fold desc="class getter/setters">	
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
    private synchronized int getMaxPool() {
        return this._maxPool;
    }

    /**
     * getMessageStorageType() method returns if the service will process
     * message alarm type in real-time. If true, the service will store the
     * alarm messages as active alarms.
     *
     * @return <code>MessageStorageType</code> value of the message type
     */
    private synchronized MessageStorageType getMessageStorageType() {
        return this._messageStorageType;
    }

    /**
     * getMessageStorageProcessingType() method returns if the service will
     * process LIVE (real-time) messages or RECOVERY (data-logger) messages. If
     * LIVE, the service will store the alarm messages as active alarms.
     *
     * @return <code>MessageStorageProcessingType</code> value of the message
     * type
     */
    private synchronized MessageStorageProcessingType getMessageStorageMode() {
        return this._messageStorageMode;
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
    // </editor-fold>

    // <editor-fold desc="class methods">
    /**
     * serve(...) method processes all incomming client socket connections using
     * their in/out streams. The processing involves: receiving the message,
     * parsing, validation, and storage.
     * <p>
     * If the message type alarm is set to true, then alarm messages are treated
     * as active alarms
     *
     * 20141128 SSD added critical error detection when database is
     * non-responsive to ensure no data is lost
     *
     * @param conn
     * @throws Exception
     */
    @Override
    public void serve(AbstractConnection conn) throws Exception {
        // critical error tracking; if detected then the function is aborted
        boolean criticalError = false;

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
        java.sql.Connection dbConn = null;

        // capture any exceptions to prevent resource leaks
        try {
            // loop as long as the service is running or the connection is
            // closed from the sender
            while (isRunning()) {
                // read a line from socket in stream and store it
                String line = in.readLine();

                // if the input is null or matches terminator, then exit the 
                // loop
                if ((line == null) || line.equals(getConnectionTerminator())) {
                    break;
                }

                // increase total messages received
                increaseTotalMessagesReceived();

                // capture any exceptions to prevent resource leaks
                try {
                    // log info for tracking
                    logDebug("MSG -> DB, "
                            + getServiceConfig().getConnectionPort() + ", "
                            + line);

                    // split received data and store it in array
                    String[] lineData = line.split(Pattern.quote(
                            getFieldDelimiter()));

                    // if array count is greater than minimum continue
                    if (lineData.length >= 5) {
                        // extract date from the received string and format it
                        // for sql datetime conversion
                        String tDate = lineData[1].substring(0, 4) + "/"
                                + lineData[1].substring(4, 6) + "/"
                                + lineData[1].substring(6, 8) + " "
                                + lineData[1].substring(8, 10) + ":"
                                + lineData[1].substring(10, 12) + ":"
                                + lineData[1].substring(12, 14)
                                + "." + lineData[1].substring(14);

                        // allocate list to manage params
                        ArrayList<DatabaseParameter> params;
                        params = new ArrayList<>();

                        // store the siteId parameter value
                        params.add(new DatabaseParameter("siteid",
                                java.sql.Types.BIGINT, Integer.parseInt(
                                        lineData[0])));
                        params.add(new DatabaseParameter("equipid",
                                java.sql.Types.BIGINT, Integer.parseInt(
                                        lineData[2])));
                        params.add(new DatabaseParameter("dtg",
                                java.sql.Types.TIMESTAMP,
                                DateUtils.convertDate2SQLTimestamp(tDate,
                                        "yyyy/MM/dd HH:mm:ss.S")));
                        params.add(new DatabaseParameter("msgtext",
                                java.sql.Types.VARCHAR, lineData[4]));
                        params.add(new DatabaseParameter("outbound",
                                java.sql.Types.VARCHAR, "N"));
                        if (getMessageStorageType() == MessageStorageType.ALARM) {
                            params.add(new DatabaseParameter("alarm",
                                    java.sql.Types.VARCHAR, "Y"));
                        } else {
                            params.add(new DatabaseParameter("alarm",
                                    java.sql.Types.VARCHAR, "N"));
                        }
                        if (getMessageStorageMode()
                                == MessageStorageProcessingType.RECOVERY) {
                            params.add(new DatabaseParameter("recovery",
                                    java.sql.Types.VARCHAR, "Y"));
                        } else {
                            params.add(new DatabaseParameter("recovery",
                                    java.sql.Types.VARCHAR, "N"));
                        }
                        params.add(new DatabaseParameter("id",
                                java.sql.Types.BIGINT, true));
                        params.add(new DatabaseParameter("status",
                                java.sql.Types.VARCHAR, true));

                        // using database manager, execute the procedure with parameters
                        Map<String, Object> result = null;

                        try {
                            dbConn = getDBManager().getConnection();
                            result = DatabaseUtils.executeProcedure(
                                    dbConn,
                                    "{call ncs3.pMessageStore(?,?,?,?,?,?,?,?,?)}",
                                    params);
                            getDBManager().releaseConnection(dbConn);
                        } catch (Exception ex) {
                            getDBManager().releaseConnection(dbConn);

                            // this is critical, if cannot save, then notify error to
                            // client so it can implement some recovery
                            criticalError = true;
                            logError(getClass().toString() + ", serve(), "
                                    + getServiceConfig().getServiceName() + " on port "
                                    + getServiceConfig().getConnectionPort() + ", "
                                    + ex.getMessage());

                            out.print("system abort," + getStatusDatabaseError()
                                    + getRecordTerminator());
                            out.flush();

                            throw new Exception(ex);
                        }

                        // return the status to the sender
                        if ((result == null) || (result.isEmpty())) {
                            out.print("system abort," + getStatusSystemError()
                                    + getRecordTerminator());
                            out.flush();
                        } else {
                            Integer record = Integer.parseInt(
                                    result.get("id").toString());

                            if (record > 0) {
                                out.print(result.toString() + ","
                                        + getStatusOk() + getRecordTerminator());
                                out.flush();
                            } else {
                                out.print(result.toString() + ","
                                        + getStatusDatabaseError()
                                        + getRecordTerminator());
                                out.flush();
                            }
                        }
                    } else {
                        // return error status to sender
                        out.print(getStatusInvalidContent()
                                + getRecordTerminator());
                        out.flush();
                    }

                    // increase total messages sent
                    increaseTotalMessagesSent();
                } catch (Exception ex) {
                    // increase total messages errored
                    increaseTotalMessagesErrored();

                    // try to return error status to sender, capture any 
                    // exceptions ignore them since the socket may have been 
                    // closed by the sender
                    if (!criticalError) {
                        try {
                            out.print(getStatusInvalidContent()
                                    + getRecordTerminator());
                            out.flush();
                        } catch (Exception exi) {
                        }
                    }

                    // log error for tracking
                    logError(getClass().toString() + ", "
                            + getServiceConfig().getServiceName() + " on port "
                            + getServiceConfig().getConnectionPort() + ", "
                            + ex.getMessage());

                    // if critical is set, then we throw exception and exit
                    if (criticalError) {
                        throw new Exception(ex);
                    }
                }

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
            // flush the outbound stream and ignore any exception
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
        }
    }
    // </editor-fold>
}
