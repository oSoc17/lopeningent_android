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

package com.dp16.runamicghent.runDataTests;

import com.dp16.runamicghent.RunData.RunAudio;
import com.dp16.runamicghent.RunData.RunDirection;

import junit.framework.Assert;

import org.junit.Test;

/**
 * Created by hendrikdepauw on 06/05/2017.
 */

public class RunAudioTests {

    @Test
    public void runAudio_normalInstruction_createsSentenceCorrectly(){
        RunAudio runAudioForward = new RunAudio(new RunDirection("forward"));
        RunAudio runAudioRight = new RunAudio(new RunDirection("right"));

        Assert.assertEquals("", runAudioForward.getAudioString());
        Assert.assertEquals("turn right", runAudioRight.getAudioString());
    }

    @Test
    public void runAudio_splitInstruction_createsSentenceCorrectly(){
        RunAudio runAudioNone = new RunAudio(new RunDirection("none"), new RunDirection("forward"));
        RunAudio runAudioNotNone = new RunAudio(new RunDirection("left"), new RunDirection("forward"));

        Assert.assertEquals("for the new route, go forward", runAudioNone.getAudioString());
        Assert.assertEquals("for the new route, go forward. Else, turn left", runAudioNotNone.getAudioString());
    }

    @Test
    public void runAudio_customSentence_createsSentenceCorrectly(){
        String customSentence = "This is a custom sentence.";
        RunAudio runAudio = new RunAudio(customSentence);

        Assert.assertEquals(customSentence, runAudio.getAudioString());
    }

    @Test
    public void runAudio_makeSentence_createsSentenceCorrectly(){
        // As this method is private, it is tested indirectly.
        // Forward is not tested here, but is tested in the other tests.

        RunAudio runAudioForward = new RunAudio(new RunDirection("left"));
        RunAudio runAudioRight = new RunAudio(new RunDirection("right"));
        RunAudio runAudioTurnaround = new RunAudio(new RunDirection("turnaround"));
        RunAudio runAudioNone = new RunAudio(new RunDirection("none"));

        Assert.assertEquals("turn left", runAudioForward.getAudioString());
        Assert.assertEquals("turn right", runAudioRight.getAudioString());
        Assert.assertEquals("please turnaround", runAudioTurnaround.getAudioString());
        Assert.assertEquals("", runAudioNone.getAudioString());
    }

}
