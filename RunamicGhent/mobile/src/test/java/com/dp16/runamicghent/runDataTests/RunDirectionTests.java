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

import com.dp16.runamicghent.RunData.RunDirection;

import junit.framework.Assert;

import org.junit.Test;

/**
 * Created by hendrikdepauw on 02/04/2017.
 */

public class RunDirectionTests {

    @Test
    public void runDirection_constructWithDirection_setsVariablesCorrectly(){
        RunDirection runDirectionNone = new RunDirection(RunDirection.Direction.NONE);
        RunDirection runDirectionRight = new RunDirection(RunDirection.Direction.RIGHT);
        RunDirection runDirectionLeft = new RunDirection(RunDirection.Direction.LEFT);
        RunDirection runDirectionForward = new RunDirection(RunDirection.Direction.FORWARD);
        RunDirection runDirectionUTurn = new RunDirection(RunDirection.Direction.UTURN);

        Assert.assertEquals("RunDirection does not set variable correctly", RunDirection.Direction.NONE, runDirectionNone.getDirection());
        Assert.assertEquals("RunDirection does not set variable correctly", RunDirection.Direction.RIGHT, runDirectionRight.getDirection());
        Assert.assertEquals("RunDirection does not set variable correctly", RunDirection.Direction.LEFT, runDirectionLeft.getDirection());
        Assert.assertEquals("RunDirection does not set variable correctly", RunDirection.Direction.FORWARD, runDirectionForward.getDirection());
        Assert.assertEquals("RunDirection does not set variable correctly", RunDirection.Direction.UTURN, runDirectionUTurn.getDirection());
    }

    @Test
    public void runDirection_constructWithString_setsVariablesCorrectly(){
        RunDirection runDirectionNone = new RunDirection("none");
        RunDirection runDirectionRight = new RunDirection("right");
        RunDirection runDirectionLeft = new RunDirection("left");
        RunDirection runDirectionForward = new RunDirection("forward");
        RunDirection runDirectionUTurn = new RunDirection("turnaround");

        Assert.assertEquals("RunDirection does not convert String to Direction correctly", RunDirection.Direction.NONE, runDirectionNone.getDirection());
        Assert.assertEquals("RunDirection does not convert String to Direction correctly", RunDirection.Direction.RIGHT, runDirectionRight.getDirection());
        Assert.assertEquals("RunDirection does not convert String to Direction correctly", RunDirection.Direction.LEFT, runDirectionLeft.getDirection());
        Assert.assertEquals("RunDirection does not convert String to Direction correctly", RunDirection.Direction.FORWARD, runDirectionForward.getDirection());
        Assert.assertEquals("RunDirection does not convert String to Direction correctly", RunDirection.Direction.UTURN, runDirectionUTurn.getDirection());
    }
}
