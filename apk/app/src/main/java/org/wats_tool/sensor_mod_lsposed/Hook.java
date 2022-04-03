package org.wats_tool.sensor_mod_lsposed;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class Hook implements IXposedHookLoadPackage {
    private final String LOG_TAG = "sersor_mod  ";

    // system_server
    private final String HOOK_TARGET = "android";

    public void handleLoadPackage(final LoadPackageParam p) throws Throwable {
        XposedBridge.log(LOG_TAG + "packageName: " + p.packageName);

        if (p.packageName.equals(HOOK_TARGET)) {
            XposedBridge.log(LOG_TAG + "hook system_server");

            // TODO
        }
    }
}
