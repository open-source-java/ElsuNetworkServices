package elsu.network.services;

import elsu.network.service.factory.ServiceFactory;
import java.io.*;
import java.net.*;
import java.util.*;

/**
 * IService interface exports the common functions of the Service class allowing
 * the all service derived objects to be shared across modules.
 *
 * @author Seraj Dhaliwal (seraj.s.dhaliwal@uscg.mil)
 */
public interface IService {

    public void addConnection(ServiceConnectionAbstract connection) throws
            Exception;

    public void addConnection(Socket socket,
            ServiceConnectionAbstract connection) throws Exception;

    public void checkConnection(ServiceConnectionAbstract conn);

    public void checkConnections();

    public void decreaseActiveConnections();

    // factory method
    public void decreaseServiceConnections();

    public int getActiveConnections();

    // factory method
    public Map getApplicationProperties();

    public Set getConnections();

    public ServiceConfig getChildConfig();

    public ServiceFactory getFactory();

    // factory method
    public int getMaximumConnections();

    public ServiceConfig getServiceConfig();

    // factory method
    public int getServiceConnections();

    public ServiceListener getListener();

    public Map getServiceProperties();

    public ThreadGroup getThreadGroup();

    public void increaseActiveConnections();

    // factory method
    public void increaseServiceConnections();

    public boolean isRunning();

    // factory method
    public void logDebug(Object obj);

    // factory method
    public void logError(Object obj);

    // factory method
    public void logInfo(Object obj);

    public void removeConnection(ServiceConnectionAbstract conn);

    public void serve(InputStream iStream, OutputStream oStream) throws
            Exception;

    public void serve(ServiceConnectionAbstract conn) throws Exception;

    // factory method
    public void setMaximumConnections(int allowedMax);

    public void shutdown();

    // service metghods
    public void start() throws Exception;

    public void toString(PrintWriter out);
}
