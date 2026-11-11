package com.screenfx.app;
import android.app.Activity; import android.app.AlertDialog; import android.content.*; import android.net.Uri; import android.os.*; import android.provider.Settings;
import android.widget.*; import java.util.*;
public class OverlayActivity extends Activity {
    SharedPreferences sp; Button toggle; TextView status;
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b); sp = getSharedPreferences("fx2", 0);
        LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL);
        int pd = (int) (20 * getResources().getDisplayMetrics().density); l.setPadding(pd, pd * 2, pd, pd);
        TextView t = new TextView(this); t.setText("System overlay"); t.setTextSize(24); l.addView(t);
        status = new TextView(this); l.addView(status);
        toggle = new Button(this); l.addView(toggle);
        Button choose = new Button(this); choose.setText("Choose look (presets + my saved looks)"); l.addView(choose);
        TextView it = new TextView(this); it.setText("\nIntensity"); l.addView(it);
        SeekBar s = new SeekBar(this); s.setMax(100); s.setProgress(sp.getInt("intensity", 100)); l.addView(s);
        s.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar sb, int v, boolean u) { sp.edit().putInt("intensity", v).apply(); push(); }
            public void onStartTrackingTouch(SeekBar sb) {} public void onStopTrackingTouch(SeekBar sb) {}
        });
        TextView n = new TextView(this); n.setText("\nCreate or edit looks in the Editor, save them, then pick them here."); l.addView(n);
        toggle.setOnClickListener(v -> {
            if (OverlayService.running) stopService(new Intent(this, OverlayService.class));
            else if (!Settings.canDrawOverlays(this)) {
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())));
                Toast.makeText(this, "Allow 'Display over other apps', then press Start", Toast.LENGTH_LONG).show();
            } else {
                Intent i = new Intent(this, OverlayService.class);
                if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
            }
            toggle.postDelayed(this::refresh, 400);
        });
        choose.setOnClickListener(v -> {
            final List<String> items = new ArrayList<>(); for (String n2 : Looks.PN) items.add(n2);
            final List<String> saved = Looks.savedNames(sp); for (String n2 : saved) items.add("* " + n2);
            new AlertDialog.Builder(this).setTitle("Choose look").setItems(items.toArray(new String[0]), (d, i) -> {
                float[] val; String name;
                if (i < Looks.PN.length) { val = Looks.preset(i); name = Looks.PN[i]; }
                else { name = saved.get(i - Looks.PN.length); val = Looks.parse(sp.getString("look_" + name, null)); }
                sp.edit().putString("active", Looks.csv(val)).putString("activeName", name).apply(); refresh(); push();
            }).show();
        });
        setContentView(l); refresh();
    }
    void push() { if (OverlayService.running) startService(new Intent(this, OverlayService.class)); }
    void refresh() {
        toggle.setText(OverlayService.running ? "Stop overlay" : "Start overlay");
        status.setText("Look: " + sp.getString("activeName", "none") + "\nOverlay: " + (OverlayService.running ? "ON" : "OFF") + "\n");
    }
    @Override protected void onResume() { super.onResume(); refresh(); }
}
