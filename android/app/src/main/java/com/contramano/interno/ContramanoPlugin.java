package com.contramano.interno;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.provider.Settings;
import android.util.Base64;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@CapacitorPlugin(name = "Contramano")
public class ContramanoPlugin extends Plugin {

    @Override
    public void load() {
        ContramanoService.listener = new ContramanoService.Listener() {
            @Override
            public void onEvent(JSObject data) {
                notifyListeners("sessionEvent", data);
            }
        };
    }

    @Override
    protected void handleOnDestroy() {
        ContramanoService.listener = null;
    }

    /** Lista las apps instaladas que tienen icono en el launcher. */
    @PluginMethod
    public void getInstalledApps(PluginCall call) {
        try {
            PackageManager pm = getContext().getPackageManager();
            Intent main = new Intent(Intent.ACTION_MAIN, null);
            main.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> list = pm.queryIntentActivities(main, 0);

            Set<String> seen = new HashSet<>();
            List<String[]> rows = new ArrayList<>();
            String own = getContext().getPackageName();

            for (ResolveInfo ri : list) {
                String pkg = ri.activityInfo.packageName;
                if (pkg.equals(own) || !seen.add(pkg)) continue;
                String label = String.valueOf(ri.loadLabel(pm));
                String icon = "";
                try {
                    icon = drawableToBase64(ri.loadIcon(pm), 56);
                } catch (Exception ignored) {
                }
                rows.add(new String[]{label, pkg, icon});
            }

            Collections.sort(rows, new Comparator<String[]>() {
                @Override
                public int compare(String[] a, String[] b) {
                    return a[0].compareToIgnoreCase(b[0]);
                }
            });

            JSArray arr = new JSArray();
            for (String[] r : rows) {
                JSObject o = new JSObject();
                o.put("name", r[0]);
                o.put("packageName", r[1]);
                o.put("icon", r[2]);
                arr.put(o);
            }
            JSObject res = new JSObject();
            res.put("apps", arr);
            call.resolve(res);
        } catch (Exception e) {
            call.reject("No se pudo listar las apps: " + e.getMessage());
        }
    }

    @PluginMethod
    public void isServiceEnabled(PluginCall call) {
        JSObject res = new JSObject();
        res.put("enabled", ContramanoService.instance != null);
        call.resolve(res);
    }

    @PluginMethod
    public void openAccessibilitySettings(PluginCall call) {
        Intent i = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getContext().startActivity(i);
        call.resolve();
    }

    @PluginMethod
    public void startSession(PluginCall call) {
        ContramanoService svc = ContramanoService.instance;
        if (svc == null) {
            call.reject("SERVICE_DISABLED");
            return;
        }
        try {
            JSArray pk = call.getArray("packages", new JSArray());
            JSArray tx = call.getArray("texts", new JSArray());
            List<String> pkgs = pk.<String>toList();
            List<String> texts = tx.<String>toList();

            int durationSec = call.getInt("durationSec", 120);
            int intervalMs = call.getInt("intervalMs", 1200);
            String mode = call.getString("mode", "random");
            boolean loop = call.getBoolean("loop", false);
            boolean forceStop = call.getBoolean("forceStop", true);

            boolean ok = svc.startSession(pkgs, durationSec, intervalMs, mode, texts, loop, forceStop);
            if (!ok) {
                call.reject("La cola está vacía");
                return;
            }
            call.resolve();
        } catch (Exception e) {
            call.reject("Error al iniciar: " + e.getMessage());
        }
    }

    @PluginMethod
    public void stopSession(PluginCall call) {
        ContramanoService svc = ContramanoService.instance;
        if (svc != null) svc.stopSession();
        call.resolve();
    }

    @PluginMethod
    public void getStatus(PluginCall call) {
        ContramanoService svc = ContramanoService.instance;
        JSObject r = new JSObject();
        r.put("serviceEnabled", svc != null);
        r.put("running", svc != null && svc.isRunning());
        if (svc != null) {
            r.put("index", svc.getIndex());
            r.put("total", svc.getTotal());
            r.put("cycle", svc.getCycle());
            r.put("packageName", svc.getCurrentPkg());
        }
        call.resolve(r);
    }

    private String drawableToBase64(Drawable d, int size) {
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        d.setBounds(0, 0, size, size);
        d.draw(c);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, bos);
        return "data:image/png;base64," + Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP);
    }
}