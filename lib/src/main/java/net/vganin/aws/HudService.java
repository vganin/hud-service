package net.vganin.aws;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
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
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class HudService extends Service {

    private static final String TAG = HudService.class.getSimpleName();

    private static final char NEW_LINE = '\n';
    private static final String DELIMITER = "-----------------";

    private static final int COLOR_SHADE = 0x55000000;

    private static class IncomingHandler extends Handler {

        private final class DeathAwareMessage implements IBinder.DeathRecipient {

            public final IBinder token;
            public String text;

            public DeathAwareMessage(IBinder token) {
                this.token = token;
            }

            public void updateText(String text) {
                this.text = text;
            }

            @Override
            public void binderDied() {
                messages.remove(token);
                invokeOnUpdateSafely();
            }

            @Override
            public String toString() {
                return text;
            }
        }

        private final Map<IBinder, DeathAwareMessage> messages = new LinkedHashMap<>();

        private boolean debugInfoShown = true;

        private final Runnable onUpdateCallback;

        private IncomingHandler(Runnable onUpdateCallback) {
            this.onUpdateCallback = onUpdateCallback;
        }

        @Override
        public void handleMessage(Message msg) {
            IBinder token = msg.getData().getBinder(Const.EXTRA_TOKEN);

            switch (msg.what) {
                case Const.MESSAGE_UPDATE_HUD:
                    if (token != null && token.isBinderAlive()) {
                        String text = msg.getData().getString(Const.EXTRA_MESSAGE);
                        update(token, text);
                    }
                    break;
                case Const.MESSAGE_REMOVE_HUD:
                    remove(token);
                    break;
                case Const.MESSAGE_TOGGLE_VISIBILITY:
                    debugInfoShown = !debugInfoShown;
                    break;
            }

            invokeOnUpdateSafely();
        }

        private void invokeOnUpdateSafely() {
            removeCallbacks(onUpdateCallback);
            post(onUpdateCallback);
        }

        private void update(IBinder token, String text) {
            DeathAwareMessage deathAwareMsg;

            if (messages.containsKey(token)) {
                deathAwareMsg = messages.get(token);
            } else {
                deathAwareMsg = new DeathAwareMessage(token);

                messages.put(token, deathAwareMsg);

                try {
                    token.linkToDeath(deathAwareMsg, 0);
                } catch (RemoteException e) {
                    Log.e(TAG, "Error during linkToDeath", e);
                }
            }

            deathAwareMsg.updateText(text);
        }

        private void remove(IBinder token) {
            if (messages.containsKey(token)) {
                DeathAwareMessage deathAwareMsg = messages.get(token);
                token.unlinkToDeath(deathAwareMsg, 0);
                messages.remove(token);
            }
        }
    }

    private class ViewUpdater implements Runnable {

        @Override
        public void run() {
            initViewSpace();

            if (viewSpaceInitialized) {
                textUpdate();
                visibilityUpdate();
            }
        }
    }

    private IncomingHandler hudHandler;
    private Messenger messenger;

    private LinearLayout root;
    private TextView debugInfo;

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

    public void textUpdate() {
        StringBuilder sb = new StringBuilder();
        Collection<IncomingHandler.DeathAwareMessage> messages = hudHandler.messages.values();

        int index = 0;
        int count = messages.size();

        for (IncomingHandler.DeathAwareMessage message : messages) {
            sb.append(message);

            sb.append(NEW_LINE);
            sb.append(DELIMITER);

            if (++index != count) {
                sb.append(NEW_LINE);
            }
        }

        debugInfo.setText(sb);
    }

    public void visibilityUpdate() {
        debugInfo.setVisibility(hudHandler.debugInfoShown ? View.VISIBLE : View.GONE);
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
            root.setPadding(10, 10, 10, 10);
            root.setBackgroundColor(COLOR_SHADE);
            windowManager.addView(root, params);

            LinearLayout.LayoutParams rootParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);

            debugInfo = new TextView(this);
            debugInfo.setTextColor(Color.GREEN);
            root.addView(debugInfo, rootParams);

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

