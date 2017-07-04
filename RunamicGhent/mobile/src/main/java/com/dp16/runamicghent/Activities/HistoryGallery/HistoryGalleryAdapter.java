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

package com.dp16.runamicghent.Activities.HistoryGallery;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.dp16.runamicghent.Activities.MainScreen.Fragments.HistoryFragment;
import com.dp16.runamicghent.Constants;
import com.dp16.runamicghent.R;
import com.dp16.runamicghent.StatTracker.RunningStatistics;
import com.google.android.gms.maps.model.LatLng;
import com.peekandpop.shalskar.peekandpop.PeekAndPop;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static com.dp16.runamicghent.Activities.Utils.preProcessRoute;

/**
 * Adapter for displaying the list of previous runs.
 * Created by lorenzvanherwaarden on 25/02/2017.
 */

public class HistoryGalleryAdapter extends RecyclerView.Adapter<HistoryGalleryAdapter.HistoryViewHolder> {
    /**
     * You need to specify your own adapter for RecyclerView.
     * The adapter provides access to the items in your data set, creates views for items,
     * and replaces the content of some of the views with new data items when the original item is no longer visible.
     */
    private ArrayList<RunningStatistics> runningStatisticsData;
    private PeekAndPop peekAndPop;
    private View peekView;
    private HistoryFragment historyFragment;

    /**
     * Provide a reference to the views for each data item
     * Complex data items may need more than one view per item, and
     * you provide access to all the views for a data item in a view holder
     */
    public static class HistoryViewHolder extends RecyclerView.ViewHolder {
        CardView cv;
        TextView duration;
        TextView distance;
        TextView speed;
        TextView date;
        RatingBar ratingBar;

        int runId;

        final Context context;

        public HistoryViewHolder(View itemView) {
            super(itemView);
            context = itemView.getContext();
            cv = (CardView) itemView.findViewById(R.id.cv);
            duration = (TextView) itemView.findViewById(R.id.duration);
            distance = (TextView) itemView.findViewById(R.id.distance);
            speed = (TextView) itemView.findViewById(R.id.speed);
            date = (TextView) itemView.findViewById(R.id.date);
            ratingBar = (RatingBar) itemView.findViewById(R.id.ratingbar);
        }
    }

    /**
     * Provide a suitable constructor (depends on the kind of dataset)
     *
     * @param runningStatisticsData
     */
    public HistoryGalleryAdapter(List<RunningStatistics> runningStatisticsData, PeekAndPop peekAndPop, HistoryFragment historyFragment) {
        this.runningStatisticsData = (ArrayList<RunningStatistics>) runningStatisticsData;
        this.historyFragment = historyFragment;
        this.peekAndPop = peekAndPop;
        peekView = peekAndPop.getPeekView();

        this.peekAndPop.setOnGeneralActionListener(new PeekAndPop.OnGeneralActionListener() {
            @Override
            public void onPeek(View view, int position) {
                loadPeekAndPop(position);
            }

            @Override
            public void onPop(View view, int position) {
                ImageView bmImage = (ImageView) peekView.findViewById(R.id.map);
                bmImage.setImageDrawable(null);
            }
        });
    }

    private void loadPeekAndPop(int position) {
        String url = "http://maps.googleapis.com/maps/api/staticmap?size=500x500&scale=2&path=color:0x4caf50EE";
        List<LatLng> route = runningStatisticsData.get(position).getRoute();
        route = preProcessRoute(route, Constants.Smoothing.SMOOTHING_ENABLED);

        /*
        366 as amount of route points we can put in the url is well chosen. The formula to derive it is:
            Max characters url = 8192
            length per point added to url is = 22 -> e.g. "|51.2141231,4.91491402".length()
            amount = 8192 - url.length() - "&key=".length() - Constants.MapSettings.STATIC_MAPS_KEY.length()) / 22
        We then use this amount to derive the step for the for loop going over route
         */
        int step = (int) Math.ceil((double) route.size() / 366);
        for (int i = 0; i < route.size(); i += step) {
            String lat = Double.toString(route.get(i).latitude);
            String lon = Double.toString(route.get(i).longitude);
            url = url.concat("|" + lat.substring(0, Math.min(lat.length(), 10)) + "," + lon.substring(0, Math.min(lon.length(), 10)));
        }

        url = url.concat("&key=" + Constants.MapSettings.STATIC_MAPS_KEY);

        new DownloadImageTask(position)
                .execute(url);
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;
        TextView durationPeek;
        TextView distancePeek;

        public DownloadImageTask(int position) {
            this.bmImage = (ImageView) peekView.findViewById(R.id.map);
            this.durationPeek = (TextView) peekView.findViewById(R.id.duration_peek);
            this.distancePeek = (TextView) peekView.findViewById(R.id.distance_peek);
            durationPeek.setText(runningStatisticsData.get(position).getRunDuration().toString());
            distancePeek.setText(runningStatisticsData.get(position).getTotalDistance().toString());
        }

        @Override
        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
                in.close();
            } catch (Exception e) {
                Log.e("Error", e.getMessage(), e);
                historyFragment.getView().post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(historyFragment.getContext(), R.string.internet_connection, Toast.LENGTH_LONG).show();
                    }
                });
            }
            return mIcon11;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
        }
    }

    /**
     * Create new views (invoked by the layout manager)
     */
    @Override
    public HistoryGalleryAdapter.HistoryViewHolder onCreateViewHolder(ViewGroup parent,
                                                                      int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.history_cardview
                        , parent, false);
        // set the view's size, margins, paddings and layout parameters
        return new HistoryViewHolder(v);
    }

    /**
     * Replace the contents of a view (invoked by the layout manager)
     */
    @Override
    public void onBindViewHolder(final HistoryViewHolder holder, final int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        peekAndPop.addLongClickView(holder.cv, position);

        peekAndPop.setOnFlingToActionListener(new PeekAndPop.OnFlingToActionListener() {
            @Override
            public void onFlingToAction(View longClickView, int position, int direction) {
                historyFragment.switchToExpanded(runningStatisticsData.get(position));
            }
        });

        holder.cv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                historyFragment.switchToExpanded(runningStatisticsData.get(position));
            }
        });

        holder.duration.setText(runningStatisticsData.get(position).getRunDuration().toString());
        holder.distance.setText(runningStatisticsData.get(position).getTotalDistance().toString());
        holder.speed.setText(runningStatisticsData.get(position).getAverageSpeed().toString(historyFragment.getContext()));
        holder.date.setText(runningStatisticsData.get(position).getStartTimeDate());
        holder.ratingBar.setRating((float) runningStatisticsData.get(position).getRating());
        holder.runId = position;
    }


    /**
     * Return the size of your dataset (invoked by the layout manager)
     *
     * @return Amount of items in the dataset.
     */
    @Override
    public int getItemCount() {
        return runningStatisticsData.size();
    }
}
