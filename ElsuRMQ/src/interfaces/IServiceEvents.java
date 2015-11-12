/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package interfaces;

import java.net.*;
import messages.*;

/**
 *
 * @author dhaliwal-admin
 */
public interface IServiceEvents {

    public void OnClientConnect(Socket client);

    public void OnClientDisconnect(Socket client);

    public void OnClientError(Socket client, Exception e);

    public void OnClientReceive(Socket client, AbstractBulletin packet);

    public void OnClientSend(Socket client, AbstractBulletin packet);
}
