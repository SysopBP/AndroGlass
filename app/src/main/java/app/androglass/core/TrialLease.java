package app.androglass.core;

/** A memory-only, monotonic test lease that ends if SystemUI restarts during the test. */
public final class TrialLease {
    public static final long DURATION_MS = 90_000L;
    private long deadline;
    private int ownerPid;
    private int generation;

    /** Starts a fresh explicit user test; never restored from disk or on boot. */
    public synchronized void begin(long now) {
        deadline = now + DURATION_MS;
        ownerPid = 0;
        generation++;
    }

    /** Disables the current test and invalidates in-flight configuration reads. */
    public synchronized void stop() {
        deadline = 0;
        ownerPid = 0;
        generation++;
    }

    /** Binds a trial to one SystemUI process; a replacement process cancels it. */
    public synchronized long claim(int pid, long now) {
        if (pid <= 0 || now >= deadline) return 0;
        if (ownerPid != 0 && ownerPid != pid) {
            stop();
            return 0;
        }
        ownerPid = pid;
        return deadline;
    }

    /** Reads remaining test time without claiming the SystemUI lease. */
    public synchronized long remaining(long now) { return Math.max(0, deadline - now); }
    public synchronized int generation() { return generation; }
}
