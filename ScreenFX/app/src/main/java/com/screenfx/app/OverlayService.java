package com.screenfx.app;
import android.app.*; import android.content.*; import android.content.res.Configuration; import android.graphics.PixelFormat;
import android.os.*; import android.util.DisplayMetrics; import android.view.*;
public class OverlayService extends Service {
    static boolean running; WindowManager wm; FxView view;
    @Override public IBinder onBind(Intent i) { return null; }
    WindowManager.LayoutParams lp() {
        DisplayMetrics m = new DisplayMetrics(); wm.getDefaultDisplay().getRealMetrics(m);
        int type = Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(m.widthPixels, m.heightPixels, type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.TOP | Gravity.LEFT; p.x = 0; p.y = 0; return p;
    }
    @Override public void onCreate() {
        super.onCreate(); running = true;
        Notification.Builder nb;
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            nm.createNotificationChannel(new NotificationChannel("fx", "ScreenFX", NotificationManager.IMPORTANCE_LOW));
            nb = new Notification.Builder(this, "fx");
        } else nb = new Notification.Builder(this);
        PendingIntent pi = PendingIntent.getActivity(this, 0, new Intent(this, OverlayActivity.class), 0);
        startForeground(1, nb.setContentTitle("ScreenFX overlay on").setContentText("Tap to change look")
            .setSmallIcon(android.R.drawable.ic_menu_view).setContentIntent(pi).build());
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        view = new FxView(this); wm.addView(view, lp());
    }
    @Override public int onStartCommand(Intent i, int f, int id) { if (view != null) view.reload(); return START_STICKY; }
    @Override public void onConfigurationChanged(Configuration c) {
        super.onConfigurationChanged(c); if (view != null) { wm.updateViewLayout(view, lp()); view.reload(); }
    }
    @Override public void onDestroy() { running = false; if (view != null) wm.removeView(view); super.onDestroy(); }
}
