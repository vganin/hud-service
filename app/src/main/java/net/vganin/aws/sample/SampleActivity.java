package net.vganin.aws.sample;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.RemoteViews;

import net.vganin.aws.DebugTextHud;
import net.vganin.aws.Hud;
import net.vganin.aws.HudManager;

import java.util.Random;

public class SampleActivity extends AppCompatActivity {

    private final String[] data = new String[] {
            "Two wrongs don't make a right.",
            "The pen is mightier than the sword.",
            "When in Rome, do as the Romans.",
            "The squeaky wheel gets the grease.",
            "When the going gets tough, the tough get going.",
    };

    private final Random rand = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        findViewById(R.id.toggle_hud_visibility).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleHudVisibility();
            }
        });

        useHud();
    }

    @Override
    protected void onDestroy() {
        HudManager.removeAll(this);

        super.onDestroy();
    }

    private void useHud() {
        HudManager.add(this, new Hud() {

            private final RemoteViews remoteView =
                    new RemoteViews(getPackageName(), R.layout.remote_layout);

            private int rotation = 0;

            @Override
            public RemoteViews getUpdate() {
                remoteView.setCharSequence(R.id.text, "setText", data[rand.nextInt(data.length)]);
                remoteView.setInt(R.id.icon, "setImageLevel", rotation);

                rotation += 1250; // RotateDrawable.MAX_LEVEL / 8
                rotation %= 10000; // RotateDrawable.MAX_LEVEL

                return remoteView;
            }
        });

        HudManager.add(this, new DebugTextHud(this) {
            @Override
            public CharSequence getMessageUpdate() {
                return DateUtils.formatDateTime(
                        SampleActivity.this,
                        System.currentTimeMillis(),
                        DateUtils.FORMAT_SHOW_TIME);
            }
        });
    }

    private void toggleHudVisibility() {
        HudManager.toggleVisibility(this);
    }
}
