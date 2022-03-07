package com.cs6650.server2.utilities;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class RabbitMQChannelFactory extends BasePooledObjectFactory<Channel> {
  private Connection connection;

  public RabbitMQChannelFactory() {
    try {
      ConnectionFactory factory = new ConnectionFactory();
      factory.setHost("localhost");
      factory.setUsername("user");
      factory.setPassword("123");
      this.connection = factory.newConnection();
    } catch (IOException | TimeoutException e) {
      e.printStackTrace();
    }
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