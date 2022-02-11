import lombok.AllArgsConstructor;

@AllArgsConstructor
public class RequestLog {
  private int phase;
  private long startTime;
  private String requestType;
  private long latency;
  private int responseCode;

  public String toString() {
    return String.format("{phase: %d, startTime: %d, requestType: %s, latency: %d, responseCode: %d}", phase, startTime, requestType, latency, responseCode);
  }
}