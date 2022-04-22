import org.apache.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LiftRidesLoadsTesterReport {
  static Logger log = Logger.getLogger(LiftRidesLoadsTesterReport.class.getName());

  private String reportFile;
  private long runtime;
  private RandomAccessFile reader;

  private long count = 0;
  private double meanResponseTime;
  private int medianResponseTime;
  private double throughput;
  private int p99;
  private int minResponseTime = Integer.MAX_VALUE;
  private int maxResponseTime = Integer.MIN_VALUE;


  public LiftRidesLoadsTesterReport(String reportFile, long runtime) {
    this.reportFile = reportFile;
    this.runtime = runtime;
  }

  public void printReport() {
    if (this.openReader()) {
      String line;
      String[] record;
      List<Integer> latencies = new ArrayList<>();
      try {
        while((line = reader.readLine()) != null) {
          record = line.split(",");
          count++;

          int recordLatency = Integer.parseInt(record[3]);
          latencies.add(recordLatency);
          minResponseTime = Math.min(minResponseTime, recordLatency);
          maxResponseTime = Math.max(maxResponseTime, recordLatency);
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
      this.closeReader();
      throughput = count / (runtime / 1000F);
      meanResponseTime = latencies.stream().reduce(0, Integer::sum) / (double) count;
      Collections.sort(latencies);
      medianResponseTime = latencies.get((int) (count / 2));
      p99 = latencies.get((int) (count * 0.99));
      log.info(this.toString());
    } else {
      log.error("File reader channel not available. Couldn't generate report.");
    }
  }

  public Boolean openReader() {
    try {
      this.reader = new RandomAccessFile(reportFile, "r");
      return true;
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      return false;
    }
  }

  public void closeReader() {
    try {
      this.reader.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public String toString() {
    return "\n\nLift Ride Loads Tester Summary\n"
      + "Mean response time (millisecs): " + meanResponseTime + "\n"
      + "Median response time (millisecs): " + medianResponseTime + "\n"
      + "Throughput (requests per second): " + throughput + "\n"
      + "P99 Response Time (millisecs): " + p99 + "\n"
      + "Min Response Time (millisecs): " + minResponseTime + "\n"
      + "Max Response Time (millisecs): " + maxResponseTime + "\n";
  }
}
