package net.vganin.hud;

import android.Manifest;
import android.content.Context;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.RequiresPermission;
import android.widget.RemoteViews;

/**
 * HUD entry. Implement this class and pass it to {@link HudManager#add(Context, Hud)} method to
 * show your entry as overlay.
 */
public abstract class Hud {

    public final static int MINIMUM_UPDATE_PERIOD = 100;
    public final static int NO_PERIODIC_UPDATE = -1;

    private static final int DEFAULT_UPDATE_PERIOD = 1000;

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

    /**
     * Gets update period for this HUD entry. If returned value is less than
     * {@link #MINIMUM_UPDATE_PERIOD}, the latter will be used as actual period until returned
     * value becomes greater.
     * <p>
     * Return {@link #NO_PERIODIC_UPDATE} if you don't want periodic updates. You can update
     * manually via {@link #requestUpdate(Context ctx)} method (e.g. in response to event).
     * <p>
     * In order to switch from {@link #NO_PERIODIC_UPDATE} to periodic updates you must call
     * {@link #requestUpdate(Context ctx)} at least once after this method starts returning
     * different value. This is due to the fact that Manager simply cannot discover that you changed
     * value, so you need to notify it.
     *
     * @return Value of update period in milliseconds.
     */
    public int getUpdatePeriod() {
        return DEFAULT_UPDATE_PERIOD;
    }

    /**
     * Requests to update this HUD immediately. As soon as this request will be treated, value
     * returned from {@link #getUpdate} will be used.
     */
    @RequiresPermission(Manifest.permission.SYSTEM_ALERT_WINDOW)
    public void requestUpdate(Context ctx) {
        HudManager.requestImmediateUpdate(ctx, this);
    }
}
