package site.service;

import elsu.network.services.core.*;
import elsu.network.services.*;
import elsu.common.*;
import elsu.io.*;
import elsu.network.application.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import site.core.*;

/**
 * BcsMessagePublisherServiceAbstract class is implemented as a child service to
 * support delivery of data collected by the subscriber service and to the
 * storage service. There are two different types of publishers: ALARM and
 * MESSAGE. ALARM publisher processes ALM files and MESSAGE publisher publishes
 * MSG files. For each type of publisher there are two processing types: LIVE or
 * RECOVERY. LIVE publisher processes files which are current (match the current
 * datehour) and RECOVERY publisher processes files which are older than the
 * current datehour not exceeding the recovery retention time.
 * <p>
 * This service creates only one connection and will recreate the connection if
 * there are exceptions or the equipment disconnects.
 *
 * @author Seraj Dhaliwal (seraj.s.dhaliwal@uscg.mil)
 * @see SiteMessageService
 * @see ServiceAbstract
 * @see ServiceProperties
 * @see ServiceRuntimeProperties
 */
public class SiteMessagePublisherService extends AbstractService implements
        IService {

    // <editor-fold desc="class private storage">
    // stores the shutdown string for connection, when received, the service
    // is shutdown and will not restart
    private volatile String _serviceShutdown = "#$#";
    // stores the connection terminator; when received, the connection is closed
    // if the service is not shutdown, the connection may be restarted
    private volatile String _connectionTerminator = ".";
    // stores the file mask - allows the files to include date or other 
    // service variables
    private volatile String _fileMask = null;
    // service specific data, status to track if the publisher service is 
    // running or has been shutdown
    private volatile boolean _isPublisherRunning = false;
    // service specific data, stores the last successful connection uri
    private volatile String _connectedHostUri = null;
    // service specific data, stores the file type being processed (ALM or MSG)
    // used for file mask based on the PublisherType
    private volatile String _fileTypeMap = "ALM";
    // service specific data, stores # of hosts the publisher can connect to
    // when one is not available, app.config (service.connection.hostUri.count)
    private volatile int _hostCount = 0;
    // service specific data, stores the list of all hosts the publisher can
    // deliver to, app.confg (service.connection.hostUri.?)
    private volatile List<String> _hostUriList = new ArrayList<>();
    // service specific data, stores the publisher type for the service and 
    // connection to use when processing the data.
    private volatile PublisherType _publisherType = PublisherType.ALARM;
    // service specific data, stores the idle timeout used when connection to
    // a host is not available
    private volatile int _idleTimeout = 5000;
    // service specific data, status to track if te connection maintained by the
    // publisher service is still running
    private volatile boolean _isConnectionsCreatorActive = false;
    // service specific data, stores the type of processing the publisher 
    // connection will perform (LIVE or RECOVERY)
    private volatile PublisherProcessingType _publisherProcessingType
            = PublisherProcessingType.LIVE;
    // service specific data, stores the recovery period
    private volatile FileRolloverPeriodicityType _recoveryPeriodicity
            = FileRolloverPeriodicityType.DAY;
    // service specific data, stores the recovery days beyond which the data 
    // files are removed, app.config (service.recovery.rolloverThreshold)
    private volatile int _recoveryThreshold = 7;
    // service specific data, stores the file reader channel currently used
    private volatile FileChannelTextReader _messageReader = null;
    // </editor-fold>

    // <editor-fold desc="class constructor destructor">
    public SiteMessagePublisherService(String threadGroup,
            ServiceManager serviceManager, IService parentService, 
            ServiceConfig childConfig) {
        // call the super class constructor
        super(threadGroup, serviceManager, 
                ((SiteMessageService) parentService).getServiceConfig());

        // set the parent service property, used to reference local storage and
        // other shared properties
        setParentService(parentService);

        // set the service config properties for this child service, these are
        // local to each child service
        setChildConfig(childConfig);

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

        if (getChildConfig().getAttribute("service.publisher.type").toString().equals(
                "ALARM")) {
            this._publisherType = PublisherType.ALARM;
        } else {
            this._publisherType = PublisherType.MESSAGE;
        }

        if (getChildConfig().getAttribute("service.processing.mode").toString().equals(
                "LIVE")) {
            this._publisherProcessingType = PublisherProcessingType.LIVE;
        } else {
            this._publisherProcessingType = PublisherProcessingType.RECOVERY;
        }

        try {
            this._hostCount = Integer.parseInt(
                    getChildConfig().getAttribute(
                            "service.connection.hostUri.count").toString());
        } catch (Exception ex) {
            logError(getClass().toString() + ", initializeLocalProperties(), "
                    + getServiceConfig().getServiceName() + " on port "
                    + getServiceConfig().getConnectionPort()
                    + ", invalid service.connection.hostUri.count, "
                    + ex.getMessage());
            this._hostCount = 0;
        }

        String hostUri;

        for (int i = 1; i <= getHostCount(); i++) {
            hostUri = getChildConfig().getAttribute(
                    "service.connection.hostUri." + i).toString();
            if (!hostUri.isEmpty()) {
                getHostUriList().add(hostUri);
            }

            Thread.yield();
        }

        try {
            this._idleTimeout = Integer.parseInt(
                    getChildConfig().getAttribute(
                            "service.monitor.idleTimeout").toString());
        } catch (Exception ex) {
            logError(getClass().toString() + ", initializeLocalProperties(), "
                    + getServiceConfig().getServiceName() + " on port "
                    + getServiceConfig().getConnectionPort()
                    + ", invalid service.monitor.idleTimeout, "
                    + ex.getMessage());
            this._idleTimeout = 5000;
        }

        switch (getProperty("data.recovery.periodicity").toString()) {
            case "DAY":
                this._recoveryPeriodicity = FileRolloverPeriodicityType.DAY;
                break;
            case "HOUR":
                this._recoveryPeriodicity = FileRolloverPeriodicityType.HOUR;
                break;
        }

        try {
            this._recoveryThreshold = Integer.parseInt(
                    getProperty("data.recovery.rolloverThreshold").toString());
        } catch (Exception ex) {
            logError(getClass().toString() + ", initializeLocalProperties(), "
                    + getServiceConfig().getServiceName() + " on port "
                    + getServiceConfig().getConnectionPort()
                    + ", invalid data.recovery.rolloverThreshold, "
                    + ex.getMessage());
            this._recoveryThreshold = 7;
        }
    }
    // </editor-fold>

    // <editor-fold desc="class getter/setters">
    /**
     * isConnectionsCreatorActive() method is used to track if the
     * checkConnections method is being processed. checkConnections method uses
     * a thread to create the connection to the equipment - if the thread is
     * running trying to connect to the equipment, then all other requests are
     * ignored.
     *
     * @return <code>boolean</code> value of the status of connections
     */
    private synchronized boolean isConnectionsCreatorActive() {
        return this._isConnectionsCreatorActive;
    }

    /**
     * isConnectionsCreatorActive(...) method is used to track if the
     * checkConnections method is being processed. checkConnections method uses
     * a thread to create the connection to the equipment - if the thread is
     * running trying to connect to the equipment, then all other requests are
     * ignored.
     *
     * @param value
     * @return <code>boolean</code> value of the status of connections
     */
    private synchronized boolean isConnectionsCreatorActive(boolean active) {
        this._isConnectionsCreatorActive = active;
        return isConnectionsCreatorActive();
    }

    /**
     * getConnectedHostUri() method returns the uri which the last connection
     * was successfully able to connect. This is used when new connection is
     * required to allow service to connect to the last used uri vice scanning
     * all available uri's.
     *
     * @return <code>String</code> value of the host uri.
     */
    public synchronized String getConnectedHostUri() {
        return this._connectedHostUri;
    }

    /**
     * setConnectedHostUri(...) method allows the last connected uri to be
     * stored for use.
     *
     * @param uri
     */
    public synchronized void setConnectedHostUri(String uri) {
        this._connectedHostUri = uri;
    }

    /**
     * getConnectionTerminator() method returns the string value of the
     * connection terminator which when received closes the connection to the
     * equipment.
     *
     * @return <code>String</code> value of the connection terminator
     */
    public synchronized String getConnectionTerminator() {
        return this._connectionTerminator;
    }

    /**
     * getMessageReader() method returns the reader channel for the file which
     * will be read and data sent to storage service. Data position is tracked
     * using fileStatus channel.
     *
     * @return <code>SeekableByteChannel</code>
     */
    private synchronized FileChannelTextReader getMessageReader() {
        return this._messageReader;
    }

    /**
     * getFileMask() method returns the string value of the file mask used to
     * create writer files and read incoming files from the parent service
     *
     * @return <code>String</code> value of the file mask
     */
    public synchronized String getFileMask() {
        return this._fileMask;
    }

    /**
     * setFileMask(...) method is used to set the file mask value after local
     * information like equipment id is updated to reduce the parsing when the
     * filename is formatted.
     *
     * @param value
     */
    private synchronized void setFileMask(String mask) {
        this._fileMask = mask;
    }

    /**
     * getFileTypeMap() method returns the file MAP property (ALM or MSG) used
     * in filename creation.
     *
     * @return <code>String</code> value of the file map
     */
    public synchronized String getFileTypeMap() {
        return this._fileTypeMap;
    }

    /**
     * setFileTypeMap() method sets the file MAP property based on the publisher
     * type (ALARM or MESSAGE).
     */
    private synchronized void setFileTypeMap() {
        setFileTypeMap(getPublisherType().name().toString());
    }

    /**
     * setFileTypeMap(...) method sets the file MAP property based on the string
     * variable passed (ALARM = ALM else MSG).
     *
     * @param typeMap
     */
    public synchronized void setFileTypeMap(String typeMap) {
        this._fileTypeMap = (typeMap.equals("ALARM") ? "ALM" : "MSG");
    }

    /**
     * getHostCount() method returns the count of host uri's defined in the
     * app.config which need to loaded and processed.
     *
     * @return <code>int</code> value of the host count
     */
    public synchronized int getHostCount() {
        return _hostCount;
    }

    /**
     * getHostUriList() method returns the uri array loaded from app.config. The
     * list is used by the checkConnections method to loop and check to one of
     * the available uri's.
     *
     * @return <code>List<String></code> of the host uri's.
     */
    public synchronized List<String> getHostUriList() {
        return this._hostUriList;
    }

    /**
     * getIdleTimeout() method returns the timeout value used to pause the
     * reader. It is also used to pause the loop when trying to connect to the
     * equipment and it is not responding.
     *
     * @return <code>int</code> value of the timeout
     */
    public synchronized int getIdleTimeout() {
        return _idleTimeout;
    }

    /**
     * getParentServiceAbstract() method returns the parent service object which
     * owns this child service.
     *
     * @return <code>BcsMessageServiceAbstract</code> object representing the
     * parent service
     */
    @Override
    public synchronized SiteMessageService getParentService() {
        return (SiteMessageService) super.getParentService();
    }

    /**
     * getPublisherType() method is used to return ALARM or MESSAGE. The setting
     * used to build file mask and processing of files.
     *
     * @return <code>PublisherType</code> value of this service.
     */
    public synchronized PublisherType getPublisherType() {
        return this._publisherType;
    }

    /**
     * getPublisherProcessingType() method is used to return LIVE or RECOVERY.
     * The setting is used to either process current active file or perform
     * recovery of pending files.
     *
     * @return <code>PublisherProcessingType</code> value of the service.
     */
    public synchronized PublisherProcessingType getPublisherProcessingType() {
        return this._publisherProcessingType;
    }

    /**
     * isPublisherRunning() method is used to check if the child service is
     * running. If the value is false, then the service is not active and all
     * connections will be forced closed.
     *
     * @return <code>boolean</code> value of the running variable
     */
    public synchronized boolean isPublisherRunning() {
        return this._isPublisherRunning;
    }

    /**
     * isPublisherRunning(...) method allows the value of the child service
     * running property to be changed and queried. If the value is false, then
     * the service is not active and all connections will be forced closed.
     *
     * @param running
     * @return <code>boolean</code> value of the running variable
     */
    public synchronized boolean isPublisherRunning(boolean running) {
        this._isPublisherRunning = running;
        return isPublisherRunning();
    }

    /**
     * getRecoveryThreshold() method returns the # of days the pending files
     * will be processed; after which the older files are deleted.
     *
     * @return <code>int</code> value of the # of days to recover.
     */
    private synchronized int getRecoveryThreshold() {
        return this._recoveryThreshold;
    }

    /**
     * getRecoveryPeriodicity() method returns the recovery periodicity (DAY or
     * HOUR) used to scan for pending files to read and publish.
     *
     * @return <code>FileRolloverPeriodicityType</code> value for the reader
     */
    private synchronized FileRolloverPeriodicityType getRecoveryPeriodicity() {
        return this._recoveryPeriodicity;
    }

    /**
     * getServiceAbstractShutdown() method returns the shutdown value which is
     * matched with the incoming string. If the string matches, then the
     * connection is closed and service is set to shutdown.
     *
     * @return <code>String</code> value of the service shutdown
     */
    public synchronized String getServiceShutdown() {
        return this._serviceShutdown;
    }
    // </editor-fold>

    // <editor-fold desc="class methods">
    /**
     * checkConnections() method tries to open a connection to the storage
     * server which will be receiving data. To make sure the method does not
     * block execution, the connection is created using a thread. As long as the
     * thread is trying to create a connection the method will try to reconnect
     * and new thread will not be started.
     * <p>
     * There can be multiple servers which can receive the data. Loop through
     * and find the first server which can receive data and deliver it. If no
     * servers are available, then we continue to wait for one to be available.
     */
    @Override
    public synchronized void checkConnections() {
        // check if the thread is active trying to connect?, if not then 
        // continue, else exit
        if (!isConnectionsCreatorActive()) {
            // update thread indicator to ensure multiple threads are not 
            // spawned
            isConnectionsCreatorActive(true);

            // temp location of parameter to pass to the thread
            final IService publisher = this;

            // thread to create connection to the equipment
            Thread tConnections = new Thread(new Runnable() {
                // thread run method which is executed when thread is started
                @Override
                public void run() {
                    // capture all exceptions to ensure proper handling of memory and
                    // notification to client
                    try {
                        // if the service is running and publisher is not 
                        // running then try to create the connection
                        while (isRunning() && !isPublisherRunning()) {
                            // loop through the list of available hosts which
                            // are configured to receive data
                            for (String hostUri : getHostUriList()) {
                                // check if the service is still running and 
                                // the publisher is not running
                                if (isRunning() && !isPublisherRunning()) {
                                    // capture all exceptions to ensure proper handling of memory and
                                    // notification to client
                                    try {
                                        // create socket to the equipment
                                        Socket client = new Socket(hostUri,
                                                getChildConfig().getConnectionPort());

                                        // create connection for the socket
                                        Connection dsConn
                                                = new Connection(client,
                                                        publisher);

                                        // add the connection to the service list
                                        addConnection(client, dsConn);

                                        // set the host which connected
                                        setConnectedHostUri(hostUri);

                                        // indicate that the publisher is running
                                        isPublisherRunning(true);
                                        break;
                                    } catch (Exception ex) {
                                        // indicate that the publisher is not runing
                                        isPublisherRunning(false);

                                        // log error for tracking
                                        logError(getClass().toString()
                                                + ", checkConnections(), "
                                                + getServiceConfig().getServiceName()
                                                + ", error starting listener "
                                                + " on port "
                                                + getChildConfig().getConnectionPort()
                                                + ", " + ex.getMessage());
                                    }
                                }

                                // yield processing to other threads
                                Thread.yield();
                            }

                            // yield processing to other threads for specified 
                            // time, any exceptions are ignored
                            try {
                                Thread.sleep(getIdleTimeout());
                            } catch (Exception exi) {
                            }
                        }
                    } catch (Exception exi) {
                    } finally {
                        // connection was created, reset the indicator
                        isConnectionsCreatorActive(false);
                    }
                }
            });

            // start the thread to create connection for the service.
            tConnections.start();
        }
    }

    /**
     * serve(...) method of the service which processes the client connection
     * which can be non socket based.
     * <p>
     * The method has two functions: (1) receive incoming messages response from
     * the storage server and (2) monitor for pending messages from subscriber
     * service (through files) and deliver them to the storage server.
     *
     * For incoming messages from storage server, a reader thread is created for
     * the connected socket and all data incoming logged.
     *
     * For outgoing messages to storage server, the method loops and check if
     * there is a file pending to be delivered. If a pending file is found, then
     * the file is read, data sent to the storage server. There are two modes of
     * operations (LIVE and RECOVERY).
     * <p>
     * LIVE mode reads the current active ALARM or MESSAGE file and sends the
     * data to the storage server. When datehour changes, the file is not
     * completely processed is handed over to the recovery handler.
     *
     * RECOVERY mode reads all files which have not been sent to the storage
     * server and sends both ALARM and MESSAGE data as a message to prevent old
     * alarms from triggering an action.
     *
     * 20141128 SSD added queue to track record sent and if error is reported
     * then the record is stored in the log for review
     *
     * @param conn
     * @throws Exception
     */
    @Override
    public void serve(AbstractConnection conn) throws Exception {
        // retrieve current connection count to use for reader thread name 
        // uniqueness
        long totalConnections = getTotalConnections();

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

        // local parameter for reader used by writer to store the record sent
        // and if there is error from server, the record is saved to the log
        final Queue<String> recordQueue = new ConcurrentLinkedQueue<String>();

        // create thread which reads from the socket in stream and logs the 
        // data; this is just to track acknowledgements from storage server
        Thread tReader = new Thread(new Runnable() {
            // thread run method which is executed when thread is started
            @Override
            public void run() {
                // capture all exceptions to ensure proper handling of memory and
                // notification to client
                try {
                    // loop as long as the service is running and the connection
                    // is active
                    while (isRunning() && cConn.isActive()) {
                        // read a line from socket in stream and store it
                        String line = in.readLine();

                        // if the input is null or matches terminator, then
                        // exit the loop
                        if ((line == null) || line.equals(
                                getConnectionTerminator())) {
                            break;
                        } else {
                            // increase total messages received
                            increaseTotalMessagesReceived();

                            // log info for tracking
                            logDebug("CS -> PUB, "
                                    + getChildConfig().getConnectionPort()
                                    + ", " + getPublisherType().name() + ", "
                                    + line);

                            // check for return error
                            String[] result = line.split(",");
                            if (result.length > 0) {
                                if (!result[result.length - 1].equals(
                                        getStatusOk())) {
                                    // get record for logging
                                    String record = recordQueue.poll();

                                    // log error for tracking
                                    logError("CS -> PUB, "
                                            + getChildConfig().getConnectionPort()
                                            + ", " + getPublisherType().name()
                                            + ", " + line + ", <error/" + record + ">");

                                    // store the record in recovery file for
                                    // retry (get file of reader and update _CS
                                    // to R_CS and append the record to the file)
                                    try {
                                        String recoveryFile = getMessageReader().getReaderFilename()
                                                .replace("RCV_MSG", "_MSG")
                                                .replace("_MSG", "RCV_MSG").replace("_ALM", "RCV_MSG");

                                        FileUtils.writeFile(recoveryFile, line, false);
                                    } catch (Exception ex) {
                                        logError(getClass().toString() + ", serve(), "
                                                + getServiceConfig().getServiceName() + " on port "
                                                + getChildConfig().getConnectionPort() + ", server recovery, "
                                                + ex.getMessage());
                                    }
                                }
                            } else {
                                // remove first record from arraylist
                                try {
                                    recordQueue.poll();
                                } catch (Exception exi) {
                                }
                            }
                        }

                        // yield processing to other threads
                        Thread.yield();
                    }
                } catch (Exception ex) {
                    // log error for tracking
                    logError(getClass().toString() + ", serve(), "
                            + getServiceConfig().getServiceName() + " on port "
                            + getChildConfig().getConnectionPort() + ", "
                            + ex.getMessage());
                } finally {
                    // update the publisher status to false to signal connection
                    // monitor to stop running if it is running
                    isPublisherRunning(false);

                    // set connection status to false to signal all serving
                    // loops to exit
                    cConn.isActive(false);

                    // close all socket streams and ignore any exceptions
                    try {
                        out.close();
                    } catch (Exception exi) {
                    }
                    try {
                        in.close();
                    } catch (Exception exi) {
                    }

                    // log info for tracking
                    logInfo(getClass().toString() + ", serve(), "
                            + getServiceConfig().getServiceName() + " on port "
                            + getChildConfig().getConnectionPort()
                            + ", connection closed by server");
                }
            }
        });

        // set unique name for the thread for debugging
        // 20141128 SSD added UID vice totalConnections for unique name
        tReader.setName(getServiceConfig().getServiceName() + "_"
                + getChildConfig().getServiceName()
                + "_" + getServiceConfig().getConnectionPort() + "_"
                + getChildConfig().getConnectionPort()
                + "_" + getPublisherType().name().toString()
                + "_" + UUID.randomUUID().toString());

        // start the thread to create connection for the service.
        tReader.start();

        // loop and check if there are any pending files from the subscriber
        // service which need to be delivered to the storage.  If file is
        // present, read it, send the data until end of file, and check if file
        // needs to be changed (LIVE or RECOVERY mode)
        // capture all exceptions to ensure proper handling of memory and
        // notification to client
        try {
            // loop until either the service stops running or the connection
            // has been in-activated
            while (isRunning() && cConn.isActive()) {
                // read the length of the buffer from media
                String line = getMessageReader().readline();

                // if line is not empty or the file is not at end of file
                if ((line != null) || (!getMessageReader().isEndOfFile())) {
                    // 20141129 SSD if file changed, then show status message
                    if (getMessageReader().isFileChanged()) {
                        logInfo(getClass().toString() + ", serve(), "
                                + getServiceConfig().getServiceName() + " on port "
                                + getServiceConfig().getConnectionPort()
                                + ", processing file (" + getMessageReader().getReaderFilename() + ")");

                        getMessageReader().isFileChanged(false);
                    }

                    // log info for tracking
                    logDebug("PUB -> CS, "
                            + getChildConfig().getConnectionPort() + ", "
                            + getPublisherType().name() + ", " + line);

                    // write the data read to out stream
                    recordQueue.add(line);
                    out.write(line + getRecordTerminator());
                    out.flush();

                    // increase # of messages sent
                    increaseTotalMessagesSent();

                    // yield processing to other threads
                    Thread.yield();
                }

                // exit the loop, if the service or connection is
                // terminated
                if (!isRunning() || !cConn.isActive()) {
                    break;
                }

                // yield processing to other threads
                Thread.yield();
            }
        } catch (Exception ex) {
            // log error for tracking
            logError(getClass().toString() + ", serve(), "
                    + getServiceConfig().getServiceName() + " on port "
                    + getChildConfig().getConnectionPort() + ", "
                    + ex.getMessage());
        } finally {
            // update the publisher status to false to signal connection
            // monitor to stop running if it is running
            isPublisherRunning(false);

            // set connection status to false to signal all serving
            // loops to exit
            cConn.isActive(false);

            // close all socket streams and ignore any exceptions
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

            // if service is still running, then try to restart the connection
            if (isRunning()) {
                checkConnections();
            }
        }
    }

    /**
     * shutdown() method overload from the super class is used to ensure all
     * local allocations or objects are properly disposed.
     */
    @Override
    public synchronized void shutdown() {
        // call the super method to perform termination; this also closes all
        // open connections
        super.shutdown();

        // update the publisher status to false to signal connection
        // monitor to stop running if it is running
        isPublisherRunning(false);

        // shutdown the reader if not null, ignore exceptions
        if (getMessageReader() != null) {
            try {
                getMessageReader().close();
            } catch (Exception exi) {
            }
        }
    }

    /**
     * start() method overloaded from the super class is used to update the
     * local variables for processing. At the end the method calls the
     * checkConnections method to create connection to the equipment.
     *
     * 20141128 SSD added log for file name processing and removed system.out..
     *
     * @throws java.lang.Exception
     */
    @Override
    public synchronized void start() throws Exception {
        // call the super method to perform initialization
        super.start();

        // configure the publisher type, alarm or message
        setFileTypeMap();

        // format the file mask using the site id; don't use equipment id it is 
        // included in the message in the file
        setFileMask(String.format(getParentService().getLocalStoreMask(),
                getParentService().getSiteId(),
                "%s", getFileTypeMap()));

        // open the message reader based on publisher processing type
        // 20150314 ssd added mkdirs to prevent errors in processing
        new File(getParentService().getLocalStoreDirectory()
                + "outgoing").mkdirs();
        
        try {
            if (getPublisherProcessingType() == PublisherProcessingType.LIVE) {
                this._messageReader = new FileChannelTextReader(getFileMask(),
                        getParentService().getLocalStoreDirectory() + "outgoing",
                        FileProcessingType.LIVE);
            } else {
                this._messageReader = new FileChannelTextReader(getFileMask(),
                        getParentService().getLocalStoreDirectory() + "outgoing",
                        FileProcessingType.ARCHIVE, getRecoveryPeriodicity(),
                        getRecoveryThreshold());

            }

            logInfo(getClass().toString() + ", start(), "
                    + getServiceConfig().getServiceName() + " on port "
                    + getServiceConfig().getConnectionPort()
                    + ", processing file (" + getMessageReader().getReaderFilename() + ")");
        } catch (Exception ex) {
            logError(getClass().toString() + ", start(), "
                    + getServiceConfig().getServiceName() + " on port "
                    + getServiceConfig().getConnectionPort()
                    + ", error opening reader, " + ex.getStackTrace());
        }

        // validate the connection to the equipment
        checkConnections();
    }
    // </editor-fold>
}
