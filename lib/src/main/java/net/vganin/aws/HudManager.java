package net.vganin.aws;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

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

    private static final int UPDATE_DELAY = 1000;

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

    public static void add(Context ctx, final Hud hud) {
        executeConnectionDependentAction(ctx, new Runnable() {
            @Override
            public void run() {
                postUpdate(hud);
            }
        });
    }

    public static void remove(Context ctx, Hud hud) {
        cancel(hud);

        if (isConnected()) {
            messenger.remove(hud);

            if (SCHEDULED.isEmpty()) {
                disconnect(ctx);
            }
        }
    }

    public static void toggleVisibility(Context ctx) {
        executeConnectionDependentAction(ctx, new Runnable() {
            @Override
            public void run() {
                messenger.toggleVisibility();
            }
        });
    }

    public static void removeAll(Context ctx) {
        Set<Hud> allHuds = new HashSet<Hud>(SCHEDULED.keySet());
        for (Hud hud : allHuds) {
            remove(ctx, hud);
        }
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
        ScheduledFuture future = EXECUTOR.scheduleWithFixedDelay(
                new MessageWorker(messenger, hud), 0, UPDATE_DELAY, TimeUnit.MILLISECONDS);

        SCHEDULED.put(hud, future);
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
