/*
 * Copyright (c) 2017 Hendrik Depauw
 * Copyright (c) 2017 Lorenz van Herwaarden
 * Copyright (c) 2017 Nick Aelterman
 * Copyright (c) 2017 Olivier Cammaert
 * Copyright (c) 2017 Maxim Deweirdt
 * Copyright (c) 2017 Gerwin Dox
 * Copyright (c) 2017 Simon Neuville
 * Copyright (c) 2017 Stiaan Uyttersprot
 *
 * This software may be modified and distributed under the terms of the MIT license.  See the LICENSE file for details.
 */

package com.dp16.runamicghent;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import com.dp16.runamicghent.GuiController.GuiController;
import com.dp16.runamicghent.RunData.RunAudio;
import com.dp16.eventbroker.EventBroker;
import com.dp16.eventbroker.EventListener;

import java.text.BreakIterator;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class listens for RUN_AUDIO events on the Eventbroker and plays them using TextToSpeech.
 * The objects transferred using the RUN_AUDIO events should be RunAudio objects. These
 * conain a string that can be played. If multiple RUN_AUDIO events arrive at the same time,
 * they are played in the order of arrival. They do not interrupt each other but are put in a
 * queue. The AudioManager will ask other audio sources present in the device to dim while an
 * audio message is played.
 *
 * <p>
 *     <b>Messages Produced: </b> None.
 * </p>
 * <p>
 *     <b>Messages Consumed: </b> {@link com.dp16.runamicghent.Constants.EventTypes#AUDIO}
 * </p>
 *
 * Created by hendrikdepauw on 03/05/2017.
 */
public class AudioPlayer implements EventListener{
    private TextToSpeech mTts;
    private AudioManager am;
    private Context context;

    /*
     * Variable that contains the length of the TTS queue.
     * HandleEvent and the Listener are in different threads so we need an atomic variable.
     */
    private AtomicInteger TTSQueue;

    public AudioPlayer(Context context){
        this.context = context;
        am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    /**
     * Initiates the TextToSpeech component and registers to the EventBroker.
     */
    public void start(){
        mTts = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = mTts.setLanguage(Locale.getDefault());
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("error", "This Language is not supported");
                    }
                } else {
                    Log.e("error", "Initialization Failed!");
                }
            }
        });

        TTSQueue = new AtomicInteger(0);
        mTts.setOnUtteranceProgressListener(new ttsUtteranceListener());

        EventBroker.getInstance().addEventListener(Constants.EventTypes.AUDIO, this);
    }

    /**
     * Stops the TextToSpeech component and unregisters from the EventBroker.
     */
    public void stop(){
        EventBroker.getInstance().removeEventListener(Constants.EventTypes.AUDIO, this);

        if (mTts != null) {
            mTts.stop();
            mTts.shutdown();
        }
    }

    /**
     * Play a RunAudio object. If the RunAudio object does contain an empty string (which can happen)
     * nothing is done. As far as I know the requestAudioFocus and speak method to return
     * immediately, so there is minimal delay in the EventBroker. If this appears not to be the case,
     * we should use a worker thread here.
     * @param eventType
     * @param message
     */
    public void handleEvent(String eventType, Object message){
        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(context);
        RunAudio runAudio = (RunAudio) message;

        if(!runAudio.getAudioString().isEmpty() && preference.getBoolean("pref_key_audio", true)){
            /*
             * The string is split into separate sentences, the sentences are passed one by one
             * to the TextToSpeech object. Normally it should pause after points but for some
             * reason it just does not do it. Online they suggested doing it like this. So I did.
             */
            String audioString = runAudio.getAudioString();
            BreakIterator iterator = BreakIterator.getSentenceInstance(Locale.getDefault());
            iterator.setText(audioString);
            int start = iterator.first();
            for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
                // Increase the queue content and add the sentence to the queue.
                TTSQueue.incrementAndGet();
                mTts.speak(audioString.substring(start,end), TextToSpeech.QUEUE_ADD, null, TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID);
            }
        }
    }

    /**
     * This Listener class listens to events from the TextToSpeech component. When TTS is
     * starting a sentence, onStart() is called and focus is requested from the audioManager.
     * In some cases we might already have focus, but this is not a problem. When TTS is finished
     * with a sentence, the TTSQueue variable is decreased. When it is 0, this means the queue
     * is empty and we can abandon the focus. Abandoning and requesting focus after and before
     * every sentence pose some problems with the music not starting every time.
     */
    private class ttsUtteranceListener extends UtteranceProgressListener{
        @Override
        public void onDone(String utteranceId) {
            if(TTSQueue.decrementAndGet() == 0){
                am.abandonAudioFocus(null);
            }
        }

        @Override
        public void onError(String utteranceId) {
            am.abandonAudioFocus(null);
        }

        @Override
        public void onStart(String utteranceId) {
            am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
        }
    }
}