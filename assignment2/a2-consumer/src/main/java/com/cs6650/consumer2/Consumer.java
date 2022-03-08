package com.cs6650.consumer2;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Consumer implements Runnable {
  private final String queueName;
  private final Connection connection;
  public Map<String, List<String>> recordRepository;

  public Consumer(String queueName, Connection connection, Map<String, List<String>> recordRepository) {
      this.queueName = queueName;
      this.connection = connection;
      this.recordRepository = recordRepository;
    }

  @Override
  public void run() {
    try {
      final Channel channel = connection.createChannel();
      channel.queueDeclare(queueName, true, false, false, null);
      System.out.println(" [*] Thread " + Thread.currentThread().getName() + " waiting for messages. To exit press CTRL+C");
      DeliverCallback deliverCallback = (consumerTag, delivery) -> {
        Object skierID = delivery.getProperties().getHeaders().get("skierID");
        String record = new String(delivery.getBody(), "UTF-8");
        channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        recordRepository.compute(skierID.toString(), (k, v) -> {
          List<String> currentValues = v;
          if(currentValues == null)
            currentValues = new ArrayList<>();
          currentValues.add(record);
          return currentValues;
        });
      };
      channel.basicConsume(queueName, false, deliverCallback, consumerTag -> { });
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
