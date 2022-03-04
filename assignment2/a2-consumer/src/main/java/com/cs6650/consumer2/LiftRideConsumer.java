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

  public LiftRideConsumer(String queueName, int numThreads) {
    this.queueName = queueName;
    this.numThreads = numThreads;

    factory = new ConnectionFactory();
    factory.setHost("localhost");
    factory.setUsername("user");
    factory.setPassword("123");
  }

  public void startListening() {
    ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
    Map<String, List<String>> recordRepository = new ConcurrentHashMap<>();

    try (Connection connection = factory.newConnection()) {
      for (int i = 0; i < numThreads; i++) {
        executorService.execute(new Consumer(queueName, connection.createChannel(), recordRepository));
      }
    } catch (IOException | TimeoutException e) {
      e.printStackTrace();
    }

    executorService.shutdown();
    while (!executorService.isTerminated()) {
      try {
        executorService.awaitTermination(1, TimeUnit.HOURS);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  public static void main(String[] args) {
    String queueName = args[0];
    int numThreads = Integer.parseInt(args[1]);
    LiftRideConsumer liftRideConsumer = new LiftRideConsumer(queueName, numThreads);
    liftRideConsumer.startListening();
  }
}
