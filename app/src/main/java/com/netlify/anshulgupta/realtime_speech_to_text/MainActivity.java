package com.netlify.anshulgupta.realtime_speech_to_text;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import net.gotev.speech.GoogleVoiceTypingDisabledException;
import net.gotev.speech.Speech;
import net.gotev.speech.SpeechDelegate;
import net.gotev.speech.SpeechRecognitionNotAvailable;
import net.gotev.speech.SpeechUtil;
import net.gotev.speech.ui.SpeechProgressView;

import java.util.List;

public class MainActivity extends AppCompatActivity implements SpeechDelegate {

    private final int PERMISSIONS_REQUEST = 1;

    private ImageButton button;
    private Button btnStop;
    private TextView text;
    private SpeechProgressView progress;
    private LinearLayout linearLayout;
    private Boolean isRunning = true;
    private Integer original_volume_level;
    private AudioManager audioManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Speech.init(this, getPackageName());

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        assert audioManager != null;
        original_volume_level = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        linearLayout = findViewById(R.id.linearLayout);

        button = findViewById(R.id.button);
        btnStop = findViewById(R.id.buttonStop);
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isRunning = false;
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, original_volume_level, 0);
            }
        });
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                onButtonClick();
            }
        });

        text = findViewById(R.id.text);
        progress = findViewById(R.id.progress);

        int[] colors = {
                ContextCompat.getColor(this, R.color.colorPrimary),
                ContextCompat.getColor(this, R.color.colorPrimary),
                ContextCompat.getColor(this, R.color.colorPrimary),
        };

        int[] heights = {40, 56, 48, 50, 44};
        progress.setBarMaxHeightsInDp(heights);
        progress.setColors(colors);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Speech.getInstance().shutdown();

    }

    private void onButtonClick() {
        if (Speech.getInstance().isListening()) {
            Speech.getInstance().stopListening();
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                onRecordAudioPermissionGranted();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != PERMISSIONS_REQUEST) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        } else {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission was granted, yay!
                onRecordAudioPermissionGranted();
            } else {
                // permission denied, boo!
                Toast.makeText(MainActivity.this, R.string.permission_required, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void onRecordAudioPermissionGranted() {
        button.setVisibility(View.GONE);
        linearLayout.setVisibility(View.VISIBLE);

        try {
            Speech.getInstance().stopTextToSpeech();
            Speech.getInstance().startListening(progress, MainActivity.this);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);

        } catch (SpeechRecognitionNotAvailable exc) {
            showSpeechNotSupportedDialog();

        } catch (GoogleVoiceTypingDisabledException exc) {
            showEnableGoogleVoiceTyping();
        }
    }

    @Override
    public void onStartOfSpeech() {
    }

    @Override
    public void onSpeechRmsChanged(float value) {
//        Log.d(getClass().getSimpleName(), "Speech recognition rms is now " + value +  "dB");
    }

    @Override
    public void onSpeechResult(String result) {

        text.setText(result);
        Speech.getInstance().stopTextToSpeech();

        if (isRunning) {
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        Speech.getInstance().startListening(progress, MainActivity.this);
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
                    } catch (SpeechRecognitionNotAvailable speechRecognitionNotAvailable) {
                        speechRecognitionNotAvailable.printStackTrace();
                    } catch (GoogleVoiceTypingDisabledException e) {
                        e.printStackTrace();
                    }
                }
            }, 100);
        } else {

            button.setVisibility(View.VISIBLE);
            linearLayout.setVisibility(View.GONE);
        }
    }


    @Override
    public void onSpeechPartialResults(List<String> results) {
        text.setText("");
        for (String partial : results) {
            text.append(partial + " ");
        }
    }

    private void showSpeechNotSupportedDialog() {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        SpeechUtil.redirectUserToGoogleAppOnPlayStore(MainActivity.this);
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.speech_not_available)
                .setCancelable(false)
                .setPositiveButton(R.string.yes, dialogClickListener)
                .setNegativeButton(R.string.no, dialogClickListener)
                .show();
    }

    private void showEnableGoogleVoiceTyping() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.enable_google_voice_typing)
                .setCancelable(false)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // do nothing
                    }
                })
                .show();
    }
}
