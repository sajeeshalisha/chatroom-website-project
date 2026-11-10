
// This is example code provided to CSE3310 Spring 2026.
// You are free to use as is, or changed, any of the code provided

// Please comply with the licensing requirements for the
// open source packages being used.

// This code is based upon, and derived from the this repository
//            https:/thub.com/TooTallNate/Java-WebSocket/tree/master/src/main/example

// http server include is a GPL licensed package from
//            http://www.freeutils.net/source/jlhttp/

/*
 * Copyright (c) 2010-2020 Nathan Rajlich
 *
 *  Permission is hereby granted, free of charge, to any person
 *  obtaining a copy of this software and associated documentation
 *  files (the "Software"), to deal in the Software without
 *  restriction, including without limitation the rights to use,
 *  copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following
 *  conditions:
 *
 *  The above copyright notice and this permission notice shall be
 *  included in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 *  OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 *  HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 *  WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 *  FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 *  OTHER DEALINGS IN THE SOFTWARE.
 */

package uta.cse3310;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;


import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class App extends WebSocketServer {

  // should these message classes be in thier own file?
  // yes.
  // is there a class and some inheritance hidden in there?
  // yes.
  //////////////////////////////////////////////////

  // end of message classes
  //////////////////////////////////////////////////////////
  private int connectionId = 0;

  private final ClientManager clientManager = new ClientManager();

  private Instant startTime;

  private Statistics stats;

  private MessageHandler handler;

  public App(int port) {
    super(new InetSocketAddress(port));
  }

  public App(InetSocketAddress address) {
    super(address);
  }

  public App(int port, Draft_6455 draft) {
    super(new InetSocketAddress(port), Collections.<Draft>singletonList(draft));
  }

  @Override
  public void onOpen(WebSocket conn, ClientHandshake handshake) {

    connectionId++;
    clientManager.addClient(conn);

    System.out.println(conn.getRemoteSocketAddress().getAddress().getHostAddress() + " connected");

    handler.onClientConnected(conn, connectionId);

    // Update the running time
    stats.setRunningTime(Duration.between(startTime, Instant.now()).toSeconds());

  }

  @Override
  public void onClose(WebSocket conn, int code, String reason, boolean remote) {
    handler.onClientDisconnected(conn);
    clientManager.removeClient(conn);
    System.out.println(conn + " has closed");
    System.out.println("connected clients=" + clientManager.getClientCount());

  }

  @Override
  public void onMessage(WebSocket conn, String message) {
    System.out
        .println("< " + Duration.between(startTime, Instant.now()).toMillis() + " " + "-" + " " + escape(message));
    handler.onClientMessage(conn, message);

    // Bring in the data from the webpage
    // It can be a name, or a message.

    // Design Requirement: For every message recieved, there is one message sent to
    // every client. (at least one message sent to the client that started it,
    // perhaps a message to every client.)

    // Follow this rule, and the system will be responsive and always acknowledge
    // input.


    // Update the running time
    stats.setRunningTime(Duration.between(startTime, Instant.now()).toSeconds());


  }

  @Override
  public void onMessage(WebSocket conn, ByteBuffer message) {
    
  }

  @Override
  public void onError(WebSocket conn, Exception ex) {
    ex.printStackTrace();
    if (conn != null) {
      // some errors like port binding failed may not be assignable to a specific
      // websocket
    }
  }

  @Override
  public void onStart() {
    setConnectionLostTimeout(0);
    stats = new Statistics();
    startTime = Instant.now();
    handler = new MessageHandler(this, clientManager);
  }

  private String escape(String S) {
    if(S == null)return null;
    StringBuilder result = new StringBuilder(S.length());
    // turns " into \"
   // String retval = new String();
    // this routine is very slow.
    // but it is not called very often
    for (int i = 0; i < S.length(); i++) {
      Character ch = S.charAt(i);
      if (ch == '\"') {
        //retval = retval + '\\';
        result.append('\\');
      }
      //retval = retval + ch;
      result.append(ch);
    }
    //return retval;
    return result.toString();
  }

  public static void main(String[] args) {

    int port = 9050;
    // Set up the http server

    HttpServer H = new HttpServer(port, "./html");
    H.start();
    System.out.println("http Server started on port: " + port);

    // create and start the websocket server

    port = 9051;

    App A = new App(port);
    A.setReuseAddr(true);
    A.start();
    System.out.println("websocket Server started on port: " + port);

  }
}
