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

import com.dp16.runamicghent.GuiController.GuiController;
import com.dp16.runamicghent.R;

/**
 * This class can contain a direction and is used for navigation purposes.
 * Its purpose is to be stored in a RunRoutePoint object.
 * Extra type of directions can be added in the enum. Please also adjust
 * the switch statement in convertStringToDirection to include the new Direction.
 * Created by hendrikdepauw on 31/03/2017.
 */

public class RunDirection {
    public enum Direction {NONE, FORWARD, LEFT, RIGHT, UTURN}

    private Direction direction;

    public RunDirection(Direction direction) {
        setDirection(direction);
    }

    public RunDirection(String direction) {
        setDirection(direction);
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public void setDirection(String direction) {
        this.direction = convertStringToDirection(direction);
    }

    public Direction getDirection() {
        return direction;
    }

    /**
     * Checks if two RunDirection objects are the same. Mostly used in testing.
     *
     * @param other Object to compare this object to.
     * @return True if other object is the same (value wise), false otherwise.
     */
    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (other == this) {
            return true;
        }
        if (!(other instanceof RunDirection)) {
            return false;
        }

        // Other object is a RunDirection and is not null.
        RunDirection otherRunDirection = (RunDirection) other;
        return this.getDirection().equals(otherRunDirection.getDirection());
    }

    /**
     * Fix for SonarQube
     * According to the Java Language Specification, there is a contract between equals(Object) and hashCode():
     * If two objects are equal according to the equals(Object) method, then calling the hashCode method on each of the two objects must produce the same integer result.
     * It is not required that if two objects are unequal according to the equals(java.lang.Object) method, then calling the hashCode method on each of the two objects must produce distinct integer results.
     */
    @Override
    public int hashCode() {
        return 0;
    }

    /**
     * This method converts a string returned by the server into a Direction.
     *
     * @param direction String returned from the server
     * @return a Direction variable corresponding to the String input
     */
    public static Direction convertStringToDirection(String direction) {
        switch (direction) {
            case "forward":
                return Direction.FORWARD;
            case "left":
                return Direction.LEFT;
            case "right":
                return Direction.RIGHT;
            case "turnaround":
                return Direction.UTURN;
            default:
                return Direction.NONE;
        }
    }

    public String toString(){
        switch (direction) {
            case LEFT:
                return GuiController.getInstance().getContext().getString(R.string.audio_left);
            case RIGHT:
                return GuiController.getInstance().getContext().getString(R.string.audio_right);
            case UTURN:
                return GuiController.getInstance().getContext().getString(R.string.audio_uturn);
            case FORWARD:
                return GuiController.getInstance().getContext().getString(R.string.audio_forward);
            default:
                return "";
        }
    }
}