package org.wats_tool.sensor_mod_lsposed;

import java.util.HashSet;

import android.os.Process;
import android.os.Handler;
import android.os.MemoryFile;
import android.hardware.HardwareBuffer;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorDirectChannel;
import android.hardware.SensorManager.DynamicSensorCallback;
import android.hardware.TriggerEventListener;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import de.robv.android.xposed.XC_MethodHook;

public class Hook implements IXposedHookLoadPackage {
    private final String LOG_TAG = "sensor_mod  ";
    private final String P_VERSION = "sensor_mod version 0.1.0-a5";

    private final ConfigHost ch = new ConfigHost();

    private final HashSet<Integer> pidSet = new HashSet<>();

    // Hook Initialisierung des Ausführungsportals
    @Override
    public void handleLoadPackage(final LoadPackageParam p) throws Throwable {
        int pid = Process.myPid();
        // 避免重复 hook
        if (pidSet.contains(pid)) {
            XposedBridge.log("handleLoadPackage: "+LOG_TAG + "#### pid " + pid + "  skip hook  packageName: " + p.packageName);
            return;
        }
        pidSet.add(pid);

        XposedBridge.log("handleLoadPackage: "+LOG_TAG + "#### pid " + pid + "  HOOK " + P_VERSION + "  packageName: " + p.packageName);

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
                XposedBridge.log("handleLoadPackage:beforeHookedMethod "+LOG_TAG + "SystemSensorManager.registerListenerImpl()");

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
                XposedBridge.log("handleLoadPackage:beforeHookedMethod "+LOG_TAG + "SystemSensorManager.configureDirectChannelImpl");

                Sensor sensor = (Sensor) pa.args[1];

                debugSensor(sensor, "(SensorDirectChannel)");
                // TODO
            }
            @Override
            protected void afterHookedMethod(MethodHookParam pa) throws Throwable {
                // nothing to do
            }
        });

        // 用于测试
        debugHook(p);

        // 加载配置
        ch.load();
    }

    private void debugHook(final LoadPackageParam p) {
        // #### android.hardware.SensorManager
        // https://github.com/aosp-mirror/platform_frameworks_base/blob/android10-d4-s1-release/core/java/android/hardware/SensorManager.java

        // public List<Sensor> getSensorList(int type)
        findAndHookMethod("android.hardware.SensorManager", p.classLoader, "getSensorList",
            int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam pa) throws Throwable {
                XposedBridge.log("handleLoadPackage:debugHook "+LOG_TAG + "SensorManager.getSensorList()");
                // TODO
            }
            @Override
            protected void afterHookedMethod(MethodHookParam pa) throws Throwable {}
        });

        // public List<Sensor> getDynamicSensorList(int type)
        findAndHookMethod("android.hardware.SensorManager", p.classLoader, "getDynamicSensorList",
            int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam pa) throws Throwable {
                XposedBridge.log("handleLoadPackage:beforeHookedMethod "+LOG_TAG + "SensorManager.getDynamicSensorList()");
                // TODO
            }
            @Override
            protected void afterHookedMethod(MethodHookParam pa) throws Throwable {}
        });

        // public Sensor getDefaultSensor(int type)
        findAndHookMethod("android.hardware.SensorManager", p.classLoader, "getDefaultSensor",
            int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam pa) throws Throwable {
                XposedBridge.log("handleLoadPackage:beforeHookedMethod "+LOG_TAG + "SensorManager.getDefaultSensor(" + pa.args[0] + ")");
                // TODO
            }
            @Override
            protected void afterHookedMethod(MethodHookParam pa) throws Throwable {}
        });

        // public Sensor getDefaultSensor(int type, boolean wakeUp)
        findAndHookMethod("android.hardware.SensorManager", p.classLoader, "getDefaultSensor",
            int.class, boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam pa) throws Throwable {
                XposedBridge.log("handleLoadPackage:beforeHookedMethod "+LOG_TAG + "SensorManager.getDefaultSensor(" + pa.args[0] + ", " + pa.args[1] + ")");
                // TODO
            }
            @Override
            protected void afterHookedMethod(MethodHookParam pa) throws Throwable {}
        });

        // public boolean registerListener(SensorEventListener listener, Sensor sensor, int samplingPeriodUs, int maxReportLatencyUs)
        findAndHookMethod("android.hardware.SensorManager", p.classLoader, "registerListener",
            SensorEventListener.class, Sensor.class, int.class, int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam pa) throws Throwable {
                XposedBridge.log("handleLoadPackage:beforeHookedMethod "+LOG_TAG + "SensorManager.registerListener()  1");
                // TODO
            }
            @Override
            protected void afterHookedMethod(MethodHookParam pa) throws Throwable {}
        });

        // public boolean registerListener(SensorEventListener listener, Sensor sensor, int samplingPeriodUs, Handler handler)
        findAndHookMethod("android.hardware.SensorManager", p.classLoader, "registerListener",
            SensorEventListener.class, Sensor.class, int.class, Handler.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam pa) throws Throwable {
                XposedBridge.log("handleLoadPackage:beforeHookedMethod "+LOG_TAG + "SensorManager.registerListener()  2");
                // TODO
            }
            @Override
            protected void afterHookedMethod(MethodHookParam pa) throws Throwable {}
        });

        // public boolean registerListener(SensorEventListener listener, Sensor sensor, int samplingPeriodUs, int maxReportLatencyUs, Handler handler)
        findAndHookMethod("android.hardware.SensorManager", p.classLoader, "registerListener",
            SensorEventListener.class, Sensor.class, int.class, int.class, Handler.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam pa) throws Throwable {
                XposedBridge.log("handleLoadPackage:beforeHookedMethod "+LOG_TAG + "SensorManager.registerListener()  3");
                // TODO
            }
            @Override
            protected void afterHookedMethod(MethodHookParam pa) throws Throwable {}
        });

        // public SensorDirectChannel createDirectChannel(MemoryFile mem)
        findAndHookMethod("android.hardware.SensorManager", p.classLoader, "createDirectChannel",
            MemoryFile.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam pa) throws Throwable {
                XposedBridge.log("handleLoadPackage:beforeHookedMethod "+LOG_TAG + "SensorManager.createDirectChannel()  1");
                // TODO
            }
            @Override
            protected void afterHookedMethod(MethodHookParam pa) throws Throwable {}
        });

        // public SensorDirectChannel createDirectChannel(HardwareBuffer mem)
        findAndHookMethod("android.hardware.SensorManager", p.classLoader, "createDirectChannel",
            HardwareBuffer.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam pa) throws Throwable {
                XposedBridge.log("handleLoadPackage:beforeHookedMethod "+LOG_TAG + "SensorManager.createDirectChannel()  2");
                // TODO
            }
            @Override
            protected void afterHookedMethod(MethodHookParam pa) throws Throwable {}
        });

        // public void registerDynamicSensorCallback(DynamicSensorCallback callback, Handler handler)
        findAndHookMethod("android.hardware.SensorManager", p.classLoader, "registerDynamicSensorCallback",
            DynamicSensorCallback.class, Handler.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam pa) throws Throwable {
                XposedBridge.log("handleLoadPackage:beforeHookedMethod "+LOG_TAG + "SensorManager.registerDynamicSensorCallback()");
                // TODO
            }
            @Override
            protected void afterHookedMethod(MethodHookParam pa) throws Throwable {}
        });

        // public boolean requestTriggerSensor(TriggerEventListener listener, Sensor sensor)
        findAndHookMethod("android.hardware.SensorManager", p.classLoader, "requestTriggerSensor",
            TriggerEventListener.class, Sensor.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam pa) throws Throwable {
                XposedBridge.log("handleLoadPackage:beforeHookedMethod "+LOG_TAG + "SensorManager.requestTriggerSensor()");
                // TODO
            }
            @Override
            protected void afterHookedMethod(MethodHookParam pa) throws Throwable {}
        });

        // #### android.hardware.SystemSensorManager
        // https://github.com/aosp-mirror/platform_frameworks_base/blob/android10-d4-s1-release/core/java/android/hardware/SystemSensorManager.java

        // TODO
        // findAndHookMethod("android.hardware.SystemSensorManager", p.classLoader, "",
        //     SensorDirectChannel.class, Sensor.class, int.class,
        // new XC_MethodHook() {
        //     @Override
        //     protected void beforeHookedMethod(MethodHookParam pa) throws Throwable {
        //         XposedBridge.log(LOG_TAG + "SystemSensorManager.");
        //         // TODO
        //     }
        //     @Override
        //     protected void afterHookedMethod(MethodHookParam pa) throws Throwable {}
        // });

        // TODO
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
        XposedBridge.log("debugHook:debugSensor "+LOG_TAG + "  sensor id " + s.getId() + "  type " + s.getType() + " " + s.getStringType());
        XposedBridge.log("debugHook:debugSensor "+LOG_TAG + "    name " + s.getName() + "  #### fullClassName " + fullClassName);
    }

    class MyListener implements SensorEventListener {
        private ConfigHost ch;
        private SensorEventListener next;
        private Sensor sensor;
        // Erstes Inbetriebnahmezeichen
        private boolean first = true;
        // Berechnungsmethode zur Modifizierung von Sensordaten
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
            XposedBridge.log("debugHook:onAccuracyChanged "+LOG_TAG + "onAccuracyChanged()  " + accuracy);
            debugSensor(sensor, getFullClassName(next));

            next.onAccuracyChanged(sensor, accuracy);
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (first) {
                first = false;
                // DEBUG
                XposedBridge.log("debugHook:onAccuracyChanged "+LOG_TAG + "onSensorChanged(first)  " + joinArray(event.values));
                debugSensor(sensor, getFullClassName(next));
            }
            // Berechnung modifizieren
            if (calc != null) {
                calc.calc(event);
            }
            // Übertragungen
            next.onSensorChanged(event);
        }
    }

    // Laden von Konfigurationsdateien
    class ConfigHost {
        // Pfad der Konfigurationsdatei
        private final String CONF_PATH = "/data/dalvik-cache/sensor_mod.txt";
        // Sensor-Modus: f Feste Werte
        private final String MODE_F = "f";
        // Sensor-Modus: d Offset-Summierung
        private final String MODE_D = "d";
        // Sensor-Modus: dr Versetzte Drehung
        private final String MODE_DR = "dr";

        // Sonderzeichen in der Konfigurationsdatei: # Hinweis
        private final String C_COMMENT = "#";
        // Sonderzeichen in der Konfigurationsdatei: (Leerzeichen) Zeilentrenner
        private final String C_SEP = " ";

        // Konfigurationsdatei zuletzt aktualisiert am (Zur Vermeidung von Doppelbelastungen)
        private long mtime = 0;

        // Versuch, eine Konfigurationsdatei zu laden, Wenn es keine Änderungen am Dokument gibt (Zeit ändern) wird nicht nachgeladen
        public void load() {
            XposedBridge.log("debugHook:load "+LOG_TAG + "ConfigHost.load()  " + CONF_PATH);

            // TODO
        }

        // Zugriff auf Berechnungsmethoden je nach Sensortyp
        // Return null Zeigt keine Änderung an
        public CalcSensorEventData getCalc(int type) {
            // TODO
            return null;
        }

        // TODO
    }

    // Zum Ändern von Sensordaten (Berechnung)
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

    // Öffentliche Codes
    abstract class CalcSensorEventData0 implements CalcSensorEventData {
        protected float[] config;
        protected String debug = null;

        public CalcSensorEventData0(float[] config, String debug) {
            this.config = config;
            this.debug = debug;
        }

        @Override
        public void calc(SensorEvent event) {
            
            XposedBridge.log("F Befehl erkannt");
            XposedBridge.log("Config contains: " + java.util.Arrays.toString(config));
            debug = "true";
            // Vor der Änderung
            if (debug != null) {
                XposedBridge.log("debugHook:calc "+LOG_TAG + debug + " -> " + joinArray(event.values));
            }
            // Daten ändern
            doCalc(event);

            // Nach der Änderung
            if (debug != null) {
                XposedBridge.log("debugHook:calc "+LOG_TAG + debug + " <- " + joinArray(event.values));
            }
        }

        abstract protected void doCalc(SensorEvent event);
    }

    // Modus: f Feste Werte
    class CalcSensorEventDataF extends CalcSensorEventData0 {
        public CalcSensorEventDataF(float[] config, String debug) {
            super(config, debug);
        }

        @Override
        protected void doCalc(SensorEvent event) {
            // TODO
        }
    }

    // Modus: d Offset-Summierung
    class CalcSensorEventDataD extends CalcSensorEventData0 {
        public CalcSensorEventDataD(float[] config, String debug) {
            super(config, debug);
        }

        @Override
        protected void doCalc(SensorEvent event) {
            // TODO
        }
    }

    // Modus: dr Versetzte Drehung
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
