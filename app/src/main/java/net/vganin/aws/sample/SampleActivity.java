package net.vganin.aws.sample;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

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

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        useHud();
    }

    private void useHud() {
        HudManager.add(this, new Hud() {
            @Override
            public String getMessage() {
                return data[rand.nextInt(data.length)];
            }
        });
    }
}
