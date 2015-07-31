/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package elsu.network.services;

import elsu.network.service.factory.*;
import java.io.*;
import java.net.*;
import java.util.*;

/**
 * IServiceInternal interface exports the common functions of the Service class. 
 * This is internal interface and should not be used to derive new services
 *
 * @author Seraj Dhaliwal (seraj.s.dhaliwal@uscg.mil)
 */
public interface IServiceInternal {

    public void addConnection(AbstractServiceConnection connection) throws
            Exception;

    public void checkConnection(AbstractServiceConnection conn);

    public void checkConnections();

    public void decreaseActiveConnections();

    public void decreaseServiceConnections();

    public int getActiveConnections();

    public Map getApplicationProperties();

    public Set getConnections();

    public ServiceConfig getChildConfig();

    public ServiceFactory getFactory();

    public int getMaximumConnections();

    public int getServiceConnections();

    public ServiceListener getListener();

    public Map getServiceProperties();

    public void increaseActiveConnections();

    public void increaseServiceConnections();

    public void setMaximumConnections(int allowedMax);

    public void toString(PrintWriter out);
    
}
