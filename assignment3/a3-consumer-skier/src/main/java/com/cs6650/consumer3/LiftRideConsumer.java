package com.cs6650.consumer3;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.support.*;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.io.IOException;
import java.util.concurrent.*;

public class LiftRideConsumer {
  private final int numThreads;
  private final String queueName;

  private ExecutorService executorService;
  private Connection rabbitMQConnection = null;
  private GenericObjectPool<StatefulRedisConnection<String, String>> redisConnectionPool;
  private RedisClient redisClient;

  public LiftRideConsumer(String queueName, int numThreads) {
    this.queueName = queueName;
    this.numThreads = numThreads;
  }

  public void startListening() {
    executorService = Executors.newFixedThreadPool(numThreads);
    redisConnectionPool = createRedisConnectionPool();
    ConnectionFactory rabbitMQConnectionFactory = createRabbitMQConnectionFactory();

    try {
      rabbitMQConnection = rabbitMQConnectionFactory.newConnection(executorService);
      for (int i = 0; i < numThreads; i++) {
        executorService.execute(new Consumer(queueName, rabbitMQConnection, redisConnectionPool.borrowObject()));
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void stopListening() {
    if (rabbitMQConnection != null && rabbitMQConnection.isOpen()) {
      try {
        rabbitMQConnection.close();
        redisConnectionPool.close();
        redisClient.shutdown();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    executorService.shutdown();
  }

  private GenericObjectPool<StatefulRedisConnection<String, String>> createRedisConnectionPool() {
    String host = System.getenv("REDIS_HOST");
    int port = Integer.parseInt(System.getenv("REDIS_PORT"));
    redisClient = RedisClient.create(RedisURI.create(host, port));
    GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
    poolConfig.setMaxTotal(numThreads);
    return ConnectionPoolSupport.createGenericObjectPool(redisClient::connect, poolConfig);
  }

  private ConnectionFactory createRabbitMQConnectionFactory() {
    ConnectionFactory rabbitMQConnectionfactory = new ConnectionFactory();
    rabbitMQConnectionfactory.setHost(System.getenv("RABBITMQ_HOST"));
    rabbitMQConnectionfactory.setUsername(System.getenv("RABBITMQ_USER"));
    rabbitMQConnectionfactory.setPassword(System.getenv("RABBITMQ_PASSWORD"));
    return rabbitMQConnectionfactory;
  }

  public static void main(String[] args) throws InterruptedException {
    String queueName = args[0];
    int numThreads = Integer.parseInt(args[1]);
    int timeout = Integer.parseInt(args[2]);

    LiftRideConsumer liftRideConsumer = new LiftRideConsumer(queueName, numThreads);
    liftRideConsumer.startListening();
    TimeUnit.MINUTES.sleep(timeout);
    liftRideConsumer.stopListening();
  }
}
