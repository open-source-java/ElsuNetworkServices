package elsu.network.services.system;

import elsu.network.services.core.*;
import elsu.network.services.*;
import elsu.common.*;
import elsu.io.*;
import elsu.network.application.*;
import java.io.*;
import java.util.regex.*;
import java.security.*;
import org.apache.commons.codec.binary.*;

public class FileTransferService extends AbstractService implements IService {

    // <editor-fold desc="class private storage">
    // storage for service shutdown string when received terminates the service
    private volatile String _serviceShutdown = "#$#";

    // storage for connection terminator when received closes the connection
    private volatile String _connectionTerminator = ".";

    // local storage for local directory use, if set to true, then the user
    // path is ignored for all commands
    private volatile boolean _localStoreUseAlways = false;

    // local storage for directory where simulator files are stored
    private volatile String _localStoreDirectory = null;

    // local storage for buffer size in file io operations
    private volatile int _fileIOBufferSize = 512;

    // local storage for connection idle time
    private volatile int _connectionTimeout = 5000;
    // </editor-fold>

    // <editor-fold desc="class constructor destructor">
    /**
     * FileTransferServiceAbstract(...) constructor creates the object based on
     * the factory, thread group, and service config.
     *
     * @param factory
     * @param threadGroup
     * @param serviceConfig
     * @see ServiceFactory
     * @see AbstractService
     * @see ServiceProperties
     * @see AbstractConnection
     * @see ServiceConnectionBasic
     * @see ServiceConnectionCustom
     */
    public FileTransferService(String threadGroup, ServiceManager serviceManager,
            ServiceConfig serviceConfig) {
        // call the super class constructor
        super(threadGroup, serviceManager, serviceConfig);
        // local config properties for local reference by class method
        // initializeLocalProperties();
    }

    /**
     * initializeProperties() is a generic method to consolidate all initial
     * variable instantiation outside of class constructor. It allows the
     * variables to be reset from another method within a class if required.
     *
     */
    @Override
    protected void initializeLocalProperties() {
        super.initializeLocalProperties();

        this._serviceShutdown = getProperty("application.framework.attributes.key.service.shutdown").toString();
        this._connectionTerminator
                = getProperty("application.framework.attributes.key.connection.terminator").toString();

        try {
            this._localStoreUseAlways = Boolean.valueOf(
                    getServiceConfig().getAttributes().get(
                            "key.localStore.useAlways").toString());
        } catch (Exception ex) {
            logError(getClass().toString() + ", initializeLocalProperties(), "
                    + getServiceConfig().getServiceName() + " on port "
                    + getServiceConfig().getConnectionPort()
                    + ", invalid fileTransferService.attributes.localStore.useAlways, "
                    + ex.getMessage());
            this._localStoreUseAlways = true;
        }

        this._localStoreDirectory = getProperty("application.framework.attributes.key.localStore.directory").toString();

        try {
            this._fileIOBufferSize = Integer.parseInt(
                    getServiceConfig().getAttributes().get(
                            "key.bufferSize").toString());
        } catch (Exception ex) {
            logError(getClass().toString() + ", initializeLocalProperties(), "
                    + getServiceConfig().getServiceName() + " on port "
                    + getServiceConfig().getConnectionPort()
                    + ", invalid fileTransferService.attributes.bufferSize, "
                    + ex.getMessage());
            this._fileIOBufferSize = 1024;
        }

        try {
            this._connectionTimeout = Integer.parseInt(
                    getProperty("application.framework.attributes.key.connection.idleTimeout").toString()) * 1000;
        } catch (Exception ex) {
            logError(getClass().toString() + ", initializeLocalProperties(), "
                    + getServiceConfig().getServiceName() + " on port "
                    + getServiceConfig().getConnectionPort()
                    + ", invalid connection.idleTimeout, "
                    + ex.getMessage());
            this._connectionTimeout = 5000;
        }
    }
    // </editor-fold>

    // <editor-fold desc="class getter/setters">
    /**
     * getConnectionIdleTimeout() method returns the int value of the connection
     * idle timeout - used to wait for the client connection to completely
     * receive all data sent from the server.
     *
     * @return <code>int</code> value of the connection idle timeout
     */
    private synchronized int getConnectionIdleTimeout() {
        return this._connectionTimeout;
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
     * getFileIOBufferSize() method returns the value of the file io buffer size
     * variable as defined by the config parameter.
     *
     * @return <code>int</code> value of the buffer size.
     */
    public synchronized int getFileIOBufferSize() {
    	return this._fileIOBufferSize;
    }

    /**
     * getLocalStoreDirectory() method returns the value of the config variable
     * which specifies the path where all the files will be created
     *
     * @return <code>String</code> value of the local storage path
     */
    private synchronized String getLocalStoreDirectory() {
    	return this._localStoreDirectory;
    }

    /**
     * getLocalStoreUseAlways() method returns the value of the config variable
     * which specifies the useAlways setting - if true; the local storage over-
     * rides the user server directory for all operations.
     *
     * @return <code>Boolean</code> value of the useAlways variable.
     */
    private synchronized Boolean getLocalStoreUseAlways() {
    	return this._localStoreUseAlways;
    }
    // </editor-fold>

    // <editor-fold desc="class methods">
    /**
     * serve(...) method processes all incoming client socket connections using
     * their in/out streams.
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
        final BufferedReader in = new BufferedReader(new InputStreamReader(
                cConn.getClient().getInputStream()));

        // local parameter for reader thread access, passes the socket out 
        // stream
        final PrintWriter out = new PrintWriter(new BufferedWriter(
                new OutputStreamWriter(cConn.getClient().getOutputStream())));

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
                    // service commands GET, PUT, REMOVE, CHECK, LIST

                    // log info for tracking
                    logDebug("FTS -> STR, "
                            + getServiceConfig().getConnectionPort() + ", "
                            + line);

                    // get or put, then do a binary loop once the initial
                    // command string is recognized.  the file transfer is 
                    // complete when the number of bytes received match the
                    // command bytes
                    // split received data and store it in array
                    String[] lineData = line.split(Pattern.quote(
                            getFieldDelimiter()));

                    // if array count is greater than minimum continue
                    // GET,Directory Alias,Filename,BINARY|ASCII
                    // PUT,Directory Alias,Filename,BINARY|ASCII,APPEND,Size
                    // REMOVE,Directory Alias,recursive,include directories,searh mask|list of files
                    // CHECK,Directory Alias,BINARY|ASCII,search mask|list of files
                    // LIST,Directory Alias,recursive,directories only,depth,searh mask
                    if (lineData.length >= 1) {
                        // if get, then retreive the arguments
                        String ftCommand = lineData[0].toLowerCase();
                        // GET ASCII/BIN sourcepath file
                        switch (ftCommand) {
                            case "get":
                                // check minimum of 4 arguments for get command
                                if (lineData.length >= 4) {
                                    // parse 1, source path
                                    String ftSourcePath = lineData[1].toLowerCase();

                                    // parse 2, file name
                                    String ftFilename = lineData[2].toLowerCase();

                                    // parse 3, binary/ascii option
                                    boolean ftBinary = false;
                                    if ((lineData[3].toLowerCase().equals("binary"))
                                            || (lineData[3].toLowerCase().equals("b"))) {
                                        ftBinary = true;
                                    }

                                    // local variables for process tracking
                                    AbstractFileChannelReader fcReader = null;
                                    long ftSize = 0L;
                                    long bytesSent = 0L;

                                    // checksum calculation for bytes read from file
                                    MessageDigest digest
                                            = MessageDigest.getInstance("MD5");

                                    // catch (Exception ex) socket
                                    // client is notified vice terminating
                                    try {
                                        // file is not open until the first read is
                                        // performed, therefore the size of the file
                                        // cannot be sent until after read
                                        boolean sendFileSize = true;

                                        // if binary to byte buffer reads vice line
                                        // reads
                                        if (ftBinary) {
                                            // loop until the file is sent and the
                                            // service is running
                                            while (isRunning()) {
                                                // read file content for buffer size
                                                // until EOF, send content to client
                                            }
                                        } else {
                                            // open the sourcepath + file
                                            fcReader = new FileChannelTextReader(
                                                    ftFilename, ftSourcePath);

                                            // send ok, file size
                                            out.write(getStatusOk()
                                                    + getRecordTerminator());
                                            out.flush();

                                            // loop until the file is sent and the
                                            // service is running
                                            while (isRunning()) {
                                                // read line from the file
                                                String ftLine = fcReader.readline();

                                                // send the size if this is first run
                                                if (sendFileSize) {
                                                    // send the info the user
                                                    ftSize
                                                            = fcReader.getReaderSize();
                                                    out.write(ftSize
                                                            + getRecordTerminator());
                                                    out.flush();

                                                    // reset the file size send indicator
                                                    sendFileSize = false;
                                                }

                                                // as long as the reader is valid, send
                                                // the data
                                                if (fcReader.isReaderValid()) {
                                                    // calculate the file bytes sent
                                                    bytesSent += ftLine.length();
                                                    bytesSent
                                                            += getRecordTerminator().length();

                                                    // perform calculation for md5
                                                    digest.update(ftLine.getBytes());
                                                    digest.update(
                                                            getRecordTerminator().getBytes());

                                                    // send the data to the client
                                                    out.write(ftLine
                                                            + getRecordTerminator());
                                                    out.flush();
                                                } else {
                                                    break;
                                                }
                                            }
                                        }
                                    } catch (Exception ex) {
                                        // exception is thrown when end of file is
                                        // reached - this is normal

                                        // compare the file size to bytes sent, if
                                        // they match, then ignore the exception
                                        // note: text file may not have newline
                                        // at the end of last line
                                        if (ftSize <= bytesSent) {
                                            // send ok, file size
                                            out.write(getStatusOk() + ", " + ftSize
                                                    + ", " + Hex.encodeHexString(
                                                            digest.digest())
                                                    + getRecordTerminator());
                                            out.flush();
                                        } else {
                                            // send error to client for tracking and log it
                                            try {
                                                out.write(getStatusInvalidContent()
                                                        + ", get, "
                                                        + ex.getMessage());
                                                out.flush();
                                            } catch (Exception exi) {
                                            }

                                            logError(getClass().toString() + ", "
                                                    + getServiceConfig().getServiceName()
                                                    + " on port "
                                                    + getServiceConfig().getConnectionPort()
                                                    + ", get, " + ex.getMessage());
                                        }
                                    } finally {
                                        try {
                                            fcReader.close();
                                        } catch (Exception exi) {
                                        }
                                    }
                                } else {
                                    // return error status to sender
                                    out.print(getStatusInvalidContent()
                                            + getRecordTerminator());
                                    out.flush();
                                }
                                break;
                            case "put":
                                if (lineData.length >= 5) {
                                    // parse 1, destination path
                                    String ftDestinationPath
                                            = lineData[1].toLowerCase();

                                    // parse 2, file name
                                    String ftFilename = lineData[2].toLowerCase();

                                    // parse 3, binary/ascii option
                                    boolean ftBinary = false;
                                    if ((lineData[3].toLowerCase().equals("binary"))
                                            || (lineData[3].toLowerCase().equals("b"))) {
                                        ftBinary = true;
                                    }

                                    // parse 3, append option
                                    boolean ftAppend = false;
                                    if ((lineData[4].toLowerCase().equals("true"))
                                            || (lineData[4].toLowerCase().equals("t"))
                                            || (lineData[4].toLowerCase().equals("append"))
                                            || (lineData[4].toLowerCase().equals("a"))) {
                                        ftAppend = true;
                                    }

                                    // parse 5, file size to receive
                                    long ftSize = Long.parseLong(lineData[5]);

                                    // local variables for process tracking
                                    FileChannelTextWriter fcWriter = null;

                                    // checksum calculation for bytes read from file
                                    MessageDigest digest
                                            = MessageDigest.getInstance("MD5");

                                    try {
                                        // open the sourcepath + file
                                        fcWriter = new FileChannelTextWriter(
                                                ftFilename, ftDestinationPath,
                                                ftAppend);

                                        // send ok, file size
                                        out.write(getStatusOk()
                                                + getRecordTerminator());
                                        out.flush();

                                        // read bytes from client and store them,
                                        // if binary, then do char loop else do
                                        // line loop
                                        long bytesRead = 0L;

                                        // if this is binary file, process it using
                                        // byte buffer logic
                                        if (ftBinary) {
                                            // allocate read buffer for processing
                                            char[] ftBuffer = new char[25];
                                            int ftBufferSize;

                                            // keep retrieving the file until the
                                            // number of bytes have been read
                                            while (isRunning() && bytesRead < ftSize) {
                                                // retrieve the # of bytes from the
                                                // client stream input
                                                ftBufferSize = in.read(ftBuffer);

                                                // if valid data, then store it
                                                if (ftBufferSize > 0) {
                                                    // track # of bytes stored
                                                    bytesRead += ftBufferSize;

                                                    // track the md5 digest for data
                                                    digest.update(new String(
                                                            ftBuffer).getBytes());

                                                    // store the information
                                                    fcWriter.write(ftBuffer);
                                                }

                                                // check if the stream is valid
                                                if (ftBufferSize == -1) {
                                                    break;
                                                }
                                            }

                                            // send ok, bytes read
                                            out.write(getStatusOk() + ", "
                                                    + bytesRead + ", "
                                                    + Hex.encodeHexString(
                                                            digest.digest())
                                                    + getRecordTerminator());
                                            out.flush();
                                        } else {
                                            // if this is text file, process it using
                                            // read line logic

                                            // local vars for optimization of fixed
                                            // information
                                            int newlineLength
                                                    = GlobalStack.LINESEPARATOR.length();
                                            byte[] newlineArray
                                                    = GlobalStack.LINESEPARATOR.getBytes();

                                            // receive data as long as the service
                                            // is running and the bytes read is less
                                            // than the delivery size
                                            while (isRunning() && bytesRead < ftSize) {
                                                // read a line from the socket client
                                                String ftLine = in.readLine();

                                                // increase the # of received count;
                                                // remove marker offset for newline
                                                bytesRead += ftLine.length() - 1;

                                                // perform calculation for md5
                                                String dataLine
                                                        = ftLine.substring(1);
                                                digest.update(dataLine.getBytes());

                                                // write the data to the output file
                                                fcWriter.write(dataLine);

                                                // if addnewline is true, add new line
                                                if (ftLine.charAt(0) == '+') {
                                                    bytesRead += newlineLength;

                                                    // perform calculation for md5
                                                    digest.update(newlineArray);

                                                    // write the data to the output file
                                                    fcWriter.write(
                                                            GlobalStack.LINESEPARATOR);
                                                } else {
                                                    // end of file received, exit
                                                    break;
                                                }
                                            }
                                        }

                                        // send ok, bytes read
                                        out.write(getStatusOk() + ", " + bytesRead
                                                + ", " + Hex.encodeHexString(
                                                        digest.digest())
                                                + getRecordTerminator());
                                        out.flush();
                                    } catch (Exception ex) {
                                        // send error to client for tracking and log it
                                        try {
                                            out.write(getStatusInvalidContent()
                                                    + ", put, " + ex.getMessage());
                                            out.flush();
                                        } catch (Exception exi) {
                                        }

                                        logError(getClass().toString() + ", "
                                                + getServiceConfig().getServiceName()
                                                + " on port "
                                                + getServiceConfig().getConnectionPort()
                                                + ", put, " + ex.getMessage());
                                    } finally {
                                        try {
                                            fcWriter.close();
                                        } catch (Exception exi) {
                                        }
                                    }
                                } else {
                                    // return error status to sender
                                    out.print(getStatusInvalidContent()
                                            + getRecordTerminator());
                                    out.flush();
                                }
                                break;
                            case "remove":
                                if (lineData.length >= 5) {
                                    // parse 1, directory alias
                                    String ftDestinationPath
                                            = lineData[1].toLowerCase();

                                    // parse 2, recursive
                                    Boolean ftSubDirectories = false;
                                    try {
                                        ftSubDirectories = Boolean.valueOf(
                                                lineData[2].toLowerCase());
                                    } catch (Exception exi) {
                                    }

                                    // parse 3, remove directories
                                    Boolean ftDirectories = false;
                                    try {
                                        ftDirectories = Boolean.valueOf(
                                                lineData[3].toLowerCase());
                                    } catch (Exception exi) {
                                    }

                                    // parse 4, search mask or list of files to remove
                                    for (int files = 4; files < lineData.length;
                                            files++) {
                                        String ftFilename
                                                = lineData[files].toLowerCase();

                                        // delete the files, collect result and
                                        // errors in string csv (filename result
                                        // error, ...)
                                    }
                                } else {
                                    // return error status to sender
                                    out.print(getStatusInvalidContent()
                                            + getRecordTerminator());
                                    out.flush();
                                }
                                break;
                            case "check":
                                if (lineData.length >= 4) {
                                    // parse 1, directory alias
                                    String ftDestinationPath
                                            = lineData[1].toLowerCase();

                                    // parse 2, binary/ascii option
                                    String ftType = lineData[2].toLowerCase();

                                    // parse 3, search mask or list of file names
                                    for (int files = 3; files < lineData.length;
                                            files++) {
                                        String ftFilename
                                                = lineData[files].toLowerCase();

                                        // get size and checksums for the files,
                                        // collect results and errors in string csv
                                        // (filename result error, ...)
                                    }
                                } else {
                                    // return error status to sender
                                    out.print(getStatusInvalidContent()
                                            + getRecordTerminator());
                                    out.flush();
                                }
                                break;
                            case "list":
                                if (lineData.length >= 6) {
                                    // parse 1, directory alias
                                    String ftDestinationPath
                                            = lineData[1].toLowerCase();

                                    // parse 2, recursive
                                    Boolean ftSubDirectories = false;
                                    try {
                                        ftSubDirectories = Boolean.valueOf(
                                                lineData[2].toLowerCase());
                                    } catch (Exception exi) {
                                    }

                                    // parse 3, directories only
                                    Boolean ftDirectoriesOnly = false;
                                    try {
                                        ftDirectoriesOnly = Boolean.valueOf(
                                                lineData[3].toLowerCase());
                                    } catch (Exception exi) {
                                    }

                                    // parse 4, depth of scan
                                    int ftDepth = 0;
                                    try {
                                        ftDepth = Integer.parseInt(
                                                lineData[4].toLowerCase());
                                    } catch (Exception exi) {
                                    }

                                    // parse 5, file search mask
                                    String ftSearchMask = lineData[5].toLowerCase();

                                    // call the utilities function to get the list of files
                                    // and return them to the client - this should be done
                                    // using events to limit the memory allocation
                                } else {
                                    // return error status to sender
                                    out.print(getStatusInvalidContent()
                                            + getRecordTerminator());
                                    out.flush();
                                }
                                break;
                        }

                        // increase total messages sent
                        increaseTotalMessagesSent();
                    }
                } catch (Exception ex) {
                    // increase total messages errored
                    increaseTotalMessagesErrored();

                    // try to return error status to sender, capture any 
                    // exceptions ignore them since the socket may have been 
                    // closed by the sender
                    try {
                        out.print(getStatusInvalidContent()
                                + getRecordTerminator());
                        out.flush();
                    } catch (Exception exi) {
                    }

                    // log error for tracking
                    logError(getClass().toString() + ", "
                            + getServiceConfig().getServiceName() + " on port "
                            + getServiceConfig().getConnectionPort() + ", "
                            + ex.getMessage());
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
    // </editor-fold>

    @Override
    public void checkConnection(AbstractConnection connection) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void checkConnections() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
