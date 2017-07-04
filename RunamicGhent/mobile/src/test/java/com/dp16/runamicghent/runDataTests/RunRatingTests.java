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

import com.dp16.runamicghent.RunData.RunRating;

import junit.framework.Assert;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * Created by hendrikdepauw on 06/05/2017.
 */

public class RunRatingTests {

    @Test
    public void runRating_getters_returnCorrectly(){
        String tag = "fgchkgvjlbknlm";
        float rating = 4.5465767f;

        RunRating runRating = new RunRating(tag, rating);

        Assert.assertEquals(tag, runRating.getTag());
        Assert.assertEquals(rating, runRating.getRating());
        Assert.assertEquals(Float.toString(rating), runRating.getStringRating());
    }

    @Test
    public void runRating_setters_setCorrectly(){
        String tag1 = "fgchkgvjlbknlm";
        String tag2 = "cr6wuq57ie6gicb";
        float rating1 = 4.5465767f;
        float rating2 = 2.7568734f;

        RunRating runRating = new RunRating(tag1, rating1);
        runRating.setRating(rating2);
        runRating.setTag(tag2);

        Assert.assertEquals(tag2, runRating.getTag());
        Assert.assertEquals(rating2, runRating.getRating());
        Assert.assertEquals(Float.toString(rating2), runRating.getStringRating());
    }

}
