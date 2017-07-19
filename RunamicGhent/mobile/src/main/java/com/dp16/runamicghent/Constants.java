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

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.Objects;

/**
 * Class containing public static final constants used throughout the application.
 * All classes have private constructor, because these classes only contain values.
 * Created by hendrikdepauw on 01/03/2017.
 */

public class Constants {

    // Is this a debug environment?
    public static final Boolean DEVELOP = true;

    private static final String UTILITY_CLASS_ERROR = "Utility Class";

    /**
     * Server configuration constants.
     */
    public class Server {
        public static final String ADDRESS = "groep16.cammaert.me";
        public static final String MONGOPORT = "27017";

        private Server() {
            throw new IllegalAccessError(UTILITY_CLASS_ERROR);
        }
    }

    /**
     * Smoothing toggle.
     */
    public class Smoothing {

        public static final boolean SMOOTHING_ENABLED = true;

        private Smoothing() {
            throw new IllegalAccessError(UTILITY_CLASS_ERROR);
        }
    }

    /**
     * Constants for setting keys
     */
    public class SettingTypes {
        public static final String PREF_KEY_DEBUG_LOCATION_MOCK = "pref_key_debug_location_mock";
        public static final String PREF_KEY_DEBUG_FAKE_LATLNG_LOCATIONPROVIDER = "pref_key_debug_fake_latlng_locationprovider";
        public static final String PREF_KEY_DEBUG_ROUTE_SMOOTHER = "pref_key_debug_route_smoother";
        public static final String PREF_KEY_ROUTING_PARK = "pref_key_dynamic_park";
        public static final String PREF_KEY_ROUTING_WATER = "pref_key_dynamic_water";

        public static final String PREF_KEY_AUDIO_DIRECTIONS = "pref_key_audio_directions";
        public static final String PREF_KEY_AUDIO_CUSTOM_SOUND = "pref_key_audio_custom_sound";
        public static final String PREF_KEY_AUDIO_LEFT_RIGHT_CHANNEL = "pref_key_audio_left_right_channel";

        public static final String USER_WENT_THROUGH_TOURGUIDE_START = "user_went_through_tourguide_start";
        public static final String USER_WENT_THROUGH_TOURGUIDE_HISTORY = "user_went_through_tourguide_history";
        public static final String USER_WENT_THROUGH_TOURGUIDE_SETTINGS = "user_went_through_tourguide_settings";

        private SettingTypes() {
            throw new IllegalAccessError(UTILITY_CLASS_ERROR);
        }
    }

    /**
     * Constants for all types of events that are processed by EventBroker.
     */
    public class EventTypes {
        public static final String LOCATION = "LOCATION";
        public static final String LOCATION_ACCURATE = "LOCATION_ACCURATE";
        public static final String RAW_LOCATION = "RAW_LOCATION";
        public static final String SPEED = "SPEED";
        public static final String TRACK = "TRACK";
        public static final String TRACK_REQUEST = "TRACK_REQUEST";
        public static final String TRACK_LOADED = "TRACK_LOADED";
        public static final String RATING = "RATING";
        public static final String DISTANCE = "DISTANCE";
        public static final String DURATION = "DURATION";
        public static final String IS_IN_CITY = "IS_IN_CITY";
        public static final String NOT_IN_CITY = "NOT_IN_CITY";
        public static final String STATUS_CODE = "STATUS_CODE";
        public static final String IN_CITY = "IN_CITY";

        // Dynamic routing
        public static final String OFFROUTE = "OFFROUTE";
        public static final String ABNORMAL_HEART_RATE = "ABNORMAL_HEART_RATE";

        //Storage
        public static final String STORE_RUNNINGSTATISTICS = "STORE_RUNNINGSTATISTICS";
        public static final String LOAD_RUNNINGSTATISTICS = "LOAD_RUNNINGSTATISTICS";
        public static final String LOADED_RUNNINGSTATISTICS = "LOADED_RUNNINGSTATISTICS";
        public static final String DELETE_RUNNINGSTATISTICS = "DELETE_RUNNINGSTATISTICS";

        public static final String STORE_AGGREGATESTATISTICS = "STORE_AGGREGATESTATISTICS";
        public static final String LOAD_AGGREGATESTATISTICS = "LOAD_AGGREGATESTATISTICS";
        public static final String LOADED_AGGREGATESTATISTICS = "LOADED_AGGREGATESTATISTICS";
        public static final String DELETE_AGGREGATESTATISTICS = "DELETE_AGGREGATESTATISTICS";

        public static final String SYNC_WITH_DATABASE = "SYNC_WITH_DATABASE";

        //Navigation
        public static final String NAVIGATION_DIRECTION = "NAVIGATION_DIRECTION";
        public static final String SPLIT_POINT = "SPLIT_POINT";
        public static final String AUDIO = "AUDIO";

        //android wear event types
        public static final String HEART_RESPONSE = "HEART_RESPONSE";
        public static final String START_WEAR = "START_WEAR";
        public static final String STOP_WEAR = "STOP_WEAR";
        public static final String PAUSE_WEAR = "PAUSE_WEAR";

        private EventTypes() {
            throw new IllegalAccessError(UTILITY_CLASS_ERROR);
        }

    }

    /**
     * Constants for communication with android wear device
     */
    public class WearMessageTypes {

        //from wear to mobile
        public static final String HEART_RATE_MESSAGE_WEAR = "/heartRateMessageWear";
        public static final String REQUEST_STATE_MESSAGE_WEAR = "/requestStateMessageWear";

        //from mobile to wear
        public static final String START_RUN_MOBILE = "/startRun";
        public static final String STOP_RUN_MOBILE = "/stopRun";
        public static final String PAUSE_RUN_MOBILE = "/pauseRun";

        //navigation constants
        public static final String NAVIGATE_LEFT = "/navigateLeft";
        public static final String NAVIGATE_RIGHT = "/navigateRight";
        public static final String NAVIGATE_STRAIGHT = "/navigateStraight";
        public static final String NAVIGATE_UTURN = "/navigateUTurn";

        public static final String RUN_STATE_START_MESSAGE_MOBILE = "/runStateStart";
        public static final String RUN_STATE_PAUSED_MESSAGE_MOBILE = "/runStateStop";
        public static final String TIME_UPDATE_MESSAGE_MOBILE = "/timeUpdate";
        public static final String SPEED_UPDATE_MESSAGE_MOBILE = "/speedUpdate";
        public static final String DISTANCE_UPDATE_MESSAGE_MOBILE = "/distanceUpdate";

        private WearMessageTypes() {
            throw new IllegalAccessError(UTILITY_CLASS_ERROR);
        }
    }

    /**
     * Constants for activities (for GuiController)
     */
    public class ActivityTypes {

        public static final String MAINMENU = "MAINMENU";
        public static final String RUNNINGVIEW = "RUNNINGVIEW";
        public static final String RESTTEST = "RESTTEST";
        public static final String DEBUG = "DEVELOP";
        public static final String HISTORYEXPANDED = "HISTORYEXPANDED";
        public static final String LOGIN = "LOGIN";
        public static final String SETTINGS = "SETTINGS";
        public static final String CHANGEPROFILE = "CHANGEPROFILE";
        public static final String LICENCES = "LICENCES";

        private ActivityTypes() {
            throw new IllegalAccessError(UTILITY_CLASS_ERROR);
        }

    }

    /**
     * Constants used by {@link RouteChecker}.
     */
    public class RouteChecker {

        // Interval between onRouteChecker messages
        public static final int INTERVAL = 5000;

        // Accuracy for onRouteChecker
        public static final int ACCURACY = 40;

        private RouteChecker() {
            throw new IllegalAccessError(UTILITY_CLASS_ERROR);
        }
    }

    /**
     * Constants for displaying Google Maps.
     */
    public class MapSettings {

        // Constant used in the location settings dialog.
        public static final int REQUEST_CHECK_SETTINGS = 0x1;

        // The desired interval for location updates. Inexact. Updates may be more or less frequent.
        public static final long UPDATE_INTERVAL = 2000;

        // The fastest rate for active location updates. Exact. Updates will never be more frequent than this value.
        public static final long FASTEST_UPDATE_INTERVAL = 1000;

        // Smallest displacement needed in meters before new location update is received
        public static final float SMALLEST_DISPLACEMENT = 2.0f;

        // Max Zoom level for google map
        public static final float MAX_ZOOM = 19.0f;

        // Min zoom level for google map
        public static final float MIN_ZOOM = 10.0f;

        // Desired zoom level for google map
        public static final float DESIRED_ZOOM = 17.0f;
        // Maximum desired zoom
        public static final float MAX_DESIRED_ZOOM = 22.0f;

        // Does Google RunningMap need compass
        public static final boolean COMPASS = true;

        // Can Google Maps be tilted
        public static final boolean TILT = false;

        // Padding to be added (in dp) to the bounding box of the displayed route
        public static final int ROUTE_PADDING = 20;

        // Padding to be added (in dp) to left and right side of map
        public static final int SIDE_MAP_PADDING = 8;

        // Number of samples for the rolling average
        public static final int ROLLING_SIZE = 4;

        // Discount factor is used for rotating map. It determines how much influence the newly calculated bearing has.
        public static final float DISCOUNT_FACTOR = 0.5f;

        public static final String STATIC_MAPS_KEY = "AIzaSyAl3P4q7jc4il8jVmGGZk6f-BwsNeC_xyc";

        private MapSettings() {
            throw new IllegalAccessError(UTILITY_CLASS_ERROR);
        }
    }

    /**
     * Constant strings, used for persistence.
     */
    public class Storage {

        public static final String RUNNINGSTATISTICSDIRECTORY = "runningstatistics";
        public static final String RUNNINGSTATISTICSGHOSTDIRECTORY = "runningstatisticsghost";
        public static final String AGGREGATERUNNINGSTATISTICSDIRECTORY = "aggregaterunningstatistics";

        public static final String RUNNINGSTATISTICSCOLLECTION = "runningStatistics";
        public static final String RUNNINGSTATISTICSGHOSTCOLLECTION = "runningStatisticsGhost";
        public static final String AGGREGATERUNNINGSTATISTICSCOLLECTION = "aggregateRunningStatistics";

        public static final int MINUTES_BETWEEN_SERVER_SYNC_TRIES = 15;
        public static final String MONGODBNAME = "DRIG_userdata";

        public static final String MONGOUSER = "client";
        public static final String MONGOPASS = "dynamicrunninginghentSmart";

        // Logger name found by decompiling the mongodb java driver jar
        // Consider this unstable. That is no problem as we would only get a lot of logging messages if this would break.
        public static final String LOGGERNAME = "org.mongodb.driver.cluster";
        public static final boolean TURNOFFLOGGING = true;

        private Storage() {
            throw new IllegalAccessError(UTILITY_CLASS_ERROR);
        }
    }

    /**
     * All cities the app supports.
     */
    public static class Cities {

        public static final String GHENT = "GHENT";
        protected static final String[] CITY_LIST = {GHENT};

        private Cities() {
            throw new IllegalAccessError("Utility class");
        }

        public static boolean cityIsKnown(String city) {
            for (String city1 : CITY_LIST) {
                if (Objects.equals(city1, city))
                    return true;
            }
            return false;
        }
    }

    /**
     * Bounding boxes containing Ghent.
     * Used by {@link com.dp16.runamicghent.DataProvider.InCityChecker}
     */
    public static class CityBoundingBoxes {

        //Coordinate "box" containing the city of Ghent.
        public static final LatLngBounds latLngBoundGhent = new LatLngBounds(new LatLng(50.8077000, 3.0350000), new LatLng(51.3014000, 4.1858000));

        private CityBoundingBoxes() {
            throw new IllegalAccessError("Utility class");
        }
    }

    /**
     * Constants for location updates
     */
    public static class Location {
        // Constants used to check if the location updates are accurate/inaccurate
        public static final float MAX_GOOD_ACCURACY = 10.0f;
        public static final float MIN_BAD_ACCURACY = 14.0f;
        public static final int COUNTER_MAX = 3;

        private Location() {
            throw new IllegalAccessError(UTILITY_CLASS_ERROR);
        }
    }

    /**
     * Constants used for route generation.
     */
    public static class RouteGenerator {

        public static final double MINIMAL_BOUND = 0.5;
        public static final double MAXIMAL_BOUND = 1.0;
        public static final double FRACTION_BOUND = 0.1;

        public static final double FIRST_BORDER = MINIMAL_BOUND / FRACTION_BOUND;
        public static final double SECOND_BORDER = MAXIMAL_BOUND / FRACTION_BOUND;

        public static final int MIN_LENGTH = 1;
        public static final int MAX_LENGTH = 50;
        public static final int DEFAULT_LENGTH = 5;

        public static final double BEGINNER_SPEED = 8;
        public static final double AVERAGE_SPEED = 10;
        public static final double EXPERT_SPEED = 12;

        public static final double AVERAGE_SPEED_DIFFERENCE = 1.5;

        private RouteGenerator() {
            throw new IllegalAccessError(UTILITY_CLASS_ERROR);
        }
    }

    /**
     * Constants for dynamic routing
     */
    public static class DynamicRouting {

        // Min and Max values for rangebar
        public static final int RANGEBAR_LOW = 60;
        public static final int RANGEBAR_HIGH = 210;

        // Standard upper and lower heart rate limit
        public static final int HEART_RATE_LOWER = 130;
        public static final int HEART_RATE_UPPER = 170;

        // Min and Max values for slider
        public static final int SLIDER_LOW = 0;
        public static final int SLIDER_HIGH = 100;

        // Default slider position
        public static final int DEFAULT_SLIDER = 20;

        // Number of samples for the rolling average
        public static final int ROLLING_SIZE = 4;

        // Minimal time that rolling average of heart rate needs to be under lower limit or above upper limit
        public static final long HEART_RATE_MIN_TIME = 1000 * 60L;

        // Time to wait after a Abnormal heart rate event has been set
        public static final long HEART_RATE_WAIT_TIME = 5 * 1000 * 60L;

        public static final String TAG_LOWER = "LOWER";
        public static final String TAG_UPPER = "UPPER";

        private DynamicRouting() {
            throw new IllegalAccessError(UTILITY_CLASS_ERROR);
        }

    }

}
