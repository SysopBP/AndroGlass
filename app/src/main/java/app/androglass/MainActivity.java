package app.androglass;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import app.androglass.xposed.GlassClockView;

/** Standalone setup and appearance controls for the first bounded SystemUI experiment. */
public final class MainActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView hookStatus;
    private TextView countdown;
    private Button start;
    private GlassClockView preview;
    private Bundle lastState = new Bundle();
    private final Runnable refresh = new Runnable() {
        @Override public void run() { updateStatus(); handler.postDelayed(this, 1_000L); }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(0xFF0B0D11);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(24), dp(28), dp(24), dp(32));
        content.setOnApplyWindowInsetsListener((view, insets) -> {
            int top;
            int bottom;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                top = bars.top; bottom = bars.bottom;
            } else { top = insets.getSystemWindowInsetTop(); bottom = insets.getSystemWindowInsetBottom(); }
            view.setPadding(dp(24), dp(28) + top, dp(24), dp(32) + bottom);
            return insets;
        });
        scroll.addView(content);
        setContentView(scroll);
        label(content, "A N D R O G L A S S", 13, 0xFFC891A2);
        label(content, "Your status.\nReimagined.", 36, Color.WHITE);
        label(content, "INDEPENDENT SYSTEMUI PREVIEW  ·  0.1.0", 11, 0xFF8995AA);

        LinearLayout appearance = card(content);
        label(appearance, "Black frost", 22, Color.WHITE);
        label(appearance, "A dark glass clock with a fine metallic edge.", 14, 0xFFABB4C4);
        preview = new GlassClockView(this);
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(dp(150), dp(54));
        previewParams.gravity = Gravity.CENTER_HORIZONTAL;
        previewParams.topMargin = dp(24); previewParams.bottomMargin = dp(12);
        appearance.addView(preview, previewParams);
        label(appearance, "APPEARANCE PREVIEW — not connection status", 10, 0xFF8995AA);
        LinearLayout colors = new LinearLayout(this);
        colors.setOrientation(LinearLayout.HORIZONTAL);
        appearance.addView(colors);
        colorButton(colors, "Silver", ConfigProvider.SILVER);
        colorButton(colors, "Wine", ConfigProvider.WINE);
        colorButton(colors, "Ice", ConfigProvider.CYAN);

        LinearLayout test = card(content);
        label(test, "SystemUI connection", 21, Color.WHITE);
        hookStatus = label(test, "Checking connection…", 14, 0xFFBBC5D7);
        countdown = label(test, "Test is off", 16, 0xFFC891A2);
        start = button(test, "Start 90-second test", () -> change("start", null), true);
        button(test, "Stop & restore stock", () -> change("stop", null), false);
        label(test, "This first test replaces only the clock's visible surface. Your original time format, status icons and shade gestures stay in place. The glass effect is drawn shading, not background blur.", 13, 0xFF8995AA);

        LinearLayout setup = card(content);
        label(setup, "Connect the module", 21, Color.WHITE);
        label(setup, "1. Use a working Android 17-compatible Xposed framework with modern API 102.\n\n2. Enable AndroGlass and select System UI (com.android.systemui) only.\n\n3. Restart the phone, return here, then start the test when the connection appears.", 14, 0xFFBBC5D7);
        label(setup, "Tests end automatically and pause on the lock screen. A SystemUI restart cancels the active test. Unsupported layouts keep the stock clock. If needed, disable AndroGlass in your module manager and restart.", 13, 0xFF8995AA);
        button(setup, "Save connection report", this::saveReport, false);
        label(content, "Android 17 / One UI compatibility is awaiting device verification.\nNo Galaxy Island or D2 dependency.", 12, 0xFF8995AA);
    }

    @Override protected void onResume() { super.onResume(); handler.post(refresh); }
    @Override protected void onPause() { handler.removeCallbacks(refresh); super.onPause(); }

    /** Reads actual provider heartbeats; installing the APK alone never counts as hook activation. */
    private void updateStatus() {
        try {
            Bundle result = getContentResolver().call(ConfigProvider.URI, "read", null, null);
            if (result == null) throw new IllegalStateException("No configuration response");
            lastState = result;
            long age = SystemClock.elapsedRealtime() - result.getLong("heartbeat", 0);
            boolean connected = result.getLong("heartbeat", 0) > 0 && age < 5_000L;
            hookStatus.setText((connected ? "Connected · " : "Not connected · ") + result.getString("status", "Waiting for module"));
            long remaining = result.getLong("remaining", 0);
            countdown.setText(remaining > 0 ? "Test ends in " + (remaining + 999) / 1_000 + " seconds" : "Test is off · stock clock active");
            start.setEnabled(connected && remaining == 0);
            preview.update(DateFormat.getTimeFormat(this).format(new Date()), dp(22), Typeface.create("sans-serif-medium", Typeface.NORMAL), result.getInt("accent", ConfigProvider.SILVER));
        } catch (Exception failure) {
            hookStatus.setText("Connection unavailable: " + failure.getClass().getSimpleName());
            start.setEnabled(false);
        }
    }

    private void change(String action, Bundle data) {
        try { getContentResolver().call(ConfigProvider.URI, action, null, data); updateStatus(); }
        catch (Exception failure) { Toast.makeText(this, "Could not apply setting", Toast.LENGTH_LONG).show(); }
    }

    private void colorButton(LinearLayout parent, String title, int color) {
        Button button = button(parent, title, () -> {
            Bundle data = new Bundle(); data.putInt("accent", color); change("style", data);
        }, false);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(52), 1);
        params.setMargins(dp(2), dp(12), dp(2), 0); button.setLayoutParams(params);
        button.setTextColor(color);
    }

    /** Uses the system file picker so the user can save the report directly in Downloads. */
    private void saveReport() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("text/plain");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_TITLE, "AndroGlass-connection-report.txt");
        startActivityForResult(intent, 1);
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != 1 || result != RESULT_OK || data == null || data.getData() == null) return;
        String report = "AndroGlass 0.1.0-preview.1\nModel: " + Build.MODEL + "\nAndroid: " + Build.VERSION.RELEASE
            + "\nSDK: " + Build.VERSION.SDK_INT + "\nBuild: " + Build.DISPLAY
            + "\nStatus: " + lastState.getString("status", "Unknown")
            + "\nHeartbeat age ms: " + (SystemClock.elapsedRealtime() - lastState.getLong("heartbeat", 0))
            + "\nTest remaining ms: " + lastState.getLong("remaining", 0)
            + "\nRequired scope: com.android.systemui\n";
        try (OutputStream stream = getContentResolver().openOutputStream(data.getData())) {
            if (stream == null) throw new IllegalStateException("No output stream");
            stream.write(report.getBytes(StandardCharsets.UTF_8));
            Toast.makeText(this, "Connection report saved", Toast.LENGTH_SHORT).show();
        } catch (Exception failure) { Toast.makeText(this, "Could not save report", Toast.LENGTH_LONG).show(); }
    }

    private LinearLayout card(LinearLayout parent) {
        LinearLayout card = new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(20), dp(20), dp(20), dp(20));
        card.setBackground(background(0xFF151922, 0xFF2B303C));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(22); parent.addView(card, params); return card;
    }

    private TextView label(LinearLayout parent, String text, int size, int color) {
        TextView view = new TextView(this); view.setText(text); view.setTextSize(size); view.setTextColor(color);
        view.setPadding(0, dp(5), 0, dp(7)); view.setLineSpacing(dp(2), 1f);
        parent.addView(view); return view;
    }

    private Button button(LinearLayout parent, String text, Runnable action, boolean primary) {
        Button button = new Button(this); button.setText(text); button.setAllCaps(false); button.setTextColor(Color.WHITE);
        button.setBackground(background(primary ? 0xFF76334B : 0xFF202632, primary ? 0xFFA34C68 : 0xFF394151));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(52)); params.topMargin = dp(12);
        parent.addView(button, params); button.setOnClickListener(view -> action.run()); return button;
    }

    private GradientDrawable background(int color, int border) {
        GradientDrawable result = new GradientDrawable(); result.setColor(color); result.setCornerRadius(dp(22));
        result.setStroke(dp(1), border); return result;
    }
    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
