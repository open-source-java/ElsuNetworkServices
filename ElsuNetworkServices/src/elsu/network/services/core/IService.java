package elsu.network.services.core;

import elsu.network.services.core.AbstractConnection;
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

    public ServiceConfig getServiceConfig();

    public boolean isRunning();

    public void start() throws Exception;

    public void shutdown();
    
    public void logDebug(Object obj);

    public void logError(Object obj);

    public void logInfo(Object obj);

    public void addConnection(Socket socket,
            AbstractConnection connection) throws Exception;

    public ThreadGroup getThreadGroup();

    public void removeConnection(AbstractConnection conn);

    public void serve(AbstractConnection conn) throws Exception;
}
