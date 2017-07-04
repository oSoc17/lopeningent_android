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

import com.dp16.runamicghent.RunData.RunDuration;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Unit tests for RunData.RunDuration
 * Created by Nick on 25-3-2017.
 */

public class RunDurationTests {

    @Test
    public void runDuration_addAFewSeconds_returnsRightAmount() {
        int maxIterations = 1547;
        RunDuration runDuration = new RunDuration();
        for (int i = 0; i < maxIterations; i++) {
            runDuration.addSecond();
        }
        assertEquals("RunDuration does not count seconds correctly", maxIterations, runDuration.getSecondsPassed());
    }

    @Test
    public void runDuration_constructor_initializesCorrectly(){
        int secondsPassed = 76898;
        RunDuration runDuration = new RunDuration(secondsPassed);

        assertEquals("RunDuration does not initialize secondsPassed correctly", secondsPassed, runDuration.getSecondsPassed());
    }

    @Test
    public void runDuration_lessThanHourtoString_formatsCorrectly() {
        int duration = 1457; // less than an hour
        RunDuration runDuration = new RunDuration();
        for (int i = 0; i < duration; i++) {
            runDuration.addSecond();
        }
        String shortPattern = "(\\d+):(\\d+)";
        Pattern regexPattern = Pattern.compile(shortPattern);
        Matcher matcher = regexPattern.matcher(runDuration.toString());
        if (matcher.find()) {
            int minutes = Integer.parseInt(matcher.group(1));
            int seconds = Integer.parseInt(matcher.group(2));
            assertEquals("RunDuration string formatting does not confirm the interface description (less than hour)", duration / 60, minutes);
            assertEquals("RunDuration string formatting does not confirm the interface description (less than hour)", duration % 60, seconds);
        } else {
            fail("RunDuration string formatting does not confirm the interface description (less than hour): does not follow regex");
        }
    }

    @Test
    public void runDuration_lessThanMinuteToString_formatsCorrectly() {
        int duration = 45;
        RunDuration runDuration = new RunDuration();
        for (int i = 0; i < duration; i++) {
            runDuration.addSecond();
        }
        String shortPattern = "(\\d+):(\\d+)";
        Pattern regexPattern = Pattern.compile(shortPattern);
        Matcher matcher = regexPattern.matcher(runDuration.toString());
        if (matcher.find()) {
            int minutes = Integer.parseInt(matcher.group(1));
            int seconds = Integer.parseInt(matcher.group(2));
            assertEquals("RunDuration string formatting does not confirm the interface description (less than minute)", duration / 60, minutes);
            assertEquals("RunDuration string formatting does not confirm the interface description (less than minute): no padding in minutes", 0, minutes);
            assertEquals("RunDuration string formatting does not confirm the interface description (less than minute)", duration % 60, seconds);
        } else {
            fail("RunDuration string formatting does not confirm the interface description (less than minute): does not follow regex");
        }
    }

    @Test
    public void runDuration_moreThanHourToString_formatsCorrectly() {
        int duration = 145879; // more than an hour
        RunDuration runDuration = new RunDuration();
        for (int i = 0; i < duration; i++) {
            runDuration.addSecond();
        }
        String shortPattern = "(\\d+):(\\d+):(\\d+)";
        Pattern regexPattern = Pattern.compile(shortPattern);
        Matcher matcher = regexPattern.matcher(runDuration.toString());
        if (matcher.find()) {
            int hours = Integer.parseInt(matcher.group(1));
            int minutes = Integer.parseInt(matcher.group(2));
            int seconds = Integer.parseInt(matcher.group(3));
            assertEquals("RunDuration string formatting does not confirm the interface description (more than hour)", duration / 3600, hours);
            assertEquals("RunDuration string formatting does not confirm the interface description (more than hour)", (duration / 60) % 60, minutes);
            assertEquals("RunDuration string formatting does not confirm the interface description (more than hour)", duration % 60, seconds);
        } else {
            fail("RunDuration string formatting does not confirm the interface description (more than hour): does not follow regex");
        }
    }

    @Test
    public void runDuration_moreThanHourLessThan10MinLessThan10Sec_formatsCorrectly() {
        int duration = 3722; // more than an hour, with minutes < 10 and seconds < 10
        RunDuration runDuration = new RunDuration();
        for (int i = 0; i < duration; i++) {
            runDuration.addSecond();
        }
        String shortPattern = "(\\d+):(\\d+):(\\d+)";
        Pattern regexPattern = Pattern.compile(shortPattern);
        Matcher matcher = regexPattern.matcher(runDuration.toString());
        if (matcher.find()) {
            int hours = Integer.parseInt(matcher.group(1));
            int minutes = Integer.parseInt(matcher.group(2));
            int seconds = Integer.parseInt(matcher.group(3));
            assertEquals("RunDuration string formatting does not confirm the interface description (more than hour)", duration / 3600, hours);
            assertEquals("RunDuration string formatting does not confirm the interface description (more than hour)", (duration / 60) % 60, minutes);
            assertEquals("RunDuration string formatting does not confirm the interface description (more than hour)", duration % 60, seconds);
        } else {
            fail("RunDuration string formatting does not confirm the interface description (more than hour): does not follow regex");
        }
    }

    @Test
    public void runDuration_add_addsCorrectly(){
        int duration1 = 3676;
        int duration2 = 976;

        RunDuration runDuration1 = new RunDuration(duration1);
        RunDuration runDuration2 = new RunDuration(duration2);
        runDuration1.add(runDuration2);

        assertEquals("RunDistance add does not add two runDistances correctly", duration1+duration2, runDuration1.getSecondsPassed());
    }

    @Test
    public void runDuration_toAudioString_formatsCorrectly(){
        RunDuration runDuration1 = new RunDuration(61);
        String audioString1 = "1 minute and 1 second.";

        RunDuration runDuration2 = new RunDuration(3600);
        String audioString2 = "1 hour, 0 minutes and 0 seconds.";

        RunDuration runDuration3 = new RunDuration(7261);
        String audioString3 = "2 hours, 1 minute and 1 second.";

        assertEquals(audioString1, runDuration1.toAudioString());
        assertEquals(audioString2, runDuration2.toAudioString());
        assertEquals(audioString3, runDuration3.toAudioString());
    }
}
