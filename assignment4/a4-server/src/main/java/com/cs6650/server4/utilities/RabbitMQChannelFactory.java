package com.cs6650.server4.utilities;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class RabbitMQChannelFactory extends BasePooledObjectFactory<Channel> {
  private ConnectionFactory factory;
  private Connection connection;

  public RabbitMQChannelFactory() throws IOException, TimeoutException {
    this.factory = new ConnectionFactory();
    this.factory.setHost(System.getenv("RABBITMQ_HOST"));
    this.factory.setUsername(System.getenv("RABBITMQ_USER"));
    this.factory.setPassword(System.getenv("RABBITMQ_PASSWORD"));
    this.connection = factory.newConnection();
  }

  @Override
  public Channel create() throws IOException {
    return connection.createChannel();
  }

  @Override
  public PooledObject<Channel> wrap(Channel channel) {
    return new DefaultPooledObject<>(channel);
  }
}