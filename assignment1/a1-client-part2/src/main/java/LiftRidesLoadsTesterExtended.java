import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class LiftRidesLoadsTesterExtended {
  static Logger log = Logger.getLogger(LiftRidesLoadsTesterExtended.class.getName());

  private final ClientConfig clientConfig = new ClientConfig();
  private final AtomicInteger successfulCount = new AtomicInteger(0);
  private final AtomicInteger unsuccessfulCount = new AtomicInteger(0);

  public static FileChannel channel;
  public static RandomAccessFile writer;

  static {
    String reportFile = "logs/requests/log_" + new SimpleDateFormat("yyyyMMddHHmm'.csv'").format(new Date());

    try {
      FileUtils.touch(new File(reportFile));

      writer = new RandomAccessFile(reportFile, "rw");
      channel = writer.getChannel();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void testLoad() {
    log.info(this.clientConfig.toString());
    long phasesExecutorStartTime = System.currentTimeMillis();
    int numThreads = clientConfig.getNumThreads();
    ExecutorService executorService = Executors.newFixedThreadPool(numThreads);

    try {
      double progressToReleaseLatch = 0.2;

      int phaseOneNumThreads = numThreads / 4;
      CountDownLatch phaseOneLatch = new CountDownLatch((int) Math.ceil(phaseOneNumThreads * progressToReleaseLatch));
      Runnable phaseOneExecutor = new LiftRidesPhaseExtended(1, executorService, phaseOneNumThreads,
          0.2, 1, 90, phaseOneLatch, successfulCount, unsuccessfulCount);
      executorService.execute(phaseOneExecutor);
      phaseOneLatch.await();

      CountDownLatch phaseTwoLatch = new CountDownLatch((int) Math.ceil(numThreads * progressToReleaseLatch));
      Runnable phaseTwoExecutor = new LiftRidesPhaseExtended(2, executorService, numThreads,
          0.6, 91, 360, phaseTwoLatch, successfulCount, unsuccessfulCount);
      executorService.execute(phaseTwoExecutor);
      phaseTwoLatch.await();

      int phaseThreeNumThreads = numThreads / 5;
      CountDownLatch phaseThreeLatch = new CountDownLatch(phaseThreeNumThreads);
      Runnable phaseThreeExecutor = new LiftRidesPhaseExtended(3, executorService, phaseThreeNumThreads,
          0.2, 361, 420, phaseThreeLatch, successfulCount, unsuccessfulCount);
      executorService.execute(phaseThreeExecutor);
      phaseThreeLatch.await();

    } catch (InterruptedException e) {
      log.error(e);
    }
    executorService.shutdown();
    while (!executorService.isTerminated()) {
      try {
        executorService.awaitTermination(1, TimeUnit.HOURS);
      } catch (InterruptedException e) {
        log.error(e);
      }
    }


    long totalRunTime = (System.currentTimeMillis() - phasesExecutorStartTime);
    log.info("Successful requests: " + this.successfulCount.get());
    log.info("Unsuccessful requests: " + this.unsuccessfulCount.get());
    log.info("Total run time / wall time (milliseconds): " + totalRunTime);
    log.info("Total throughput in requests per second: " + ((this.successfulCount.get() + this.unsuccessfulCount.get()) / (totalRunTime / 1000)));

    try {
      channel.close();
      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    LiftRidesLoadsTesterExtended phasesExecutor = new LiftRidesLoadsTesterExtended();
    phasesExecutor.testLoad();
  }
}
