package com.dp16.runamicghent.Activities.MainScreen.CustomSettings;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.IdRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.dp16.runamicghent.R;

import java.util.ArrayList;


/**
 * Created by beheerder on 20/07/2017.
 */

public class SettingsAdapter extends BaseAdapter {
    private Context context;
    private ArrayList<String> items;
    private LayoutInflater layoutInflater;
    private SharedPreferences preferences;
    SharedPreferences.Editor editor;

    private class ViewHolder {
         RadioButton poi1;
         RadioButton poi2;
         RadioGroup rdgPoi;

    }

    public SettingsAdapter(Context c, ArrayList<String> items){
        this.context = c;
        this.items = items;
        this.layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        preferences =  PreferenceManager.getDefaultSharedPreferences(context);


    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public  Object getItem(int position){
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent){

        final ViewHolder viewHolder;
        if (convertView == null){
            convertView = layoutInflater.inflate(R.layout.settings_item_view, null);
            viewHolder = new SettingsAdapter.ViewHolder();
            viewHolder.rdgPoi = (RadioGroup) convertView.findViewById(R.id.rdgPoi);
            viewHolder.poi1 = (RadioButton) convertView.findViewById(R.id.rdbPoi1);
            viewHolder.poi2 = (RadioButton) convertView.findViewById(R.id.rdbPoi2);
            convertView.setTag(viewHolder);
        }
        else {
            viewHolder = (SettingsAdapter.ViewHolder) convertView.getTag();
        }

        String item = items.get(position);
        viewHolder.poi1.setText(item);
        viewHolder.poi1.setTag(item);
        viewHolder.poi2.setText(context.getString(R.string.no) + " " +item);
        viewHolder.poi2.setTag(item);
        //set selected
        if (preferences.getBoolean(item,false)){
            viewHolder.poi1.setChecked(true);
            viewHolder.poi1.setTextColor(context.getResources().getColorStateList(R.color.cardview_light_background));
            viewHolder.poi2.setTextColor(context.getResources().getColorStateList(R.color.cardview_shadow_start_color));
        }
        else
        {
            viewHolder.poi1.setChecked(false);
            viewHolder.poi1.setTextColor(context.getResources().getColorStateList(R.color.cardview_shadow_start_color));
            viewHolder.poi2.setTextColor(context.getResources().getColorStateList(R.color.cardview_light_background));
        }
        viewHolder.rdgPoi.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                editor = preferences.edit();
                RadioButton radioButton = (RadioButton) group.getChildAt(0);
                RadioButton radioButton2 = (RadioButton) group.getChildAt(1);
                if (viewHolder.poi1.isChecked()){
                    editor.putBoolean(radioButton.getTag().toString(),true);
                    radioButton2.setTextColor(context.getResources().getColorStateList(R.color.cardview_shadow_start_color));
                   radioButton.setTextColor(context.getResources().getColorStateList(R.color.cardview_light_background));
                }
                else {
                    editor.putBoolean(radioButton.getTag().toString(),false);
                    radioButton2.setTextColor(context.getResources().getColorStateList(R.color.cardview_light_background));
                    radioButton.setTextColor(context.getResources().getColorStateList(R.color.cardview_shadow_start_color));
                }
                editor.apply();
            }
        });

        return convertView;
    }
}
