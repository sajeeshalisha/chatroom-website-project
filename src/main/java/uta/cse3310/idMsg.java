 package uta.cse3310;

public class idMsg {
    // this message is sent to the client that
    // just opened up a connection
    String msgType = "idMsg";
    int connectionId;

    public idMsg(int c) {
      connectionId = c;
    }
  }