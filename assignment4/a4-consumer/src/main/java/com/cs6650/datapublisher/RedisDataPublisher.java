package com.cs6650.datapublisher;

import io.lettuce.core.api.sync.RedisCommands;

import java.util.Map;

public interface RedisDataPublisher {
  void publishRecord(RedisCommands<String, String> commands, Map<String, Object> headers, String message);
}
