package app.androglass.xposed;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.View;
import app.androglass.ConfigProvider;

/** Decorative clock capsule; deliberately has no touch handling or accessibility duplicate. */
public final class GlassClockView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bounds = new RectF();
    private String text = "";
    private int accent = ConfigProvider.SILVER;
    private float textPixels;
    private Typeface typeface = Typeface.DEFAULT;

    public GlassClockView(Context context) {
        super(context);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        setClickable(false);
        setFocusable(false);
    }

    /** Mirrors the stock text, including locale and 12/24-hour formatting, without its position logic. */
    public void update(String value, float textSize, Typeface face, int color) {
        if (text.equals(value) && textPixels == textSize && typeface == face && accent == color) return;
        text = value;
        textPixels = textSize;
        typeface = face;
        accent = color;
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float density = getResources().getDisplayMetrics().density;
        float inset = density;
        bounds.set(inset, inset, getWidth() - inset, getHeight() - inset);
        float radius = Math.max(0, bounds.height() / 2f);
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new LinearGradient(0, 0, getWidth(), getHeight(),
            new int[]{0xEA303742, 0xF00C1017, 0xE51C202B}, null, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(bounds, radius, radius, paint);
        paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(density * 0.8f);
        paint.setColor((accent & 0x00FFFFFF) | 0x99000000);
        canvas.drawRoundRect(bounds, radius, radius, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setTypeface(typeface);
        paint.setTextSize(textPixels);
        paint.setTextAlign(Paint.Align.CENTER);
        float available = Math.max(1, getWidth() - density * 6);
        float textWidth = paint.measureText(text);
        if (textWidth > available) paint.setTextSize(textPixels * available / textWidth);
        float baseline = getHeight() / 2f - (paint.ascent() + paint.descent()) / 2f;
        canvas.drawText(text, getWidth() / 2f, baseline, paint);
    }
}
