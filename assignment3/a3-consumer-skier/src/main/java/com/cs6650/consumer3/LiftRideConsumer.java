package com.cs6650.consumer3;

import com.cs6650.datapublisher.RedisDataPublisher;
import com.cs6650.datapublisher.RedisResortDataPublisher;
import com.cs6650.datapublisher.RedisSkierDataPublisher;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.support.ConnectionPoolSupport;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class LiftRideConsumer {
  private ExecutorService executorService;
  private Connection rabbitMQConnection;
  private GenericObjectPool<StatefulRedisConnection<String, String>> redisConnectionPool;
  private RedisClient redisClient;

  public static void main(String[] args) throws InterruptedException {
    String exchangeName = args[0];
    String queueName = args[1];
    int numThreads = Integer.parseInt(args[2]);
    int redisDatabase = Integer.parseInt(args[3]);
    int timeout = Integer.parseInt(args[4]);

    LiftRideConsumer liftRideConsumer = new LiftRideConsumer();
    liftRideConsumer.startListening(exchangeName, queueName, redisDatabase, numThreads);
    TimeUnit.MINUTES.sleep(timeout);
    liftRideConsumer.stopListening();
  }

  public void startListening(String exchangeName, String queueName, int redisDatabase, int numThreads) {
    executorService = Executors.newFixedThreadPool(numThreads);
    redisConnectionPool = createRedisConnectionPool(redisDatabase, numThreads);
    ConnectionFactory rabbitMQConnectionFactory = createRabbitMQConnectionFactory();
    RedisDataPublisher redisDataPublisher = createRedisDataPublisher(queueName);

    try {
      rabbitMQConnection = rabbitMQConnectionFactory.newConnection(executorService);
      for (int i = 0; i < numThreads; i++) {
        executorService.execute(new Consumer(exchangeName, queueName, rabbitMQConnection, redisConnectionPool.borrowObject(), redisDataPublisher));
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

  private GenericObjectPool<StatefulRedisConnection<String, String>> createRedisConnectionPool(int redisDatabase, int numThreads) {
    String host = System.getenv("REDIS_HOST");
    int port = Integer.parseInt(System.getenv("REDIS_PORT"));
    RedisURI redisURI = RedisURI.create(host, port);
    redisURI.setDatabase(redisDatabase);
    redisClient = RedisClient.create(redisURI);
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

  public RedisDataPublisher createRedisDataPublisher(String modelName) {
    RedisDataPublisher redisDataPublisher = null;
    switch (modelName) {
      case "skier":
        redisDataPublisher = new RedisSkierDataPublisher();
        break;
      case "resort":
        redisDataPublisher = new RedisResortDataPublisher();
        break;
    }
    return redisDataPublisher;
  }
}
