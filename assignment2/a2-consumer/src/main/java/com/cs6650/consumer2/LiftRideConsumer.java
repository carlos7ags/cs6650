package com.cs6650.consumer2;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.io.IOException;
import java.util.ArrayList;
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

    try (Connection connection = factory.newConnection(executorService)) {

      for (int i = 0; i < numThreads; i++) {
        executorService.execute(new Consumer(queueName, connection, recordRepository));
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

  public static void main(String[] args) throws IOException, TimeoutException {
    String queueName = args[0];
    int numThreads = Integer.parseInt(args[1]);
    Map<String, List<String>> recordRepository = new ConcurrentHashMap<>();

    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("localhost");
    factory.setUsername("user");
    factory.setPassword("123");
    Connection connection = factory.newConnection();

    Runnable runnable = new Runnable() {
      @Override
      public void run() {
        try {
          final Channel channel = connection.createChannel();
          channel.queueDeclare(queueName, true, false, false, null);
          // max one message per receiver
          channel.basicQos(1);
          System.out.println(" [*] Thread waiting for messages. To exit press CTRL+C");

          DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            Object skierID = delivery.getProperties().getHeaders().get("skierID");
            String record = new String(delivery.getBody(), "UTF-8");
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            recordRepository.compute(skierID.toString(), (k, v) -> {
              List<String> vals = v;
              if(vals == null)
                vals = new ArrayList<>();
              vals.add(record);
              return vals;
            });
          };
          // process messages
          channel.basicConsume(queueName, false, deliverCallback, consumerTag -> { });
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    };
    // start threads and block to receive messages
    Thread recv1 = new Thread(runnable);
    Thread recv2 = new Thread(runnable);
    Thread recv3 = new Thread(runnable);
    Thread recv4 = new Thread(runnable);
    recv1.start();
    recv2.start();
    recv3.start();
    recv4.start();



  }
}
