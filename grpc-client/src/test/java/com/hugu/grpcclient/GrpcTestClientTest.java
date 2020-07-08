package com.hugu.grpcclient;

import com.hugu.grpc.proto.GrpcTest.RequestType;
import com.hugu.grpc.proto.GrpcTest.ServiceResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.Before;
import org.junit.Test;


public class GrpcTestClientTest {

  private String serverAddress = "127.0.0.1:60051";

  private GrpcTestClient client;

  @Before
  public void setUp() {
    client = new GrpcTestClient(serverAddress,null);
  }

  @Test
  public void asyncHandleService() {
    List<CompletableFuture<ServiceResponse>> tasks = new ArrayList<>();
    for(int i=0;i<1000;i++){
      tasks.add(client.asyncHandleService("test",false,0, RequestType.ADD,new HashMap<>(),new ArrayList<>()));
    }
    CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()])).join();
  }
}
