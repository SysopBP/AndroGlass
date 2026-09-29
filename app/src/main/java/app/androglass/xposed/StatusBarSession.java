package app.androglass.xposed;

import android.app.KeyguardManager;
import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;
import app.androglass.ConfigProvider;

/** Owns one attached status-bar host; all mutations are reversible and bounded by a local deadline. */
final class StatusBarSession implements View.OnAttachStateChangeListener, ViewTreeObserver.OnPreDrawListener {
    private static final long POLL_MS = 1_000L;
    private final View host;
    private final Handler main = new Handler(Looper.getMainLooper());
    private HandlerThread thread;
    private Handler worker;
    private GlassClockView glass;
    private TextView clock;
    private float originalAlpha;
    private boolean hiding;
    private boolean attached;
    private boolean failed;
    private long deadline;
    private int accent = ConfigProvider.SILVER;
    private int attachmentGeneration;
    private volatile String report = "Status-bar host connected; test is off.";
    private final Runnable watchdog = this::checkDeadline;

    StatusBarSession(View host) { this.host = host; }

    /** Registers one listener; repeat hook callbacks are de-duplicated by the module. */
    void install() {
        host.addOnAttachStateChangeListener(this);
        if (host.isAttachedToWindow()) onViewAttachedToWindow(host);
    }

    @Override public void onViewAttachedToWindow(View view) {
        if (attached) return;
        attached = true;
        failed = false;
        int generation = ++attachmentGeneration;
        host.getViewTreeObserver().addOnPreDrawListener(this);
        thread = new HandlerThread("AndroGlass-config");
        thread.start();
        worker = new Handler(thread.getLooper());
        Handler pollingWorker = worker;
        pollingWorker.post(new Runnable() {
            @Override public void run() {
                Bundle config = null;
                try {
                    Bundle status = new Bundle();
                    status.putString("status", report);
                    host.getContext().getContentResolver().call(ConfigProvider.URI, "report", null, status);
                    config = host.getContext().getContentResolver().call(ConfigProvider.URI, "read", null, null);
                } catch (Exception failure) {
                    Log.w("AndroGlass", "Configuration unavailable; restoring stock clock", failure);
                }
                Bundle result = config;
                main.post(() -> {
                    if (!attached || generation != attachmentGeneration) return;
                    deadline = result == null ? 0 : result.getLong("deadline", 0);
                    accent = result == null ? ConfigProvider.SILVER : result.getInt("accent", ConfigProvider.SILVER);
                    renderSafely();
                });
                pollingWorker.postDelayed(this, POLL_MS);
            }
        });
        main.post(watchdog);
    }

    @Override public void onViewDetachedFromWindow(View view) {
        attached = false;
        attachmentGeneration++;
        main.removeCallbacks(watchdog);
        if (worker != null) worker.removeCallbacksAndMessages(null);
        if (thread != null) thread.quitSafely();
        worker = null;
        thread = null;
        if (host.getViewTreeObserver().isAlive()) host.getViewTreeObserver().removeOnPreDrawListener(this);
        restore();
    }

    /** Enforces expiry even if the app IPC stops responding. */
    private void checkDeadline() {
        if (!attached) return;
        renderSafely();
        main.postDelayed(watchdog, POLL_MS);
    }

    @Override public boolean onPreDraw() { renderSafely(); return true; }

    /** Checks the lease, lock state and exact clock bounds before hiding any stock pixels. */
    private void renderSafely() {
        try {
            if (failed || !attached || SystemClock.elapsedRealtime() >= deadline) {
                restore();
                if (!failed) report = "Status-bar host connected; test is off.";
                return;
            }
            KeyguardManager keyguard = host.getContext().getSystemService(KeyguardManager.class);
            if (keyguard == null || keyguard.isKeyguardLocked()) {
                restore();
                report = "Test paused on lock screen.";
                return;
            }
            if (!(host instanceof ViewGroup)) {
                restore();
                report = "Unsupported host layout; stock clock retained.";
                return;
            }
            ViewGroup group = (ViewGroup) host;
            TextView target = findClock(group);
            if (target == null || !target.isShown() || target.getWidth() <= 0 || target.getHeight() <= 0) {
                restore();
                report = "Clock target not visible or not found; stock layout retained.";
                return;
            }
            if (!hiding && target.getAlpha() <= 0f) {
                restore();
                report = "Stock clock is hidden; waiting for a visible clock.";
                return;
            }
            if (clock != target) { restore(); clock = target; }
            Rect rect = new Rect();
            target.getDrawingRect(rect);
            group.offsetDescendantRectToMyCoords(target, rect);
            if (rect.left < 0 || rect.top < 0 || rect.right > group.getWidth() || rect.bottom > group.getHeight()) {
                restore();
                report = "Clock bounds unsupported; stock layout retained.";
                return;
            }
            if (glass == null) {
                glass = new GlassClockView(host.getContext());
                group.getOverlay().add(glass);
            }
            glass.layout(rect.left, rect.top, rect.right, rect.bottom);
            glass.update(target.getText().toString(), target.getTextSize(), target.getTypeface(), accent);
            if (!hiding) { originalAlpha = target.getAlpha(); hiding = true; }
            target.setAlpha(0f);
            report = "LIVE: frosted clock attached inside SystemUI. Stock gestures retained.";
        } catch (Throwable failure) {
            failed = true;
            restore();
            report = "Rendering stopped after an error; stock clock restored.";
            Log.e("AndroGlass", "ANDROGLASS_RENDER_STOPPED", failure);
        }
    }

    /** Uses the documented resource name rather than changing every TextView in SystemUI. */
    private TextView findClock(ViewGroup group) {
        int id = host.getResources().getIdentifier("clock", "id", "com.android.systemui");
        if (id == 0) return null;
        View candidate = group.findViewById(id);
        return candidate instanceof TextView ? (TextView) candidate : null;
    }

    /** Removes only our overlay and restores the original clock alpha without reparenting views. */
    private void restore() {
        try {
            if (hiding && clock != null) clock.setAlpha(originalAlpha);
            if (glass != null && host instanceof ViewGroup) ((ViewGroup) host).getOverlay().remove(glass);
        } catch (Throwable failure) {
            Log.e("AndroGlass", "ANDROGLASS_RESTORE_FAILED", failure);
        } finally {
            hiding = false;
            clock = null;
            glass = null;
        }
    }
}
