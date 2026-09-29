import app.androglass.core.TrialLease;

/** Executable regression checks; no Android SDK or third-party test dependency required. */
public final class TrialLeaseTest {
    public static void main(String[] args) {
        TrialLease lease = new TrialLease();
        check(lease.claim(10, 1_000) == 0, "inactive on construction");
        lease.begin(1_000);
        check(lease.claim(10, 1_001) == 91_000, "first SystemUI process binds");
        check(lease.claim(10, 2_000) == 91_000, "same process retains lease");
        check(lease.claim(11, 2_001) == 0, "restarted SystemUI cancels trial");
        check(lease.claim(10, 2_002) == 0, "old process cannot revive trial");
        lease.begin(5_000);
        check(lease.claim(10, 94_999) != 0, "active before deadline");
        check(lease.claim(10, 95_000) == 0, "expires at deadline");
        lease.begin(100_000);
        int generation = lease.generation();
        lease.stop();
        check(lease.remaining(100_001) == 0, "stop clears time");
        check(lease.generation() > generation, "stop invalidates earlier generation");
        check(new TrialLease().claim(10, 100_001) == 0, "app restart stays off");
        System.out.println("PASS: 10 trial lease checks");
    }
    private static void check(boolean condition, String name) {
        if (!condition) throw new AssertionError(name);
    }
}
