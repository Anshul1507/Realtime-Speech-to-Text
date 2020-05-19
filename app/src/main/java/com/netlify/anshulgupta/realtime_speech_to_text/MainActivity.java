package com.netlify.anshulgupta.realtime_speech_to_text;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
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

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements SpeechDelegate {

    private final int PERMISSIONS_REQUEST = 1;
    private static final String TAG = "MainActivity";
    private ImageButton button;
    private Button btnStop, btnResultErase;
    private TextView text, resultText;
    private SpeechProgressView progress;
    private LinearLayout linearLayout;
    private Boolean isRunning = true;
    private Integer original_volume_level;
    private AudioManager audioManager;

    private ArrayList<String> textList;
    private ArrayList<String> speechList, partialSpeechList;
    private Set set = new LinkedHashSet();
    private ArrayList<String> textAllList = new ArrayList<String>();
    private int counterSpan = 0, idx = 0, prevIdx = 0, prevCounterSpan = 0, partialResultSpeechCounter = 0;
    private SpannableString spannableString;
    private ForegroundColorSpan foregroundRedSpan, foregroundGreenSpan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Speech.init(this, getPackageName());

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        assert audioManager != null;
        original_volume_level = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        linearLayout = findViewById(R.id.layout_progress);
        button = findViewById(R.id.button);
        btnStop = findViewById(R.id.buttonStop);
        text = findViewById(R.id.text);
        progress = findViewById(R.id.progress);
        btnResultErase = findViewById(R.id.button_clear_result_text);
        resultText = findViewById(R.id.speech_result_text);

        linearLayout.setVisibility(View.GONE);
        text.setText(R.string.sample1);

        String paraText = text.getText().toString();
        spannableString = new SpannableString(paraText);
        foregroundRedSpan = new ForegroundColorSpan(Color.RED);
        foregroundGreenSpan = new ForegroundColorSpan(Color.GREEN);


        textList = new ArrayList<>();
        textList.addAll(getWords(paraText.toLowerCase()));

        textAllList = new ArrayList<String>(Arrays.asList(paraText.split(" ")));

        btnResultErase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resultText.setText("Speech Result :-> ");
            }
        });

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isRunning = true;
                spannableString.removeSpan(foregroundGreenSpan);
                spannableString.removeSpan(foregroundRedSpan);
                text.setText(spannableString);
                button.setVisibility(View.GONE);
                btnStop.setVisibility(View.VISIBLE);
                linearLayout.setVisibility(View.VISIBLE);
                counterSpan = 0;
                prevIdx = 0;
                Speech.getInstance().stopTextToSpeech();
                onButtonClick();
            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isRunning = false;
                btnStop.setVisibility(View.GONE);
                Toast.makeText(getApplicationContext(), "Hold on to start again", Toast.LENGTH_SHORT).show();
                linearLayout.setVisibility(View.GONE);
                Speech.getInstance().stopTextToSpeech();
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, original_volume_level, 0);
            }
        });

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

    public static ArrayList<String> getWords(String text) {
        ArrayList<String> words = new ArrayList<String>();
        BreakIterator breakIterator = BreakIterator.getWordInstance();
        breakIterator.setText(text);
        int lastIndex = breakIterator.first();
        while (BreakIterator.DONE != lastIndex) {
            int firstIndex = lastIndex;
            lastIndex = breakIterator.next();
            if (lastIndex != BreakIterator.DONE && Character.isLetterOrDigit(text.charAt(firstIndex))) {
                words.add(text.substring(firstIndex, lastIndex));
            }
        }

        return words;
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
        Log.d(TAG, "onSpeechResult: " + result.toLowerCase());
        Speech.getInstance().stopTextToSpeech();
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
    }


    @Override
    public void onSpeechPartialResults(List<String> results) {
        resultText.append("\n");

        for(String partial : results){
            Log.d(TAG, "onSpeechPartialResults: " + partial);
        }
//        set.addAll(Arrays.asList(results.toString().split(" ")));
//        if(idx<=textAllList.size()){
//            matchingAlgorithm(set);
//        }else{
//            //Finished
//        }

    }

    private void matchingAlgorithm(Set getList){

        /* Check for eg: 2 and two (Integer and string of number) -
         * -> using separate array list for them.
         */
        ArrayList<String> numIntegerList = new ArrayList<>(Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7", "8", "9"));
        ArrayList<String> numStringList = new ArrayList<>(Arrays.asList("zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine"));

        /* Copy the data of getList into array list, so upon clearing it from above function, data remains safe */
        ArrayList<String> partialSpeechResult = new ArrayList<>();
        partialSpeechResult.addAll(0,getList);
        Log.d("Test", "matchingAlgorithm: " + partialSpeechResult.size() + "\n " + partialSpeechResult.toString());

        for(int i=0;i<partialSpeechResult.size();i++){
            Log.d("--------------> ", "run: " + idx + " -> " + textAllList.size());
            boolean numericValueFlag = false;
            String numSpeechValue = null;

            if ((int) (partialSpeechResult.get(i).charAt(0)) <= 57 && (int) (partialSpeechResult.get(i).charAt(0)) >= 48) {
                /* Safety for integer overflows with string type ArrayList */
                if (partialSpeechResult.get(i).length() <= 1) {
                    numSpeechValue = partialSpeechResult.get(i);
                    if ((partialSpeechResult.get(i)).equals(numIntegerList.get(Integer.parseInt(String.valueOf(partialSpeechResult.get(i).charAt(0)))))) {
                        // works only if it matches with numValue List elements
                        partialSpeechResult.set(i, numStringList.get(Integer.parseInt(partialSpeechResult.get(i))));
                    }
                }
            }

            if (numSpeechValue != null ) {
                if (textList.get(idx).substring(0, 1).equals(numSpeechValue) ||
                        textList.get(idx).substring(textList.get(idx).length() - 1).equals(numSpeechValue)) {
                    numericValueFlag = true;
                }
            }

            /* General Algorithm for checking word in partial speech list at cur idx|| numeric matching*/
            if(partialSpeechResult.contains(textList.get(idx)) || numericValueFlag){
                Log.d("Test", "Matched Word " + textList.get(idx));
                spannableString.removeSpan(foregroundRedSpan);
                spannableString.setSpan(foregroundGreenSpan,0,counterSpan+textAllList.get(idx).length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                text.setText(spannableString);
                counterSpan += textAllList.get(idx).length()+1; //update counterSpan for next word & +1 for white space
                idx++;
            }else{
                /* If first word didn't match and if there is second word and matched -> move forward with accuracy of 50% */
//                if( (textAllList.size()>idx+1) && partialSpeechResult.size()+ && partialSpeechResult.contains(textList.get(idx+1)) ){
//                    Log.d("Test", "Second Matched Word " + textList.get(idx+1));
//                    spannableString.removeSpan(foregroundRedSpan);
//                    counterSpan += textAllList.get(idx).length()+1; //for previous word length
//                    spannableString.setSpan(foregroundGreenSpan,0,counterSpan+textAllList.get(idx+1).length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
//                    sampleText.setText(spannableString);
//                    counterSpan += textAllList.get(idx+1).length()+1; //update counterSpan for next word & +1 for white space
//                    idx+=2; //two words matched
//                }else{
                /* Both the words didn't matched */
                Log.d("Test", " No Matched Word " + textList.get(idx));
                spannableString.setSpan(foregroundRedSpan,counterSpan,counterSpan+textAllList.get(idx).length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                text.setText(spannableString);
//                }
            }

            /* Another Check for exiting */
            if(idx == textList.size()){
                //Finished
                break;
            }

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
