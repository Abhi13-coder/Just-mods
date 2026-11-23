package com.screenfx.app;
import android.graphics.*; import java.util.Random;
/** Draws only what a translucent window can do: tinted/gray/white/black layers, gradients, noise, lines. */
public class OverlayRenderer {
    Bitmap noise; BitmapShader ns; Matrix m = new Matrix(); Random rnd = new Random(); Paint bp = new Paint(), lp = new Paint();
    float dens; float[] lines; int lw, lh;
    public OverlayRenderer(float density) {
        dens = density; noise = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888);
        for (int y = 0; y < 128; y++) for (int x = 0; x < 128; x++) {
            int a = rnd.nextInt(256); noise.setPixel(x, y, rnd.nextBoolean() ? Color.argb(a, 255, 255, 255) : Color.argb(a, 0, 0, 0));
        }
        ns = new BitmapShader(noise, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
    }
    void layer(Canvas c, int r, int g, int b, float a) {
        if (a < 0.004f) return; if (a > 0.9f) a = 0.9f; c.drawColor(Color.argb((int) (a * 255), r, g, b));
    }
    /** fxOnly = true draws just the texture effects (grain, vignette, haze, leak, scanlines). Returns true if animated. */
    public boolean draw(Canvas c, int w, int h, float[] p, boolean fxOnly) {
        if (w == 0 || h == 0) return false;
        if (!fxOnly) {
            if (p[1] < 0) layer(c, 128, 128, 128, -p[1] / 100f * 0.55f);
            if (p[2] < 0) layer(c, 128, 128, 128, -p[2] / 100f * 0.45f);
            if (p[0] > 0) layer(c, 255, 255, 255, p[0] / 100f * 0.35f); else if (p[0] < 0) layer(c, 0, 0, 0, -p[0] / 100f * 0.5f);
            layer(c, 215, 215, 215, p[6] / 100f * 0.28f);
            if (p[7] > 0) layer(c, 255, 255, 255, p[7] / 100f * 0.12f);
            if (p[8] < 0) layer(c, 70, 70, 70, -p[8] / 100f * 0.15f);
            if (p[4] > 0) layer(c, 255, 140, 40, p[4] / 100f * 0.30f); else if (p[4] < 0) layer(c, 60, 120, 255, -p[4] / 100f * 0.30f);
            if (p[5] > 0) layer(c, 255, 60, 200, p[5] / 100f * 0.20f); else if (p[5] < 0) layer(c, 60, 220, 90, -p[5] / 100f * 0.20f);
        }
        layer(c, 255, 255, 255, p[12] / 100f * 0.28f);
        if (p[13] > 0) {
            int a = (int) (p[13] * 1.5f);
            lp.setShader(new LinearGradient(w, 0, w * 0.1f, h * 0.55f, new int[]{Color.argb(a, 255, 80, 30), Color.argb(a / 2, 255, 200, 80), 0}, new float[]{0f, 0.3f, 1f}, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, w, h, lp);
        }
        if (p[11] > 0) {
            float r = (float) Math.hypot(w, h) / 2f;
            lp.setShader(new RadialGradient(w / 2f, h / 2f, r, new int[]{0, 0, Color.argb((int) (p[11] * 1.7f), 0, 0, 0)}, new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, w, h, lp);
        }
        if (p[14] > 0) {
            if (lines == null || lw != w || lh != h) {
                float step = 4 * dens; int n = (int) (h / step); lines = new float[n * 4]; lw = w; lh = h;
                for (int i = 0; i < n; i++) { lines[i * 4] = 0; lines[i * 4 + 1] = i * step; lines[i * 4 + 2] = w; lines[i * 4 + 3] = i * step; }
            }
            lp.setShader(null); lp.setColor(Color.argb((int) (p[14] / 100f * 100), 0, 0, 0)); lp.setStrokeWidth(1.2f * dens); c.drawLines(lines, lp);
        }
        if (p[10] > 0) {
            m.setTranslate(rnd.nextInt(128), rnd.nextInt(128)); ns.setLocalMatrix(m);
            bp.setShader(ns); bp.setAlpha((int) (p[10] / 100f * 80)); c.drawRect(0, 0, w, h, bp);
            return true;
        }
        return false;
    }
}
