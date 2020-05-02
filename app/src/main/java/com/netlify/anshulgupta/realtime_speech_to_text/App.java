package com.netlify.anshulgupta.realtime_speech_to_text;

import android.app.Application;

import net.gotev.speech.Logger;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        Logger.setLogLevel(Logger.LogLevel.DEBUG);
    }
}