package im.zego.customaudioio;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;

public class AudioCustomActivity extends Activity {
    private Button capture,render,captured, received;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.audio_custom);
        capture=findViewById(R.id.capture);
        render=findViewById(R.id.render);
        captured =findViewById(R.id.capturedAudioData);
        received = findViewById(R.id.ReceivedAudioData);

        capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent =new Intent(AudioCustomActivity.this, AudioCustomCaptureActivity.class);
                startActivity(intent);
            }
        });

        render.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent =new Intent(AudioCustomActivity.this, AudioCustomRenderActivity.class);
                startActivity(intent);
            }
        });

        captured.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent =new Intent(AudioCustomActivity.this, CapturedAudioActivity.class);
                startActivity(intent);
            }
        });

        received.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent =new Intent(AudioCustomActivity.this, ReceivedAudioActivity.class);
                startActivity(intent);
            }
        });

    }
}
