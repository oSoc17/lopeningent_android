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

package com.dp16.runamicghent.statTrackerTests;

import android.preference.PreferenceManager;

import com.dp16.runamicghent.Persistence.EventBasedPersistence;
import com.dp16.runamicghent.StatTracker.AggregateRunningStatistics;
import com.dp16.runamicghent.StatTracker.AggregateRunningStatisticsHandler;
import com.dp16.runamicghent.StatTracker.RunningStatistics;
import com.dp16.eventbroker.EventBroker;
import com.google.gson.Gson;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/**
 * Unit tests for StatTracker.AggregateRunningStatisticsHandler
 * Created by hendrikdepauw on 31/03/2017.
 */

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class AggregateRunningStatisticsHandlerTest {
    //Use these JSONs as RunningStatistics data
    private String runningStatisticsString = "{\"distance\":[{\"time\":3009,\"value\":{\"distance\":10}},{\"time\":5267,\"value\":{\"distance\":15}},{\"time\":7519,\"value\":{\"distance\":23}},{\"time\":12319,\"value\":{\"distance\":38}},{\"time\":14291,\"value\":{\"distance\":44}},{\"time\":16588,\"value\":{\"distance\":54}},{\"time\":18221,\"value\":{\"distance\":59}}],\"heartrate\":[{\"time\":768,\"value\":{\"heartRate\":91}},{\"time\":3009,\"value\":{\"heartRate\":93}},{\"time\":5270,\"value\":{\"heartRate\":97}},{\"time\":7521,\"value\":{\"heartRate\":112}},{\"time\":7990,\"value\":{\"heartRate\":125}},{\"time\":8781,\"value\":{\"heartRate\":128}},{\"time\":9675,\"value\":{\"heartRate\":139}},{\"time\":12319,\"value\":{\"heartRate\":138}},{\"time\":13187,\"value\":{\"heartRate\":158}},{\"time\":14292,\"value\":{\"heartRate\":122}},{\"time\":14493,\"value\":{\"heartRate\":128}},{\"time\":16589,\"value\":{\"heartRate\":137}},{\"time\":18225,\"value\":{\"heartRate\":126}},{\"time\":19888,\"value\":{\"heartRate\":112}}],\"location\":[{\"time\":2,\"value\":{\"latitude\":51.05642681027887,\"longtitude\":3.7218255814001924}},{\"time\":756,\"value\":{\"latitude\":51.05638513176279,\"longtitude\":3.7218319105997235}},{\"time\":3007,\"value\":{\"latitude\":51.05630621803996,\"longtitude\":3.7219117446153462}},{\"time\":5267,\"value\":{\"latitude\":51.05627496628596,\"longtitude\":3.72196853414289}},{\"time\":7519,\"value\":{\"latitude\":51.05621738382048,\"longtitude\":3.722021651262107}},{\"time\":7987,\"value\":{\"latitude\":51.05620800241747,\"longtitude\":3.7220463702997733}},{\"time\":8779,\"value\":{\"latitude\":51.05618126294101,\"longtitude\":3.7220582294207776}},{\"time\":9674,\"value\":{\"latitude\":51.0561863670908,\"longtitude\":3.722109439916077}},{\"time\":12316,\"value\":{\"latitude\":51.05609879076006,\"longtitude\":3.7221437596685796}},{\"time\":13183,\"value\":{\"latitude\":51.05608627850543,\"longtitude\":3.7222034936537614}},{\"time\":14290,\"value\":{\"latitude\":51.056070244598914,\"longtitude\":3.7222140868880618}},{\"time\":14491,\"value\":{\"latitude\":51.05606307859757,\"longtitude\":3.7222132730638604}},{\"time\":16588,\"value\":{\"latitude\":51.05599088210567,\"longtitude\":3.7222753259710584}},{\"time\":18220,\"value\":{\"latitude\":51.05595354948207,\"longtitude\":3.7223201546080054}},{\"time\":19885,\"value\":{\"latitude\":51.05592054695048,\"longtitude\":3.722327303889097}}],\"rating\":4.0,\"runDuration\":{\"secondsPassed\":20},\"speed\":[{\"time\":759,\"value\":{\"speed\":0.0}},{\"time\":3008,\"value\":{\"speed\":1.846862030029297}},{\"time\":5268,\"value\":{\"speed\":2.1035044270177043}},{\"time\":7520,\"value\":{\"speed\":2.744257572249239}},{\"time\":7989,\"value\":{\"speed\":3.613164665264711}},{\"time\":8780,\"value\":{\"speed\":3.7704154673501304}},{\"time\":9675,\"value\":{\"speed\":3.9326338759972264}},{\"time\":12318,\"value\":{\"speed\":3.8645708680462647}},{\"time\":13186,\"value\":{\"speed\":4.540272539298878}},{\"time\":14291,\"value\":{\"speed\":3.0070791545996993}},{\"time\":14492,\"value\":{\"speed\":3.54972862179513}},{\"time\":16588,\"value\":{\"speed\":3.9943046966466516}},{\"time\":18224,\"value\":{\"speed\":3.559263370468459}},{\"time\":19886,\"value\":{\"speed\":2.8246932839269014}}],\"startTime\":1490964924481}";
    private RunningStatistics runningStatistics;

    private EventBasedPersistence persistence;

    @Before
    public void init(){
        Gson gson = new Gson();
        runningStatistics = gson.fromJson(runningStatisticsString, RunningStatistics.class);

        EventBroker.getInstance().start();

        PreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.application).edit().putString("client token", "mytoken").commit();
        persistence = new EventBasedPersistence(RuntimeEnvironment.application);
        persistence.getController().getServerStorage().setDatabaseToUse("DRIG_unittest");
        persistence.start();
    }

    @Test
    public void aggregateRunningStatisticsHandler_getAggregateRunningStatistics_nothingAdded(){
        //AggregateRunningStatistics is loaded for the first time thus should be all zeros
        AggregateRunningStatisticsHandler aggregateRunningStatisticsHandler = new AggregateRunningStatisticsHandler();

        AggregateRunningStatistics aggregateRunningStatistics = aggregateRunningStatisticsHandler.getAggregateRunningStatistics();

        Assert.assertEquals("AggregateRunningStatisticsHandler does not read/initialize total distance correctly", 0, aggregateRunningStatistics.getTotalDistance().getDistance());
        Assert.assertEquals("AggregateRunningStatisticsHandler does not read/initialize average distance correctly", 0, aggregateRunningStatistics.getAverageDistance().getDistance());
        Assert.assertEquals("AggregateRunningStatisticsHandler does not read/initialize total duration correctly", 0, aggregateRunningStatistics.getTotalDuration().getSecondsPassed());
        Assert.assertEquals("AggregateRunningStatisticsHandler does not read/initialize average duration correctly", 0, aggregateRunningStatistics.getAverageDuration().getSecondsPassed());
        Assert.assertEquals("AggregateRunningStatisticsHandler does not read/initialize average heart rate correctly", 0, aggregateRunningStatistics.getAverageHeartRate().getHeartRate());
        Assert.assertEquals("AggregateRunningStatisticsHandler does not read/initialize average speed correctly", 0.0, aggregateRunningStatistics.getAverageRunSpeed().getSpeed());
        Assert.assertEquals("AggregateRunningStatisticsHandler does not read/initialize number of runs correctly", 0, aggregateRunningStatistics.getNumberOfRuns());
    }

    @Test
    public void aggregateRunningStatisticsHandler_getAggregateRunningStatistics_correctlyLoaded() {
        //AggregateRunningStatistics is loaded for the first time thus should be all zeros
        AggregateRunningStatisticsHandler aggregateRunningStatisticsHandler1 = new AggregateRunningStatisticsHandler();
        aggregateRunningStatisticsHandler1.addRunningStatistics(runningStatistics);

        //Have to wait for the first handler to correctly save the AggregateRunningStatistics
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        AggregateRunningStatisticsHandler aggregateRunningStatisticsHandler2 = new AggregateRunningStatisticsHandler();
        AggregateRunningStatistics aggregateRunningStatistics = aggregateRunningStatisticsHandler2.getAggregateRunningStatistics();

        Assert.assertEquals("AggregateRunningStatisticsHandler does not load/save first total distance correctly", runningStatistics.getTotalDistance().getDistance(), aggregateRunningStatistics.getTotalDistance().getDistance());
        Assert.assertEquals("AggregateRunningStatisticsHandler does not load/save first average distance correctly", runningStatistics.getTotalDistance().getDistance(), aggregateRunningStatistics.getAverageDistance().getDistance());
        Assert.assertEquals("AggregateRunningStatisticsHandler does not load/save first total duration correctly", runningStatistics.getRunDuration().getSecondsPassed(), aggregateRunningStatistics.getTotalDuration().getSecondsPassed());
        Assert.assertEquals("AggregateRunningStatisticsHandler does not load/save first average duration correctly", runningStatistics.getRunDuration().getSecondsPassed(), aggregateRunningStatistics.getAverageDuration().getSecondsPassed());
        Assert.assertEquals("AggregateRunningStatisticsHandler does not load/save first average heart rate correctly", runningStatistics.getAverageHeartRate().getHeartRate(), aggregateRunningStatistics.getAverageHeartRate().getHeartRate());
        Assert.assertEquals("AggregateRunningStatisticsHandler does not load/save first average speed correctly", runningStatistics.getAverageSpeed().getSpeed(), aggregateRunningStatistics.getAverageRunSpeed().getSpeed());
        Assert.assertEquals("AggregateRunningStatisticsHandler does not hanload/savedle first number of runs correctly", 1, aggregateRunningStatistics.getNumberOfRuns());
    }
}
