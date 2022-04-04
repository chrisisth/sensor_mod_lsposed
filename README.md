# sensor_mod_lsposed
<https://github.com/wats-tool/sensor_mod_lsposed>

[![build](https://github.com/wats-tool/sensor_mod_lsposed/actions/workflows/ci.yml/badge.svg)](https://github.com/wats-tool/sensor_mod_lsposed/actions)

修改手机传感器 (比如 旋转角度) 的数值
(Modify android sensor value with LSPosed)


## 主要需求

最近在使用 `爱奇艺 VR` app 时 (廉价手机盒子 VR) 发现,
必须平视前方, 画面角度才是对的.
也就是说, 无法实现躺着玩 VR (视角偏上) (摔 ! ~~

这怎么能忍 ?
不行, 必须实现躺着玩儿.


## 下载安装

请从 [github actions](https://github.com/wats-tool/sensor_mod_lsposed/actions) 页面下载编译好的 apk 文件.


## 使用说明

安装之后在 LSPosed app 里面启用模块.
并启用想修改的目标 app.

默认配置不启用任何功能, 需要编写配置文件实现对指定传感器数值的修改.
需要使用 root 权限写入配置文件.

修改配置文件之后, 需要重启目标 app 才能生效.

### TODO 配置文件

配置文件路径: `/data/dalvik-cache/sensor_mod.txt`

配置文件格式栗子:

```sh
# 以 # 开头的行是注释, 以及空白行会被直接忽略
#
# 格式如下: (一行是一条配置)
# 传感器(类型) 模式 数值1 数值2 数值3 .. .
#
# 模式:
# + f: 固定数值, app 获取到的传感器输出始终为设置的固定数值
# + d: 偏移相加, app 获取到的数值为传感器原始数值加上设置的偏移
# + dr: 偏移旋转, 按照坐标系旋转传感器的数值
#
# 数值*: 后跟若干个数值, 不同类型的传感器对应的数值个数不同

# TODO 实际栗子
```


## 编译

直接使用 Android Studio 打开项目并编译.


## 主要设计

廉价手机盒子 VR 使用手机自带传感器实现 `3DoF`,
也就是 3 轴的旋转角度.

涉及到的传感器 可能 有:
加速度传感器 (重力), 磁场传感器 (指南针),
角加速度传感器 (陀螺仪) 等.
也可能有 方向传感器(角度传感器), 比较混乱.

Android 系统对传感器数据的处理过程如下:

1. 传感器硬件
2. 内核驱动 (kernel, 可能随内核开源)
3. 硬件抽象层 (HAL, `/vendor` 里面的闭源驱动)
4. Android 框架层 (framework, `SensorManager`)
5. 应用获取传感器数据 (app)

由于 HAL 驱动一般是闭源的, 并且不同具体机型的硬件不同, 驱动很可能也不同.
因此从 HAL 及以下层次修改传感器数据比较困难.

app 和系统的接口是 `SensorManager`, 这部分接口是稳定一致的,
因此方便下手.

`SensorManager` 的关键代码是 java, 因此使用 `Xposed` 框架修改比较方便.
Xposed 比较老了, 目前一般使用 `LSPosed` 框架来代替.
LSPosed 的 API 兼容 Xposed, 使用 `magisk` 的 `zygisk` 实现 zygote 注入.

要实现躺着玩儿, 就是将手机传感器的坐标在空间中旋转一定角度,
从而实现当手机屏幕向下时, app 从传感器数据认为手机方向是竖直的.
这需要对传感器数据进行对应修改, 涉及到空间旋转的矩阵坐标运算.


## 技术细节

首先祭出 `SensorManager` 的 [官方文档](https://developer.android.google.cn/reference/android/hardware/SensorManager).

app 获取传感器数据的代码示例如下:

```java
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

首先 (1) 使用 `getSystemService(SENSOR_SERVICE)` 获取 `SensorManager` 实例,
然后 (2) 使用 `getDefaultSensor(Sensor.TYPE_*)` 获取传感器, 然后 (3) 使用 `registerListener()` 注册监听器,
最后 (4) 在 `SensorEventListener.onSensorChanged(SensorEvent)` 回调中获得传感器数据.

### SensorManager 和 SystemSensorManager

对应框架代码 (`platform_frameworks_base`) [`SensorManager.java`](https://github.com/aosp-mirror/platform_frameworks_base/blob/android10-d4-s1-release/core/java/android/hardware/SensorManager.java).
但是 `SensorManager` 只是 `abstract class`, 实际干活的是 [`SystemSensorManager.java`](https://github.com/aosp-mirror/platform_frameworks_base/blob/android10-d4-s1-release/core/java/android/hardware/SystemSensorManager.java).

在 `SystemSensorManager` 中 [`registerListenerImpl()`](https://github.com/aosp-mirror/platform_frameworks_base/blob/android10-d4-s1-release/core/java/android/hardware/SystemSensorManager.java#L145)
实际进行 `SensorEventListener` 的注册:

```java
/** @hide */
@Override
protected boolean registerListenerImpl(
    SensorEventListener listener,
    Sensor sensor,
    int delayUs,
    Handler handler,
    int maxBatchReportLatencyUs,
    int reservedFlags
) {
    //
}
```

可以对这个函数进行 hook, 将传入的 `SensorEventListener` 包装一层,
替换成自己实现的代码, 从而对回调的传感器数据进行修改.

### 修改传感器数据的计算

TODO

### 配置文件路径选择

`/data/dalvik-cache/sensor_mod.txt`

通过测试可知, 普通 app 具有对这个位置的读取权限.


## 相关链接

+ [magisk](https://github.com/topjohnwu/Magisk/)

+ [LSPosed](https://github.com/lsposed/lsposed)

+ [Xposed 模块编写教程](https://github.com/rovo89/XposedBridge/wiki/Development-tutorial)

+ [Xposed Framework API](https://api.xposed.info/reference/packages.html)

+ [JCenter 关闭后下载 xposed api 依赖](https://www.jianshu.com/p/7d4611546423)

+ [SensorManager 官方文档](https://developer.android.google.cn/reference/android/hardware/SensorManager)


## LICENSE

```
sensor_mod_lsposed: Modify android sensor value with LSPosed
Copyright (C) 2022  sceext

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
```
