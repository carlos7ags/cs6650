import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.api.SkiersApi;
import io.swagger.client.model.LiftRide;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class LiftRidesThread implements Runnable {
  private final int RESORT_ID = 1;
  private final String SEASON_ID = "2022";
  private final String DAY_ID = "27";

  private final CountDownLatch nextPhaseLatch;
  private final AtomicInteger successfulCount;
  private final AtomicInteger unsuccessfulCount;

  private final int requestsNumber;
  private final int skiLiftsNumber;
  private final int startSkierId;
  private final int endSkierId;
  private final int starTime;
  private final int endTime;

  private final Random random = new Random();
  private final SkiersApi apiInstance = new SkiersApi();

  public LiftRidesThread(int requestsNumber, int skiLiftsNumber,
                         int startSkierId, int endSkierId, int starTime, int endTime, String serverBasePath,
                         CountDownLatch nextPhaseLatch, AtomicInteger successfulCount, AtomicInteger unsuccessfulCount) {
    this.requestsNumber = requestsNumber;
    this.skiLiftsNumber = skiLiftsNumber;
    this.startSkierId = startSkierId;
    this.endSkierId = endSkierId;
    this.starTime = starTime;
    this.endTime = endTime;
    this.nextPhaseLatch = nextPhaseLatch;
    this.successfulCount = successfulCount;
    this.unsuccessfulCount = unsuccessfulCount;

    ApiClient client = apiInstance.getApiClient();
    client.setBasePath(serverBasePath);
  }

  private void postNewLiftRide(SkiersApi apiInstance, int skierId, int liftId, int time, int waitTime) {
    int tries = 0;
    int maxTries = 5;
    LiftRide liftRide = new LiftRide();
    liftRide.setLiftID(liftId);
    liftRide.setTime(time);
    liftRide.setWaitTime(waitTime);

    while (true) {
      try {
        apiInstance.writeNewLiftRide(liftRide, this.RESORT_ID, this.SEASON_ID, this.DAY_ID, skierId);
        this.successfulCount.getAndIncrement();
        break;
      } catch (ApiException e) {
        if (++tries == maxTries) {
          this.unsuccessfulCount.getAndIncrement();
          break;
        }
      }
    }
  }

  @Override
  public void run() {
    for (int i = 0; i < this.requestsNumber; i++) {
      int skierId = random.ints(this.startSkierId, this.endSkierId + 1).findAny().getAsInt();
      int liftId = random.ints(1, this.skiLiftsNumber + 1).findAny().getAsInt();
      int time = random.ints(this.starTime, this.endTime + 1).findAny().getAsInt();
      int waitTime = random.ints(0, 10 + 1).findAny().getAsInt();
      this.postNewLiftRide(apiInstance, skierId, liftId, time, waitTime);
    }
    this.nextPhaseLatch.countDown();
  }
}
