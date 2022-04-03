package com.cs6650.consumer3;

import com.rabbitmq.client.*;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.support.AsyncPool;

import java.io.IOException;

public class Consumer implements Runnable {
  private final String queueName;
  private final Connection rabbitMQConnection;
  private final AsyncPool<StatefulRedisConnection<String, String>> redisConnectionAsyncPool;

  public Consumer(String queueName, Connection rabbitMQConnection, AsyncPool<StatefulRedisConnection<String, String>> redisConnectionAsyncPool) {
      this.queueName = queueName;
      this.rabbitMQConnection = rabbitMQConnection;
      this.redisConnectionAsyncPool = redisConnectionAsyncPool;
    }

  @Override
  public void run() {
    try {
      final Channel channel = rabbitMQConnection.createChannel();
      channel.queueDeclare(queueName, true, false, false, null);
      System.out.println(" [*] Thread " + Thread.currentThread().getName() + " waiting for messages. To exit press CTRL+C");
      DeliverCallback deliverCallback = (consumerTag, delivery) -> {
        Object skierID = delivery.getProperties().getHeaders().get("skierID");
        String record = new String(delivery.getBody(), "UTF-8");
        channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        redisConnectionAsyncPool.acquire().thenCompose(connection -> {
          RedisAsyncCommands<String, String> async = connection.async();
          async.rpush(skierID.toString(), record);
          return async.exec().whenComplete((s, throwable) -> redisConnectionAsyncPool.release(connection));
        });
      };
      channel.basicConsume(queueName, false, deliverCallback, consumerTag -> { });
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
