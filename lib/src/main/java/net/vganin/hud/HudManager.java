package net.vganin.hud;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.annotation.RequiresPermission;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Communication bridge between client and service.
 */
public final class HudManager {

    private static class MessageWorker implements Runnable {
        private final HudMessenger hudMessenger;
        private final Hud hud;

        private MessageWorker(HudMessenger hudMessenger, Hud hud) {
            this.hudMessenger = hudMessenger;
            this.hud = hud;
        }

        @Override
        public void run() {
            hudMessenger.update(hud);
        }
    }

    private static final ServiceConnection CONNECTION = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            messenger = new HudMessenger(service);
            connecting = false;

            runAllPendingActions();
            clearActionQueue();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            cancelAll();
            clearActionQueue();

            messenger = null;
        }
    };

    private static final ScheduledExecutorService EXECUTOR =
            Executors.newSingleThreadScheduledExecutor();

    private static final Map<Hud, ScheduledFuture> SCHEDULED = new LinkedHashMap<>();

    private static final List<Runnable> TO_EXECUTE_AFTER_CONNECT = new LinkedList<>();

    private static HudMessenger messenger;
    private static boolean connecting = false;

    private HudManager() {
        throw new UnsupportedOperationException("Global static class."
                + " Not supposed to be instantiated.");
    }

    public static boolean isConnected() {
        return messenger != null;
    }

    /**
     * Adds HUD to overlay zone. One will ask HUD for updates periodically.
     *
     * @param ctx Context.
     * @param hud HUD implementation.
     */
    @RequiresPermission(Manifest.permission.SYSTEM_ALERT_WINDOW)
    public static void add(Context ctx, final Hud hud) {
        executeConnectionDependentAction(ctx, new Runnable() {
            @Override
            public void run() {
                postUpdate(hud);
            }
        });
    }

    /**
     * Removes HUD from overlay zone. One will stop asking for updates from this HUD.
     *
     * @param ctx Context.
     * @param hud HUD implementation.
     */
    @RequiresPermission(Manifest.permission.SYSTEM_ALERT_WINDOW)
    public static void remove(Context ctx, Hud hud) {
        cancel(hud);

        if (isConnected()) {
            messenger.remove(hud);

            if (SCHEDULED.isEmpty()) {
                disconnect(ctx);
            }
        }
    }

    /**
     * Toggle entire overlay zone visibility. Client must make something in order to use
     * this method.
     *
     * @param ctx Context.
     */
    @RequiresPermission(Manifest.permission.SYSTEM_ALERT_WINDOW)
    public static void toggleVisibility(Context ctx) {
        executeConnectionDependentAction(ctx, new Runnable() {
            @Override
            public void run() {
                messenger.toggleVisibility();
            }
        });
    }

    /**
     * Removes all HUDs from overlay zone.
     *
     * @param ctx Context.
     */
    @RequiresPermission(Manifest.permission.SYSTEM_ALERT_WINDOW)
    public static void removeAll(Context ctx) {
        Set<Hud> allHuds = new HashSet<>(SCHEDULED.keySet());
        for (Hud hud : allHuds) {
            remove(ctx, hud);
        }
    }

    /**
     * Requests to update this HUD immediately, ignoring its update period returned by
     * {@link Hud#getUpdatePeriod()} for this moment.
     *
     * @param ctx Context.
     * @param hud HUD implementation.
     */
    @RequiresPermission(Manifest.permission.SYSTEM_ALERT_WINDOW)
    public static void requestImmediateUpdate(Context ctx, final Hud hud) {
        executeConnectionDependentAction(ctx, new Runnable() {
            @Override
            public void run() {
                postUpdate(hud);
            }
        });
    }

    private static void connect(Context ctx) {
        ctx.getApplicationContext().bindService(
                new Intent(ctx, HudService.class),
                CONNECTION,
                Context.BIND_AUTO_CREATE);

        connecting = true;
    }

    private static void disconnect(Context ctx) {
        ctx.getApplicationContext().unbindService(CONNECTION);

        // Need to call it here too because onServiceDisconnected call can be pretty delayed, so
        // when addHUD called right after unbindService, it may happen so old messenger will
        // be reused which leads to 'java.lang.IllegalArgumentException: Service not registered'
        messenger = null;
    }

    private static void postUpdate(Hud hud) {
        cancel(hud);
        schedule(hud);
    }

    private static void executeConnectionDependentAction(Context ctx, Runnable runnable) {
        if (isConnected()) {
            runnable.run();
        } else {
            scheduleAction(runnable);

            if (!connecting) {
                connect(ctx);
            }
        }
    }

    private static void scheduleAction(Runnable runnable) {
        TO_EXECUTE_AFTER_CONNECT.add(runnable);
    }

    private static void runAllPendingActions() {
        for (Runnable task : TO_EXECUTE_AFTER_CONNECT) {
            task.run();
        }
    }

    private static void clearActionQueue() {
        TO_EXECUTE_AFTER_CONNECT.clear();
    }

    private static void schedule(Hud hud) {
        Runnable work = new MessageWorker(messenger, hud);

        if (hud.getUpdatePeriod() == Hud.NO_PERIODIC_UPDATE) {
            EXECUTOR.execute(work);
        } else {
            int updateDelay = Math.max(hud.getUpdatePeriod(), Hud.MINIMUM_UPDATE_PERIOD);

            ScheduledFuture future = EXECUTOR.scheduleWithFixedDelay(
                    work, 0, updateDelay, TimeUnit.MILLISECONDS);
            SCHEDULED.put(hud, future);
        }
    }

    private static void cancel(Hud hud) {
        ScheduledFuture future = SCHEDULED.get(hud);
        if (future != null) {
            future.cancel(true);
        }

        SCHEDULED.remove(hud);
    }

    private static void cancelAll() {
        Set<Hud> allHuds = new HashSet<>(SCHEDULED.keySet());
        for (Hud hud : allHuds) {
            cancel(hud);
        }
    }
}
