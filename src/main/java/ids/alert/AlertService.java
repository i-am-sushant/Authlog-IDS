package ids.alert;

import java.io.PrintStream;
import java.time.Instant;
import java.util.Objects;

public final class AlertService {
    private final PrintStream out;

    public AlertService() {
        this(System.out);
    }

    public AlertService(PrintStream out) {
        this.out = Objects.requireNonNull(out, "out");
    }

    public void alertIpBruteForce(String ip, int count, String windowLabel, Instant at) {
        out.println("RULE=IP_BRUTE_FORCE time=" + iso(at) + " ip=" + ip + " count=" + count + " window=" + windowLabel);
    }

    public void alertUserBruteForce(String user, int count, String windowLabel, Instant at) {
        out.println("RULE=USER_BRUTE_FORCE time=" + iso(at) + " user=" + user + " count=" + count + " window=" + windowLabel);
    }

    public void alertFailToSuccess(String user, String ip, int count, String windowLabel, Instant at) {
        out.println("RULE=FAIL_TO_SUCCESS time=" + iso(at) + " user=" + user + " ip=" + ip + " count=" + count + " window=" + windowLabel);
    }

    private String iso(Instant at) {
        return at == null ? "" : at.toString();
    }
}
