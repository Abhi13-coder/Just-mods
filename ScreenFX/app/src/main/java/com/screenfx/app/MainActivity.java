package com.screenfx.app;
import android.app.Activity; import android.content.Intent; import android.os.Bundle; import android.view.ViewGroup; import android.widget.*;
public class MainActivity extends Activity {
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL);
        int pd = (int) (20 * getResources().getDisplayMetrics().density); l.setPadding(pd, pd * 2, pd, pd);
        TextView t = new TextView(this); t.setText("ScreenFX"); t.setTextSize(28); l.addView(t);
        TextView d = new TextView(this); d.setText("Two separate tools:\n"); l.addView(d);
        Button e = new Button(this); e.setText("1. Editor - preview, adjust, presets, save"); l.addView(e);
        Button o = new Button(this); o.setText("2. System overlay - turn a look on for the whole phone"); l.addView(o);
        e.setOnClickListener(v -> startActivity(new Intent(this, EditorActivity.class)));
        o.setOnClickListener(v -> startActivity(new Intent(this, OverlayActivity.class)));
        TextView n = new TextView(this);
        n.setText("\nNote: the overlay can only draw layers on top of the screen (no root/ADB). It reproduces some adjustments exactly, some partly, and some not at all. The editor marks each slider and has an 'Overlay' preview that shows exactly what the system-wide result will look like.");
        l.addView(n); setContentView(l);
    }
}
