package com.screenfx.app;
import android.graphics.Bitmap;
public class Adjust {
    static float cl(float v) { return v < 0 ? 0 : (v > 1 ? 1 : v); }
    public static Bitmap apply(Bitmap src, float[] p) {
        int w = src.getWidth(), h = src.getHeight(); int[] px = new int[w * h]; src.getPixels(px, 0, w, 0, 0, w, h);
        float br = p[0] / 100f * 0.35f, ct = p[1] / 100f, cf = 1f + ct * (ct > 0 ? 1f : 0.8f), sat = 1f + p[2] / 100f;
        double ang = Math.toRadians(p[3] * 1.8); float cs = (float) Math.cos(ang), sn = (float) Math.sin(ang);
        float wm = p[4] / 100f, tn = p[5] / 100f, fd = p[6] / 100f * 0.25f, sh = p[7] / 100f * 0.3f, hl = p[8] / 100f * 0.3f;
        boolean doHue = p[3] != 0;
        for (int i = 0; i < px.length; i++) {
            int c = px[i]; float r = ((c >> 16) & 255) / 255f, g = ((c >> 8) & 255) / 255f, b = (c & 255) / 255f;
            float l = 0.299f * r + 0.587f * g + 0.114f * b;
            float add = br + sh * (1 - l) * (1 - l) + hl * l * l;
            r += add; g += add; b += add;
            r = (r - 0.5f) * cf + 0.5f; g = (g - 0.5f) * cf + 0.5f; b = (b - 0.5f) * cf + 0.5f;
            r = fd + r * (1 - fd); g = fd + g * (1 - fd); b = fd + b * (1 - fd);
            float l2 = 0.299f * r + 0.587f * g + 0.114f * b;
            r = l2 + (r - l2) * sat; g = l2 + (g - l2) * sat; b = l2 + (b - l2) * sat;
            if (doHue) {
                float y = 0.299f * r + 0.587f * g + 0.114f * b, iq = 0.596f * r - 0.274f * g - 0.322f * b, qq = 0.211f * r - 0.523f * g + 0.312f * b;
                float i2 = iq * cs - qq * sn, q2 = iq * sn + qq * cs;
                r = y + 0.956f * i2 + 0.621f * q2; g = y - 0.272f * i2 - 0.647f * q2; b = y - 1.106f * i2 + 1.703f * q2;
            }
            r *= 1f + wm * 0.15f + tn * 0.04f; b *= 1f - wm * 0.15f + tn * 0.04f; g *= 1f - tn * 0.12f;
            px[i] = (c & 0xFF000000) | (((int) (cl(r) * 255f + 0.5f)) << 16) | (((int) (cl(g) * 255f + 0.5f)) << 8) | (int) (cl(b) * 255f + 0.5f);
        }
        if (p[9] > 0) {
            float k = p[9] / 100f * 1.5f; int[] o = px.clone();
            for (int y = 1; y < h - 1; y++) for (int x = 1; x < w - 1; x++) {
                int idx = y * w + x; int res = 0xFF000000;
                for (int ch = 0; ch < 3; ch++) {
                    int shf = 16 - 8 * ch; int s = 0;
                    for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) s += (px[idx + dy * w + dx] >> shf) & 255;
                    float v = (px[idx] >> shf) & 255; v = v + k * (v - s / 9f);
                    int iv = (int) (v + 0.5f); if (iv < 0) iv = 0; if (iv > 255) iv = 255; res |= iv << shf;
                }
                o[idx] = res;
            }
            px = o;
        }
        Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); out.setPixels(px, 0, w, 0, 0, w, h); return out;
    }
}
