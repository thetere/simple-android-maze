package ar.edu.ips.aus.seminario2.sampleproject;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class FinishSplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.finish_splash);

        Button btnCallGame = (Button) findViewById(R.id.replay_button);
        btnCallGame.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent elJuegoPosta = new Intent(FinishSplashActivity.this, GameSelectionActivity.class);
                startActivity(elJuegoPosta);
                finish();
            }
        });


    }
}
