package com.cs6650.server3.utilities;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import org.apache.commons.pool2.ObjectPool;
import java.util.Map;

public class RabbitMQUtil {
  private final ObjectPool<Channel> pool;

  public RabbitMQUtil(ObjectPool<Channel> pool) {
    this.pool = pool;
  }

  public void publishRecord(String exchangeName, Map<String, Object> headers, byte[] message) throws Exception {
    AMQP.BasicProperties properties = new AMQP.BasicProperties().builder().headers(headers).build();
    Channel channel = pool.borrowObject();
    channel.exchangeDeclare(exchangeName, "fanout");
    channel.basicPublish(exchangeName, "", properties, message);
    pool.returnObject(channel);
  }
}
