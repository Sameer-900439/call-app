package com.example.fakecallangel;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;

public class FakeCallActivity extends AppCompatActivity {

    TextView tvName, tvStatus;
    Button btnAnswer, btnHangup, btnSpeaker, btnMute, btnKeypad;
    GridLayout layoutControls;

    MediaPlayer ringtonePlayer;
    MediaPlayer voicePlayer;
    Vibrator vibrator;
    AudioManager audioManager;

    boolean isSpeakerOn = false;
    boolean isMuted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fake_call);

        // Bind Views
        tvName = findViewById(R.id.tvIncomingName);
        tvStatus = findViewById(R.id.tvCallStatus);
        btnAnswer = findViewById(R.id.btnAnswer);
        btnHangup = findViewById(R.id.btnHangup);
        layoutControls = findViewById(R.id.layoutControls);
        btnSpeaker = findViewById(R.id.btnSpeaker);
        btnMute = findViewById(R.id.btnMute);
        btnKeypad = findViewById(R.id.btnKeypad);

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        // Get Data
        String name = getIntent().getStringExtra("name");
        String audioFileName = getIntent().getStringExtra("audioFileName");
        if(name != null) tvName.setText(name);

        // --- 1. START RINGING ---
        startRinging();

        // --- 2. ANSWER BUTTON ---
        btnAnswer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopRinging();

                // UI Changes
                btnAnswer.setVisibility(View.GONE);
                btnHangup.setVisibility(View.VISIBLE);
                layoutControls.setVisibility(View.VISIBLE);
                tvStatus.setText("00:00");

                // Start Voice
                startVoice(audioFileName);
            }
        });

        // --- 3. HANGUP BUTTON ---
        btnHangup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopVoice();
                finish(); // Close screen
            }
        });

        // --- 4. SPEAKER BUTTON ---
        btnSpeaker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isSpeakerOn) {
                    // Turn Speaker ON
                    audioManager.setMode(AudioManager.MODE_NORMAL);
                    audioManager.setSpeakerphoneOn(true);
                    btnSpeaker.setAlpha(1.0f);
                    isSpeakerOn = true;
                } else {
                    // Turn Speaker OFF
                    audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                    audioManager.setSpeakerphoneOn(false);
                    btnSpeaker.setAlpha(0.5f);
                    isSpeakerOn = false;
                }
            }
        });

        // --- 5. MUTE BUTTON ---
        btnMute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (voicePlayer != null) {
                    if (!isMuted) {
                        voicePlayer.setVolume(0, 0);
                        btnMute.setAlpha(1.0f);
                        isMuted = true;
                    } else {
                        voicePlayer.setVolume(1, 1);
                        btnMute.setAlpha(0.5f);
                        isMuted = false;
                    }
                }
            }
        });
    }

    private void startRinging() {
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            ringtonePlayer = MediaPlayer.create(this, notification);
            if (ringtonePlayer != null) {
                ringtonePlayer.setLooping(true);
                ringtonePlayer.start();
            }
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null) vibrator.vibrate(new long[]{0, 1000, 1000}, 0);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void stopRinging() {
        if (ringtonePlayer != null) { ringtonePlayer.stop(); ringtonePlayer.release(); ringtonePlayer = null; }
        if (vibrator != null) vibrator.cancel();
    }

    private void startVoice(String fileName) {
        // Set to Earpiece initially
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        audioManager.setSpeakerphoneOn(false);

        try {
            if (fileName != null) {
                File customFile = new File(getFilesDir(), fileName);
                if (customFile.exists()) {
                    voicePlayer = MediaPlayer.create(this, Uri.fromFile(customFile));
                    if (voicePlayer != null) voicePlayer.start();
                } else {
                    Toast.makeText(this, "Audio file not found on phone", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "No audio file selected", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void stopVoice() {
        if (voicePlayer != null) { voicePlayer.release(); voicePlayer = null; }
        audioManager.setMode(AudioManager.MODE_NORMAL);
        audioManager.setSpeakerphoneOn(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRinging();
        stopVoice();
    }
}