package app.androglass.xposed;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/** Non-interactive battery glyph used only after a verified SystemUI battery target is found. */
final class GlassBatteryView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF body = new RectF();
    private int accent;

    GlassBatteryView(Context context) {
        super(context);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        setClickable(false);
        setFocusable(false);
    }

    void update(int color) {
        if (accent == color) return;
        accent = color;
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float d = getResources().getDisplayMetrics().density;
        float stroke = Math.max(d, getHeight() * 0.075f);
        float cap = Math.max(d * 1.5f, getWidth() * 0.08f);
        float top = stroke * 1.4f;
        float bottom = getHeight() - stroke * 1.4f;
        float right = getWidth() - cap - stroke;
        body.set(stroke, top, right, bottom);

        paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(stroke);
        paint.setColor(accent);
        canvas.drawRoundRect(body, body.height() * 0.22f, body.height() * 0.22f, paint);

        paint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(right + stroke * 0.65f,
                getHeight() * 0.36f,
                getWidth() - stroke * 0.25f,
                getHeight() * 0.64f,
                stroke * 0.4f, stroke * 0.4f, paint);

        // Visible test fill. Battery percentage/state mirroring comes after Samsung target verification.
        float inset = stroke * 1.65f;
        float fillRight = body.left + (body.width() - inset * 2f) * 0.72f + inset;
        canvas.drawRoundRect(body.left + inset, body.top + inset,
                fillRight, body.bottom - inset,
                body.height() * 0.10f, body.height() * 0.10f, paint);
    }
}