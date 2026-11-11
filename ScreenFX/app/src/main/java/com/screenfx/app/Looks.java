package com.screenfx.app;
import android.content.SharedPreferences; import java.util.*;
public class Looks {
    public static final int N = 15;
    public static final String[] KEYS = {"Brightness","Contrast","Saturation","Hue","Warmth","Tint","Fade","Shadows","Highlights","Sharpness","Grain","Vignette","Haze / glow","Light leak","Scanlines"};
    // overlay support: 2 = full, 1 = one direction / approximate, 0 = editor only
    public static final int[] OVL = {2,1,1,0,2,2,2,1,1,0,2,2,2,2,2};
    public static final int[] MIN = {-100,-100,-100,-100,-100,-100,0,-100,-100,0,0,0,0,0,0};
    public static final String[] PN = {"Original","Time","Latte","Moody","Vintage","Golden hour","Cinematic","Virtual HDR","Cyberpunk","Retro CRT","Dreamy","Black & white"};
    static final int[][] PV = {
        {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
        {0,8,-10,0,25,0,20,10,-10,0,15,25,0,0,0},
        {5,-8,-15,0,35,3,15,15,-10,0,5,10,10,0,0},
        {-8,18,-20,0,-20,-4,8,-15,-10,10,12,40,0,0,0},
        {0,-5,-25,0,30,5,30,10,-15,0,35,40,0,15,0},
        {5,5,10,0,55,5,8,10,-5,0,8,25,15,35,0},
        {-3,22,-8,0,-12,-6,5,-8,-8,15,18,45,0,0,0},
        {2,28,25,0,5,0,0,35,-30,35,0,12,0,0,0},
        {-4,20,30,-25,-15,25,0,0,0,10,10,40,0,0,0},
        {0,10,-5,0,10,0,10,0,0,0,25,55,0,0,55},
        {8,-12,5,0,12,5,12,20,-10,0,5,15,45,20,0},
        {0,15,-100,0,0,0,5,0,0,10,15,25,0,0,0}};
    public static float[] preset(int i) { float[] f = new float[N]; for (int k = 0; k < N; k++) f[k] = PV[i][k]; return f; }
    public static String csv(float[] p) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < N; i++) { if (i > 0) s.append(','); s.append(Math.round(p[i])); }
        return s.toString();
    }
    public static float[] parse(String s) {
        float[] p = new float[N]; if (s == null) return p;
        String[] a = s.split(",");
        for (int i = 0; i < N && i < a.length; i++) { try { p[i] = Float.parseFloat(a[i]); } catch (Exception e) { } }
        return p;
    }
    public static List<String> savedNames(SharedPreferences sp) {
        List<String> l = new ArrayList<>();
        for (String n : sp.getString("names", "").split("\n")) if (!n.isEmpty()) l.add(n);
        return l;
    }
    public static void save(SharedPreferences sp, String name, float[] p) {
        name = name.replace("\n", " ").trim(); if (name.isEmpty()) name = "My look";
        List<String> l = savedNames(sp); if (!l.contains(name)) l.add(name);
        StringBuilder j = new StringBuilder();
        for (int i = 0; i < l.size(); i++) { if (i > 0) j.append('\n'); j.append(l.get(i)); }
        sp.edit().putString("names", j.toString()).putString("look_" + name, csv(p)).apply();
    }
}
