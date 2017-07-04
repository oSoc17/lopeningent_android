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

package com.dp16.runamicghent.util;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.fail;

/**
 * Helper class for some common threading problems encountered in unit tests.
 * Created by Nick on 28-3-2017.
 */

public class ThreadingUtils {

    /**
     * Waits until an atomic variable has a certain value or fails with the message "-failMessage- -timeout- ms"  after a timeout.
     *
     * @param waitTimePerIteration How long the thread should sleep each waiting iteration (in ms)
     * @param maxIterations        How many waiting iterations there should be
     * @param failMessage          Message to pass to junit.fail() upon timeout
     * @param atomic               Variable that must be waited upon
     * @param expectedValue        Value above variable should be.
     */
    public static void waitUntilAtomicVariableReachesValue(int waitTimePerIteration, int maxIterations, String failMessage, AtomicInteger atomic, int expectedValue) {
        int i = 0;
        while (true) {
            if (i > maxIterations) {
                fail(failMessage + waitTimePerIteration * maxIterations + " ms");
            }
            if (atomic.get() == expectedValue) {
                return;
            }
            try {
                Thread.sleep(waitTimePerIteration);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            i++;
        }
    }

    /**
     * Waits one second until an atomic variable has a certain value or fails with the message "-failMessage- 1000 ms"  after a timeout.
     * <p>
     * Uses {@link #waitUntilAtomicVariableReachesValue(int, int, String, AtomicInteger, int)} internally with the first two arguments to 25 and 40.
     *
     * @param failMessage   Message to pass to junit.fail() upon timeout
     * @param atomic        Variable that must be waited upon
     * @param expectedValue Value above variable should be.
     */
    public static void waitOneSecUntilAtomicVariableReachesValue(String failMessage, AtomicInteger atomic, int expectedValue) {
        waitUntilAtomicVariableReachesValue(25, 40, failMessage, atomic, expectedValue);
    }
}
