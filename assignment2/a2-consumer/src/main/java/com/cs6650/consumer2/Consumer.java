package com.cs6650.consumer2;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class Consumer implements Runnable {
  private final String queueName;
  private final Channel channel;
  public Map<String, List<String>> recordRepository;

  public Consumer(String queueName, Channel channel, Map<String, List<String>> recordRepository) {
      this.queueName = queueName;
      this.channel = channel;
      this.recordRepository = recordRepository;
    }

  @Override
  public void run() {
    try {
      DefaultConsumer consumer = new HashMapConsumer(channel, recordRepository);
      channel.basicConsume(queueName, true, consumer);
      channel.close();
    } catch (IOException | TimeoutException e) {
      e.printStackTrace();
    }
  }
}
