package com.cs6650.consumer4;

import com.cs6650.datapublisher.RedisDataPublisher;
import com.cs6650.datapublisher.RedisSkierDataPublisher;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.apache.log4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.Map;


public class Consumer implements Runnable {
  static Logger log = Logger.getLogger(RedisSkierDataPublisher.class.getName());

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
      channel.exchangeDeclare(exchangeName, "fanout");
      channel.queueDeclare(queueName, true, false, false, null);
      channel.queueBind(queueName, exchangeName, "");
      log.info(" [*] Thread " + Thread.currentThread().getName() + " waiting for messages. To exit press CTRL+C");
      DeliverCallback deliverCallback = (consumerTag, delivery) -> {
        String record = new String(delivery.getBody(), StandardCharsets.UTF_8);
        Map<String, Object> headers = delivery.getProperties().getHeaders();
        redisDataPublisher.publishRecord(commands, headers, record);
        channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
      };
      channel.basicConsume(queueName, false, deliverCallback, consumerTag -> {
      });
    } catch (Exception e) {
      log.error(e);
    }
  }
}
