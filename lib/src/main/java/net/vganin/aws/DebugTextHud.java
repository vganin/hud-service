package net.vganin.aws;

import android.content.Context;
import android.widget.RemoteViews;

public abstract class DebugTextHud extends Hud {

    private final RemoteViews remoteView;

    public DebugTextHud(Context ctx) {
        remoteView = new RemoteViews(ctx.getPackageName(), R.layout.debug_text_hud);
    }

    public abstract CharSequence getMessageUpdate();

    @Override
    public final RemoteViews getUpdate() {
        CharSequence messageUpdate = getMessageUpdate();
        if (messageUpdate != null) {
            remoteView.setTextViewText(R.id.debug_text, messageUpdate);
            return remoteView;
        }
        return null;
    }
}
