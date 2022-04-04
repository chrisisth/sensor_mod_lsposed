package org.wats_tool.sensor_mod_lsposed;

import java.util.HashSet;

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
    private final String P_VERSION = "sensor_mod version 0.1.0-a4";

    private final ConfigHost ch = new ConfigHost();

    private final HashSet<ClassLoader> classLoaderSet = new HashSet<>();

    // Hook 初始化执行入口
    @Override
    public void handleLoadPackage(final LoadPackageParam p) throws Throwable {
        // 避免重复 hook
        if (classLoaderSet.contains(p.classLoader)) {
            XposedBridge.log(LOG_TAG + "#### sikp hook  packageName: " + p.packageName);
            return;
        }
        classLoaderSet.add(p.classLoader);

        // 不挑选 hook 目标
        XposedBridge.log(LOG_TAG + "#### HOOK " + P_VERSION + "  packageName: " + p.packageName);

        // protected boolean registerListenerImpl(
        // [0] SensorEventListener listener,
        // [1] Sensor sensor,
        // [2] int delayUs,
        // [3] Handler handler,
        // [4] int maxBatchReportLatencyUs,
        // [5] int reservedFlags
        // )
        findAndHookMethod("android.hardware.SystemSensorManager", p.classLoader, "registerListenerImpl",
            SensorEventListener.class, Sensor.class, int.class, Handler.class, int.class, int.class,
        new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam pa) throws Throwable {
                XposedBridge.log(LOG_TAG + "SystemSensorManager.registerListenerImpl()");

                SensorEventListener listener = (SensorEventListener) pa.args[0];
                Sensor sensor = (Sensor) pa.args[1];

                debugSensor(sensor, getFullClassName(listener));

                // 替换 SensorEventListener
                SensorEventListener l2 = new MyListener(ch, listener, sensor);
                pa.args[0] = l2;
            }
            @Override
            protected void afterHookedMethod(MethodHookParam pa) throws Throwable {
                // nothing to do
            }
        });

        // protected int configureDirectChannelImpl(
        // [0] SensorDirectChannel channel,
        // [1] Sensor sensor,
        // [2] int rate
        // )
        findAndHookMethod("android.hardware.SystemSensorManager", p.classLoader, "configureDirectChannelImpl",
            SensorDirectChannel.class, Sensor.class, int.class,
        new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam pa) throws Throwable {
                XposedBridge.log(LOG_TAG + "SystemSensorManager.configureDirectChannelImpl");

                Sensor sensor = (Sensor) pa.args[1];

                debugSensor(sensor, "(SensorDirectChannel)");
                // TODO
            }
            @Override
            protected void afterHookedMethod(MethodHookParam pa) throws Throwable {
                // nothing to do
            }
        });

        // 加载配置
        ch.load();
    }

    public String getFullClassName(SensorEventListener listener) {
        String fullClassName =
            listener == null ? "" :
            (listener.getClass().getEnclosingClass() != null
            ? listener.getClass().getEnclosingClass().getName()
            : listener.getClass().getName());
        return fullClassName;
    }

    public void debugSensor(Sensor s, String fullClassName) {
        XposedBridge.log(LOG_TAG + "  sensor id " + s.getId() + "  type " + s.getType() + " " + s.getStringType());
        XposedBridge.log(LOG_TAG + "    name " + s.getName() + "  #### fullClassName " + fullClassName);
    }

    class MyListener implements SensorEventListener {
        private ConfigHost ch;
        private SensorEventListener next;
        private Sensor sensor;
        // 首次调试标志
        private boolean first = true;
        // 修改传感器数据的计算方法
        private CalcSensorEventData calc = null;

        public MyListener(ConfigHost ch, SensorEventListener next, Sensor sensor) {
            this.ch = ch;
            this.next = next;
            this.sensor = sensor;

            calc = ch.getCalc(sensor.getType());
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // DEBUG
            XposedBridge.log(LOG_TAG + "onAccuracyChanged()  " + accuracy);
            debugSensor(sensor, getFullClassName(next));

            next.onAccuracyChanged(sensor, accuracy);
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (first) {
                first = false;
                // DEBUG
                XposedBridge.log(LOG_TAG + "onSensorChanged(first)  " + joinArray(event.values));
                debugSensor(sensor, getFullClassName(next));
            }
            // 修改计算
            if (calc != null) {
                calc.calc(event);
            }
            // 透传
            next.onSensorChanged(event);
        }
    }

    // 配置文件加载
    class ConfigHost {
        // 配置文件路径
        private final String CONF_PATH = "/data/dalvik-cache/sensor_mod.txt";
        // 传感器模式: f 固定数值
        private final String MODE_F = "f";
        // 传感器模式: d 偏移相加
        private final String MODE_D = "d";
        // 传感器模式: dr 偏移旋转
        private final String MODE_DR = "dr";

        // 配置文件特殊字符: # 注释
        private final String C_COMMENT = "#";
        // 配置文件特殊字符: (空格) 行内分隔符
        private final String C_SEP = " ";

        // 配置文件最后更新时间 (用于避免重复加载)
        private long mtime = 0;

        // 尝试加载配置文件, 如果文件无变更 (修改时间) 则不重新加载
        public void load() {
            XposedBridge.log(LOG_TAG + "ConfigHost.load()  " + CONF_PATH);

            // TODO
        }

        // 根据传感器类型获取计算方法
        // 返回 null 表示不修改
        public CalcSensorEventData getCalc(int type) {
            // TODO
            return null;
        }

        // TODO
    }

    // 用于修改传感器数据 (计算)
    interface CalcSensorEventData {
        public void calc(SensorEvent event);
    }

    public String joinArray(float[] data) {
        String o = "";
        for (float i : data) {
            o += " " + i;
        }
        return o;
    }

    // 公共代码
    abstract class CalcSensorEventData0 implements CalcSensorEventData {
        protected float[] config;
        protected String debug = null;

        public CalcSensorEventData0(float[] config, String debug) {
            this.config = config;
            this.debug = debug;
        }

        @Override
        public void calc(SensorEvent event) {
            // 修改之前
            if (debug != null) {
                XposedBridge.log(LOG_TAG + debug + " -> " + joinArray(event.values));
            }
            // 修改数据
            doCalc(event);

            // 修改之后
            if (debug != null) {
                XposedBridge.log(LOG_TAG + debug + " <- " + joinArray(event.values));
            }
        }

        abstract protected void doCalc(SensorEvent event);
    }

    // 模式: f 固定数值
    class CalcSensorEventDataF extends CalcSensorEventData0 {
        public CalcSensorEventDataF(float[] config, String debug) {
            super(config, debug);
        }

        @Override
        protected void doCalc(SensorEvent event) {
            // TODO
        }
    }

    // 模式: d 偏移相加
    class CalcSensorEventDataD extends CalcSensorEventData0 {
        public CalcSensorEventDataD(float[] config, String debug) {
            super(config, debug);
        }

        @Override
        protected void doCalc(SensorEvent event) {
            // TODO
        }
    }

    // 模式: dr 偏移旋转
    class CalcSensorEventDataDr extends CalcSensorEventData0 {
        public CalcSensorEventDataDr(float[] config, String debug) {
            super(config, debug);
        }

        @Override
        protected void doCalc(SensorEvent event) {
            // TODO
        }
    }
}
