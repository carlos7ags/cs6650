package com.cs6650.consumer2;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

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
      Channel channel = connection.createChannel();

      channel.basicQos(100);
      System.out.println(" [*] Thread waiting for messages. To exit press CTRL+C");
      channel.queueDeclare(queueName, false, false, false, null);

      DeliverCallback deliverCallback = (consumerTag, delivery) -> {
        String skierID = (String) delivery.getProperties().getHeaders().get("skierID");
        String record = new String(delivery.getBody(), "UTF-8");
        channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        System.out.println( "Callback thread ID = " + Thread.currentThread().getId() + " Received '" + record + "'" + " id '" + skierID + "'");
        recordRepository.compute(skierID, (k, v) -> {
          List<String> vals = v;
          if(vals == null)
            vals = new ArrayList<>();
          vals.add(record);
          return vals;
        });
      };
      channel.basicConsume(queueName, false, deliverCallback, consumerTag -> { });
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
