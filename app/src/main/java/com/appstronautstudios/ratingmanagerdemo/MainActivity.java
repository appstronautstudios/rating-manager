package com.appstronautstudios.ratingmanagerdemo;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.appstronautstudios.ratingmanager.managers.RatingManager;
import com.appstronautstudios.ratingmanager.utils.YesNoCancelListener;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RatingManager.getInstance().init(MainActivity.this);

        configure();
    }

    private void configure() {
        TextView daysTV = findViewById(R.id.days);
        TextView sessionsTV = findViewById(R.id.sessions);
        View forceRTA = findViewById(R.id.force_rta);
        View forceRAB = findViewById(R.id.force_rab);
        TextView lastShownTV = findViewById(R.id.last_shown);

        daysTV.setText(RatingManager.getInstance().getDaysSinceInstall(MainActivity.this) + "");
        sessionsTV.setText(RatingManager.getInstance().getNumberOfSessions(MainActivity.this) + "");
        forceRTA.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                RatingManager.getInstance().showRTA(MainActivity.this, new YesNoCancelListener() {
                    @Override
                    public void onYes() {
                        Toast.makeText(MainActivity.this, "Yes", Toast.LENGTH_SHORT).show();
                        configure();
                    }

                    @Override
                    public void onNo() {
                        Toast.makeText(MainActivity.this, "No", Toast.LENGTH_SHORT).show();
                        configure();
                    }

                    @Override
                    public void onCancel() {
                        Toast.makeText(MainActivity.this, "Cancel", Toast.LENGTH_SHORT).show();
                        configure();
                    }
                });
            }
        });
        forceRAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                RatingManager.getInstance().showRAB(MainActivity.this, 4, "appstronautstudios@gmail.com", new YesNoCancelListener() {
                    @Override
                    public void onYes() {
                        Toast.makeText(MainActivity.this, "Yes", Toast.LENGTH_SHORT).show();
                        configure();
                    }

                    @Override
                    public void onNo() {
                        Toast.makeText(MainActivity.this, "No", Toast.LENGTH_SHORT).show();
                        configure();
                    }

                    @Override
                    public void onCancel() {
                        Toast.makeText(MainActivity.this, "Cancel", Toast.LENGTH_SHORT).show();
                        configure();
                    }
                });
            }
        });
        lastShownTV.setText("last shown:" + RatingManager.getInstance().getLastTimeRtaShown(MainActivity.this));
    }
}
