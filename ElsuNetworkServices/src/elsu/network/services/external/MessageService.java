package elsu.network.services.external;

import elsu.common.DateUtils;
import elsu.common.FileUtils;
import elsu.common.GlobalStack;
import elsu.io.FileChannelTextWriter;
import elsu.io.FileRolloverPeriodicityType;
import elsu.network.application.*;
import elsu.network.core.ServiceStartupType;
import elsu.network.services.core.*;
import elsu.network.services.*;
import java.io.*;
import java.net.Socket;
import java.util.Calendar;

public class MessageService extends AbstractService implements IService {

	// <editor-fold desc="class private storage">
	// stores the shutdown string for connection, when received, the service
	// is shutdown and will not restart
	private volatile String _serviceShutdown = "#$#";
	// stores the connection terminator; when received, the connection is closed
	// if the service is not shutdown, the connection may be restarted
	private volatile String _connectionTerminator = ".";
	// stores the local storage directory for all data used by the service
	private volatile String _localStoreDirectory = null;
	// stores the file mask - allows the files to include date or other
	// service variables
	private volatile String _localStoreMask = null;
	// service specific data, host uri of the equipment which the collector
	// connects to
	private volatile String _hostUri = null;
	// service specific port # of the equipment
	private volatile int _port = 0;
	// service specific data, status to track if the subscriber service is
	// running or has been shutdown
	private volatile boolean _isSubscriberRunning = false;
	// service specific data, site name the service is supporting
	private volatile String _siteName = null;
	// service specific data, site id of the site name
	private volatile int _siteId = 0;
	// connection of site for two-way comms
	private volatile Connection _siteConnection = null;
	private volatile String _siteMessage = "";
	private volatile String _pendingMessage = "";
	// stores the file mask - allows the files to include date or other
	// service variables
	private volatile String _fileMask = null;
	// service specific data, stores the idle timeout used when connection to
	// a host is not available
	private volatile int _idleTimeout = 5000;
	// service specific data, stores the no data timeout used when connection 
	// has not received any data
	private volatile int _noDataTimeout = 5000;
	// counter to track the number of records received between each monitoring 
	// period
	private volatile int _recordCounter = 0;
	// service specific data, stores the idle timeout used when connection to
	// a host is not available
	private volatile FileRolloverPeriodicityType _logRolloverPeriodicity = FileRolloverPeriodicityType.DAY;
	// service specific data, stores the idle timeout used when connection to
	// a host is not available
	private volatile int _logRolloverFrequency = 1;
	// service specific data, status to track if the connection maintained by the
	// subscriber service is still running
	private volatile boolean _isConnectionsCreatorActive = false;
	// service specific data, status to track if the data activity monitor thread 
	// is active
	private volatile boolean _isDataMonitorActive = false;
	// service specific data, stores the writer channel
	private volatile FileChannelTextWriter _messageWriter = null;
	// output terminator for output
	private volatile String _recordTerminatorOutbound = "\r\n";
	// </editor-fold>

	// <editor-fold desc="class constructor destructor">
	/**
	 * MessageService(...) constructor instantiates the class and loads the
	 * required properties from app.config to local class variables for fast
	 * direct access.
	 *
	 * @param factory
	 * @param threadGroup
	 * @param serviceConfig
	 * @see ServiceAbstract
	 */
	public MessageService(String threadGroup, ServiceManager serviceManager, ServiceConfig serviceConfig) {
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
		this._serviceShutdown = getProperty("application.framework.attributes.key.service.shutdown").toString();
		this._connectionTerminator = getProperty("application.framework.attributes.key.connection.terminator")
				.toString();
		this._localStoreDirectory = getServiceConfig().getAttribute("key.service.localStore.directory").toString();
		this._localStoreMask = getServiceConfig().getAttribute("key.service.localStore.mask").toString();

		this._siteName = getServiceConfig().getAttribute("key.service.site.name").toString();

		try {
			this._siteId = Integer.parseInt(getServiceConfig().getAttribute("key.service.site.id").toString());
		} catch (Exception ex) {
			logError(getClass().toString() + ", initializeLocalProperties(), " + getServiceConfig().getServiceName()
					+ " on port " + getServiceConfig().getConnectionPort() + ", invalid service.site.id, "
					+ ex.getMessage());
			this._siteId = 0;
		}

		try {
			this._idleTimeout = Integer
					.parseInt(getServiceConfig().getAttribute("key.service.monitor.idleTimeout").toString());
		} catch (Exception ex) {
			logError(getClass().toString() + ", initializeLocalProperties(), " + getServiceConfig().getServiceName()
					+ " on port " + getServiceConfig().getConnectionPort() + ", invalid service.monitor.idleTimeout, "
					+ ex.getMessage());
			this._idleTimeout = 5000;
		}

		try {
			this._noDataTimeout = Integer
					.parseInt(getServiceConfig().getAttribute("key.service.monitor.noDataTimeout").toString());
		} catch (Exception ex) {
			logError(getClass().toString() + ", initializeLocalProperties(), " + getServiceConfig().getServiceName()
					+ " on port " + getServiceConfig().getConnectionPort() + ", invalid service.monitor.noDataTimeout, "
					+ ex.getMessage());
			this._noDataTimeout = 5000;
		}

		this._hostUri = getServiceConfig().getAttribute("key.service.site.host").toString();

		try {
			this._port = Integer
					.parseInt(getServiceConfig().getAttribute("key.service.site.port").toString());
		} catch (Exception ex) {
			logError(getClass().toString() + ", initializeLocalProperties(), " + getServiceConfig().getServiceName()
					+ " on port " + getServiceConfig().getConnectionPort() + ", invalid service.site.port, "
					+ ex.getMessage());
			this._port = 0;
		}

		try {
			this._recordTerminatorOutbound = getServiceConfig().getAttribute("key.record.terminator.outbound").toString();
		} catch (Exception ex) {
			logError(getClass().toString() + ", initializeLocalProperties(), " + getServiceConfig().getServiceName()
					+ " on port " + getServiceConfig().getConnectionPort() + ", invalid record.terminator.outbound, "
					+ ex.getMessage());
			this._recordTerminatorOutbound = "\r\n";
		}
		
		String periodicity = "DAY";
		try {
			periodicity = getServiceConfig().getAttribute("key.service.log.rollover.periodicity").toString();
			this._logRolloverPeriodicity = FileRolloverPeriodicityType.valueOf(periodicity);
		} catch (Exception ex) {
			logError(getClass().toString() + ", initializeLocalProperties(), " + getServiceConfig().getServiceName()
					+ " on port " + getServiceConfig().getConnectionPort() + ", invalid service.log.rollover.periodicity, "
					+ ex.getMessage());
			this._logRolloverPeriodicity = FileRolloverPeriodicityType.DAY;
		}
		
		try {
			this._logRolloverFrequency = Integer
					.parseInt(getServiceConfig().getAttribute("key.service.log.rollover.frequency").toString());
		} catch (Exception ex) {
			logError(getClass().toString() + ", initializeLocalProperties(), " + getServiceConfig().getServiceName()
					+ " on port " + getServiceConfig().getConnectionPort() + ", invalid service.log.rollover.frequency, "
					+ ex.getMessage());
			this._logRolloverFrequency = 1;
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
	 * isConnectionsCreatorActive() method is used to track if the
	 * checkConnections method is being processed. checkConnections method uses
	 * a thread to create the connection to the equipment - if the thread is
	 * running trying to connect to the equipment, then all other requests are
	 * ignored.
	 *
	 * @return <code>boolean</code> value of the status of connections
	 */
	private synchronized boolean isDataMonitorActive() {
		return this._isDataMonitorActive;
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
	private synchronized boolean isDataMonitorActive(boolean active) {
		this._isDataMonitorActive = active;
		return isDataMonitorActive();
	}

	/**
	 * getConnectionTerminator() method returns the connection terminator used
	 * to signal the connection to terminate gracefully.
	 *
	 * @return <code>String</code> returns the connection terminator value.
	 */
	public synchronized String getConnectionTerminator() {
		return this._connectionTerminator;
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
	 * getNoDataTimeout() method returns the timeout value used to reset connection
	 * with no received data. It is also used to pause the loop when trying to 
	 * connect to the equipment and it is not responding.
	 *
	 * @return <code>int</code> value of the timeout
	 */
	public synchronized int getNoDataTimeout() {
		return _noDataTimeout;
	}
	
	/**
	 * getRecordCounter() method returns the number of records received.
	 * 
	 * @return <code>int</code> value of recordsCounter
	 */
	public synchronized int getRecordCounter() {
		return this._recordCounter;
	}
	
	/*
	 * increaseRecordCounter() method increases the record counter by 1.
	 */
	private synchronized void increaseRecordCounter() {
		this._recordCounter++;
	}
	
	/*
	 * resetRecordCounter() method resets the record counter to 0.
	 */
	private synchronized void resetRecordCounter() {
		this._recordCounter = 0;
	}

	/**
	 * getLocalStoreDirectory() method returns the local storage directory used
	 * by the service and its child services. For security purposes all input
	 * output is restricted to this storage directory.
	 * <p>
	 * Note this is not a relative path from the application location but full
	 * path.
	 *
	 * @return <code>String</code> returns the directory full path.
	 */
	public synchronized String getLocalStoreDirectory() {
		return this._localStoreDirectory;
	}

	/**
	 * getLocalStoreMask() method returns the file mask used to create file
	 * names for saving data received by collectors or reading data.
	 * <p>
	 * %s_%s_%s.txt currently defined is formatted with site_id, datetime, and
	 * equipment id.
	 *
	 * @return <code>String</code> is the file mask.
	 */
	public synchronized String getLocalStoreMask() {
		return this._localStoreMask;
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
	 * getPort() method returns the equipment id (also used as port) of the
	 * connection. The service uses this to open the connection to the equipment
	 * to transfer messages.
	 *
	 * @return <code>int</code> value of the equipment id
	 */
	public synchronized int getPort() {
		return this._port;
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
	 * getSiteConnection() return the current site connection established to retrieve
	 * data.
	 * 
	 * @return <code>Connection</code> value of the connection.
	 */
	public synchronized Connection getSiteConnection() {
		return this._siteConnection;
	}

	/**
	 * setSiteConnection(connection) sets the current site connection established to retrieve
	 * data.
	 */
	public synchronized void setSiteConnection(Connection conn) {
		this._siteConnection = conn;
	}
	
	/**
	 * getServiceAbstractShutdown() method returns the value which when received
	 * through the client will shutdown the service.
	 *
	 * @return <code>String</code> value of the shutdown string
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

	/**
	 * getSiteId() method returns the site id currently being used by the
	 * message service and is shared by its child services.
	 *
	 * @return <code>int</code> value of the site
	 */
	public synchronized int getSiteId() {
		return this._siteId;
	}

	/**
	 * getSiteName() merhod returns the site name of the site id. This
	 * information is purely for logging and not used by the service.
	 *
	 * @return <code>String</code> value of the site name
	 */
	public synchronized String getSiteName() {
		return this._siteName;
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
					// capture all exceptions to ensure proper handling of
					// memory and
					// notification to client
					try {
						// if the service is running and subscriber is not
						// running then try to create the connection
						while (isRunning() && !isSubscriberRunning()) {
							// capture all exceptions to ensure proper handling
							// of memory and
							// notification to client
							try {
								logInfo(getClass().toString() + ", checkConnections() - createConnections, "
										+ getServiceConfig().getServiceName() + " on port " + getPort());

								// create socket to the equipment
								Socket client = new Socket(getHostUri(), getPort());

								// create connection for the socket
								Connection dsConn = new Connection(client, collector);

								// add the connection to the service list
								addConnection(client, dsConn);
								setSiteConnection(dsConn);

								// indicate that the subscriber is running
								isSubscriberRunning(true);
							} catch (Exception ex) {
								// indicate that the subscriber is not runing
								isSubscriberRunning(false);

								// log error for tracking
								logError(getClass().toString() + ", checkConnections() - createConnections, "
										+ getServiceConfig().getServiceName() + ", error creating connection "
										+ getServiceConfig().getServiceName() + " on port " + getPort() + ", "
										+ ex.getMessage());
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
		
		// create a thread to monitor the connection activity for data
		if (!isDataMonitorActive()) {
			// update thread indicator to ensure multiple threads are not
			// spawned
			isDataMonitorActive(true);
			
			// thread to create connection to the equipment
			Thread tMonitor = new Thread(new Runnable() {
				// thread run method which is executed when thread is started
				@Override
				public void run() {
					// capture all exceptions to ensure proper handling of
					// memory and
					// notification to client
					try {
						// if the service is running and subscriber is not
						// running then try to create the connection
						while (isRunning()) {
							// yield processing to other threads for specified
							// time, any exceptions are ignored
							try {
								Thread.sleep(getNoDataTimeout());
							} catch (Exception exi) {
							}

							// check for data and reset only if subscriber is connected
							if (isSubscriberRunning()) {
								logInfo("CS -> BCS, checking for data, " + getNoDataTimeout() + ", " + getRecordCounter());

								try {
									// if no data received, reset the connection
									if (getRecordCounter() == 0) {
										logInfo(getClass().toString() + ", checkConnections() - noDataTimeout, "
												+ getServiceConfig().getServiceName() + " on port " + getPort());
	
										Connection dsConn = getSiteConnection();
	
										if (dsConn != null) {
											// set connection status to false to signal all serving
											// loops to exit
											dsConn.isActive(false);
											
											// remove connection - to clear the queue
											removeConnection(dsConn);
											setSiteConnection(null);
											isSubscriberRunning(false);
		
											// restart the connection
											if (isRunning()) {
												checkConnections();
											}
										}
									} else {
										resetRecordCounter();
									}
								} catch (Exception ex) {
									// log error for tracking
									logError(getClass().toString() + ", checkConnections() - noDataTimeout, "
											+ getServiceConfig().getServiceName() + ", error shutting down and restarting connection "
											+ getServiceConfig().getServiceName() + " on port " + getPort() + ", "
											+ ex.getMessage());
								}
							}
						}
					} catch (Exception exi) {
					} finally {
						// connection was created, reset the indicator
						isDataMonitorActive(false);
					}
				}
			});

			// start the thread to create connection for the service.
			tMonitor.start();
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
		// local parameter for reader thread access, passes the connection
		// object
		final Connection cConn = (Connection) conn;

		// local parameter for reader thread access, passes the socket in stream
		final BufferedReader in = new BufferedReader(new InputStreamReader(cConn.getClient().getInputStream()));

		// local parameter for reader thread access, passes the socket out
		// stream
		final PrintWriter out = new PrintWriter(
				new BufferedWriter(new OutputStreamWriter(cConn.getClient().getOutputStream())));

		// this is to prevent socket to stay open after error
		try {
			// store the incomming message to text file and send completion
			// code back to the sending service. the method continues to
			// loop until either the service is terminated or the connection
			// is closed or there is an exception in processing
			while (isRunning()) {
				// read line from the socket stream
				String line = in.readLine();

				// if the input is null or the value matches connection
				// terminator then disconnect the client
				if ((line == null) || (line.equals(getConnectionTerminator()))) {
					break;
				} else {
					// increase the total # of incomming messages
					increaseTotalMessagesReceived();
					increaseRecordCounter();
					
					// read the incomming message, parse it, validate it, and
					// then store it for subscriber to pickup and deliver to
					// the equipment it is connected to.
					try {
						// log info for debugging
						logInfo("CS -> BCS, " + getServiceConfig().getConnectionPort() + ", " + getRecordCounter() + ", " + line);

						// parse the data based on the field delimiter
						// String[] parseData = line.split(Pattern.quote(
						// getFieldDelimiter()));

						// if there is an exception in saving we need
						// to notify the client and exit.
						try {
							// this is a message, store it through the
							// message writer
							getMessageWriter().write(line + getRecordTerminatorOutbound());
							
							// if site connection is valid, send the info to site connection
							if ((cConn != getSiteConnection()) && (getSiteConnection() != null)) {
								this._pendingMessage = line;
							} else if ((cConn == getSiteConnection()) && (getSiteConnection() != null)) {
								// increase the message sent count
								increaseTotalMessagesSent();

								if (!this._pendingMessage.isEmpty()) {
									out.print(this._pendingMessage + getRecordTerminator());
									out.flush();
									
									this._pendingMessage = "";
								}
							}
						} catch (Exception ex) {
							// increase the message error queue
							increaseTotalMessagesErrored();

							// log error for tracking
							logError(getClass().toString() + ", serve(), " + getServiceConfig().getServiceName() + ", "
									+ getStatusInvalidContent() + ", writing to output stream, " + ex.getMessage());

						}
					} catch (Exception ex) {
						// increase the message error queue
						increaseTotalMessagesErrored();

						// log error for tracking
						logError(getClass().toString() + ", serve(), " + getServiceConfig().getServiceName() + ", "
								+ getStatusInvalidContent() + ", error parsing fields, " + ex.getMessage());
					}
				}

				// yield processing to other threads
				Thread.yield();
			}
		} catch (Exception ex) {
			// log error for tracking
			logError(getClass().toString() + ", serve(), " + getServiceConfig().getServiceName() + ", "
					+ ex.getMessage());
		} finally {
			// set connection status to false to signal all serving
			// loops to exit
			cConn.isActive(false);

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

			// log info for tracking
			logInfo(getClass().toString() + ", server(), " + getServiceConfig().getServiceName() + " on port "
					+ getServiceConfig().getConnectionPort() + ", connection closed by server");

			if (isRunning() && isSubscriberRunning()) {
				logError(getClass().toString() + ", server(), " + getServiceConfig().getServiceName() + " on port "
						+ getServiceConfig().getConnectionPort() + ", connection closed by server");
			}
			
			// remove connection - to clear the queue
			removeConnection(cConn);
			setSiteConnection(null);
			isSubscriberRunning(false);

			// we have exited the method, but if the service is still running
			// restart the connection
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
	 * start() method overloaded from the super class is used to instantiate
	 * child services.
	 *
	 * @throws java.lang.Exception
	 */
	@Override
	public synchronized void start() throws Exception {
		// call the super method to perform initialization
		super.start();

		// format the file mask using the site id
		setFileMask(String.format(getLocalStoreMask(), getSiteId(), "%s", "%s"));

		// clear all old *_CS.txt files, old messages from parent service
		// are invalid and should not be processed
		// 20150314 ssd added mkdirs to prevent errors in processing
		new File(getLocalStoreDirectory() + "incomming").mkdirs();
		//FileUtils.deleteFiles(getLocalStoreDirectory() + "incomming\\",
		//		String.format(getFileMask(), ".*", getSiteName() + "_CS"), false);

		// open the writer channels; don't use equipment id it is included in
		// the message in the file
		this._messageWriter = new FileChannelTextWriter(String.format(getFileMask(), "%s", "MSG"),
				getLocalStoreDirectory() + "incomming", this._logRolloverPeriodicity);
		this._messageWriter.setRolloverFrequency(this._logRolloverFrequency);

        // validate the connection to the equipment
        checkConnections();
	}
	// </editor-fold>
}
