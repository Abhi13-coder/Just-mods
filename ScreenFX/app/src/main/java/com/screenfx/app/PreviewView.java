package com.screenfx.app;
import android.content.Context; import android.graphics.*; import android.view.View;
public class PreviewView extends View {
    Bitmap src, out; float[] p = new float[Looks.N]; boolean overlayMode, busy, dirty;
    OverlayRenderer rr; Rect dst = new Rect(); Paint bmp = new Paint(Paint.FILTER_BITMAP_FLAG);
    public PreviewView(Context c) { super(c); rr = new OverlayRenderer(c.getResources().getDisplayMetrics().density); }
    public void setSource(Bitmap b) { src = b; out = b; refresh(); }
    public void setParams(float[] np) { p = np.clone(); refresh(); }
    public void setOverlayMode(boolean o) { overlayMode = o; refresh(); }
    void refresh() {
        if (src == null) return;
        if (overlayMode) { out = src; invalidate(); return; }
        if (busy) { dirty = true; return; }
        busy = true; final float[] pc = p.clone(); final Bitmap s = src;
        new Thread(() -> {
            final Bitmap r = Adjust.apply(s, pc);
            post(() -> { out = r; busy = false; invalidate(); if (dirty) { dirty = false; refresh(); } });
        }).start();
    }
    @Override protected void onDraw(Canvas c) {
        if (out == null) return; int w = getWidth(), h = getHeight(); c.drawColor(Color.BLACK);
        float s = Math.min(w / (float) out.getWidth(), h / (float) out.getHeight());
        int dw = (int) (out.getWidth() * s), dh = (int) (out.getHeight() * s), l = (w - dw) / 2, t = (h - dh) / 2;
        dst.set(l, t, l + dw, t + dh); c.drawBitmap(out, null, dst, bmp);
        c.save(); c.clipRect(dst); c.translate(l, t);
        boolean anim = rr.draw(c, dw, dh, p, !overlayMode);
        c.restore(); if (anim) postInvalidateDelayed(90);
    }
}
