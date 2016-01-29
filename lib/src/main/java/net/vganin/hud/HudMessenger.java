package net.vganin.hud;

import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.BundleCompat;
import android.util.Log;

final class HudMessenger {

    private static final String TAG = HudMessenger.class.getSimpleName();

    private static class SenderRunnable implements Runnable {

        Messenger messenger;
        Message message;

        SenderRunnable(Messenger messenger, Message message) {
            this.messenger = messenger;
            this.message = message;
        }

        @Override
        public void run() {
            try {
                messenger.send(message);
            } catch (RemoteException e) {
                Log.e(TAG, "Error during removeViews send", e);
            }
        }
    }

    private Messenger mMessenger;
    private Handler mMainHandler = new Handler(Looper.getMainLooper());

    HudMessenger(IBinder target) {
        mMessenger = new Messenger(target);
    }

    void update(Hud hud) {
        Message message = createBaseMessage(hud);
        message.what = Const.MESSAGE_UPDATE_HUD;
        sendSafely(message);
    }

    void remove(Hud hud) {
        Message message = createBaseMessage(hud);
        message.what = Const.MESSAGE_REMOVE_HUD;
        sendSafely(message);
    }

    void toggleVisibility() {
        Message message = new Message();
        message.what = Const.MESSAGE_TOGGLE_VISIBILITY;
        sendSafely(message);
    }

    private Message createBaseMessage(Hud hud) {
        Message message = new Message();
        Bundle args = message.getData();
        BundleCompat.putBinder(args, Const.EXTRA_TOKEN, hud.mToken);
        args.putParcelable(Const.EXTRA_MESSAGE, hud.getUpdate());
        return message;
    }

    private void sendSafely(Message message) {
        mMainHandler.post(new SenderRunnable(mMessenger, message));
    }
}
