package net.vganin.aws;

import android.content.Context;
import android.os.Binder;
import android.os.IBinder;
import android.widget.RemoteViews;

/**
 * HUD entry. Implement this class and pass it to {@link HudManager#add(Context, Hud)} method to
 * show your entry as overlay.
 */
public abstract class Hud {

    /**
     * Hud ID. Additionally it helps to notify service whether process is alive to remove dead entry.
     */
    final IBinder mToken = new Binder();

    /**
     * Gets remote view to hand to the service. This view will be displayed as overlay.
     *
     * @return Any remote view group.
     */
    public abstract RemoteViews getUpdate();
}
