package net.vganin.aws;

/**
 * Communication protocol between {@link HudManager} and {@link HudService}.
 */
final class Const {

    /**
     * Message ID for updating HUD.
     */
    static final int MESSAGE_UPDATE_HUD = 0;

    /**
     * Message ID for removing HUD.
     */
    static final int MESSAGE_REMOVE_HUD = 1;

    /**
     * Message ID for toggling HUD visibility.
     */
    static final int MESSAGE_TOGGLE_VISIBILITY = 2;

    /**
     * Process (and client) identifier. Must be of type {@link android.os.IBinder}.
     */
    static final String EXTRA_TOKEN = "extra_token";

    /**
     * Data bundle for service to display. Must be of type {@link android.widget.RemoteViews}.
     */
    static final String EXTRA_MESSAGE = "extra_message";
}
