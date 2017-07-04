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

package com.dp16.runamicghent.StatTracker;

import com.dp16.runamicghent.RunData.RunDistance;
import com.dp16.runamicghent.RunData.RunDuration;
import com.dp16.runamicghent.RunData.RunHeartRate;
import com.dp16.runamicghent.RunData.RunSpeed;

import java.io.Serializable;

/**
 * This class stores the aggregated statistics of a user.
 * It can be updated by passing it a runningstatistics object.
 * This class should never be called directly, only via the AggregateRunningStatisticsHandler.
 * Created by hendrikdepauw on 29/03/2017.
 */

public class AggregateRunningStatistics implements Serializable {
    private RunDistance totalDistance;
    private RunDistance averageDistance;
    private RunDuration totalDuration;
    private RunDuration averageDuration;
    private RunHeartRate averageHeartRate;
    private RunSpeed averageRunSpeed;

    private int numberOfRuns;

    /**
     * Constructor initializes all values to zero.
     */
    public AggregateRunningStatistics() {
        totalDistance = new RunDistance(0);
        averageDistance = new RunDistance(0);
        totalDuration = new RunDuration(0);
        averageDuration = new RunDuration(0);
        averageHeartRate = new RunHeartRate(0);
        averageRunSpeed = new RunSpeed(0);
        numberOfRuns = 0;
    }

    /**
     * This method updates all statistics by passing it a new RunningStatistics object.
     *
     * @param runningStatistics new run to be incorporated in the statistics.
     */
    public void handleRunningStatistics(RunningStatistics runningStatistics) {
        totalDistance.add(runningStatistics.getTotalDistance());
        averageDistance = new RunDistance((averageDistance.getDistance() * numberOfRuns
                + runningStatistics.getTotalDistance().getDistance()) / (numberOfRuns + 1));

        totalDuration.add(runningStatistics.getRunDuration());
        averageDuration = new RunDuration((averageDuration.getSecondsPassed() * numberOfRuns
                + runningStatistics.getRunDuration().getSecondsPassed()) / (numberOfRuns + 1));

        averageHeartRate = new RunHeartRate((averageHeartRate.getHeartRate() * numberOfRuns
                + runningStatistics.getAverageHeartRate().getHeartRate()) / (numberOfRuns + 1));

        averageRunSpeed = new RunSpeed((averageRunSpeed.getSpeed() * numberOfRuns
                + runningStatistics.getAverageSpeed().getSpeed()) / (numberOfRuns + 1));

        numberOfRuns++;
    }

    /**
     * @return RunDistance containing total distance ran by the user.
     */
    public RunDistance getTotalDistance() {
        return totalDistance;
    }

    /**
     * @return RunDistance containing average distance of a run.
     */
    public RunDistance getAverageDistance() {
        return averageDistance;
    }

    /**
     * @return RunDuration containing total time ran by the user.
     */
    public RunDuration getTotalDuration() {
        return totalDuration;
    }

    /**
     * @return Runduration containing average duration of a run.
     */
    public RunDuration getAverageDuration() {
        return averageDuration;
    }

    /**
     * @return RunHeartRate containing average HeartRate of the user
     */
    public RunHeartRate getAverageHeartRate() {
        return averageHeartRate;
    }

    /**
     * @return RunSpeed containing average speed of the user
     */
    public RunSpeed getAverageRunSpeed() {
        return averageRunSpeed;
    }

    /**
     * @return amount of runs
     */
    public int getNumberOfRuns() {
        return numberOfRuns;
    }
}
