package controller;

// Create a thread that gets output from the server and displays
// it to the user. We use a separate thread for this so that we
// can receive asynchronous output
public class ClientReceiver extends Thread {

    public ClientSender _sender = null;

    public ClientReceiver(ClientSender sender) {
        _sender = sender;
    }

    @Override
    public void run() {
        char[] buffer = new char[1024];
        int chars_read;
        
        try {
            // Read characters until the stream closes
            while ((chars_read = _sender._fromServer.read(buffer)) != -1) {
                // Loop through the array of characters, and
                // print them out, converting all \n characters
                // to the local platform's line terminator.
                // This could be more efficient, but it is probably
                // faster than the network is, which is good enough
                for (int i = 0; i < chars_read; i++) {
                    if (buffer[i] == '\n') {
                        _sender._toUser.println();
                    } else {
                        _sender._toUser.print(buffer[i]);
                    }
                }
                
                _sender._toUser.flush();
            }
        } catch (Exception ex){
            _sender._toUser.println(ex);
        } finally {
            // When the server closes the connection, the loop above
            // will end. Tell the user what happened, and call
            // System.exit(), causing the main thread to exit along
            // with this one.
            _sender._toUser.println("Connection closed by server.");

            _sender._fromServer = null;
            _sender._toUser = null;

            _sender.interrupt();
            System.exit(0);
        }
    }
}
