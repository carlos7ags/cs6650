package com.cs6650.datapublisher;

import io.lettuce.core.api.sync.RedisCommands;

import java.util.Map;

public class RedisResortDataPublisher extends RedisDataPublisher {

  @Override
  public void publishRecord(RedisCommands<String, String> commands, Map<String, Object> headers, String message) {
    Object skierID = headers.get("skierID");
    commands.set(skierID.toString(), message);
  }
}
