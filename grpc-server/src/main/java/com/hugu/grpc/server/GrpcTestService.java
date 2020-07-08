package com.hugu.grpc.server;

import com.hugu.grpc.proto.GrpcTest.ServiceInput;
import com.hugu.grpc.proto.GrpcTest.ServiceResponse;
import com.hugu.grpc.proto.ServiceTaskGrpc.ServiceTaskImplBase;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GrpcTestService extends ServiceTaskImplBase {

  private static Logger logger = LogManager.getLogger(GrpcTestService.class);

  //使用线程池处理任务
  private ThreadPoolExecutor executorService;

  private static final int MAX_CAPACITY = 10000;

  public GrpcTestService(int capacity) {
    if(capacity > MAX_CAPACITY) {
      capacity = MAX_CAPACITY;
    }
    //获取当前运行环境CPU核数
    int cores = Runtime.getRuntime().availableProcessors();
    BlockingQueue taskQueue = new LinkedBlockingQueue(capacity);
    //初始化线程池设置
    executorService = new ThreadPoolExecutor(
        1,              //线程池基本线程数量  在线程队列taskQueue没有满之前是不会去创建新的线程到线程池中的，如果taskQueue中的任务已经满了，同时当前线程池中线程的数量<maximumPoolSize，则会创建新的线程
        2*cores,    //线程池中最大线程数量  如果当前线程池中线程池数量已经到了maximumPoolSize，同时taskQueue队列也满了，则表示线程池无法处理任务，要使用reject策略
        60L,
        TimeUnit.SECONDS,
        taskQueue
    );

    //设置reject拒绝策略
    executorService.setRejectedExecutionHandler((r, executor) -> {
      if(r instanceof InnerTask) {
        //执行任务定义的reject方法
        ((InnerTask) r).handleReject();
      }
    });
  }


  /**
   * GrpcServer绑定方法
   * @param request  （入参）
   * @param responseObserver （返回回调函数）
   */
  @Override
  public void handleService(ServiceInput request,
      StreamObserver<ServiceResponse> responseObserver) {
    //当调用GRPCServer方法时，构造任务并丢进任务队列中执行
    InnerTask innerTask = new InnerTask(request,responseObserver);
    executorService.execute(innerTask);
  }

  /**
   * 线程池运行Task
   */
  public class InnerTask implements Runnable {

    private ServiceInput request;
    private StreamObserver<ServiceResponse> responseObserver;
    public InnerTask(ServiceInput request, StreamObserver<ServiceResponse> responseObserver) {
      this.request = request;
      this.responseObserver = responseObserver;
    }
    @Override
    public void run() {
      //运行业务的函数
      logger.info("start to handle task");
      try {
        int code = (int) (Math.random() * 100);
        if(code == 20) {
          //模拟任务运行过程中出现异常
          throw new RuntimeException("handle task error");
        }
        //模拟业务正常运行耗时500毫秒
        Thread.sleep(500);
        logger.info("success handle task");
        ServiceResponse response = ServiceResponse.newBuilder()
            .setCode(0)
            .setMessage("success").build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
      }catch (Exception ex) {
        ServiceResponse response = ServiceResponse.newBuilder()
            .setCode(999)
            .setMessage("fail").build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
      }
    }

    //定义reject方法
    public void handleReject() {
      ServiceResponse response = ServiceResponse.newBuilder()
          .setCode(555)
          .setMessage("task has been rejected.").build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    }
  }
}
