package com.screenfx.app;
import android.app.Activity; import android.app.AlertDialog; import android.content.*; import android.graphics.*; import android.net.Uri;
import android.os.Build; import android.os.Bundle; import android.provider.Settings; import android.view.ViewGroup; import android.widget.*; import java.io.InputStream; import java.util.*;
public class EditorActivity extends Activity {
    SharedPreferences sp; PreviewView pv; float[] p = new float[Looks.N]; SeekBar[] bars = new SeekBar[Looks.N]; TextView[] labels = new TextView[Looks.N];
    Button modeBtn, liveBtn; boolean overlayMode, loading, live;
    android.os.Handler hd = new android.os.Handler(android.os.Looper.getMainLooper());
    Runnable pushRun = () -> {
        sp.edit().putString("active", Looks.csv(p)).putString("activeName", "Live editor").putInt("intensity", 100).apply();
        if (OverlayService.running) startService(new Intent(this, OverlayService.class));
    };
    void changed() {
        pv.setParams(live ? new float[Looks.N] : p);
        if (live) { hd.removeCallbacks(pushRun); hd.postDelayed(pushRun, 60); }
    }
    int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density + 0.5f); }
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b); sp = getSharedPreferences("fx2", 0);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(0, dp(24), 0, 0);
        pv = new PreviewView(this);
        root.addView(pv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (getResources().getDisplayMetrics().heightPixels * 0.36f)));
        pv.setSource(sampleBitmap());
        LinearLayout row = new LinearLayout(this); root.addView(row);
        modeBtn = new Button(this); modeBtn.setText("Preview: Editor (full)"); row.addView(modeBtn, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2f));
        Button pick = new Button(this); pick.setText("Photo"); row.addView(pick, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Button reset = new Button(this); reset.setText("Reset"); row.addView(reset, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        modeBtn.setOnClickListener(v -> { overlayMode = !overlayMode; modeBtn.setText(overlayMode ? "Preview: Overlay (system-wide)" : "Preview: Editor (full)"); pv.setOverlayMode(overlayMode); });
        pick.setOnClickListener(v -> { Intent i = new Intent(Intent.ACTION_GET_CONTENT); i.setType("image/*"); startActivityForResult(i, 1); });
        reset.setOnClickListener(v -> setAll(new float[Looks.N]));
        liveBtn = new Button(this); liveBtn.setText("Live on whole screen: OFF (tap to turn on)"); root.addView(liveBtn);
        liveBtn.setOnClickListener(v -> {
            if (!live) {
                if (!Settings.canDrawOverlays(this)) {
                    startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())));
                    Toast.makeText(this, "Allow 'Display over other apps', then tap again", Toast.LENGTH_LONG).show(); return;
                }
                live = true; pushRun.run();
                if (!OverlayService.running) {
                    Intent i = new Intent(this, OverlayService.class);
                    if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
                }
                liveBtn.setText("Live on whole screen: ON (sliders change the screen now)");
            } else { live = false; liveBtn.setText("Live on whole screen: OFF (overlay stays as last set)"); }
            changed();
        });
        ScrollView sv = new ScrollView(this); LinearLayout col = new LinearLayout(this); col.setOrientation(LinearLayout.VERTICAL); col.setPadding(dp(12), 0, dp(12), dp(24)); sv.addView(col);
        root.addView(sv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        TextView ph = new TextView(this); ph.setText("Presets"); col.addView(ph);
        HorizontalScrollView hs = new HorizontalScrollView(this); LinearLayout hr = new LinearLayout(this); hs.addView(hr); col.addView(hs);
        for (int k = 0; k < Looks.PN.length; k++) {
            final int kk = k; Button pb = new Button(this); pb.setText(Looks.PN[k]); hr.addView(pb);
            pb.setOnClickListener(v -> setAll(Looks.preset(kk)));
        }
        TextView lg = new TextView(this); lg.setTextSize(12);
        lg.setText("\n[ok] works in overlay   [~] partly / one direction   [x] editor only. Use 'Preview: Overlay' to see the real system-wide result.\n"); col.addView(lg);
        for (int i = 0; i < Looks.N; i++) {
            final int idx = i; labels[i] = new TextView(this); col.addView(labels[i]);
            bars[i] = new SeekBar(this); bars[i].setMax(100 - Looks.MIN[i]); bars[i].setProgress(-Looks.MIN[i]); col.addView(bars[i]);
            bars[i].setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(SeekBar sb, int v, boolean u) { p[idx] = v + Looks.MIN[idx]; label(idx); if (!loading) changed(); }
                public void onStartTrackingTouch(SeekBar sb) {} public void onStopTrackingTouch(SeekBar sb) {}
            });
            label(i);
        }
        Button save = new Button(this); save.setText("Save as my look"); col.addView(save);
        Button load = new Button(this); load.setText("Load my saved look"); col.addView(load);
        Button use = new Button(this); use.setText("Use this as the overlay look"); col.addView(use);
        save.setOnClickListener(v -> askSave());
        load.setOnClickListener(v -> {
            final List<String> names = Looks.savedNames(sp);
            if (names.isEmpty()) { Toast.makeText(this, "No saved looks yet", Toast.LENGTH_SHORT).show(); return; }
            new AlertDialog.Builder(this).setTitle("My looks").setItems(names.toArray(new String[0]),
                (d, i) -> setAll(Looks.parse(sp.getString("look_" + names.get(i), null)))).show();
        });
        use.setOnClickListener(v -> {
            sp.edit().putString("active", Looks.csv(p)).putString("activeName", "Editor look").apply();
            if (OverlayService.running) startService(new Intent(this, OverlayService.class));
            Toast.makeText(this, "Set. Open 'System overlay' and press Start.", Toast.LENGTH_LONG).show();
        });
        setContentView(root);
    }
    void label(int i) {
        String tag = Looks.OVL[i] == 2 ? "[ok]" : (Looks.OVL[i] == 1 ? "[~]" : "[x]");
        labels[i].setText(Looks.KEYS[i] + ": " + (int) p[i] + "   " + tag);
    }
    void setAll(float[] np) {
        loading = true;
        for (int i = 0; i < Looks.N; i++) { p[i] = np[i]; bars[i].setProgress((int) p[i] - Looks.MIN[i]); label(i); }
        loading = false; changed();
    }
    void askSave() {
        final EditText et = new EditText(this); et.setHint("Look name");
        new AlertDialog.Builder(this).setTitle("Save look").setView(et)
            .setPositiveButton("Save", (d, w) -> { Looks.save(sp, et.getText().toString(), p); Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show(); })
            .setNegativeButton("Cancel", null).show();
    }
    @Override protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == 1 && res == RESULT_OK && data != null && data.getData() != null) {
            Bitmap bm = decode(data.getData()); if (bm != null) pv.setSource(bm); else Toast.makeText(this, "Could not open image", Toast.LENGTH_SHORT).show();
        }
    }
    Bitmap decode(Uri u) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options(); o.inJustDecodeBounds = true;
            InputStream is = getContentResolver().openInputStream(u); BitmapFactory.decodeStream(is, null, o); is.close();
            int s = 1; while (Math.max(o.outWidth, o.outHeight) / s > 1000) s *= 2;
            o = new BitmapFactory.Options(); o.inSampleSize = s;
            is = getContentResolver().openInputStream(u); Bitmap b = BitmapFactory.decodeStream(is, null, o); is.close();
            float k = 520f / Math.max(b.getWidth(), b.getHeight());
            if (k < 1f) b = Bitmap.createScaledBitmap(b, (int) (b.getWidth() * k), (int) (b.getHeight() * k), true);
            return b;
        } catch (Exception e) { return null; }
    }
    Bitmap sampleBitmap() {
        int W = 360, H = 480; Bitmap b = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888); Canvas c = new Canvas(b); Paint pt = new Paint(Paint.ANTI_ALIAS_FLAG);
        pt.setShader(new LinearGradient(0, 0, 0, H * 0.6f, new int[]{0xFF2E6FD8, 0xFF8FC1F0, 0xFFFFD3A0}, null, Shader.TileMode.CLAMP)); c.drawRect(0, 0, W, H, pt); pt.setShader(null);
        pt.setColor(0xFFFFF2B0); c.drawCircle(280, 120, 34, pt);
        pt.setColor(0xFF4A5A6A); c.drawOval(new RectF(-80, 250, 260, 420), pt);
        pt.setColor(0xFF2F7D3C); c.drawOval(new RectF(80, 290, 460, 520), pt);
        pt.setColor(0xFFE0A98A); c.drawCircle(110, 300, 42, pt);
        pt.setColor(0xFF3B2A20); c.drawArc(new RectF(68, 256, 152, 320), 180, 180, true, pt);
        int[] gr = {0xFF000000, 0xFF333333, 0xFF666666, 0xFF999999, 0xFFCCCCCC, 0xFFFFFFFF};
        int[] sw = {0xFFE53935, 0xFFFB8C00, 0xFFFDD835, 0xFF43A047, 0xFF1E88E5, 0xFF8E24AA};
        for (int i = 0; i < 6; i++) { pt.setColor(gr[i]); c.drawRect(i * 60, 400, i * 60 + 60, 430, pt); pt.setColor(sw[i]); c.drawRect(i * 60, 430, i * 60 + 60, H, pt); }
        return b;
    }
}
