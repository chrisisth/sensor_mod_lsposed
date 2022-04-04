package org.wats_tool.sensor_mod_lsposed;

import android.os.Handler;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorDirectChannel;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import de.robv.android.xposed.XC_MethodHook;

public class Hook implements IXposedHookLoadPackage {
    private final String LOG_TAG = "sensor_mod  ";
    private final String P_VERSION = "sensor_mod version 0.1.0-a1";

    // system_server
    private final String HOOK_TARGET = "android";

    private final ConfigHost ch = new ConfigHost();

    @Override
    public void handleLoadPackage(final LoadPackageParam p) throws Throwable {
        XposedBridge.log(LOG_TAG + "packageName: " + p.packageName);
        // 只需要 hook system_server
        if (!p.packageName.equals(HOOK_TARGET)) {
            return;
        }

        XposedBridge.log(LOG_TAG + "hook system_server, " + P_VERSION);

        findAndHookMethod("android.hardware.SystemSensorManager", p.classLoader, "registerListenerImpl",
            SensorEventListener.class, Sensor.class, int.class, Handler.class, int.class, int.class,
        new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam pa) throws Throwable {
                // protected boolean registerListenerImpl(
                // [0] SensorEventListener listener,
                // [1] Sensor sensor,
                // [2] int delayUs,
                // [3] Handler handler,
                // [4] int maxBatchReportLatencyUs,
                // [5] int reservedFlags
                // )
                XposedBridge.log(LOG_TAG + "SystemSensorManager.registerListenerImpl()");

                SensorEventListener listener = (SensorEventListener) pa.args[0];
                Sensor sensor = (Sensor) pa.args[1];

                debugSensor(sensor, listener);

                // TODO
            }
            @Override
            protected void afterHookedMethod(MethodHookParam pa) throws Throwable {
                // nothing to do
            }
        });
    }

    public void debugSensor(Sensor s, SensorEventListener listener) {
        XposedBridge.log(LOG_TAG + "sensor id  " + s.getId());
        XposedBridge.log(LOG_TAG + "sensor type  " + s.getType() + "  " + s.getStringType());
        XposedBridge.log(LOG_TAG + "sensor name  " + s.getName());

        String fullClassName =
            listener.getClass().getEnclosingClass() != null
            ? listener.getClass().getEnclosingClass().getName()
            : listener.getClass().getName();
        XposedBridge.log(LOG_TAG + "fullClassName  " + fullClassName);
    }

    class MyListener implements SensorEventListener {
        private ConfigHost ch;
        private SensorEventListener next;

        public MyListener(ConfigHost ch, SensorEventListener next) {
            this.ch = ch;
            this.next = next;
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // TODO 
            XposedBridge.log(LOG_TAG + "onAccuracyChanged()  " + accuracy);
            debugSensor(sensor, next);

            next.onAccuracyChanged(sensor, accuracy);
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            // TODO
            next.onSensorChanged(event);
        }
    }

    // 配置文件加载
    class ConfigHost {
        // 配置文件路径
        private final String CONF_PATH = "/data/dalvik-cache/sensor_mod.txt";
        // 传感器模式: f 固定数值
        private final String MODE_F = "f";
        // 传感器模式: d 偏移
        private final String MODE_D = "d";

        // 配置文件特殊字符: # 注释
        private final String C_COMMENT = "#";
        // 配置文件特殊字符: (空格) 行内分隔符
        private final String C_SEP = " ";

        // 尝试加载配置文件, 如果文件无变更 (修改时间) 则不重新加载
        public void load() {
            XposedBridge.log(LOG_TAG + "ConfigHost.load()  " + CONF_PATH);

            // TODO
        }

        // TODO
    }
}
