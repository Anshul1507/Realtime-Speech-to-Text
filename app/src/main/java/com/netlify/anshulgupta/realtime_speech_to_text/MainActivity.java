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
import java.util.List;

public class MainActivity extends AppCompatActivity implements SpeechDelegate {

    private final int PERMISSIONS_REQUEST = 1;
    private static final String TAG = "MainActivity";
    private ImageButton button;
    private Button btnStop;
    private TextView text;
    private SpeechProgressView progress;
    private LinearLayout linearLayout;
    private Boolean isRunning = true;
    private Integer original_volume_level;
    private AudioManager audioManager;

    private ArrayList<String> textList;
    private ArrayList<String> speechList;
    private ArrayList<String> textAllList = new ArrayList<String>();
    private int counterSpan = 0,idx=0,prevIdx=0,prevCounterSpan = 0;
    private SpannableString spannableString;
    private ForegroundColorSpan foregroundRedSpan,foregroundGreenSpan;

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

        linearLayout.setVisibility(View.GONE);
        text.setText(R.string.sample1);

        String paraText = text.getText().toString();
        spannableString = new SpannableString(paraText);
        foregroundRedSpan = new ForegroundColorSpan(Color.RED);
        foregroundGreenSpan = new ForegroundColorSpan(Color.GREEN);


        textList = new ArrayList<>();
        textList.addAll(getWords(paraText.toLowerCase()));

        textAllList = new ArrayList<String>(Arrays.asList(paraText.split(" ")));


        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isRunning = true;
                button.setVisibility(View.GONE);
                btnStop.setVisibility(View.VISIBLE);
                linearLayout.setVisibility(View.VISIBLE);
                onButtonClick();
            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isRunning = false;
                btnStop.setVisibility(View.GONE);
                button.setVisibility(View.VISIBLE);
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
//        button.setVisibility(View.GONE);
//        linearLayout.setVisibility(View.VISIBLE);

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
        int counter = 0;
        ArrayList<String> numStringList = new ArrayList<>(Arrays.asList("zero","one", "two", "three", "four", "five", "six", "seven", "eight", "nine"));
        ArrayList<String> numValueList = new ArrayList<>(Arrays.asList("0","1","2","3","4","5","6","7","8","9"));
        speechList = new ArrayList<>();

        if(!result.isEmpty()) {
            speechList.clear();
            if(result.length()>1) {
                speechList.addAll(getWords(result));
            }else{
                speechList.add(result.toLowerCase());
            }
        }
        int sizeCounter = speechList.size();

        Log.d(TAG, "onSpeechResult: size of speech List => " + speechList.size());

        idx = prevIdx;

        for (int i = 0; i < speechList.size(); i++) {

            boolean numericValue = false; // Because it stops on numeric as Array list is of String data-type
            String numSpeechValue=null;

            if( (int)(speechList.get(i).charAt(0)) <= 57 && (int)(speechList.get(i).charAt(0)) >= 48){
                /* Safety for integer overflows with string type ArrayList */
                if(speechList.get(i).length() <= 1){
                    numSpeechValue = speechList.get(i);
                    if ( (speechList.get(i)).equals(numValueList.get(Integer.parseInt(String.valueOf(speechList.get(i).charAt(0))))) ){
                        // works only if it matches with numValue List elements
                        speechList.set(i, numStringList.get(Integer.parseInt(speechList.get(i))));
                    }
                }
            }

            if(numSpeechValue!=null){
                if(textList.get(prevIdx).substring(0,1).equals(numSpeechValue) ||
                   textList.get(prevIdx).substring(textList.get(prevIdx).length()-1).equals(numSpeechValue) ){
                    numericValue = true;
                }
            }


            if ( textList.get(prevIdx).substring(0,1).equals(speechList.get(i).substring(0,1).toLowerCase()) ||
                 textList.get(prevIdx).substring(textList.get(prevIdx).length()-1).equals(speechList.get(i).substring(speechList.get(i).length()-1).toLowerCase()) ||
                    numericValue
            ) {
                Log.d(TAG, "onSpeechResults: Matched Word " + speechList.get(i) + " -> " + textList.get(prevIdx) + " to " + textAllList.get(prevIdx));
                spannableString.removeSpan(foregroundRedSpan);
                spannableString.setSpan(foregroundGreenSpan, 0, counterSpan+textAllList.get(prevIdx).length() , Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                text.setText(spannableString);
                counterSpan += textAllList.get(prevIdx).length()+1;
                idx++;
                prevIdx++;
                if(sizeCounter>1){ counter++; }

            } else {
                //check for next word matching, if yes then both are accepted (if first word didn't matched)
                if( (textList.size()>prevIdx+1) && ( speechList.get(i).substring(0,1).toLowerCase().equals(textList.get(prevIdx+1).substring(0,1)) ||
                   speechList.get(i).substring(speechList.get(i).length()-1).equals(textList.get(prevIdx+1).substring(textList.get(prevIdx+1).length()-1)) )
                ){
                    counterSpan += textAllList.get(prevIdx).length()+1;
                    spannableString.removeSpan(foregroundRedSpan);
                    spannableString.setSpan(foregroundGreenSpan, 0, counterSpan+textAllList.get(prevIdx+1).length() , Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    text.setText(spannableString);
                    counterSpan += textAllList.get(prevIdx+1).length()+1;
                    idx+=2;
                    prevIdx+=2;
                    if(sizeCounter>1){ counter+=2; }
                }else {
                    Log.d(TAG, "onSpeechResults: Unmatched Word " + speechList.get(i) + " -> " + textList.get(prevIdx) + " to " + textAllList.get(prevIdx));
                    spannableString.setSpan(foregroundRedSpan, counterSpan, counterSpan + textAllList.get(prevIdx).length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    text.setText(spannableString);
                    idx++;
                }
            }

            if(i==sizeCounter-1 && counter==0){
                /* When no word is matched or size is one -> counter value is 0 */
                speechList.clear();
            }

            if(i==sizeCounter-1 && counter>0){
                /*Checking on 50% accuracy*/
                Log.d(TAG, "onSpeechResult: Accuracy: -> " + (float)(sizeCounter/(float)counter)*100);
                if(sizeCounter/counter<=2){
                    spannableString.removeSpan(foregroundRedSpan);
                    spannableString.setSpan(foregroundGreenSpan, 0, counterSpan , Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    text.setText(spannableString);
                }else{
                    spannableString.setSpan(foregroundRedSpan,prevCounterSpan,counterSpan, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    text.setText(spannableString);
                }
            }
            prevCounterSpan = counterSpan;
        }
            Log.d(TAG, "onSpeechResult: -----> Empty Running of loop ");

        //TODO :: stop listener on nearly 10 empty cont. speech result (Mainly Internet connection)
            if(prevIdx<textList.size()) {
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

    }


    @Override
    public void onSpeechPartialResults(List<String> results) {
//        text.setText("");
//        for (String partial : results) {
//            Log.d(TAG, "onSpeechPartialResults: " + partial);
//            text.append(partial + " ");
//        }
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
