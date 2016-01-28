package net.vganin.aws;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.RemoteViews;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Service for displaying HUD entries.
 * <p>
 * In order to use it you must add it in AndroidManifest as {@code service}. {@link HudManager} will
 * communicate to it via explicit intent.
 * <p>
 * You must declare {@link android.Manifest.permission#SYSTEM_ALERT_WINDOW} usage in manifest as well.
 * On Android >= 6 (SKD >= 23) service will deal with runtime permissions itself. You must be prepared
 * that Android will show 'draw overlays' setting on the first start. Enable permission for your app.
 * Then the HUDs must show on.
 */
public final class HudService extends Service {

    private static final String TAG = HudService.class.getSimpleName();

    private static class IncomingHandler extends Handler {

        private final class DeathAwareView implements IBinder.DeathRecipient {

            public final IBinder token;
            public RemoteViews remoteView;

            public DeathAwareView(IBinder token) {
                this.token = token;
            }

            public void update(RemoteViews remoteView) {
                this.remoteView = remoteView;
            }

            @Override
            public void binderDied() {
                remoteViews.remove(token);
                invokeOnUpdateSafely();
            }
        }

        private final Map<IBinder, DeathAwareView> remoteViews = new LinkedHashMap<>();

        private final Runnable onUpdateCallback;

        private boolean viewsAreShown = true;

        private IncomingHandler(Runnable onUpdateCallback) {
            this.onUpdateCallback = onUpdateCallback;
        }

        @Override
        public void handleMessage(Message msg) {
            IBinder token = msg.getData().getBinder(Const.EXTRA_TOKEN);

            switch (msg.what) {
                case Const.MESSAGE_UPDATE_HUD:
                    if (token != null && token.isBinderAlive()) {
                        RemoteViews remoteViews = msg.getData().getParcelable(Const.EXTRA_MESSAGE);
                        update(token, remoteViews);
                    }
                    break;
                case Const.MESSAGE_REMOVE_HUD:
                    remove(token);
                    break;
                case Const.MESSAGE_TOGGLE_VISIBILITY:
                    viewsAreShown = !viewsAreShown;
                    break;
            }

            invokeOnUpdateSafely();
        }

        private void invokeOnUpdateSafely() {
            removeCallbacks(onUpdateCallback);
            post(onUpdateCallback);
        }

        private void update(IBinder token, RemoteViews remoteView) {
            DeathAwareView deathAwareMsg;

            if (this.remoteViews.containsKey(token)) {
                deathAwareMsg = this.remoteViews.get(token);
            } else {
                deathAwareMsg = new DeathAwareView(token);

                this.remoteViews.put(token, deathAwareMsg);

                try {
                    token.linkToDeath(deathAwareMsg, 0);
                } catch (RemoteException e) {
                    Log.e(TAG, "Error during linkToDeath", e);
                }
            }

            deathAwareMsg.update(remoteView);
        }

        private void remove(IBinder token) {
            if (remoteViews.containsKey(token)) {
                DeathAwareView deathAwareMsg = remoteViews.get(token);
                token.unlinkToDeath(deathAwareMsg, 0);
                remoteViews.remove(token);
            }
        }
    }

    private class ViewUpdater implements Runnable {

        @Override
        public void run() {
            initViewSpace();

            if (viewSpaceInitialized) {
                viewsUpdate();
                visibilityUpdate();
            }
        }
    }

    private IncomingHandler hudHandler;
    private Messenger messenger;

    private LinearLayout root;

    private boolean viewSpaceInitialized = false;

    @Override
    public IBinder onBind(Intent intent) {
        return messenger.getBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        enforceSystemAlertPermission();
        initViewSpace();

        hudHandler = new IncomingHandler(new ViewUpdater());
        messenger = new Messenger(hudHandler);
    }

    @Override
    public void onDestroy() {
        deInitViewSpace();

        hudHandler.removeCallbacksAndMessages(null);

        super.onDestroy();
    }

    private void viewsUpdate() {
        root.removeAllViews();

        Collection<IncomingHandler.DeathAwareView> remoteViews = hudHandler.remoteViews.values();

        for (IncomingHandler.DeathAwareView remoteView : remoteViews) {
            if (remoteView.remoteView != null) {
                View view = remoteView.remoteView.apply(this, root);
                root.addView(view);
            }
        }
    }

    private void visibilityUpdate() {
        root.setVisibility(hudHandler.viewsAreShown ? View.VISIBLE : View.GONE);
    }

    private void initViewSpace() {
        boolean canDrawOverlays = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            canDrawOverlays = Settings.canDrawOverlays(this);
        }

        if (!viewSpaceInitialized && canDrawOverlays) {
            WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);
            params.gravity = Gravity.START | Gravity.BOTTOM;

            root = new LinearLayout(this);
            root.setOrientation(LinearLayout.VERTICAL);
            windowManager.addView(root, params);

            viewSpaceInitialized = true;
        }
    }

    private void deInitViewSpace() {
        if (viewSpaceInitialized) {
            WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            windowManager.removeView(root);

            viewSpaceInitialized = false;
        }
    }

    private void enforceSystemAlertPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent startingIntent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                startingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(startingIntent);
            }
        }
    }
}

