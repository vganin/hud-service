package net.vganin.aws;

import android.os.Binder;
import android.os.IBinder;

public abstract class Hud {

    final IBinder mToken = new Binder();

    public abstract String getMessage();
}
