package com.hugu.grpcclient;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.hugu.grpc.proto.GrpcTest;
import com.hugu.grpc.proto.GrpcTest.AddRequest;
import com.hugu.grpc.proto.GrpcTest.QueryRequest;
import com.hugu.grpc.proto.GrpcTest.RequestType;
import com.hugu.grpc.proto.GrpcTest.ServiceInput;
import com.hugu.grpc.proto.GrpcTest.ServiceResponse;
import com.hugu.grpc.proto.ServiceTaskGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.NameResolver;
import io.grpc.internal.DnsNameResolverProvider;
import io.grpc.util.RoundRobinLoadBalancerFactory;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

public class GrpcTestClient {

  private static final Logger logger = LogManager.getLogger(GrpcTestClient.class);

  private ManagedChannel channel;

  private ExecutorService service;

  private ServiceTaskGrpc.ServiceTaskFutureStub stub;

  public GrpcTestClient(String serverAddress, NameResolver.Factory nameResolverFactory) {
    this(ManagedChannelBuilder.forTarget(serverAddress)
        .nameResolverFactory(nameResolverFactory == null ? new DnsNameResolverProvider(): nameResolverFactory)
        .loadBalancerFactory(RoundRobinLoadBalancerFactory.getInstance())
        .usePlaintext()
        .build());
    int core = Runtime.getRuntime().availableProcessors();
    this.service = Executors.newFixedThreadPool(core);
  }

  private GrpcTestClient(ManagedChannel channel) {
    this.channel = channel;
    stub = ServiceTaskGrpc.newFutureStub(channel);
  }

  public CompletableFuture<ServiceResponse> asyncHandleService(String userName, boolean isAdd, int code, RequestType requestType, Map<String, String> inputMap, List<String> ids) {
    CompletableFuture<ServiceResponse> handleServiceFuture = new CompletableFuture<>();
    ServiceInput.Builder inputBuilder = ServiceInput.newBuilder();
    inputBuilder.setUserName(userName);
    inputBuilder.setIsAdd(isAdd);
    inputBuilder.setCode(code);
    inputBuilder.setRequestType(requestType);
    inputBuilder.putAllInputParams(inputMap);
    AddRequest addRequest = AddRequest.newBuilder().addAllAddInfos(ids).build();
    inputBuilder.setAddRequest(addRequest);
    ListenableFuture<ServiceResponse> resultFuture = stub.handleService(inputBuilder.build());
    Futures.addCallback(resultFuture, new FutureCallback<ServiceResponse>() {
      @Override
      public void onSuccess(@NullableDecl ServiceResponse serviceResponse) {
        handleServiceFuture.complete(serviceResponse);
      }

      @Override
      public void onFailure(Throwable throwable) {
        handleServiceFuture.completeExceptionally(throwable);
      }
    },service);
    return handleServiceFuture;
  }


  public void shutdown(int shutDownTime) throws InterruptedException {
    if (null != service) {
      service.shutdown();
    }
    service = null;
    if (null != channel) {
      channel.shutdown().awaitTermination(shutDownTime, TimeUnit.MILLISECONDS);
    }
    channel = null;
    stub = null;
  }
}
