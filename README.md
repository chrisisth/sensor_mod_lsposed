# sensor_mod_lsposed
<https://github.com/wats-tool/sensor_mod_lsposed>

[! [build](https://github.com/wats-tool/sensor_mod_lsposed/actions/workflows/ci.yml/badge.svg)](https://github.com/wats-tool/sensor_ mod_lsposed/actions)

Ändern Sie den Wert eines Telefonsensors (z. B. den Drehwinkel)
(Ändern Sie den Wert des Androiden-Sensors mit LSPosed)


## Hauptanforderungen

Kürzlich habe ich bei der Verwendung der App "Aiki VR" (billige Telefonzelle VR) festgestellt, dass,
Der Winkel des Bildschirms ist nur korrekt, wenn Sie geradeaus schauen.
Mit anderen Worten, es ist nicht möglich, VR im Liegen (Draufsicht) zu spielen (ups! ~~Wie kann das sein?

Wie kann ich das ertragen?
Nein, man muss auch im Liegen spielen können.


## Herunterladen und installieren

Bitte laden Sie die kompilierte apk-Datei von der Seite [github actions](https://github.com/wats-tool/sensor_mod_lsposed/actions) herunter.


## Gebrauchsanweisung

Nach der Installation aktivieren Sie das Modul in der LSPosed-App.
Aktivieren Sie die Zielanwendung, die Sie ändern möchten.

Die Standardkonfiguration aktiviert keine Funktionen und erfordert eine Konfigurationsdatei, um die angegebenen Sensorwerte zu ändern.
Die Konfigurationsdatei muss mit Root-Rechten geschrieben werden.

Nach der Änderung der Konfigurationsdatei muss die Zielanwendung neu gestartet werden, damit sie wirksam wird.

### TODO Konfigurationsdatei

Pfad der Konfigurationsdatei: `/data/dalvik-cache/sensor_mod.txt`

Konfigurationsdateiformat chestnut:

``sch
# Zeilen, die mit # beginnen, sind Kommentare, und leere Zeilen werden ignoriert.
#
# Das Format ist wie folgt: (eine Zeile ist eine Konfiguration)
# Sensor (Typ) Modus Wert1 Wert2 Wert3 ... .
#...
... # Modus:
+ f: fester Wert, die App erhält immer den Sensorausgang als festen Wert eingestellt
# + d: Offset summiert, die App erhält den ursprünglichen Sensorwert plus den eingestellten Offset
# + dr: Offsetrotation, der Wert des Sensors wird entsprechend dem Koordinatensystem gedreht
#
+ dr: die Anzahl der Werte, gefolgt von einer Anzahl von Werten, unterschiedlich für verschiedene Arten von Sensoren

# TODO Tatsächliche Kastanie
```.


## Kompilieren

Öffnen Sie das Projekt direkt in Android Studio und kompilieren Sie es.


## Hauptentwurf

Billige Telefonzelle VR, die die Sensoren des Telefons nutzt, um 3DoF zu implementieren,
der der 3-Achsen-Drehwinkel ist.

Dabei kann es sich um Sensoren handeln:
Beschleunigungssensor (Schwerkraft), Magnetfeldsensor (Kompass),
Winkelbeschleunigungssensoren (Gyroskop), usw.
Es kann auch Richtungssensoren (Winkelsensoren) geben, die verwirrend sein können.

Der Prozess der Verarbeitung von Sensordaten in Android läuft folgendermaßen ab:

1. Sensor-Hardware
2. Kernel-Treiber (Kernel, möglicherweise Open Source mit dem Kernel)
3. Hardware-Abstraktionsschicht (HAL, Treiber mit geschlossenem Quellcode in `/vendor`)
4. die Android-Rahmenschicht (Rahmen, "SensorManager")
5. Anwendung zum Abrufen von Sensordaten (App)

Da HAL-Treiber in der Regel Closed-Source sind und die Treiber für verschiedene Hardwaremodelle unterschiedlich sein können.
Daher ist es schwierig, Sensordaten der HAL-Ebene und darunter zu ändern.

Die Schnittstelle zwischen der App und dem System ist der `SensorManager`, der eine stabile und konsistente Schnittstelle darstellt,
Diese Schnittstelle ist stabil und konsistent und daher leicht zu handhaben.

Der Schlüsselcode für `SensorManager` ist Java, so dass es einfacher ist, ihn mit dem `Xposed`-Framework zu ändern.
Xposed ist älter und wird jetzt im Allgemeinen durch das `LSPosed` Framework ersetzt.
Die API von LSPosed ist kompatibel mit Xposed und verwendet `zygisk` von `magisk`, um die Zygote-Injektion zu implementieren.

Um dies im Liegen zu tun, werden die Sensorkoordinaten des Telefons im Raum um einen bestimmten Winkel gedreht,
Dadurch kann die App anhand der Sensordaten davon ausgehen, dass das Telefon vertikal ausgerichtet ist, wenn der Bildschirm des Telefons nach unten zeigt.
Dies erfordert eine entsprechende Änderung der Sensordaten, die eine Matrix von räumlich gedrehten Koordinaten beinhaltet.


## Technische Details

Zunächst stellen wir die [offizielle Dokumentation] (https://developer.android.google.cn/reference/android/hardware/SensorManager) für `SensorManager` vor.

Das Codebeispiel für die App zum Abrufen der Sensordaten lautet wie folgt:

``java
public class SensorActivity extends Activity implements SensorEventListener {
    private final SensorManager mSensorManager;
    private final Sensor mAccelerometer;

    public SensorActivity() {
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void onSensorChanged(SensorEvent event) {
    }
}
```

Zuerst (1) verwenden Sie `getSystemService(SENSOR_SERVICE)`, um die Instanz `SensorManager` zu erhalten,
Dann (2) verwenden Sie `getDefaultSensor(Sensor.TYPE_*)`, um den Sensor zu erhalten, dann (3) verwenden Sie `registerListener()`, um den Listener zu registrieren,
und schließlich (4) die Sensordaten mit dem Callback `SensorEventListener.onSensorChanged(SensorEvent)` abrufen.

### SensorManager und SystemSensorManager

Entsprechender Rahmencode (`platform_frameworks_base`) [`SensorManager.java`](https://github.com/aosp-mirror/platform_frameworks_base/blob/android10-d4 -s1-release/core/java/android/hardware/SensorManager.java).
Aber `SensorManager` ist nur eine `abstrakte Klasse`, und diejenige, die tatsächlich die Arbeit macht, ist [`SystemSensorManager.java`](https://github.com/aosp-mirror/platform_frameworks_base/blob /android10-d4-s1-release/core/java/android/hardware/SystemSensorManager.java).

In `SystemSensorManager` [`registerListenerImpl()`](https://github.com/aosp-mirror/platform_frameworks_base/blob/android10-d4-s1- release/core/java/android/hardware/SystemSensorManager.java#L145)
Tatsächliche Registrierung des ``SensorEventListener``:

``java
/** @hide */
@Override
protected boolean registerListenerImpl(
    SensorEventListener-Listener,
    Sensor Sensor,
    int delayUs,
    Handler handler,
    int maxBatchReportLatencyUs,
    int reservedFlags
) {
    //
}
```

Diese Funktion kann mit einem Hook versehen werden, um eine Schicht des eingehenden `SensorEventListener` zu umhüllen,
und ersetzen Sie ihn durch Ihren eigenen Code, um die Sensordaten des Rückrufs zu ändern.

### Ändern der Berechnung von Sensordaten

TODO

### Auswahl des Konfigurationsdateipfades

'/data/dalvik-cache/sensor_mod.txt`

Der Test zeigt, dass die normale Anwendung Lesezugriff auf diesen Speicherort hat.


## Verwandte Links

+ [magisk](https://github.com/topjohnwu/Magisk/)

+ [LSPosed](https://github.com/lsposed/lsposed)

+ [Xposed Module Writing Tutorial](https://github.com/rovo89/XposedBridge/wiki/Development-tutorial)

+ [Xposed Framework API](https://api.xposed.info/reference/packages.html)

+ [xposed api-Abhängigkeiten herunterladen, nachdem JCenter geschlossen wurde](https://www.jianshu.com/p/7d4611546423)

+ [SensorManager offizielle Dokumentation](https://developer.android.google.cn/reference/android/hardware/SensorManager)


## LICENSE

```
sensor_mod_lsposed: Ändern von Android-Sensorwerten mit LSPosed
Copyright (C) 2022 sceext

Dieses Programm ist freie Software: Sie können es weiterverteilen und/oder verändern
unter den Bedingungen der GNU General Public License, veröffentlicht von
der Free Software Foundation, entweder Version 3 der Lizenz oder
(nach Ihrer Wahl) jede spätere Version.

Dieses Programm wird in der Hoffnung verteilt, dass es nützlich sein wird,
Dieses Programm wird in der Hoffnung verteilt, dass es nützlich ist, aber OHNE JEGLICHE GARANTIE; sogar ohne die stillschweigende Garantie von
Dieses Programm wird in der Hoffnung verteilt, dass es nützlich ist, aber OHNE JEGLICHE GARANTIE; auch ohne die stillschweigende Garantie der HANDELSÜBLICHKEIT oder der EIGNUNG FÜR EINEN BESTIMMTEN ZWECK.
GNU General Public License für weitere Details.

Sie sollten eine Kopie der 