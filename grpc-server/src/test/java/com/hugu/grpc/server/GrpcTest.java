package com.hugu.grpc.server;

public class GrpcTest {

  public static void main(String [] args) throws InterruptedException {
    GrpcTestServer grpcTestServer = new GrpcTestServer(60051, 1000);
    grpcTestServer.startServer();

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      grpcTestServer.shutDown();
    }));

    grpcTestServer.awaitCompletion();
  }
}
