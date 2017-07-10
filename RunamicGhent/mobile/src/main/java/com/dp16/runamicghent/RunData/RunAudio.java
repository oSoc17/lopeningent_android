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

package com.dp16.runamicghent.RunData;

import android.app.Activity;

import com.dp16.runamicghent.Constants;
import com.dp16.runamicghent.GuiController.GuiController;
import com.dp16.runamicghent.R;

import static com.dp16.runamicghent.RunData.RunDirection.Direction.*;

/**
 * This class holds a string that can be played by the text to speech component in the
 * AudioPlayer. There are different ways to construct these objects. The logic is implemented in
 * different constructors. They differ in their input arguments. If you need different logics but
 * the same arguments, we will have to find another solution. For example by setting the string
 * using methods instead of the constructor. If no audio should be played by the AudioPlayer,
 * choose an empty string.
 * Created by hendrikdepauw on 03/05/2017.
 */

public class RunAudio {
    private String audioString;


    /**
     * Just a normal navigation direction. Forward and none are not played.
     * For left and right we use GO in front, for turnaround we use please.
     * It just sounds better that way.
     * @param runDirection Direction to be converted to a string.
     */
    public RunAudio(RunDirection runDirection) {
        if (runDirection.getDirection() == FORWARD){
            audioString = "";
        } else {
            audioString = makeSentence(runDirection);
        }
    }

    /**
     * Use this constructor when the route splits. This method will distinguish between two cases,
     * where the normal RunDirection is None and when it is not. For the rest it works the same
     * as above.
     * @param normalRunDirection Direction to follow the normal route.
     * @param newRunDirection Direction to follow the new route.
     */
    public RunAudio(RunDirection normalRunDirection, RunDirection newRunDirection){

        audioString = GuiController.getInstance().getContext().getString(R.string.audio_new_route).concat(makeSentence(newRunDirection));
        if (normalRunDirection.getDirection() != NONE){
            audioString = audioString.concat(GuiController.getInstance().getContext().getString(R.string.audio_else));
            audioString = audioString.concat(makeSentence(normalRunDirection));
        }
    }

    /**
     * Complete custom sentence.
     * @param audioString String to be played by tts.
     */
    public RunAudio(String audioString){
        this.audioString = audioString;
    }

    /**
     * Returns te audioString. Used by the AudioPlayer
     * @return audioString
     */
    public String getAudioString(){
        return audioString;
    }

    /**
     * Apparently it is polite to speak with at least two words. So this method does that for you.
     * <li>please turnaround</li>
     * <li>turn right</li>
     * <li>turn left</li>
     * <li>go forward</li>
     * @param runDirection Direction to be converted to short sentence.
     * @return Sentence with direction.
     */
    private String makeSentence(RunDirection runDirection){
        switch (runDirection.getDirection()){
            case UTURN:
                return GuiController.getInstance().getContext().getString(R.string.audio_couple_meters).concat(GuiController.getInstance().getContext().getString(R.string.audio_please).concat(runDirection.toString()));
            case RIGHT:
            case LEFT:
                return GuiController.getInstance().getContext().getString(R.string.audio_couple_meters).concat(GuiController.getInstance().getContext().getString(R.string.audio_turn).concat(runDirection.toString()));
            case FORWARD:
                return GuiController.getInstance().getContext().getString(R.string.audio_go).concat(runDirection.toString());
            default:
                return "";
        }
    }
}
