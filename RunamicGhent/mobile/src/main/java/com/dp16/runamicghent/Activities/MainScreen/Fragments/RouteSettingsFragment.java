/**
 * Copyright (c) 2017 Redouane Arroubai
 *
 * This software may be modified and distributed under the terms of the MIT license.  See the LICENSE file for details.
 */
package com.dp16.runamicghent.Activities.MainScreen.Fragments;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.IdRes;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.dp16.eventbroker.EventListener;
import com.dp16.eventbroker.EventPublisher;
import com.dp16.runamicghent.Activities.MainScreen.CustomSettings.SettingsAdapter;
import com.dp16.runamicghent.GuiController.GuiController;
import com.dp16.runamicghent.R;

import java.lang.reflect.Array;
import java.util.ArrayList;

import static android.support.constraint.R.id.parent;
import static com.facebook.FacebookSdk.getApplicationContext;

public class RouteSettingsFragment extends Fragment {


    public static final String TAG = RouteSettingsFragment.class.getSimpleName();
    private View view;
    boolean initialDisplay = true;

    private boolean mAutoIncrement = false;
    private boolean mAutoDecrement = false;
    private static int REP_DELAY = 50;
    private Handler repeatUpdateHandler = new Handler();

    SharedPreferences preferences;
    SharedPreferences.Editor editor;

    ListView lstPoi;

    private ArrayList<String > poiList;

    private Context fgContext = getContext();

    /*
    *
    *   Variables for the parameters of the route: to be added
     */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences =PreferenceManager.getDefaultSharedPreferences(getContext());
        editor = preferences.edit();
        //gui
        GuiController guiController = GuiController.getInstance();
        poiList = guiController.getPoiTags();




    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             final Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_routesettings, container, false);
        //difficulty dropdownlist
        Spinner spinnerDif = (Spinner) view.findViewById(R.id.spDifficulty);
        ArrayAdapter<CharSequence> adapter2 = ArrayAdapter.createFromResource(getContext(),R.array.difficulty, R.layout.spinner_item_view);
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDif.setAdapter(adapter2);
        //fill listview
         lstPoi = (ListView) view.findViewById(R.id.lstPoi);
        SettingsAdapter workoutListAdapter = new SettingsAdapter(this.getActivity(), poiList);
        lstPoi.setAdapter(workoutListAdapter);
        //Radiogroups
        RadioGroup rdgDistanceTime = (RadioGroup) view.findViewById(R.id.parDistanceTime);
        //get preference
        getPreference(rdgDistanceTime);
        //Set buttons
        getDTvalue();
        setButtons();
        //Preference changed
        rdgDistanceTime.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                changePreference(group);
                //First save value then get new value of corresponding parameter
                getDTvalue();
                setButtons();
            }
        });

        spinnerDif.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                // your code here
                /*Spinner SelectedListener excecutes TWICE:
                   1: execute on build
                   2: execute when user select
                   Use boolean initialDisplay to be able to differentiate between those 2 executions
                 */
                if (initialDisplay)
                {
                    getDifficulty();
                    initialDisplay = false;
                }
                else
                {
                    saveDifficulty();
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // your code here
            }

        });

        return view;
    }
    //Set buttons method (Distance or Time methods)
    public void setButtons(){
        final Button btnSubstract =  (Button)view.findViewById(R.id.btnSubsDTValue);
        final Button btnAdd = (Button)view.findViewById(R.id.btnAddDTValue);
        //SetButtons
        //Set add
        btnAdd.setOnClickListener(new View.OnClickListener(){
            @Override
            public  void onClick(View v){
                increment();
            }
        });
        btnAdd.setOnLongClickListener(new View.OnLongClickListener(){
            @Override
            public  boolean onLongClick(View v){
                mAutoIncrement = true;
                repeatUpdateHandler.post(new RptUpdater());
                return false;

            }
        });
        btnAdd.setOnTouchListener(new View.OnTouchListener(){
            public boolean onTouch(View v, MotionEvent event) {
                if( (event.getAction()==MotionEvent.ACTION_UP || event.getAction()==MotionEvent.ACTION_CANCEL)
                        && mAutoIncrement ){
                    mAutoIncrement = false;
                }
                return false;
            }
        });
        //set substract
        btnSubstract.setOnClickListener(new View.OnClickListener(){
            @Override
            public  void onClick(View v){
                decrement();
            }
        });
        btnSubstract.setOnLongClickListener(new View.OnLongClickListener(){
            @Override
            public  boolean onLongClick(View v){
                mAutoDecrement = true;
                repeatUpdateHandler.post(new RptUpdater());
                return false;

            }
        });
        btnSubstract.setOnTouchListener(new View.OnTouchListener(){
            public boolean onTouch(View v, MotionEvent event) {
                if( (event.getAction()==MotionEvent.ACTION_UP || event.getAction()==MotionEvent.ACTION_CANCEL)
                        && mAutoDecrement ){

                    mAutoDecrement = false;
                }
                return false;
            }
        });
    }
    //Get preference method
    public void getPreference(RadioGroup radioGroup){
        boolean checked = false;
        RadioButton radioButton = (RadioButton)radioGroup.getChildAt(1);
        RadioButton radioButton2 = (RadioButton)radioGroup.getChildAt(0);
        checked = preferences.getBoolean(radioButton.getTag().toString(),false);
        if (checked){
            radioButton.setChecked(true);
            radioButton.setTextColor(getResources().getColorStateList(R.color.cardview_light_background));
            radioButton2.setTextColor(getResources().getColorStateList(R.color.cardview_shadow_start_color));
        }
        else {
            radioButton.setTextColor(getResources().getColorStateList(R.color.cardview_shadow_start_color));
            radioButton2.setTextColor(getResources().getColorStateList(R.color.cardview_light_background));
        }
    }
    //Change preference method
    public void changePreference(RadioGroup radioGroup){

        RadioButton radioButton = (RadioButton)radioGroup.getChildAt(1);
        RadioButton radioButton2 = (RadioButton)radioGroup.getChildAt(0);
        //radioButton2.getId()
        if (radioButton.isChecked())
        {
            editor.putBoolean(radioButton.getTag().toString(), true);
            radioButton.setTextColor(getResources().getColorStateList(R.color.cardview_light_background));
            radioButton2.setTextColor(getResources().getColorStateList(R.color.cardview_shadow_start_color));
        }

        else
        {
            editor.putBoolean(radioButton.getTag().toString(),false);
            radioButton.setTextColor(getResources().getColorStateList(R.color.cardview_shadow_start_color));
            radioButton2.setTextColor(getResources().getColorStateList(R.color.cardview_light_background));
        }

        editor.apply();
    }
    //Store and get preference value
    public void getDTvalue(){
       // final TextView tvDT = (TextView)view.findViewById(R.id.tvDT);
        TextView tvValue = (TextView)view.findViewById(R.id.tvDTValue);
        RadioButton rdbDistance = (RadioButton)view.findViewById(R.id.rdbDistance);
        String getValue;
        if (rdbDistance.isChecked()){
            hideSpinner();
            getValue = preferences.getString("distanceValue", "0.0");
           // tvDT.setText("km");
        }
        else {
            addSpinner();
            getValue = preferences.getString("timeValue", "00:00");
           // tvDT.setText("h:m");
        }
        tvValue.setText(getValue);
    }
    //Save DTvalue
    /*
    public void saveDTvalue(){
        TextView tvValue = (TextView)view.findViewById(R.id.tvDTValue);
        RadioButton rdbDistance = (RadioButton)view.findViewById(R.id.rdbDistance);
        String value = tvValue.getText().toString();
        if (rdbDistance.isChecked()){
            editor.putString("timeValue",value);

        }
        else{
            editor.putString("distanceValue",value);
        }
        editor.apply();
    }*/

    public void saveDifficulty(){
        Spinner spinner = (Spinner)view.findViewById(R.id.spDifficulty);
        int index = spinner.getSelectedItemPosition();
        editor.putInt("difficulty",index);
        editor.apply();
    }

    public void getDifficulty(){
        Spinner spinner = (Spinner)view.findViewById(R.id.spDifficulty);
        int index = preferences.getInt("difficulty",0);
        spinner.setSelection(index);
    }



    //Dynamically add and remove spinner
    public void addSpinner(){
        Spinner spinner = (Spinner) view.findViewById(R.id.spDifficulty);
        spinner.setVisibility(View.VISIBLE);
    }

    public void hideSpinner(){
        Spinner spinner = (Spinner) view.findViewById(R.id.spDifficulty);
        spinner.setVisibility(View.GONE);
    }

    //Class for on button pressed check
    class RptUpdater implements Runnable {
        public void run() {
            if( mAutoIncrement ){
                increment();
                repeatUpdateHandler.postDelayed( new RptUpdater(), REP_DELAY );
            } else if( mAutoDecrement ){
                decrement();
                repeatUpdateHandler.postDelayed( new RptUpdater(), REP_DELAY );
            }
        }
    }

    //button pressed increment/decrement
   public void increment(){
       RadioButton rdbDistance = (RadioButton)view.findViewById(R.id.rdbDistance);
       TextView tvValue = (TextView)view.findViewById(R.id.tvDTValue);
       if (rdbDistance.isChecked()){
           double value = Double.parseDouble(tvValue.getText().toString());
           value += 0.1;
           value = (double) Math.round(((value * 100) * 10) / 10)/100;
           tvValue.setText(String.valueOf(value));
           editor.putString("distanceValue",String.valueOf(value));

       }
       else {
           String time = tvValue.getText().toString();
           int h = Integer.parseInt(time.substring(0,2));
           int m = Integer.parseInt(time.substring(3,5));
           m += 1;
           if(m >= 60){
               m = 00;
               h += 1;
           }
           String newtime = String.format("%02d", h)+":"+String.format("%02d", m);
           editor.putString("timeValue",newtime);
           tvValue.setText(newtime);
       }
       editor.apply();

   }

   public void decrement(){
       RadioButton rdbDistance = (RadioButton)view.findViewById(R.id.rdbDistance);
       TextView tvValue = (TextView)view.findViewById(R.id.tvDTValue);
       if (rdbDistance.isChecked()){
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
           editor.putString("distanceValue",tvValue.getText().toString());
       }
       else {
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
           editor.putString("timeValue",tvValue.getText().toString());
       }
       editor.apply();
   }

    /**
     * Retrieves the route settings that have been defined
     */

    public void getCurrentSettings() {

    }




}
