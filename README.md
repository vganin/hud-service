## HUD Service
It allows you to add remotable updatable views on screen overlay which will be ALIVE ALL THE TIME unless you say them not to be or you app process dies. Be aware of memory leaks ofc.

### Usage
Basically what you are required to do is setup HUD Service itself in `AndroidManifest.xml` like this:
```
<service
    android:name="net.vganin.hud.HudService">

    <intent-filter>
        <action android:name="net.vganin.aws.action.HUD" />
    </intent-filter>
</service>
```
Also add required `SYSTEM_ALERT_WINDOW` permission (it is special permission, on API 23 or higher settings activity will be opened automatically on first use in order for you to grant permission manually; see [docs](http://developer.android.com/intl/ru/reference/android/Manifest.permission.html#SYSTEM_ALERT_WINDOW) for details):
```
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW"/>
```
Then use provided `HudManager` API. Look at sample `app` project for examples.
