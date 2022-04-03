package com.cs6650.consumer3;

import com.rabbitmq.client.*;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.apache.commons.pool2.impl.GenericObjectPool;

import java.util.ArrayList;
import java.util.List;

public class Consumer implements Runnable {
  private final String queueName;
  private final Connection rabbitMQConnection;
  private final GenericObjectPool<StatefulRedisConnection<String, String>> redisConnectionPool;
  private RedisCommands<String, String> commands;
  StatefulRedisConnection<String, String> connection;

  public Consumer(String queueName, Connection rabbitMQConnection, GenericObjectPool<StatefulRedisConnection<String, String>> redisConnectionPool) {
      this.queueName = queueName;
      this.rabbitMQConnection = rabbitMQConnection;
      this.redisConnectionPool = redisConnectionPool;
    }

  @Override
  public void run() {
    try {
      final Channel channel = rabbitMQConnection.createChannel();
      channel.queueDeclare(queueName, true, false, false, null);
      connection = redisConnectionPool.borrowObject();
      commands = connection.sync();
      System.out.println(" [*] Thread " + Thread.currentThread().getName() + " waiting for messages. To exit press CTRL+C");
      DeliverCallback deliverCallback = (consumerTag, delivery) -> {
        Object skierID = delivery.getProperties().getHeaders().get("skierID");
        Object day = delivery.getProperties().getHeaders().get("day");
        String record = new String(delivery.getBody(), "UTF-8");
        channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        publishRecordToRedis(skierID, day, record);
      };
      channel.basicConsume(queueName, false, deliverCallback, consumerTag -> { });
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private synchronized void publishRecordToRedis(Object skierID, Object day, String record) {
    String currentRecordsList = commands.hget(skierID.toString(), day.toString());
    if (currentRecordsList != null) {
      commands.hset(skierID.toString(), day.toString(), addRecord(currentRecordsList, record));
    } else {
      List<String> recordsList = new ArrayList<>();
      recordsList.add(record);
      commands.hset(skierID.toString(), day.toString(), recordsList.toString());
    }
  }

  private String addRecord(String listOfRecords, String record) {
    return listOfRecords.substring(0, listOfRecords.length() - 1) + ", " + record + "]";
  }
}
