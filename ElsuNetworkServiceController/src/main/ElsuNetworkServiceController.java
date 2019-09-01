package main;

import controller.*;

/**
 * This program connects to a server at a specified host and port. It reads text
 * from the console and sends it to the server. It reads text from the server
 * and sends it to the console.
 *
 * java -jar DataClient.jar 127.0.0.1 3001 -- control port java -jar
 * DataClient.jar 127.0.0.1 3004 -- message service port
 */
public class ElsuNetworkServiceController {

    public static int _maxThreads = 80;
    public static int _maxRecords = 7500;
    public static int _maxDays = 15;

    public static void main(String[] args) {
        try {
            // Check the number of arguments
            if (args.length != 2) {
                throw new IllegalArgumentException("Wrong number of args");
            }

            // Parse the host and port specifications
            final String host = args[0];
            final int port = Integer.parseInt(args[1]);

            // initialize the client and activate it
            ElsuNetworkServiceController client
                    = new ElsuNetworkServiceController();
            client.startThread(host, port);
        } // If anything goes wrong, print an error message
        catch (Exception ex){
            System.err.println(ex);
            System.err.println("Usage: java GenericClient <hostname> <port>");
        }
    }

    // fts, 5500, get|M:\Temp\dataLogger\outgoing|201_2014061019_MSG.txt|ascii
    // fts, 5500, put|append|M:\Temp\dataLogger\outgoing|201_2014061019_MSG.txt|ascii|2100
    // add bcsMessageService bcsMessageService2 5001 service.site.name=ECO03TMP service.site.id=306 sub:4033:service.hostUri=172.16.28.33 sub:4034:service.hostUri=172.16.28.34 sub:4035:service.hostUri=172.16.28.35 sub:4036:service.hostUri=172.16.28.36 sub:4037:service.hostUri=172.16.28.37
    // add xml <?xml version="1.0" encoding="UTF-8"?><service name="bcsMessageService"><port>5000</port><class>elsu.network.services.client.bcs.BcsMessageService</class><startupType>DELAYEDSTART</startupType><serviceType>CLIENT</serviceType><ignoreConnectionLimit>true</ignoreConnectionLimit><maxConnections>10</maxConnections><attributes><key name="service.localStore.directory">M:\Temp\dataLogger\</key><key name="service.localStore.mask">%s_%s_%s.txt</key><key name="service.listener">true</key><key name="service.site.name">EC03RCA</key><key name="service.site.id">307</key><key name="service.parser.field.name">message</key><key name="service.parser.field.delimiter">,</key><key name="service.parser.field.index">1</key><key name="service.parser.field.length">10</key><key name="service.parser.field.values">5,7,9,12,17,25,38</key></attributes></service>
    public void startThread(String host, int port) {
        // create socket
        ClientSender sender = new ClientSender(host, port);

        // run the current thread
        sender.start();
    }
}
