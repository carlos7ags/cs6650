package com.cs6650.datapublisher;

import io.lettuce.core.api.sync.RedisCommands;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RedisSkierDataPublisher extends RedisDataPublisher {

  @Override
  public void publishRecord(RedisCommands<String, String> commands, Map<String, Object> headers, String message) {
    Object skierID = headers.get("skierID");
    Object day = headers.get("day");
    String currentRecordsList = commands.hget(skierID.toString(), day.toString());
    if (currentRecordsList != null) {
      commands.hset(skierID.toString(), day.toString(), addRecord(currentRecordsList, message));
    } else {
      List<String> recordsList = new ArrayList<>();
      recordsList.add(message);
      commands.hset(skierID.toString(), day.toString(), recordsList.toString());
    }
  }
}
