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

import android.app.Application;

import com.dp16.runamicghent.RunData.RunAudio;
import com.dp16.runamicghent.RunData.RunDirection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/**
 * Created by Stiaan on 18/05/2017.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class AudioPlayerTests {

    private Application application;

    @Before
    public void init(){
        application = RuntimeEnvironment.application;
    }

    @Test
    public void audioPlayer_make_start_stop(){

        AudioPlayer audioPlayer = new AudioPlayer(application);

        audioPlayer.start();
        audioPlayer.stop();
    }

    @Test
    public void audioPlayer_handle_event(){
        AudioPlayer audioPlayer = new AudioPlayer(application);
        RunAudio runAudio = new RunAudio(new RunDirection(RunDirection.Direction.FORWARD));
        audioPlayer.start();
        audioPlayer.handleEvent("" ,runAudio);

        audioPlayer.stop();
    }
}
