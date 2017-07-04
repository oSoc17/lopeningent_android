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

import com.dp16.runamicghent.StatTracker.AggregateRunningStatistics;
import com.dp16.runamicghent.StatTracker.RunningStatistics;
import com.google.gson.Gson;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for StatTracker.AggregateRunningStatistics
 * Created by hendrikdepauw on 30/03/2017.
 */

public class AggregateRunningStatisticsTest {
    //Use these JSONs as RunningStatistics data
    private String runningStatisticsString1 = "{\"distance\":[{\"time\":3028,\"value\":{\"distance\":6}},{\"time\":5289,\"value\":{\"distance\":14}},{\"time\":7543,\"value\":{\"distance\":25}},{\"time\":8799,\"value\":{\"distance\":31}},{\"time\":12339,\"value\":{\"distance\":40}},{\"time\":14306,\"value\":{\"distance\":48}},{\"time\":16610,\"value\":{\"distance\":56}},{\"time\":18232,\"value\":{\"distance\":61}}],\"heartrate\":[{\"time\":783,\"value\":{\"heartRate\":78}},{\"time\":3028,\"value\":{\"heartRate\":91}},{\"time\":5294,\"value\":{\"heartRate\":103}},{\"time\":7544,\"value\":{\"heartRate\":134}},{\"time\":8009,\"value\":{\"heartRate\":129}},{\"time\":8801,\"value\":{\"heartRate\":183}},{\"time\":9698,\"value\":{\"heartRate\":132}},{\"time\":12342,\"value\":{\"heartRate\":133}},{\"time\":13198,\"value\":{\"heartRate\":128}},{\"time\":14306,\"value\":{\"heartRate\":135}},{\"time\":14506,\"value\":{\"heartRate\":254}},{\"time\":16613,\"value\":{\"heartRate\":163}},{\"time\":18236,\"value\":{\"heartRate\":131}},{\"time\":19901,\"value\":{\"heartRate\":116}}],\"location\":[{\"time\":775,\"value\":{\"latitude\":51.05638944012265,\"longtitude\":3.7218474910481056}},{\"time\":3028,\"value\":{\"latitude\":51.056337455805675,\"longtitude\":3.721893725595474}},{\"time\":5286,\"value\":{\"latitude\":51.05630062013317,\"longtitude\":3.721987109014804}},{\"time\":7541,\"value\":{\"latitude\":51.05619917236525,\"longtitude\":3.722020313085547}},{\"time\":8006,\"value\":{\"latitude\":51.05620909171632,\"longtitude\":3.72203532465587}},{\"time\":8799,\"value\":{\"latitude\":51.056171724468754,\"longtitude\":3.722083693546751}},{\"time\":9694,\"value\":{\"latitude\":51.05618671029883,\"longtitude\":3.7221091244644597}},{\"time\":12339,\"value\":{\"latitude\":51.05609888944868,\"longtitude\":3.7221557218588774}},{\"time\":13196,\"value\":{\"latitude\":51.05608488059133,\"longtitude\":3.722190269711971}},{\"time\":14304,\"value\":{\"latitude\":51.056056150610296,\"longtitude\":3.7222374889725813}},{\"time\":14505,\"value\":{\"latitude\":51.05604027360227,\"longtitude\":3.722233066020202}},{\"time\":16609,\"value\":{\"latitude\":51.05598498408969,\"longtitude\":3.7222593332547973}},{\"time\":18231,\"value\":{\"latitude\":51.05595813941475,\"longtitude\":3.7223210186514257}},{\"time\":19899,\"value\":{\"latitude\":51.05592817107715,\"longtitude\":3.7223438305589633}}],\"rating\":4.0,\"runDuration\":{\"secondsPassed\":19},\"speed\":[{\"time\":778,\"value\":{\"speed\":0.0}},{\"time\":3028,\"value\":{\"speed\":1.1760114669799806}},{\"time\":5291,\"value\":{\"speed\":2.3369361200640277}},{\"time\":7542,\"value\":{\"speed\":3.839714971081964}},{\"time\":8008,\"value\":{\"speed\":3.5254721744353}},{\"time\":8800,\"value\":{\"speed\":5.3065760728961395}},{\"time\":9697,\"value\":{\"speed\":3.8935093902112072}},{\"time\":12341,\"value\":{\"speed\":3.902335738177172}},{\"time\":13197,\"value\":{\"speed\":3.5861621171444744}},{\"time\":14305,\"value\":{\"speed\":3.8992996266214206}},{\"time\":14506,\"value\":{\"speed\":6.679144903395869}},{\"time\":16611,\"value\":{\"speed\":4.696364094281486}},{\"time\":18234,\"value\":{\"speed\":3.89216330691006}},{\"time\":19900,\"value\":{\"speed\":2.972401867975003}}],\"startTime\":1490964783497}";
    private String runningStatisticsString2 = "{\"distance\":[{\"time\":3009,\"value\":{\"distance\":10}},{\"time\":5267,\"value\":{\"distance\":15}},{\"time\":7519,\"value\":{\"distance\":23}},{\"time\":12319,\"value\":{\"distance\":38}},{\"time\":14291,\"value\":{\"distance\":44}},{\"time\":16588,\"value\":{\"distance\":54}},{\"time\":18221,\"value\":{\"distance\":59}}],\"heartrate\":[{\"time\":768,\"value\":{\"heartRate\":91}},{\"time\":3009,\"value\":{\"heartRate\":93}},{\"time\":5270,\"value\":{\"heartRate\":97}},{\"time\":7521,\"value\":{\"heartRate\":112}},{\"time\":7990,\"value\":{\"heartRate\":125}},{\"time\":8781,\"value\":{\"heartRate\":128}},{\"time\":9675,\"value\":{\"heartRate\":139}},{\"time\":12319,\"value\":{\"heartRate\":138}},{\"time\":13187,\"value\":{\"heartRate\":158}},{\"time\":14292,\"value\":{\"heartRate\":122}},{\"time\":14493,\"value\":{\"heartRate\":128}},{\"time\":16589,\"value\":{\"heartRate\":137}},{\"time\":18225,\"value\":{\"heartRate\":126}},{\"time\":19888,\"value\":{\"heartRate\":112}}],\"location\":[{\"time\":2,\"value\":{\"latitude\":51.05642681027887,\"longtitude\":3.7218255814001924}},{\"time\":756,\"value\":{\"latitude\":51.05638513176279,\"longtitude\":3.7218319105997235}},{\"time\":3007,\"value\":{\"latitude\":51.05630621803996,\"longtitude\":3.7219117446153462}},{\"time\":5267,\"value\":{\"latitude\":51.05627496628596,\"longtitude\":3.72196853414289}},{\"time\":7519,\"value\":{\"latitude\":51.05621738382048,\"longtitude\":3.722021651262107}},{\"time\":7987,\"value\":{\"latitude\":51.05620800241747,\"longtitude\":3.7220463702997733}},{\"time\":8779,\"value\":{\"latitude\":51.05618126294101,\"longtitude\":3.7220582294207776}},{\"time\":9674,\"value\":{\"latitude\":51.0561863670908,\"longtitude\":3.722109439916077}},{\"time\":12316,\"value\":{\"latitude\":51.05609879076006,\"longtitude\":3.7221437596685796}},{\"time\":13183,\"value\":{\"latitude\":51.05608627850543,\"longtitude\":3.7222034936537614}},{\"time\":14290,\"value\":{\"latitude\":51.056070244598914,\"longtitude\":3.7222140868880618}},{\"time\":14491,\"value\":{\"latitude\":51.05606307859757,\"longtitude\":3.7222132730638604}},{\"time\":16588,\"value\":{\"latitude\":51.05599088210567,\"longtitude\":3.7222753259710584}},{\"time\":18220,\"value\":{\"latitude\":51.05595354948207,\"longtitude\":3.7223201546080054}},{\"time\":19885,\"value\":{\"latitude\":51.05592054695048,\"longtitude\":3.722327303889097}}],\"rating\":4.0,\"runDuration\":{\"secondsPassed\":20},\"speed\":[{\"time\":759,\"value\":{\"speed\":0.0}},{\"time\":3008,\"value\":{\"speed\":1.846862030029297}},{\"time\":5268,\"value\":{\"speed\":2.1035044270177043}},{\"time\":7520,\"value\":{\"speed\":2.744257572249239}},{\"time\":7989,\"value\":{\"speed\":3.613164665264711}},{\"time\":8780,\"value\":{\"speed\":3.7704154673501304}},{\"time\":9675,\"value\":{\"speed\":3.9326338759972264}},{\"time\":12318,\"value\":{\"speed\":3.8645708680462647}},{\"time\":13186,\"value\":{\"speed\":4.540272539298878}},{\"time\":14291,\"value\":{\"speed\":3.0070791545996993}},{\"time\":14492,\"value\":{\"speed\":3.54972862179513}},{\"time\":16588,\"value\":{\"speed\":3.9943046966466516}},{\"time\":18224,\"value\":{\"speed\":3.559263370468459}},{\"time\":19886,\"value\":{\"speed\":2.8246932839269014}}],\"startTime\":1490964924481}";
    private RunningStatistics runningStatistics1;
    private RunningStatistics runningStatistics2;

    @Before
    public void Init(){
        Gson gson = new Gson();

        runningStatistics1 = gson.fromJson(runningStatisticsString1, RunningStatistics.class);
        runningStatistics2 = gson.fromJson(runningStatisticsString2, RunningStatistics.class);
    }

    @Test
    public void aggregateRunningStatistics_getters_nothingAdded(){
        AggregateRunningStatistics aggregateRunningStatistics = new AggregateRunningStatistics();

        Assert.assertEquals("AggregateRunningStatistics does not read/initialize total distance correctly", 0, aggregateRunningStatistics.getTotalDistance().getDistance());
        Assert.assertEquals("AggregateRunningStatistics does not read/initialize average distance correctly", 0, aggregateRunningStatistics.getAverageDistance().getDistance());
        Assert.assertEquals("AggregateRunningStatistics does not read/initialize total duration correctly", 0, aggregateRunningStatistics.getTotalDuration().getSecondsPassed());
        Assert.assertEquals("AggregateRunningStatistics does not read/initialize average duration correctly", 0, aggregateRunningStatistics.getAverageDuration().getSecondsPassed());
        Assert.assertEquals("AggregateRunningStatistics does not read/initialize average heart rate correctly", 0, aggregateRunningStatistics.getAverageHeartRate().getHeartRate());
        Assert.assertEquals("AggregateRunningStatistics does not read/initialize average speed correctly", 0.0, aggregateRunningStatistics.getAverageRunSpeed().getSpeed());
        Assert.assertEquals("AggregateRunningStatistics does not read/initialize number of runs correctly", 0, aggregateRunningStatistics.getNumberOfRuns());
    }

    @Test
    public void aggregateRunningStatistics_handleRunningStatistics_firstRunningStatistics(){
        AggregateRunningStatistics aggregateRunningStatistics = new AggregateRunningStatistics();
        aggregateRunningStatistics.handleRunningStatistics(runningStatistics1);

        Assert.assertEquals("AggregateRunningStatistics does not handle first total distance correctly", runningStatistics1.getTotalDistance().getDistance(), aggregateRunningStatistics.getTotalDistance().getDistance());
        Assert.assertEquals("AggregateRunningStatistics does not handle first average distance correctly", runningStatistics1.getTotalDistance().getDistance(), aggregateRunningStatistics.getAverageDistance().getDistance());
        Assert.assertEquals("AggregateRunningStatistics does not handle first total duration correctly", runningStatistics1.getRunDuration().getSecondsPassed(), aggregateRunningStatistics.getTotalDuration().getSecondsPassed());
        Assert.assertEquals("AggregateRunningStatistics does not handle first average duration correctly", runningStatistics1.getRunDuration().getSecondsPassed(), aggregateRunningStatistics.getAverageDuration().getSecondsPassed());
        Assert.assertEquals("AggregateRunningStatistics does not handle first average heart rate correctly", runningStatistics1.getAverageHeartRate().getHeartRate(), aggregateRunningStatistics.getAverageHeartRate().getHeartRate());
        Assert.assertEquals("AggregateRunningStatistics does not handle first average speed correctly", runningStatistics1.getAverageSpeed().getSpeed(), aggregateRunningStatistics.getAverageRunSpeed().getSpeed());
        Assert.assertEquals("AggregateRunningStatistics does not handle first number of runs correctly", 1, aggregateRunningStatistics.getNumberOfRuns());
    }

    @Test
    public void aggregateRunningStatistics_handleRunningStatistics_secondRunningStatistics(){
        AggregateRunningStatistics aggregateRunningStatistics = new AggregateRunningStatistics();
        aggregateRunningStatistics.handleRunningStatistics(runningStatistics1);
        aggregateRunningStatistics.handleRunningStatistics(runningStatistics2);

        int totalDistance = runningStatistics1.getTotalDistance().getDistance() + runningStatistics2.getTotalDistance().getDistance();
        int averageDistance = totalDistance / 2;
        int totalDuration = runningStatistics1.getRunDuration().getSecondsPassed() + runningStatistics2.getRunDuration().getSecondsPassed();
        int averageDuration = totalDuration / 2;
        int averageHeartRate = (runningStatistics1.getAverageHeartRate().getHeartRate() + runningStatistics2.getAverageHeartRate().getHeartRate()) / 2;
        double averageSpeed = (runningStatistics1.getAverageSpeed().getSpeed() + runningStatistics2.getAverageSpeed().getSpeed()) / 2;

        Assert.assertEquals("AggregateRunningStatistics does not handle second total distance correctly", totalDistance, aggregateRunningStatistics.getTotalDistance().getDistance());
        Assert.assertEquals("AggregateRunningStatistics does not handle second average distance correctly", averageDistance, aggregateRunningStatistics.getAverageDistance().getDistance());
        Assert.assertEquals("AggregateRunningStatistics does not handle second total duration correctly", totalDuration, aggregateRunningStatistics.getTotalDuration().getSecondsPassed());
        Assert.assertEquals("AggregateRunningStatistics does not handle second average duration correctly", averageDuration, aggregateRunningStatistics.getAverageDuration().getSecondsPassed());
        Assert.assertEquals("AggregateRunningStatistics does not handle second average heart rate correctly", averageHeartRate, aggregateRunningStatistics.getAverageHeartRate().getHeartRate());
        Assert.assertEquals("AggregateRunningStatistics does not handle second average speed correctly", averageSpeed, aggregateRunningStatistics.getAverageRunSpeed().getSpeed());
        Assert.assertEquals("AggregateRunningStatistics does not handle second number of runs correctly", 2, aggregateRunningStatistics.getNumberOfRuns());
    }
}
