import lombok.AllArgsConstructor;

@AllArgsConstructor
public class RequestLog {
  private int phase;
  private long startTime;
  private String requestType;
  private long latency;
  private int responseCode;

  public String toString() {
    return String.format("%d,%d,%s,%d,%d\n", phase, startTime, requestType, latency, responseCode);
  }
}