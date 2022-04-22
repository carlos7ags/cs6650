package com.cs6650.datapublisher;

    import io.lettuce.core.api.sync.RedisCommands;

    import java.util.Map;

public class RedisSkierDataPublisher implements RedisDataPublisher {

  @Override
  public void publishRecord(RedisCommands<String, String> commands, Map<String, Object> headers, String message) {
    String key = String.join("-", headers.get("skierID").toString(), headers.get("day").toString());
    commands.rpush(key, message);
  }
}
