package com.cs6650.datapublisher;

    import com.fasterxml.jackson.databind.ObjectMapper;
    import io.lettuce.core.api.sync.RedisCommands;
    import java.io.IOException;
    import java.util.HashMap;
    import java.util.Map;
    import org.apache.log4j.Logger;

public class RedisSkierDataPublisher implements RedisDataPublisher {
    static Logger log = Logger.getLogger(RedisSkierDataPublisher.class.getName());

  @Override
  public void publishRecord(RedisCommands<String, String> commands, Map<String, Object> headers, String message) {
    String recordsKey = String.join("-",
            headers.get("resort").toString(),
            headers.get("season").toString(),
            headers.get("day").toString(),
            headers.get("skierID").toString());
    commands.rpush(recordsKey, message);

    String skierSeasonsKey = String.join("-",
              headers.get("resort").toString(),
              headers.get("skierID").toString(),
            "seasons");
    commands.sadd(skierSeasonsKey, headers.get("season").toString());

    try {
        int vertical = (int) new ObjectMapper().readValue(message, HashMap.class).get("liftID") * 10;
        String totalVerticalKey = String.join("-",
                headers.get("resort").toString(),
                headers.get("season").toString(),
                headers.get("skierID").toString(),
                "totalVertical");
        String dailyVerticalKey = String.join("-",
                headers.get("resort").toString(),
                headers.get("season").toString(),
                headers.get("day").toString(),
                headers.get("skierID").toString(),
                "dailyVertical");
        commands.incrby(totalVerticalKey, vertical);
        commands.incrby(dailyVerticalKey, vertical);
    } catch (IOException e) {
        log.error(e);
    }
  }
}
