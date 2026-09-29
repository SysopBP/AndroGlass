package app.androglass;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Process;
import android.os.SystemClock;
import app.androglass.core.TrialLease;

/** Narrow IPC bridge: only this app can enable tests; only SystemUI can claim and report them. */
public final class ConfigProvider extends ContentProvider {
    public static final Uri URI = Uri.parse("content://app.androglass.config");
    public static final int SILVER = 0xFFC3D1E7;
    public static final int WINE = 0xFFB44967;
    public static final int CYAN = 0xFF63CBDD;
    private final TrialLease lease = new TrialLease();
    private SharedPreferences preferences;
    private String status = "Waiting for SystemUI. Enable the module and restart the phone.";
    private long heartbeat;

    @Override public boolean onCreate() {
        preferences = getContext().getSharedPreferences("appearance", 0);
        return true;
    }

    /** Rejects every caller except the owning app and the OS SystemUI package's UID. */
    private boolean isSystemUi() {
        String[] packages = getContext().getPackageManager().getPackagesForUid(Binder.getCallingUid());
        if (packages != null) for (String name : packages) if ("com.android.systemui".equals(name)) return true;
        return false;
    }

    @Override public synchronized Bundle call(String method, String arg, Bundle extras) {
        boolean own = Binder.getCallingUid() == Process.myUid();
        boolean systemUi = isSystemUi();
        if (!own && !systemUi) throw new SecurityException("AndroGlass IPC is private to app/SystemUI");
        long now = SystemClock.elapsedRealtime();
        if ("start".equals(method) || "stop".equals(method) || "style".equals(method)) {
            if (!own) throw new SecurityException("Only AndroGlass can change settings");
            if ("start".equals(method)) lease.begin(now);
            else if ("stop".equals(method)) lease.stop();
            else {
                int requested = extras == null ? SILVER : extras.getInt("accent", SILVER);
                int accent = requested == WINE || requested == CYAN ? requested : SILVER;
                preferences.edit().putInt("accent", accent).apply();
            }
        } else if ("report".equals(method)) {
            if (!systemUi) throw new SecurityException("Only SystemUI can report hook state");
            String value = extras == null ? "Connected" : extras.getString("status", "Connected");
            status = value.substring(0, Math.min(value.length(), 240));
            heartbeat = now;
        } else if (!"read".equals(method)) {
            throw new IllegalArgumentException("Unknown AndroGlass operation");
        }
        Bundle result = new Bundle();
        result.putLong("deadline", systemUi ? lease.claim(Binder.getCallingPid(), now) : 0);
        result.putLong("remaining", lease.remaining(now));
        result.putInt("generation", lease.generation());
        result.putInt("accent", preferences.getInt("accent", SILVER));
        result.putString("status", status);
        result.putLong("heartbeat", heartbeat);
        return result;
    }

    @Override public Cursor query(Uri uri, String[] p, String s, String[] a, String o) { throw new UnsupportedOperationException(); }
    @Override public String getType(Uri uri) { return null; }
    @Override public Uri insert(Uri uri, ContentValues v) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri uri, String s, String[] a) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri uri, ContentValues v, String s, String[] a) { throw new UnsupportedOperationException(); }
}
