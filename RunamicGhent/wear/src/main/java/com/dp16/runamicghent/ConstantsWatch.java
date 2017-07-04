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

/**
 * This class contains the constants that are used for event publishing and message paths
 */

class ConstantsWatch {
    private static final String UTILITY_CLASS_ERROR = "Utility Class";

    public class EventTypes {

        //android wear event types: ACTION_REQUESTER
        public static final String START_MOBILE = "START_MOBILE";
        public static final String PAUSE_MOBILE = "PAUSE_MOBILE";
        public static final String STOP_MOBILE = "STOP_MOBILE";

        public static final String NAVIGATE = "NAVIGATE";

        public static final String HEART_RESPONSE = "HEART_RESPONSE";
        public static final String HEART_MEASURE_START = "HEART_MEASURE_START";
        public static final String HEART_MEASURE_STOP = "HEART_MEASURE_STOP";

        public static final String TIME_MOBILE = "TIME_MOBILE";
        public static final String RUN_STATE_START_MOBILE = "RUN_STATE_START_MOBILE";
        public static final String RUN_STATE_PAUSED_MOBILE = "RUN_STATE_PAUSED_MOBILE";
        public static final String REQUEST_STATE_WEAR = "REQUEST_STATE_WEAR";
        public static final String SPEED_MOBILE = "SPEED_MOBILE";
        public static final String DISTANCE_MOBILE = "DISTANCE_MOBILE";
        public static final String ON_STOP = "ON_STOP";


        private EventTypes() {
            throw new IllegalAccessError(UTILITY_CLASS_ERROR);
        }

    }

    /**
     * strings that are used for identifying the message type sent over bluetooth to mobile device
     */
    public class WearMessageTypes {

        //from wear to mobile: ACTION_SENDER
        public static final String HEART_RATE_MESSAGE_WEAR = "/heartRateMessageWear";
        public static final String REQUEST_STATE_MESSAGE_WEAR = "/requestStateMessageWear";

        //from mobile to wear: ACTION_SENDER
        public static final String START_RUN_MOBILE = "/startRun";
        public static final String STOP_RUN_MOBILE = "/stopRun";
        public static final String PAUSE_RUN_MOBILE = "/pauseRun";

        //navigation constants
        public static final String NAVIGATE_LEFT = "/navigateLeft";
        public static final String NAVIGATE_RIGHT = "/navigateRight";
        public static final String NAVIGATE_STRAIGHT = "/navigateStraight";
        public static final String NAVIGATE_UTURN = "/navigateUTurn";

        public static final String RUN_START_STATE_MESSAGE_MOBILE = "/runStateStart";
        public static final String RUN_STATE_PAUSED_MESSAGE_MOBILE = "/runStateStop";
        public static final String TIME_UPDATE_MESSAGE_MOBILE = "/timeUpdate";
        public static final String SPEED_UPDATE_MESSAGE_MOBILE = "/speedUpdate";
        public static final String DISTANCE_UPDATE_MESSAGE_MOBILE = "/distanceUpdate";


        private WearMessageTypes() {
            throw new IllegalAccessError(UTILITY_CLASS_ERROR);
        }
    }
}
