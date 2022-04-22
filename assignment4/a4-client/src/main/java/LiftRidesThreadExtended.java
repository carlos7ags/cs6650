import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.api.SkiersApi;
import io.swagger.client.model.LiftRide;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class LiftRidesThreadExtended implements Runnable {
  static Logger log = Logger.getLogger(LiftRidesThreadExtended.class.getName());
  private final int RESORT_ID = 27;
  private final String SEASON_ID = "2022";
  private final String DAY_ID = "3";

  private final CountDownLatch nextPhaseLatch;
  private final AtomicInteger successfulCount;
  private final AtomicInteger unsuccessfulCount;

  private final int phase;
  private final int requestsNumber;
  private final int skiLiftsNumber;
  private final int startSkierId;
  private final int endSkierId;
  private final int starTime;
  private final int endTime;

  private final Random random = new Random();
  private final SkiersApi apiInstance = new SkiersApi();

  public LiftRidesThreadExtended(int requestsNumber, int skiLiftsNumber,
                         int startSkierId, int endSkierId, int starTime, int endTime, String serverBasePath,
                         CountDownLatch nextPhaseLatch, AtomicInteger successfulCount, AtomicInteger unsuccessfulCount, int phase) {
    this.requestsNumber = requestsNumber;
    this.skiLiftsNumber = skiLiftsNumber;
    this.startSkierId = startSkierId;
    this.endSkierId = endSkierId;
    this.starTime = starTime;
    this.endTime = endTime;
    this.nextPhaseLatch = nextPhaseLatch;
    this.successfulCount = successfulCount;
    this.unsuccessfulCount = unsuccessfulCount;
    this.phase = phase;

    ApiClient client = apiInstance.getApiClient();
    client.setBasePath(serverBasePath);
  }

  private RequestLog postNewLiftRide(SkiersApi apiInstance, int skierId, int liftId, int time, int waitTime) {
    int tries = 0;
    int maxTries = 5;
    LiftRide liftRide = new LiftRide();
    liftRide.setLiftID(liftId);
    liftRide.setTime(time);
    liftRide.setWaitTime(waitTime);

    while (true) {
      try {
        long startTime = System.currentTimeMillis();
        apiInstance.writeNewLiftRide(liftRide, this.RESORT_ID, this.SEASON_ID, this.DAY_ID, skierId);
        long endTime = System.currentTimeMillis();
        RequestLog requestLog = new RequestLog(this.phase, startTime, "POST", endTime - startTime, 201);
        this.successfulCount.getAndIncrement();
        return requestLog;
      } catch (ApiException e) {
        if (++tries == maxTries) {
          this.unsuccessfulCount.getAndIncrement();
          log.trace(e.getMessage());
          return null;
        }
      }
    }
  }

  @Override
  public void run() {
    List<RequestLog> requestLogs = new ArrayList<>();
    RequestLog requestLog;

    for (int i = 0; i < this.requestsNumber; i++) {
      int skierId = random.ints(this.startSkierId, this.endSkierId + 1).findAny().getAsInt();
      int liftId = random.ints(1, this.skiLiftsNumber + 1).findAny().getAsInt();
      int time = random.ints(this.starTime, this.endTime + 1).findAny().getAsInt();
      int waitTime = random.ints(0, 10 + 1).findAny().getAsInt();
      requestLog = this.postNewLiftRide(apiInstance, skierId, liftId, time, waitTime);
      if (requestLog != null) {
        requestLogs.add(requestLog);
      }
    }

    try {
      String logsString = requestLogs.stream().map(Object::toString).collect(Collectors.joining(""));
      ByteBuffer buffer = ByteBuffer.wrap(logsString.getBytes(StandardCharsets.UTF_8));
      LiftRidesLoadsTesterExtended.channel.write(buffer);
    } catch (IOException e) {
      log.error("Failed to write in summary file.", e);
    }

    this.nextPhaseLatch.countDown();
  }
}
