package com.cs6650.datapublisher;

import io.lettuce.core.api.sync.RedisCommands;
import org.apache.log4j.Logger;

import java.util.Map;

public class RedisResortDataPublisher implements RedisDataPublisher {
  static Logger log = Logger.getLogger(RedisSkierDataPublisher.class.getName());

  @Override
  public void publishRecord(RedisCommands<String, String> commands, Map<String, Object> headers, String message) {
    String key = String.join("-",
            headers.get("resort").toString(),
            headers.get("season").toString(),
            headers.get("day").toString());
    commands.rpush(key, message);
    commands.sadd(key + "-visitedSkierID", headers.get("skierID").toString());
  }
}
