package com.cs6650.consumer2;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class LiftRideConsumer {
  private final int numThreads;
  private final String queueName;
  private final ConnectionFactory factory;
  private final ExecutorService executorService;

  private Connection connection = null;

  public final Map<String, List<String>> recordRepository;

  public LiftRideConsumer(String queueName, int numThreads, ConnectionFactory factory) {
    this.queueName = queueName;
    this.numThreads = numThreads;
    this.factory = factory;
    this.executorService = Executors.newFixedThreadPool(numThreads);
    this.recordRepository = new ConcurrentHashMap<>();
  }

  public void startListening() {
    try {
      this.connection = factory.newConnection(executorService);
      for (int i = 0; i < numThreads; i++) {
        executorService.execute(new Consumer(queueName, connection, recordRepository));
      }
    } catch (IOException | TimeoutException e) {
      e.printStackTrace();
    }
  }


  public void stopListening() {
    if (this.connection != null && this.connection.isOpen()) {
      try {
        this.connection.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    executorService.shutdown();
  }


  public static void main(String[] args) throws InterruptedException {
    String queueName = args[0];
    int numThreads = Integer.parseInt(args[1]);
    int timeout = Integer.parseInt(args[2]);

    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost(System.getenv("RABBITMQ_HOST"));
    factory.setUsername(System.getenv("RABBITMQ_USER"));
    factory.setPassword(System.getenv("RABBITMQ_PASSWORD"));

    LiftRideConsumer liftRideConsumer = new LiftRideConsumer(queueName, numThreads, factory);
    liftRideConsumer.startListening();
    TimeUnit.MINUTES.sleep(timeout);
    liftRideConsumer.stopListening();
  }
}
