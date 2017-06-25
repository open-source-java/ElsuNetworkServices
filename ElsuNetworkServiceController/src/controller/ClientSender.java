/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package controller;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 *
 * @author dhaliwal-admin
 */
public class ClientSender extends Thread {

    public Socket _socket = null;
    // Set up streams for reading from and writing to the server.
    public Reader _fromServer = null;
    public PrintWriter _toServer = null;
    // The to_user stream is final for use in the anonymous class below
    public BufferedReader _fromUser = null;
    public PrintWriter _toUser = null;
    public ClientReceiver _receiver = null;

    public ClientSender(String host, int port) {
        try {
            // Connect to the specified host and port
            _socket = new Socket(host, port);

            // Set up streams for reading from and writing to the server.
            // The from_server stream is final for use in the inner class below
            _fromServer = new InputStreamReader(_socket.getInputStream());
            _toServer = new PrintWriter(_socket.getOutputStream());

            // Set up streams for reading from and writing to the console
            // The to_user stream is final for use in the anonymous class below
            _fromUser = new BufferedReader(new InputStreamReader(System.in));

            // Pass true for auto-flush on println()
            _toUser = new PrintWriter(System.out, true);

            // Tell the user that we've connected
            _toUser.println("Connected to " + _socket.getInetAddress()
                    + ":" + _socket.getPort());

            // create receiver and activate it
            _receiver = new ClientReceiver(this);

            // We set the priority of the server-to-user thread above to be
            // one level higher than the main thread. We shouldn't have to do
            // this, but on some operating systems, output sent to the console
            // doesn't appear when a thread at the same priority level is
            // blocked waiting for input from the console.
            _receiver.setPriority(Thread.currentThread().getPriority() + 1);

            // Now start the server-to-user thread
            _receiver.start();
        } catch (Exception ex){
            System.err.println(ex);
            System.err.println("Usage: java GenericClient <hostname> <port>");
        }
    }

    @Override
    public void run() {
        try {
            // In parallel, read the user's input and pass it on to the server.
            String line = "";

            while ((line = _fromUser.readLine()) != null) {
                _toServer.print(line + "\n");
                _toServer.flush();
            }

//            // with port 5500 file transfer service
//            _toServer.print("put|ascii|false|M:\\Temp\\dataLogger\\outgoing|Import_MSG.txt|2813511" + "\n");
//            _toServer.flush();
//
//            // checksum calculation for bytes read from file
//            MessageDigest digest = MessageDigest.getInstance("MD5");
//
//            // in parallel, read the file and send it to client
//            FileChannelTextReader tr = new FileChannelTextReader("201_2014051813_ALM.txt", "M:\\Temp\\dataLogger\\outgoing\\hold");
//            while (true) {
//                String result = tr.readline();
//
//                if (tr.isReaderValid()) {
//                    if (tr.isEndOfFile()) {
//                        _toServer.print("*" + result + CoreUtilities.NEWLINE);
//                        _toServer.flush();
//
//                        // perform calculation for md5
//                        digest.update(result.getBytes());
//                    } else {
//                        _toServer.print("+" + result + CoreUtilities.NEWLINE);
//                        _toServer.flush();
//
//                        // perform calculation for md5
//                        digest.update(result.getBytes());
//                        digest.update(CoreUtilities.NEWLINE.getBytes());
//                    }
//
//                } else {
//                    break;
//                }
//            }
//
//            System.out.println(Hex.encodeHexString(digest.digest()));
            Random rand = new Random();
            String sThreadID = //Long.toString(this.getId());
                    Integer.toString(rand.nextInt());

            // If the user types a Ctrl-D (Unix) or Ctrl-Z (Windows) to end
            // their input, we'll get an EOF, and the loop above will exit.
            // When this happens, we stop the server-to-user thread and close
            // the socket.
            _toUser.println("Connection closed by client.");
        } catch (Exception ex){
        } finally {
            _receiver.interrupt();
            System.exit(0);
        }
    }

}
