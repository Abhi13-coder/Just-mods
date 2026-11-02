package com.screenfx.app;
import android.content.Context; import android.content.SharedPreferences; import android.graphics.Canvas; import android.view.View;
public class FxView extends View {
    OverlayRenderer rr; float[] p = new float[Looks.N]; SharedPreferences sp;
    public FxView(Context c) {
        super(c); sp = c.getSharedPreferences("fx2", 0);
        rr = new OverlayRenderer(c.getResources().getDisplayMetrics().density); reload();
    }
    public void reload() {
        float[] a = Looks.parse(sp.getString("active", null)); float k = sp.getInt("intensity", 100) / 100f;
        for (int i = 0; i < Looks.N; i++) p[i] = a[i] * k; invalidate();
    }
    @Override protected void onDraw(Canvas c) { if (getWidth() == 0) return; if (rr.draw(c, getWidth(), getHeight(), p, false)) postInvalidateDelayed(90); }
}
