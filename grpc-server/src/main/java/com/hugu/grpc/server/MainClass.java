package com.hugu.grpc.server;

public class MainClass {
  public static void main(String [] args) {
    GrpcTestServer server = new GrpcTestServer(60051,10000);
    server.startServer();
  }
}
