package net.vganin.hud.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.RemoteViews;

import net.vganin.hud.DebugTextHud;
import net.vganin.hud.Hud;
import net.vganin.hud.HudManager;

import java.util.Random;

public class SampleActivity extends AppCompatActivity {

    private static final int PROVERB_UPDATE_PERIOD = 10000;

    private final String[] proverbs = new String[] {
            "Two wrongs don't make a right.",
            "The pen is mightier than the sword.",
            "When in Rome, do as the Romans.",
            "The squeaky wheel gets the grease.",
            "When the going gets tough, the tough get going.",
    };

    private final Random rand = new Random();

    private Hud counterHud;

    private int counter = 0;
    private int proverbIndex = 0;
    private long lastProverbUpdate = 0;

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

        findViewById(R.id.add_to_counter).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                counter++;
                if (counterHud != null) {
                    counterHud.requestUpdate(SampleActivity.this);
                }
            }
        });

        useHud();
    }

    @Override
    protected void onDestroy() {
        HudManager.removeAll(this);

        super.onDestroy();
    }

    private static final String STATE_COUNTER = "counter";
    private static final String STATE_PROVERB_INDEX = "proverb_index";
    private static final String STATE_LAST_PROVERB_UPDATE = "last_proverb_update";

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(STATE_COUNTER, counter);
        outState.putInt(STATE_PROVERB_INDEX, proverbIndex);
        outState.putLong(STATE_LAST_PROVERB_UPDATE, lastProverbUpdate);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        counter = savedInstanceState.getInt(STATE_COUNTER, 0);
        proverbIndex = savedInstanceState.getInt(STATE_PROVERB_INDEX, proverbIndex);
        lastProverbUpdate = savedInstanceState.getLong(STATE_LAST_PROVERB_UPDATE, lastProverbUpdate);
    }

    private void useHud() {
        HudManager.add(this, new Hud() {

            private final RemoteViews remoteView =
                    new RemoteViews(getPackageName(), R.layout.remote_layout);

            private int rotation = 0;

            @Override
            public RemoteViews getUpdate() {
                updateTextIfNeeded();

                remoteView.setCharSequence(R.id.text, "setText", proverbs[proverbIndex]);
                remoteView.setInt(R.id.icon, "setImageLevel", rotation);

                rotation -= 1000;
                rotation %= 10000; // RotateDrawable.MAX_LEVEL

                return remoteView;
            }

            @Override
            public int getUpdatePeriod() {
                return Hud.MINIMUM_UPDATE_PERIOD;
            }

            private void updateTextIfNeeded() {
                if (System.currentTimeMillis() - lastProverbUpdate > PROVERB_UPDATE_PERIOD) {
                    int newProverbIndex = rand.nextInt(proverbs.length - 1);
                    proverbIndex = newProverbIndex == proverbIndex
                            ? proverbs.length - 1 : newProverbIndex;
                    lastProverbUpdate = System.currentTimeMillis();
                }
            }
        });

        counterHud = new DebugTextHud(this) {

            private static final int HALF_MINUTE = 30 * 1000; // ms

            @Override
            public CharSequence getMessageUpdate() {
                String update = DateUtils.formatDateTime(
                        SampleActivity.this,
                        System.currentTimeMillis(),
                        DateUtils.FORMAT_SHOW_TIME);
                update += " Button was pressed " + counter + " times";
                return update;
            }

            @Override
            public int getUpdatePeriod() {
                return HALF_MINUTE;
            }
        };
        HudManager.add(this, counterHud);
    }

    private void toggleHudVisibility() {
        HudManager.toggleVisibility(this);
    }
}
