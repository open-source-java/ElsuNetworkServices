package elsu.network.services;

import elsu.network.service.factory.*;
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
            AbstractServiceConnection connection) throws Exception;

    public ThreadGroup getThreadGroup();

    public void removeConnection(AbstractServiceConnection conn);

    public void serve(InputStream iStream, OutputStream oStream) throws
            Exception;

    public void serve(AbstractServiceConnection conn) throws Exception;
}
