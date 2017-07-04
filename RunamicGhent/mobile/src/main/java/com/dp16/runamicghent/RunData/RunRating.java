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

/**
 * This class is used to associate a Run with its rating. Run is signified by TAG.
 * Created by lorenzvanherwaarden on 10/04/2017.
 */
public class RunRating {
    private String tag;
    private float rating;

    public RunRating(String tag, float rating) {
        this.tag = tag;
        this.rating = rating;
    }

    public String getTag() {
        return tag;
    }

    public float getRating() {
        return rating;
    }

    public String getStringRating() { return Float.toString(rating); }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public void setRating(Float rating) {
        this.rating = rating;
    }

}
