package app.androglass.xposed;

import android.util.Log;
import android.view.View;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.WeakHashMap;
import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam;
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam;

/** Hooks only the known status-bar host; missing Samsung classes leave the stock UI alone. */
public final class AndroGlassModule extends XposedModule {
    private static final String TAG = "AndroGlass";
    private final WeakHashMap<View, WeakReference<StatusBarSession>> sessions = new WeakHashMap<>();

    @Override public void onModuleLoaded(ModuleLoadedParam param) {
        log(Log.INFO, TAG, "ANDROGLASS_LOADED modern API 102 prototype");
    }

    @Override public void onPackageReady(PackageReadyParam param) {
        if (!"com.android.systemui".equals(param.getPackageName())) return;
        int installed = 0;
        try {
            Class<?> host = Class.forName("com.android.systemui.statusbar.phone.PhoneStatusBarView", false, param.getClassLoader());
            for (String name : new String[]{"onFinishInflate", "onAttachedToWindow"}) {
                try {
                    Method method = host.getDeclaredMethod(name);
                    method.setAccessible(true);
                    hook(method).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept(chain -> {
                        Object result = chain.proceed();
                        Object receiver = chain.getThisObject();
                        if (receiver instanceof View) {
                            View view = (View) receiver;
                            view.post(() -> connect(view));
                        }
                        return result;
                    });
                    installed++;
                } catch (Throwable failure) {
                    log(Log.WARN, TAG, "ANDROGLASS_HOOK_SKIPPED " + name, failure);
                }
            }
        } catch (Throwable failure) {
            log(Log.WARN, TAG, "ANDROGLASS_HOST_UNSUPPORTED; stock status bar retained", failure);
        }
        log(Log.INFO, TAG, "ANDROGLASS_HOOKS_READY count=" + installed);
    }

    /** Runs after the platform callback, on the host's UI thread, without replacing its result. */
    private void connect(View host) {
        try {
            WeakReference<StatusBarSession> reference = sessions.get(host);
            if (reference != null && reference.get() != null) return;
            StatusBarSession session = new StatusBarSession(host);
            sessions.put(host, new WeakReference<>(session));
            session.install();
        } catch (Throwable failure) {
            log(Log.ERROR, TAG, "ANDROGLASS_ATTACH_FAILED", failure);
        }
    }
}
