package site.service;

import elsu.network.services.core.*;
import elsu.network.services.*;
import elsu.common.*;
import elsu.io.*;
import elsu.network.application.*;
import java.io.*;
import java.net.*;
import java.util.*;

/**
 * BcsMessageSubscriberServiceAbstract class is implemented as a child service
 * to support collection of data from equipment. Each equipment has its own port
 * so each service creates a connection to th equipment to receive and send
 * data. The service is implemented as a subscriber, it subscribes to the
 * equipment, receives messages, stores them into either ALARM or MESSAGE file
 * for delivery by the publisher service.
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
public class SiteMessageSubscriberService extends AbstractService implements
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
    // service specific data, equipment id from which the collector is
    // receiving messages or alarms.  this is also the port # of the equipment
    private volatile int _equipmentId = 0;
    // service specific data, host uri of the equipment which the collector
    // connects to
    private volatile String _hostUri = null;
    // service specific data, status to track if the subscriber service is 
    // running or has been shutdown
    private volatile boolean _isSubscriberRunning = false;
    // service specific data, stores the idle timeout used when connection to
    // a host is not available
    private volatile int _idleTimeout = 5000;
    // service specific data, status to track if te connection maintained by the
    // subscriber service is still running
    private volatile boolean _isConnectionsCreatorActive = false;
    // service specific data, stores the writer channel
    private volatile FileChannelTextWriter _messageWriter = null;
    // service specific data, stores the recovery period
    private volatile FileRolloverPeriodicityType _recoveryPeriodicity
            = FileRolloverPeriodicityType.DAY;
    // output terminator for output
    private volatile String _recordTerminatorOutbound = "\r\n";
    // </editor-fold>

    // <editor-fold desc="class constructor destructor">
    /**
     * BcsMessageSubscriberServiceAbstract(...) constructor creates the child
     * service object, initializes all local variables from either the parent
     * service, application, or child service properties.
     * <p>
     * All connections of the child service are managed under the parent service
     * threadgroup.
     *
     * @param factory
     * @param threadGroup
     * @param parentService
     * @param childConfig
     */
    public SiteMessageSubscriberService(String threadGroup,
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
        this._serviceShutdown = getProperty("application.framework.attributes.key.service.shutdown").toString();
        this._connectionTerminator
                = getProperty("application.framework.attributes.key.connection.terminator").toString();

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

        this._equipmentId = getChildConfig().getConnectionPort();
        this._hostUri
                = getChildConfig().getAttribute("service.hostUri").toString();

        switch (getProperty("data.recovery.periodicity").toString()) {
            case "DAY":
                this._recoveryPeriodicity = FileRolloverPeriodicityType.DAY;
                break;
            case "HOUR":
                this._recoveryPeriodicity = FileRolloverPeriodicityType.HOUR;
                break;
        }

        try {
            this._recordTerminatorOutbound
                    = getChildConfig().getAttribute(
                            "record.terminator.outbound").toString();
        } catch (Exception ex) {
            logError(getClass().toString() + ", initializeLocalProperties(), "
                    + getServiceConfig().getServiceName() + " on port "
                    + getServiceConfig().getConnectionPort()
                    + ", invalid record.terminator.outbound, " + ex.getMessage());
            this._recordTerminatorOutbound = "\r\n";
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
     * getEquipmentId() method returns the equipment id (also used as port) of
     * the connection. The service uses this to open the connection to the
     * equipment to transfer messages.
     *
     * @return <code>int</code> value of the equipment id
     */
    public synchronized int getEquipmentId() {
        return this._equipmentId;
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
     * getHostUri() method returns the uri of the equipment this service will be
     * connecting to.
     *
     * @return <code>String</code> value of the host uri
     */
    public synchronized String getHostUri() {
        return this._hostUri;
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
     * getFileChannelWriter() method returns the byte channel used to store the
     * message received by the service connection.
     *
     * @return <code>SeekableByteChannel</code>
     */
    private synchronized FileChannelTextWriter getMessageWriter() {
        return this._messageWriter;
    }

    /**
     * getRecordTerminatorOutbound() method returns the record terminator which
     * is sent with the outbound packets. The method/config allows custom
     * terminator to be specified so both incoming and outbound can work
     * independently.
     *
     * @return string
     */
    private synchronized String getRecordTerminatorOutbound() {
        return this._recordTerminatorOutbound;
    }

    /**
     * getPendingFileList() method scans the service storage directory and
     * returns any files matching the file mask. The file scan return list is
     * limited to the config limit of 10 and can be changed.
     * <p>
     * To improve performance, the files selected are not scanned and sorted,
     * but returned as selected by the file files api.
     *
     * @return <code>ArrayList</code> of files found.
     */
    private ArrayList getPendingFileList() {
        return FileUtils.findFiles(getParentService().getLocalStoreDirectory()
                + "incomming\\",
                String.format(getFileMask(), ".*", getEquipmentId() + "_CS"),
                false, 10);
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

    /**
     * isSubscriberRunning() method is used to check if the child service is
     * running. If the value is false, then the service is not active and all
     * connections will be forced closed.
     *
     * @return <code>boolean</code> value of the running variable
     */
    public synchronized boolean isSubscriberRunning() {
        return this._isSubscriberRunning;
    }

    /**
     * isSubscriberRunning(...) method allows the value of the child service
     * running property to be changed and queried. If the value is false, then
     * the service is not active and all connections will be forced closed.
     *
     * @param running
     * @return <code>boolean</code> value of the running variable
     */
    public synchronized boolean isSubscriberRunning(boolean running) {
        this._isSubscriberRunning = running;
        return isSubscriberRunning();
    }
    // </editor-fold>

    // <editor-fold desc="class methods">
    /**
     * checkConnections() method tries to open a connection to the equipment
     * which will be providing data for collection by this subscriber. To make
     * sure the method does not block execution, the connection is created using
     * a thread. As long as the thread is trying to create a connection the
     * method will try to reconnect and new thread will not be started.
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
            final IService collector = this;

            // thread to create connection to the equipment
            Thread tConnections = new Thread(new Runnable() {
                // thread run method which is executed when thread is started
                @Override
                public void run() {
                    // capture all exceptions to ensure proper handling of memory and
                    // notification to client
                    try {
                        // if the service is running and subscriber is not 
                        // running then try to create the connection
                        while (isRunning() && !isSubscriberRunning()) {
                            // capture all exceptions to ensure proper handling of memory and
                            // notification to client
                            try {
                                // create socket to the equipment
                                Socket client = new Socket(getHostUri(),
                                        getEquipmentId());

                                // create connection for the socket
                                Connection dsConn
                                        = new Connection(client,
                                                collector);

                                // add the connection to the service list
                                addConnection(client, dsConn);

                                // indicate that the subscriber is running
                                isSubscriberRunning(true);
                            } catch (Exception ex) {
                                // indicate that the subscriber is not runing
                                isSubscriberRunning(false);

                                // log error for tracking
                                logError(getClass().toString()
                                        + ", checkConnections(), "
                                        + getServiceConfig().getServiceName()
                                        + ", error creating connection "
                                        + getChildConfig().getServiceName()
                                        + " on port " + getEquipmentId()
                                        + ", " + ex.getMessage());
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
     * The method has two functions: (1) receive incoming messages and store
     * them to file and (2) monitor for pending messages from parent service and
     * deliver them to the equipment.
     *
     * For incoming messages from equipment, a reader thread is created for the
     * connected socket and all data incoming is stored in a file.
     *
     * For incoming messages from parent service, the method loops and check if
     * there is a file pending to be delivered to the equipment. If a pending
     * file is found, then the file is read, data sent to the equipment on the
     * out stream and file deleted from the system. Exceptions are ignored and
     * file is removed even if there is an exception so old messages will not be
     * sent to the equipment at a later date.
     *
     * 2014/11/18 ssd updated inbound message file to contain multiple messages
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

        // create thread which reads from the socket in stream and stores data
        // to the file for publisher to read and send
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

                            // capture all exceptions to ensure proper handling 
                            // of memory and notification to client
                            try {
                                // split the input data into separate fields
                                // based on the field delimiter
                                // String[] parseData = line.substring(0,
                                //         getParentService().getParserFieldLength()).split(
                                //                 getParentService().getParserFieldDelimiter());

                                // log info for tracking
                                logDebug("SUB -> PUB, "
                                        + getChildConfig().getConnectionPort()
                                        + ", " + getEquipmentId() + ", " + line);

                                // this is a message, store it through the
                                // message writer
                                getMessageWriter().write(line + getRecordTerminatorOutbound());
                            } catch (Exception ex) {
                                // increase total message error count
                                increaseTotalMessagesErrored();

                                // log error for tracking
                                logError(getClass().toString() + ", serve(), "
                                        + getServiceConfig().getServiceName()
                                        + ", " + getStatusInvalidContent()
                                        + " (" + line + "), " + ex.getMessage());
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
                    // update the subcriber status to false to signal connection
                    // monitor to stop running if it is running
                    isSubscriberRunning(false);

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
                    logInfo(getClass().toString() + ", server(), "
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
                + "_" + getEquipmentId() + "_" + UUID.randomUUID().toString());

        // start the thread to create connection for the service.
        tReader.start();

        // loop and check if there are any pending files from the parent
        // service which need to be delivered to the equipment.  If file is
        // present, read it, send the data, and delete the file
        // capture all exceptions to ensure proper handling of memory and
        // notification to client
        try {
            // loop until either the service stops running or the connection
            // has been in-activated
            while (isRunning() && cConn.isActive()) {
                // retrieve a list of all pending files
                ArrayList queue = getPendingFileList();

                // if the retrieved file list is empty, yield processing to 
                // other threads for specified time, any exceptions are ignored
                if (queue.isEmpty()) {
                    Thread.sleep(getIdleTimeout());
                }

                // if there are pending files, process them one by one
                if (queue.size() > 0) {
                    // for each file in the pending queue
                    for (Object fObj : queue) {
                        // if the service is still running and connection is
                        // active
                        if (isRunning() && cConn.isActive()) {
                            // local storage for input file
                            BufferedReader fStream = null;

                            // capture all exceptions to ensure proper handling 
                            // of memory and notification to client
                            try {
                                // open the pending file
                                fStream = new BufferedReader(new FileReader(
                                        fObj.toString()));

                                while (isRunning() && cConn.isActive()) {
                                    // read a line from the input file
                                    String record = fStream.readLine();

                                    // 2014/11/18 SSD added check for end of 
                                    // file marker (updated to check for null)
                                    if (record == null) {
                                        break;
                                    }

                                    // if the file is not empty, then write the
                                    // data to the socket out stream to the 
                                    // equipment
                                    if (record.length() > 0) {
                                        // log info for tracking
                                        logDebug("SUB -> SIM, "
                                                + getChildConfig().getConnectionPort()
                                                + ", " + getEquipmentId() + ", "
                                                + record);

                                        // write the data to the out stream and flush
                                        out.write(record
                                                + getRecordTerminatorOutbound());
                                        out.flush();

                                        // increase total messages sent
                                        increaseTotalMessagesSent();
                                    }
                                }
                            } catch (Exception ex) {
                                // log error for tracking
                                logError(getClass().toString() + ", serve(), "
                                        + getChildConfig().getConnectionPort()
                                        + ", " + getEquipmentId()
                                        + ", file processing error, "
                                        + ex.getMessage());
                            } finally {
                                // close input stream and delete the file,
                                // ignore the exceptions
                                try {
                                    fStream.close();
                                } catch (Exception exi) {
                                }
                                try {
                                    new File(fObj.toString()).delete();
                                } catch (Exception exi) {
                                }
                            }
                        } else {
                            // service is not running or the connection has 
                            // been closed, exit the file processing loop
                            break;
                        }

                        // yield processing to other threads
                        Thread.yield();
                    }
                }
            }
        } catch (Exception ex) {
            // log error for tracking
            logError(getClass().toString() + ", serve(), "
                    + getServiceConfig().getServiceName() + ", "
                    + ex.getMessage());
        } finally {
            // update the subcriber status to false to signal connection
            // monitor to stop running if it is running
            isSubscriberRunning(false);

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

        // update the subcriber status to false to signal connection
        // monitor to stop running if it is running
        isSubscriberRunning(false);

        // shutdown the writers if not null, ignore exceptions
        if (getMessageWriter() != null) {
            try {
                getMessageWriter().close();
            } catch (Exception exi) {
            }
        }
    }

    /**
     * start() method overloaded from the super class is used to update the
     * local variables for processing. At the end the method calls the
     * checkConnections method to create connection to the equipment.
     *
     * @throws java.lang.Exception
     */
    @Override
    public synchronized void start() throws Exception {
        // call the super method to perform initialization
        super.start();

        // format the file mask using the site id
        setFileMask(String.format(getParentService().getLocalStoreMask(),
                getParentService().getSiteId(), "%s", "%s"));

        // clear all old *_CS.txt files, old messages from parent service
        // are invalid and should not be processed
        // 20150314 ssd added mkdirs to prevent errors in processing
        new File(getParentService().getLocalStoreDirectory()
                + "incomming").mkdirs();
        FileUtils.deleteFiles(getParentService().getLocalStoreDirectory()
                + "incomming\\",
                String.format(getFileMask(), ".*", getEquipmentId() + "_CS"),
                false);

        // open the writer channels; don't use equipment id it is included in
        // the message in the file
        this._messageWriter = new FileChannelTextWriter(String.format(
                getFileMask(), "%s", "MSG"),
                getParentService().getLocalStoreDirectory() + "outgoing",
                getRecoveryPeriodicity());

        // validate the connection to the equipment
        checkConnections();
    }
    // </editor-fold>
}
