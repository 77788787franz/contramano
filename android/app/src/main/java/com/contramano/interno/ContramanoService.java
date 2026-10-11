package com.contramano.interno;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Path;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.getcapacitor.JSObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Servicio de accesibilidad que:
 *  1) abre cada app de la cola,
 *  2) navega haciendo clic en botones durante X tiempo,
 *  3) la cierra (Forzar detención o Home + kill) y pasa a la siguiente.
 *
 * Toda la cola corre AQUÍ (nativo) para que siga funcionando aunque
 * el WebView de Capacitor quede en segundo plano.
 */
public class ContramanoService extends AccessibilityService {

    public interface Listener {
        void onEvent(JSObject data);
    }

    public static ContramanoService instance;
    public static Listener listener;

    // Palabras que NUNCA se van a tocar (seguridad)
    private static final List<String> BLACKLIST = Arrays.asList(
            "uninstall", "desinstalar", "delete account", "eliminar cuenta", "borrar cuenta",
            "factory reset", "restablecer", "formatear", "llamar", "call ", "emergencia",
            "emergency", "comprar", "buy now", "pagar", "pay now", "purchase", "suscribir",
            "subscribe", "donate", "donar"
    );

    private static final List<String> FORCE_STOP_TEXTS = Arrays.asList(
            "force stop", "forzar detención", "forzar detencion", "forzar parada",
            "forçar parada", "forcer l'arrêt", "force-stop", "强行停止", "强制停止"
    );

    private static final List<String> CONFIRM_TEXTS = Arrays.asList(
            "ok", "force stop", "forzar detención", "forzar detencion", "forzar parada",
            "aceptar", "accept", "yes", "sí", "si", "forçar parada", "确定"
    );

    private static final List<String> PERMISSION_PACKAGES = Arrays.asList(
            "com.android.permissioncontroller",
            "com.google.android.permissioncontroller",
            "com.android.packageinstaller",
            "com.google.android.packageinstaller"
    );

    // Orden de prioridad para diálogos de permisos (coincidencia exacta)
    private static final List<String> ALLOW_TEXTS = Arrays.asList(
            "while using the app", "mientras la app está en uso", "mientras se usa la app",
            "mientras usas la app", "allow", "permitir", "ok", "aceptar", "accept"
    );

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();

    // Estado de la sesión
    private boolean running = false;
    private boolean closing = false;
    private List<String> queue = new ArrayList<>();
    private List<String> targetTexts = new ArrayList<>();
    private int index = 0;
    private int cycle = 1;
    private int failCount = 0;
    private long durationMs = 120000L;
    private long intervalMs = 1200L;
    private String mode = "random";
    private boolean loop = false;
    private boolean forceStop = true;
    private String currentPkg = "";
    private long appStartTime = 0L;
    private long lastRelaunch = 0L;
    private int seqCounter = 0;

    // Estado del cierre con "Forzar detención"
    private long fsStart = 0L;
    private int fsPhase = 0;

    // ───────────────────────── Ciclo de vida del servicio ─────────────────────────

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        emit("serviceConnected", null);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        stopInternal(false);
        instance = null;
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        stopInternal(false);
        instance = null;
        super.onDestroy();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // No necesitamos reaccionar a eventos: usamos un bucle propio (tick).
    }

    @Override
    public void onInterrupt() {
    }

    // ───────────────────────── API pública (la usa el plugin) ─────────────────────────

    public boolean startSession(List<String> pkgs, int durationSec, int intervalMs, String mode,
                                List<String> texts, boolean loop, boolean forceStop) {
        stopInternal(false);
        if (pkgs == null || pkgs.isEmpty()) return false;

        this.queue = new ArrayList<>(pkgs);
        this.targetTexts = texts == null ? new ArrayList<String>() : new ArrayList<>(texts);
        this.durationMs = Math.max(10, durationSec) * 1000L;
        this.intervalMs = Math.max(300, intervalMs);
        this.mode = mode == null ? "random" : mode;
        this.loop = loop;
        this.forceStop = forceStop;
        this.index = 0;
        this.cycle = 1;
        this.failCount = 0;
        this.running = true;

        emit("started", null);
        launchCurrent();
        return true;
    }

    public void stopSession() {
        stopInternal(true);
        emit("stopped", null);
    }

    public boolean isRunning() { return running; }
    public int getIndex() { return index; }
    public int getTotal() { return queue.size(); }
    public int getCycle() { return cycle; }
    public String getCurrentPkg() { return currentPkg; }

    // ───────────────────────── Control de la cola ─────────────────────────

    private void launchCurrent() {
        if (!running) return;
        currentPkg = queue.get(index);
        closing = false;
        seqCounter = 0;
        lastRelaunch = SystemClock.elapsedRealtime();

        Intent i = getPackageManager().getLaunchIntentForPackage(currentPkg);
        if (i == null) {
            log("No se pudo abrir: " + currentPkg);
            failCount++;
            if (failCount >= queue.size()) {
                stopInternal(true);
                emit("finished", null);
                return;
            }
            advance();
            return;
        }
        failCount = 0;
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try {
            startActivity(i);
        } catch (Exception e) {
            log("Error al abrir " + currentPkg + ": " + e.getMessage());
        }

        appStartTime = SystemClock.elapsedRealtime();
        JSObject d = new JSObject();
        d.put("index", index);
        d.put("total", queue.size());
        d.put("packageName", currentPkg);
        d.put("cycle", cycle);
        emit("appStarted", d);

        handler.removeCallbacks(tickRunnable);
        handler.postDelayed(tickRunnable, 3000); // espera a que cargue la app
    }

    private void advance() {
        if (!running) return;
        index++;
        if (index >= queue.size()) {
            if (loop) {
                index = 0;
                cycle++;
            } else {
                stopInternal(true);
                emit("finished", null);
                return;
            }
        }
        launchCurrent();
    }

    private void finishCurrent() {
        closing = true;
        handler.removeCallbacks(tickRunnable);
        JSObject d = new JSObject();
        d.put("packageName", currentPkg);
        emit("appFinished", d);

        if (forceStop) {
            startForceStop();
        } else {
            goHomeAndKill();
            handler.postDelayed(advanceRunnable, 900);
        }
    }

    private void goHomeAndKill() {
        performGlobalAction(GLOBAL_ACTION_HOME);
        try {
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null && currentPkg != null) am.killBackgroundProcesses(currentPkg);
        } catch (Exception ignored) {
        }
    }

    private void stopInternal(boolean bringOwnAppToFront) {
        boolean wasRunning = running;
        running = false;
        closing = false;
        handler.removeCallbacks(tickRunnable);
        handler.removeCallbacks(fsRunnable);
        handler.removeCallbacks(advanceRunnable);
        if (wasRunning && bringOwnAppToFront) {
            try {
                Intent i = getPackageManager().getLaunchIntentForPackage(getPackageName());
                if (i != null) {
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(i);
                }
            } catch (Exception ignored) {
            }
        }
    }

    private final Runnable advanceRunnable = new Runnable() {
        @Override
        public void run() {
            advance();
        }
    };

    // ───────────────────────── Cierre con "Forzar detención" ─────────────────────────

    private void startForceStop() {
        fsStart = SystemClock.elapsedRealtime();
        fsPhase = 0;
        try {
            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + currentPkg));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception e) {
            goHomeAndKill();
            handler.postDelayed(advanceRunnable, 900);
            return;
        }
        handler.removeCallbacks(fsRunnable);
        handler.postDelayed(fsRunnable, 1200);
    }

    private final Runnable fsRunnable = new Runnable() {
        @Override
        public void run() {
            if (!running) return;

            // Tiempo agotado: plan B
            if (SystemClock.elapsedRealtime() - fsStart > 10000) {
                goHomeAndKill();
                handler.postDelayed(advanceRunnable, 900);
                return;
            }

            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root != null) {
                if (fsPhase == 0) {
                    if (clickByTexts(root, FORCE_STOP_TEXTS, false)) fsPhase = 1;
                } else if (fsPhase == 1) {
                    if (clickByTexts(root, CONFIRM_TEXTS, true)) {
                        fsPhase = 2;
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                performGlobalAction(GLOBAL_ACTION_HOME);
                                handler.postDelayed(advanceRunnable, 700);
                            }
                        }, 900);
                        return;
                    }
                }
            }
            handler.postDelayed(this, 700);
        }
    };

    // ───────────────────────── Bucle principal de clics ─────────────────────────

    private final Runnable tickRunnable = new Runnable() {
        @Override
        public void run() {
            if (!running || closing) return;

            long elapsed = SystemClock.elapsedRealtime() - appStartTime;
            if (elapsed >= durationMs) {
                finishCurrent();
                return;
            }
            try {
                doAction();
            } catch (Exception e) {
                log("Error en acción: " + e.getMessage());
            }
            handler.postDelayed(this, intervalMs);
        }
    };

    private void doAction() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null || root.getPackageName() == null) return;
        String pkg = root.getPackageName().toString();

        // Diálogo de permisos del sistema → aceptar para poder seguir explorando
        if (PERMISSION_PACKAGES.contains(pkg)) {
            clickByTexts(root, ALLOW_TEXTS, true);
            return;
        }

        // Si salimos de la app (notificaciones, otra app, etc.) → volver a ella
        if (!pkg.equals(currentPkg)) {
            long now = SystemClock.elapsedRealtime();
            if (now - lastRelaunch > 4000) {
                lastRelaunch = now;
                Intent i = getPackageManager().getLaunchIntentForPackage(currentPkg);
                if (i != null) {
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    try {
                        startActivity(i);
                    } catch (Exception ignored) {
                    }
                }
            }
            return;
        }

        // Modo "texts": primero intenta pulsar los textos indicados
        if ("texts".equals(mode) && !targetTexts.isEmpty()) {
            if (clickByTexts(root, targetTexts, false)) return;
        }

        List<AccessibilityNodeInfo> clickables = new ArrayList<>();
        List<AccessibilityNodeInfo> editables = new ArrayList<>();
        List<AccessibilityNodeInfo> scrollables = new ArrayList<>();
        collect(root, clickables, editables, scrollables, 0);

        double r = random.nextDouble();

        // Rellenar un campo de texto de vez en cuando
        if (!editables.isEmpty() && r < 0.25) {
            AccessibilityNodeInfo e = editables.get(random.nextInt(editables.size()));
            Bundle args = new Bundle();
            args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "Prueba123");
            e.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
            e.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
            log("Texto escrito en un campo");
            return;
        }

        // Scroll ocasional
        if (!scrollables.isEmpty() && (clickables.isEmpty() ? r < 0.6 : r > 0.88)) {
            AccessibilityNodeInfo s = scrollables.get(random.nextInt(scrollables.size()));
            s.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
            log("Scroll");
            return;
        }

        // Botón "atrás" ocasional para explorar otras pantallas
        if (clickables.isEmpty() || r > 0.94) {
            performGlobalAction(GLOBAL_ACTION_BACK);
            log("Atrás");
            return;
        }

        AccessibilityNodeInfo target;
        if ("sequential".equals(mode)) {
            target = clickables.get(seqCounter % clickables.size());
            seqCounter++;
        } else {
            target = clickables.get(random.nextInt(clickables.size()));
        }
        clickNode(target);
    }

    // ───────────────────────── Utilidades de nodos ─────────────────────────

    private void collect(AccessibilityNodeInfo n,
                         List<AccessibilityNodeInfo> clickables,
                         List<AccessibilityNodeInfo> editables,
                         List<AccessibilityNodeInfo> scrollables,
                         int depth) {
        if (n == null || depth > 40) return;
        if (!n.isVisibleToUser()) return;

        if (n.isEditable() && n.isEnabled()) {
            editables.add(n);
        } else if (n.isClickable() && n.isEnabled() && !isBlacklisted(n)) {
            clickables.add(n);
        }
        if (n.isScrollable()) scrollables.add(n);

        int count = n.getChildCount();
        for (int i = 0; i < count; i++) {
            collect(n.getChild(i), clickables, editables, scrollables, depth + 1);
        }
    }

    private boolean isBlacklisted(AccessibilityNodeInfo n) {
        String label = labelOf(n, 0).toLowerCase(Locale.ROOT);
        for (String bad : BLACKLIST) {
            if (label.contains(bad)) return true;
        }
        return false;
    }

    private String labelOf(AccessibilityNodeInfo n, int depth) {
        if (n == null || depth > 2) return "";
        StringBuilder sb = new StringBuilder();
        if (n.getText() != null) sb.append(n.getText()).append(' ');
        if (n.getContentDescription() != null) sb.append(n.getContentDescription()).append(' ');
        if (n.getViewIdResourceName() != null) sb.append(n.getViewIdResourceName()).append(' ');
        int count = n.getChildCount();
        for (int i = 0; i < count; i++) {
            sb.append(labelOf(n.getChild(i), depth + 1));
        }
        return sb.toString();
    }

    private AccessibilityNodeInfo clickableSelfOrParent(AccessibilityNodeInfo n) {
        AccessibilityNodeInfo cur = n;
        for (int i = 0; i < 5 && cur != null; i++) {
            if (cur.isClickable() && cur.isEnabled()) return cur;
            cur = cur.getParent();
        }
        return null;
    }

    private boolean clickByTexts(AccessibilityNodeInfo root, List<String> texts, boolean exact) {
        for (String t : texts) {
            if (t == null || t.trim().isEmpty()) continue;
            String wanted = t.trim();
            List<AccessibilityNodeInfo> found = root.findAccessibilityNodeInfosByText(wanted);
            if (found == null) continue;
            for (AccessibilityNodeInfo n : found) {
                if (exact) {
                    CharSequence ct = n.getText();
                    if (ct == null || !ct.toString().trim().equalsIgnoreCase(wanted)) continue;
                }
                AccessibilityNodeInfo target = clickableSelfOrParent(n);
                if (target != null) {
                    clickNode(target);
                    return true;
                }
            }
        }
        return false;
    }

    private void clickNode(AccessibilityNodeInfo n) {
        String label = labelOf(n, 1).trim();
        if (label.length() > 40) label = label.substring(0, 40);
        boolean ok = n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        if (!ok) {
            Rect rect = new Rect();
            n.getBoundsInScreen(rect);
            tap(rect.centerX(), rect.centerY());
        }
        log("Clic: " + (label.isEmpty() ? "(sin texto)" : label));
    }

    private void tap(int x, int y) {
        if (Build.VERSION.SDK_INT < 24) return;
        Path p = new Path();
        p.moveTo(x, y);
        GestureDescription.StrokeDescription stroke = new GestureDescription.StrokeDescription(p, 0, 60);
        dispatchGesture(new GestureDescription.Builder().addStroke(stroke).build(), null, null);
    }

    // ───────────────────────── Eventos hacia JS ─────────────────────────

    private void log(String msg) {
        JSObject d = new JSObject();
        d.put("message", msg);
        emit("log", d);
    }

    private void emit(String type, JSObject data) {
        Listener l = listener;
        if (l == null) return;
        JSObject d = data == null ? new JSObject() : data;
        d.put("type", type);
        try {
            l.onEvent(d);
        } catch (Exception ignored) {
        }
    }
}