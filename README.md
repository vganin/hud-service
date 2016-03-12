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
Then use provided `HudManager` API. Look at sample `app` project for examples.
