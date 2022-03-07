package com.cs6650.server2.utilities;

import com.cs6650.server2.models.LiftRide;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.junit.Test;


public class RabbitMQUtilTest {

  @Test
  public void testRabbit() {
    RabbitMQUtil rabbitMQUtil = new RabbitMQUtil(new GenericObjectPool<>(new RabbitMQChannelFactory()));
    rabbitMQUtil.publishRecord("liftride", "12", new LiftRide());
  }
}