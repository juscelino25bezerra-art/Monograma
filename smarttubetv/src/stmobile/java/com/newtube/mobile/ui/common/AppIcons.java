package com.newtube.mobile.ui.common;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;

import com.liskovsoft.smartyoutubetv2.common.misc.NetPath;

/**
 * MONOGRAMA(icon): Settings > Appearance > App icon. The launcher icon is whichever of the two
 * activity-aliases of Splash is enabled (stmobile AndroidManifest: LauncherGrafite, the default,
 * and LauncherCoral). The package manager is the only record of the choice, so it survives
 * reinstalls of the same build and needs no preference.
 */
public final class AppIcons {
    public static final int GRAFITE = 0;
    public static final int CORAL = 1;

    private static final String ALIAS_GRAFITE = "com.newtube.mobile.LauncherGrafite";
    private static final String ALIAS_CORAL = "com.newtube.mobile.LauncherCoral";

    private AppIcons() {
    }

    /** The icon on the launcher now. */
    public static int get(@NonNull Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            int coral = pm.getComponentEnabledSetting(component(context, ALIAS_CORAL));
            return coral == PackageManager.COMPONENT_ENABLED_STATE_ENABLED ? CORAL : GRAFITE;
        } catch (RuntimeException e) {
            return GRAFITE;
        }
    }

    /**
     * Switch the launcher icon. The new entry is enabled before the old one is disabled, so the
     * app is never left without one. DONT_KILL_APP keeps this process alive; the launcher picks the
     * change up on its own, which can take a few seconds (and drops a home-screen shortcut of the
     * old entry on some launchers - the setting's summary says so).
     */
    public static void set(@NonNull Context context, int icon) {
        if (icon == get(context)) {
            return;
        }
        PackageManager pm = context.getPackageManager();
        ComponentName on = component(context, icon == CORAL ? ALIAS_CORAL : ALIAS_GRAFITE);
        ComponentName off = component(context, icon == CORAL ? ALIAS_GRAFITE : ALIAS_CORAL);
        try {
            pm.setComponentEnabledSetting(on, PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP);
            pm.setComponentEnabledSetting(off, PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
            NetPath.log("app-icon set=" + (icon == CORAL ? "coral" : "grafite"));
        } catch (RuntimeException e) {
            NetPath.log("app-icon set failed " + e.getClass().getSimpleName());
        }
    }

    private static ComponentName component(Context context, String alias) {
        return new ComponentName(context.getPackageName(), alias);
    }
}
