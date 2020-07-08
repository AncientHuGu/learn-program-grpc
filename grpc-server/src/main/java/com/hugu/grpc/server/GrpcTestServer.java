package com.hugu.grpc.server;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GrpcTestServer {

  private static final Logger logger = LogManager.getLogger(GrpcTestServer.class);
  private int serverPort;
  private int maxQueueSize;
  private Server server;

  public GrpcTestServer(int serverPort, int maxQueueSize) {
    this.serverPort = serverPort;
    this.maxQueueSize = maxQueueSize;
  }

  public void startServer() {
    try {
      this.server = ServerBuilder.forPort(serverPort)
          .addService(new GrpcTestService(maxQueueSize))
          .build();
      this.server.start();
      logger.info("server start, listen at port {}",serverPort);
    }catch (Exception ex) {
      logger.error("start server error.",ex);
    }
  }

  public void shutDown() {
    logger.info("Shut down GrpcHandleDetectedObjectService");
    this.server.shutdown();
  }

  public void awaitCompletion() throws InterruptedException {
    this.server.awaitTermination();
  }

}
