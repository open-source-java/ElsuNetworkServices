package elsu.network.services.core;

import elsu.network.services.*;
import java.io.*;
import java.net.*;

/**
 * IService interface exports the common functions of the Service class allowing
 * the all service derived objects to be shared across modules.
 *
 * @author Seraj Dhaliwal (seraj.s.dhaliwal@uscg.mil)
 */
public interface IService {

    void addConnection(AbstractConnection connection) throws Exception;

    void addConnection(Socket socket,
            AbstractConnection connection) throws Exception;
        
    void checkConnection(AbstractConnection conn);

    void checkConnections();

    ServiceConfig getChildConfig();

    ServiceListener getListener();

    ServiceConfig getServiceConfig();

    ThreadGroup getThreadGroup();

    boolean isRunning();
    
    void logDebug(Object obj);

    void logError(Object obj);

    void logInfo(Object obj);

    void removeConnection(AbstractConnection conn);

    void start() throws Exception;

    void shutdown();

    void serve(AbstractConnection conn) throws Exception;

    void toString(PrintWriter out);
}
