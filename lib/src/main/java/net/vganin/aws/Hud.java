package net.vganin.aws;

import android.os.Binder;
import android.os.IBinder;
import android.widget.RemoteViews;

public abstract class Hud {

    final IBinder mToken = new Binder();

    public abstract RemoteViews getUpdate();
}
