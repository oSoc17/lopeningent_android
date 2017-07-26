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

package com.dp16.runamicghent.DataProvider;

import android.util.Log;

import com.dp16.runamicghent.Activities.Utils;
import com.dp16.runamicghent.Constants;
import com.dp16.runamicghent.RunData.RunRating;
import com.dp16.eventbroker.EventBroker;
import com.dp16.eventbroker.EventListener;
import com.dp16.eventbroker.EventPublisher;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This DataProvider will send ratings of runs to the server. Not really a DataProvider.
 *
 * <p>
 *     <b>Messages Produced: </b> {@link com.dp16.runamicghent.Constants.EventTypes#STATUS_CODE}
 * </p>
 * <p>
 *     <b>Messages Consumed: </b> {@link com.dp16.runamicghent.Constants.EventTypes#RATING}
 * </p>
 * Created by lorenzvanherwaarden on 29/04/2017.
 */

public class RatingTransmitter implements EventPublisher, EventListener, DataProvider {

    private ExecutorService worker;

    private Boolean statusReponse;

    public RatingTransmitter(Boolean statusResponse) {
        this.statusReponse = statusResponse;
        worker = Executors.newSingleThreadExecutor();
        this.start();
    }

    /**
     * Start listening to Rating Events.
     */
    @Override
    public void start() {
        EventBroker.getInstance().addEventListener(Constants.EventTypes.RATING, this);
    }

    @Override
    public void stop() {
        EventBroker.getInstance().removeEventListener(Constants.EventTypes.RATING, this);
    }

    @Override
    public void resume() {
        start();
    }

    @Override
    public void pause() {
        stop();
    }

    /**
     * Handles the reception of RATING events.
     *
     * @param eventType: should be of type Constants.EventTypes.RATING.
     * @param message:   RunRating class with tag and rating of run
     */
    @Override
    public void handleEvent(String eventType, Object message) {
        Runnable task = new RatingTransmitter.Worker(message);
        worker.submit(task);
    }

    /**
     * Asynchronous providing of rating for run to the server.
     */
    private class Worker implements Runnable {
        private RunRating runRating;
        private RatingTransmitter ratingTransmitter;

        Worker(Object message) {
            this.runRating = (RunRating) message;
        }

        /*
          Open connection and fetch the response from the server for adding a rating.
         */
        @Override
        public void run() {

            boolean goodRequest = false;

            String body = null;
            try {

                body = URLEncoder.encode("visited_path", "UTF-8")
                        + "=" + URLEncoder.encode(runRating.getTag(), "UTF-8");
                body += "&" + URLEncoder.encode("rating", "UTF-8")
                        + "=" + URLEncoder.encode(runRating.getStringRating(), "UTF-8");


            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            JSONObject response = Utils.PostRequest(body,"http://" + Constants.Server.ADDRESS + "/route/rate/");
            if (response!=null){
                goodRequest = true;
                Log.d("tag", runRating.getTag());
                // If statusReponse is set to true, an event will be published with the status code.
                if (statusReponse){
                    EventBroker.getInstance().addEvent(Constants.EventTypes.STATUS_CODE, 200, this.ratingTransmitter);
                }
            }else{
                // If rating was not able to be sent in 3 requests, too bad. This is not such a big problem.
                if (statusReponse){
                    EventBroker.getInstance().addEvent(Constants.EventTypes.STATUS_CODE, 500, this.ratingTransmitter);
                }
            }
            Log.d("Rating sent", Boolean.toString(goodRequest));








        }


    }

}
