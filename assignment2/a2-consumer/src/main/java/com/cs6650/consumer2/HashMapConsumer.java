package com.cs6650.consumer2;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HashMapConsumer extends DefaultConsumer {
  private final Map<String, List<String>> recordRepository;

  public HashMapConsumer(Channel channel, Map<String, List<String>> recordRepository) {
    super(channel);
    this.recordRepository = recordRepository;
  }

  @Override
  public void handleDelivery(String consumerTag,
                             Envelope envelope, AMQP.BasicProperties properties,
                             byte[] body) throws IOException {
    String skierID = (String) properties.getHeaders().get("skierID");
    String record = new String(body, "UTF-8");
    System.out.println(skierID);
    recordRepository.compute(skierID, (k, v) -> {
      List<String> vals = v;
      if(vals == null)
        vals = new ArrayList<>();
      vals.add(record);
      return vals;
    });
  }
}
