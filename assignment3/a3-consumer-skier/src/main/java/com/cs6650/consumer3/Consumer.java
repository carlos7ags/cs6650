package com.cs6650.consumer3;

import com.cs6650.datapublisher.RedisDataPublisher;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

import java.nio.charset.StandardCharsets;
import java.util.Map;


public class Consumer implements Runnable {
  private final String exchangeName;
  private final String queueName;
  private final Connection rabbitMQConnection;
  private final StatefulRedisConnection<String, String> redisConnection;
  private final RedisDataPublisher redisDataPublisher;

  public Consumer(String exchangeName, String queueName, Connection rabbitMQConnection, StatefulRedisConnection<String, String> redisConnection, RedisDataPublisher redisDataPublisher) {
    this.exchangeName = exchangeName;
    this.queueName = queueName;
    this.rabbitMQConnection = rabbitMQConnection;
    this.redisConnection = redisConnection;
    this.redisDataPublisher = redisDataPublisher;
  }

  @Override
  public void run() {
    try {
      Channel channel = rabbitMQConnection.createChannel();
      RedisCommands<String, String> commands = redisConnection.sync();
      channel.queueDeclare(queueName, true, false, false, null);
      channel.queueBind(queueName, exchangeName, "");
      System.out.println(" [*] Thread " + Thread.currentThread().getName() + " waiting for messages. To exit press CTRL+C");
      DeliverCallback deliverCallback = (consumerTag, delivery) -> {
        String record = new String(delivery.getBody(), StandardCharsets.UTF_8);
        Map<String, Object> headers = delivery.getProperties().getHeaders();
        redisDataPublisher.publishRecord(commands, headers, record);
        channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
      };
      channel.basicConsume(queueName, false, deliverCallback, consumerTag -> {
      });
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
