package site.service;

import elsu.network.services.core.*;
import elsu.network.services.*;
import java.io.*;
import java.util.*;
import elsu.common.*;
import elsu.network.application.*;

/**
 *
 * @author Seraj Dhaliwal (seraj.s.dhaliwal@uscg.mil)
 */
public class SiteSimulatorService extends AbstractService implements IService {

    // <editor-fold desc="class private storage">
    // local storage for service shutdown string
    private volatile String _serviceShutdown = "#$#";

    // local storage for connection terminator string
    private volatile String _connectionTerminator = ".";

    // local storage for directory where simulator files are stored
    private volatile String _localStoreDirectory = null;

    // local storage for filename of the simulator file to use
    private volatile String _localStoreFilename = null;

    // local storage for simulator send delay
    private volatile int _sendDelay = 5000;

    // local storage for send loop (if true, then simulator keeps sending data
    // as long as the connection exists, else it terminates the connection
    // after all the file data is sent
    private volatile boolean _isSendLoop = false;

    // local storage for send count, # of records to send per loop
    private volatile int _sendCount = 1;

    // local storage for storing the full path, filename combination
    private volatile String _inPath = "";

    // local storage for simulator file data in memory
    private volatile ArrayList<String> _simData = null;
    // </editor-fold>

    // <editor-fold desc="class constructor destructor">
    public SiteSimulatorService(String threadGroup, ServiceManager serviceManager,
            ServiceConfig serviceConfig) {
        // call the super class constructor
        super(threadGroup, serviceManager, serviceConfig);

        _simData = new ArrayList<>();

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
        this._connectionTerminator = getProperty("connection.terminator").toString();

        this._localStoreDirectory = getServiceConfig().getAttribute(
                "service.localStore.directory").toString();
        this._localStoreFilename = getServiceConfig().getAttribute(
                "service.localStore.filename").toString();

        try {
            this._sendDelay = Integer.parseInt(
                    getServiceConfig().getAttribute(
                            "service.connection.send.delay").toString());
        } catch (Exception ex) {
            logError(getClass().toString() + ", initializeLocalProperties(), "
                    + getServiceConfig().getServiceName() + " on port "
                    + getServiceConfig().getConnectionPort()
                    + ", invalid service.connection.send.delay, "
                    + ex.getMessage());
            this._sendDelay = 10000;
        }

        isSendLoop(Boolean.valueOf(getServiceConfig().getAttribute(
                "service.connection.send.loop").toString()));

        try {
            this._sendCount = Integer.parseInt(
                    getServiceConfig().getAttribute(
                            "service.connection.send.count").toString());
        } catch (Exception ex) {
            logError(getClass().toString() + ", initializeLocalProperties(), "
                    + getServiceConfig().getServiceName() + " on port "
                    + getServiceConfig().getConnectionPort()
                    + ", invalid service.connection.send.count, "
                    + ex.getMessage());
            this._sendCount = 5;
        }
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
     * getLocalStoreDirectory() method returns the value of the config value
     * which specifies the path where all the files will be created
     *
     * @return <code>String</code> value of the local storage path
     */
    private synchronized String getLocalStoreDirectory() {
        return this._localStoreDirectory;
    }

    /**
     * getLocalStoreFilename() method returns the value of the local file which
     * contains the simulator data to process.
     *
     * @return <code>String</code> value of the local storage file
     */
    private synchronized String getLocalStoreFilename() {
        return this._localStoreFilename;
    }

    /**
     * getSendCount() method returns the value specifying the # of records which
     * will be sent for each loop
     *
     * @return <code>int</code> value - # of records to send
     */
    private synchronized int getSendCount() {
        return this._sendCount;
    }

    /**
     * getSendDelay() method returns the value used to pause the service
     * connection as specified by the configuration. The connection is paused
     * after the # of records returned by getSendCount() have been sent.
     *
     * @return <code>int</code> value of the delay
     */
    private synchronized int getSendDelay() {
        return this._sendDelay;
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

    /**
     * getSimData() method returns the simulator file data as memory list for
     * faster processing between multiple connections vice having hundreds of
     * file handles.
     * <p>
     * Since the list is static after initial load, there is no need for the
     * list access to be protected.
     *
     * @return <code>ArrayList<String></code> with simulator file data
     */
    private synchronized ArrayList<String> getSimData() {
        return this._simData;
    }

    /**
     * getSimFile() method returns the combination of local storage directory
     * and file.
     *
     * @return <code>String</code> value of the directory + file
     */
    private synchronized String getSimFile() {
        return getLocalStoreDirectory() + getLocalStoreFilename();
    }

    /**
     * isSendLoop() method returns true if the simulator should restart the send
     * loop after all the data has been processed or terminate the connection
     * after one loop.
     *
     * @return <code>boolean</code> value of the send loop option
     */
    private synchronized boolean isSendLoop() {
        return this._isSendLoop;
    }

    /**
     * isSendLoop(...) method allows the value of the send loop behavior to be
     * changed and returns true if the simulator should restart the send loop
     * after all the data has been processed or terminate the connection after
     * one loop.
     *
     * @param loop
     * @return <code>boolean</code> value of the send loop option
     */
    private synchronized boolean isSendLoop(boolean loop) {
        this._isSendLoop = loop;
        return isSendLoop();
    }
    // </editor-fold>

    // <editor-fold desc="class methods">
    /**
     * setSimData() method reads the simulator file and populates the ArrayList
     * to decrease in the # of file handles which will be opened to handle
     * multiple connections and reduce the memory overhead associated with
     * managing multiple IO related operations.
     */
    private synchronized void setSimData() {
        try {
            getSimData().addAll(FileUtils.readFileToList(getSimFile()));
        } catch (Exception ex) {
            logError(getClass().toString() + ", setSimData(), "
                    + getServiceConfig().getServiceName() + " on port "
                    + getServiceConfig().getConnectionPort() + ", "
                    + ex.getMessage());
        }
    }

    /**
     * serve(...) method is the main method of the service which processes the
     * client socket using the streams (in/out). The method is shared by all
     * client sockets.
     * <p>
     * This method is used by the CommandForwarderServiceAbstract to send
     * messages from the control station to the broadcast service. All messages
     * are stored to a text file and read by the respective subscriber and sent
     * to the equipment
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

        // store total records in the simulator file (from ArrayList)
        int rCount = getSimData().size();

        // local count to keep track of array list index
        int lCount = 0;

        // local class to communicate between reader thread and class action
        // this is required to allow reader thread to notify the sender method
        // that the client has disconnected so abort
        class ClientStatus {

            private boolean _connected = false;

            public synchronized boolean isConnected() {
                return this._connected;
            }

            public synchronized boolean isConnected(boolean active) {
                this._connected = active;
                return isConnected();
            }
        }

        final ClientStatus status = new ClientStatus();

        // this is to prevent socket to stay open after error
        try {
            // update the shared status monitor
            status.isConnected(true);

            // create the reader thread which received incomming messages from
            // client asynchronously
            Thread tReader = new Thread(new Runnable() {
                @Override
                public void run() {
                    // this is to prevent socket to stay open after error
                    try {
                        // as long as the shared object shows socket is good
                        while (status.isConnected()) {
                            // read the client data from the socket
                            String line = in.readLine();

                            // if the input is null or the value matches connection 
                            // terminator then disconnect the client
                            if ((line == null) || line.equals(
                                    getConnectionTerminator())) {
                                break;
                            } else {
                                // increase the total # of incomming messages
                                increaseTotalMessagesReceived();

                                // log info for tracking
                                logDebug("SIM -> EQP,"
                                        + getServiceConfig().getConnectionPort()
                                        + "," + line);
                            }

                            // yield processing to other threads
                            Thread.yield();
                        }
                    } catch (Exception ex) {
                        // log error for tracking
                        logError(getClass().toString() + ", serve(), "
                                + getServiceConfig().getServiceName()
                                + " on port "
                                + getServiceConfig().getConnectionPort() + ", "
                                + ex.getMessage());
                    } finally {
                        // update the shared status object that reader has 
                        // exited 
                        status.isConnected(false);

                        // close out all open in/out streams.
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
                                + getServiceConfig().getServiceName()
                                + " on port "
                                + getServiceConfig().getConnectionPort()
                                + ", connection closed by server");
                    }
                }
            });

            // assign reader thread name for logging purposes
            // 20141128 SSD added UID vice totalConnections for unique name
            tReader.setName(getServiceConfig().getServiceName()
                    + "_" + getServiceConfig().getConnectionPort()
                    + "_" + UUID.randomUUID().toString());

            // start the reader thread
            tReader.start();

            // loop counter to track how many messages have been sent
            int count = 1;

            // this is the main processing loop, client sends commands, which
            // are parsed, executed, and result returned to the client
            while (status.isConnected()) {
                // keep sending messages as long as the count is less than the
                // configuration count; else pause
                if (count >= getSendCount()) {
                    // reset the send counter
                    count = 1;

                    // yield processing to other threads
                    try {
                        Thread.sleep(getSendDelay());
                    } catch (Exception exi) {
                    }
                }

                // increase # of records sent
                count++;

                // if array index is less than total records
                if (lCount < rCount) {
                    // get the index data
                    String data = getSimData().get(lCount);

                    // increase the list index
                    lCount++;

                    // log info for tracking
                    logDebug("SIM -> SUB, "
                            + getServiceConfig().getConnectionPort() + ", "
                            + data);

                    // send the data to the client
                    out.print(data + getRecordTerminator());
                    out.flush();

                    // increase the total # of sent messages
                    increaseTotalMessagesSent();
                } else {
                    // if we have sent all records, check if loop is enabled
                    // if loop is enabled, then reset the array index to restart
                    // else break and close connection
                    if (isSendLoop()) {
                        lCount = 0;
                        continue;
                    } else {
                        break;
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
            // update the shared status object that reader has exited 
            status.isConnected(false);

            // close out all open in/out streams.
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

        // validate or create the directories required to support the local
        // storage directory
        // 20150314 ssd moved from initialize() to start()
        new File(this.getLocalStoreDirectory()).mkdirs();

        // load the sim data on startup
        setSimData();
    }
    // </editor-fold>
}
