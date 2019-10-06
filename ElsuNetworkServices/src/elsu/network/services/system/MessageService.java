package elsu.network.services.system;

import elsu.common.DateUtils;
import elsu.common.FileUtils;
import elsu.common.GlobalStack;
import elsu.network.application.*;
import elsu.network.core.ServiceStartupType;
import elsu.network.services.core.*;
import elsu.network.services.*;
import java.io.*;
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
	// service specific data, site name the service is supporting
	private volatile String _siteName = null;
	// service specific data, site id of the site name
	private volatile int _siteId = 0;
	// </editor-fold>

	// <editor-fold desc="class constructor destructor">
	/**
	 * MessageService(...) constructor instantiates the class and
	 * loads the required properties from app.config to local class variables
	 * for fast direct access.
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
		this._connectionTerminator = getProperty("application.framework.attributes.key.connection.terminator").toString();
		this._localStoreDirectory = getServiceConfig().getAttribute("key.service.localStore.directory").toString();
		this._localStoreMask = getServiceConfig().getAttribute("key.service.localStore.mask").toString();

		System.out.println("1001-01");
		try {
			isListener(Boolean.valueOf(getServiceConfig().getAttribute("key.service.listener").toString()));
		} catch (Exception ex) {
			logError(getClass().toString() + ", initializeLocalProperties(), " + getServiceConfig().getServiceName()
					+ " on port " + getServiceConfig().getConnectionPort() + ", invalid service.listener property, "
					+ ex.getMessage());
			isListener(false);
		}

		this._siteName = getServiceConfig().getAttribute("key.service.site.name").toString();

		try {
			this._siteId = Integer.parseInt(getServiceConfig().getAttribute("key.service.site.id").toString());
		} catch (Exception ex) {
			logError(getClass().toString() + ", initializeLocalProperties(), " + getServiceConfig().getServiceName()
					+ " on port " + getServiceConfig().getConnectionPort() + ", invalid service.site.id, "
					+ ex.getMessage());
			this._siteId = 0;
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
	public synchronized String getConnectionTerminator() {
		return this._connectionTerminator;
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
	 * names for saving data received by collectors or reading data by
	 * publishers.
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
	 * getServiceAbstractShutdown() method returns the value which when received
	 * through the client will shutdown the service.
	 *
	 * @return <code>String</code> value of the shutdown string
	 */
	public synchronized String getServiceShutdown() {
		return this._serviceShutdown;
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
			for (;;) {
				// read line from the socket stream
				String line = in.readLine();

				// if the input is null or the value matches connection
				// terminator then disconnect the client
				if ((line == null) || (line.equals(getConnectionTerminator()))) {
					break;
				} else {
					// increase the total # of incomming messages
					increaseTotalMessagesReceived();

					// read the incomming message, parse it, validate it, and
					// then store it for subscriber to pickup and deliver to
					// the equipment it is connected to.
					try {
						// log info for debugging
						logDebug("CS -> BCS, " + getServiceConfig().getConnectionPort() + ", " + line);

						// parse the data based on the field delimiter
						// String[] parseData = line.split(Pattern.quote(
						// getFieldDelimiter()));

						// create file name based on the site id,
						// and equipment id provided in the message.
						// all messages are stored in the local storage
						// directory as identified in the services
						// configuration
						String filename = getLocalStoreDirectory() + "incomming\\"
								+ String.format(getLocalStoreMask(), this.getSiteId(), DateUtils
										.convertDate2String(Calendar.getInstance().getTime(), getDatetimeFormat()),
										this.getSiteName() + "_CS");

						// if there is an exception in saving we need
						// to notify the client and exit.
						try {
							// write the message to file for delivery
							FileUtils.writeFile(filename, line + GlobalStack.LINESEPARATOR, true);
						} catch (Exception ex) {
							// increase the message error queue
							increaseTotalMessagesErrored();

							// log error for tracking
							logError(getClass().toString() + ", serve(), " + getServiceConfig().getServiceName() + ", "
									+ getStatusInvalidContent() + ", writing to output stream, " + ex.getMessage());

						}

						// send response with message ok_code
						out.print(getStatusOk() + getRecordTerminator());
						out.flush();

						// increase the message sent count
						increaseTotalMessagesSent();
					} catch (Exception ex) {
						// increase the message error queue
						increaseTotalMessagesErrored();

						// log error for tracking
						logError(getClass().toString() + ", serve(), " + getServiceConfig().getServiceName() + ", "
								+ getStatusInvalidContent() + ", error parsing fields, " + ex.getMessage());

						// there was an exception in parsing the
						// message, but ensure the client
						// did not disconnect - if client is not
						// connected ignore the exception.
						try {
							// send response with message invalid content
							out.print(getClass().toString() + ", serve(), " + getServiceConfig().getServiceName() + ", "
									+ getStatusInvalidContent() + ", " + ex.getMessage() + getRecordTerminator());
							out.flush();
						} catch (Exception exi) {
						}
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
	}
	// </editor-fold>
}
