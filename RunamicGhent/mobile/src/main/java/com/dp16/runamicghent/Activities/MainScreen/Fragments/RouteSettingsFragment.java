/**
 * Copyright (c) 2017 Redouane Arroubai
 *
 * This software may be modified and distributed under the terms of the MIT license.  See the LICENSE file for details.
 */
package com.dp16.runamicghent.Activities.MainScreen.Fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.dp16.eventbroker.EventListener;
import com.dp16.eventbroker.EventPublisher;
import com.dp16.runamicghent.R;

public class RouteSettingsFragment extends Fragment {


    public static final String TAG = RouteSettingsFragment.class.getSimpleName();
    private View view;

    /*
    *
    *   Variables for the parameters of the route: to be added
     */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getCurrentSettings();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_routesettings, container, false);
        final TextView tvDT = (TextView)view.findViewById(R.id.tvDT);
        final TextView tvDTValue = (TextView)view.findViewById(R.id.tvDTValue);
        Spinner spinner = (Spinner) view.findViewById(R.id.spPOI);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(),R.array.poi, R.layout.spinner_item_view);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        checkChecked();
        //Radiogroups
        RadioGroup rdgDistanceTime = (RadioGroup) view.findViewById(R.id.parDistanceTime);
        rdgDistanceTime.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                checkChecked();
                if (checkedId == view.findViewById(R.id.rdbTime).getId()){
                    tvDT.setText("h:m");
                    tvDTValue.setText("00:00");

                }
                else {
                    tvDTValue.setText("0.0");
                    tvDT.setText("km");
                }
            }
        });

        return view;
    }
    public void checkChecked(){
        RadioButton rdbDistance = (RadioButton)view.findViewById(R.id.rdbDistance);
        Button btnSubstract =  (Button)view.findViewById(R.id.btnSubsDTValue);
        Button btnAdd = (Button)view.findViewById(R.id.btnAddDTValue);
        if (rdbDistance.isChecked()){
            btnAdd.setOnClickListener(new View.OnClickListener(){
                @Override
                public  void onClick(View v){
                        addKm();
                }
            });
            btnSubstract.setOnClickListener(new View.OnClickListener(){
                @Override
                public  void onClick(View v){
                    substractKm();
                }
            });
        }
        else {
            btnAdd.setOnClickListener(new View.OnClickListener(){
                @Override
                public  void onClick(View v){
                    addTime();
                }
            });
            btnSubstract.setOnClickListener(new View.OnClickListener(){
                @Override
                public  void onClick(View v){

                }
            });
        }
    }
    public void substractKm(){
        TextView tvValue = (TextView)view.findViewById(R.id.tvDTValue);
        double value = Double.parseDouble(tvValue.getText().toString());
        if (value - 0.1 <0 )
        {
            value = 0;
        }
        else {
            value -= 0.1;
            value = (double) Math.round(((value * 100) * 10) / 10)/100;
        }

        tvValue.setText(String.valueOf(value));

    }
    public void addKm(){
        TextView tvValue = (TextView)view.findViewById(R.id.tvDTValue);
        double value = Double.parseDouble(tvValue.getText().toString());
        value += 1;
        value = (double) Math.round(((value * 100) * 10) / 10)/100;
        tvValue.setText(String.valueOf(value));

    }
     public void addTime(){
         TextView tvValue = (TextView)view.findViewById(R.id.tvDTValue);
         String time = tvValue.getText().toString();
         int h = Integer.parseInt(time.substring(0,2));
         int m = Integer.parseInt(time.substring(3,5));
         m += 1;
         if(m >= 60){
             m = 00;
             h += 1;
         }
         String newtime = String.format("%02d", h)+":"+String.format("%02d", m);
         tvValue.setText(newtime);
     }

    public void substractTime(){
        TextView tvValue = (TextView)view.findViewById(R.id.tvDTValue);
        String time = tvValue.getText().toString();
        int h = Integer.parseInt(time.substring(0,2));
        int m = Integer.parseInt(time.substring(3,5));
        m -= 1;
        if(m < 0){
           if (h>0){
               h--;
               m=59;
           }
           else {
               m = 0;
           }
        }
        String newtime = String.format("%02d", h)+":"+String.format("%02d", m);
        tvValue.setText(newtime);
    }

    /**
     * Retrieves the route settings that have been defined
     */

    public void getCurrentSettings() {

    }




}
