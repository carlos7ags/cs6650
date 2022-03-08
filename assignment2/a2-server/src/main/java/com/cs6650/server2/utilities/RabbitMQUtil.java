package com.cs6650.server2.utilities;

import com.cs6650.server2.models.LiftRide;
import com.google.gson.Gson;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import org.apache.commons.pool2.ObjectPool;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class RabbitMQUtil {
  private static final Gson gson = new Gson();
  private final ObjectPool<Channel> pool;

  public RabbitMQUtil(ObjectPool<Channel> pool) {
    this.pool = pool;
  }

  public void publishRecord(String queueName, String skierID, LiftRide liftRide) throws Exception {
      Channel channel = pool.borrowObject();
      Map<String, Object> headerMap = new HashMap<>();
      headerMap.put("skierID", skierID);
      AMQP.BasicProperties properties = new AMQP.BasicProperties().builder().headers(headerMap).build();
      channel.queueDeclare(queueName, true, false, false, null);
      channel.basicPublish("", queueName, properties, gson.toJson(liftRide).getBytes(StandardCharsets.UTF_8));
      pool.returnObject(channel);
  }
}
