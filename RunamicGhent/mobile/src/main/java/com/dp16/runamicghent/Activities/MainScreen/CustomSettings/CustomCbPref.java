package com.dp16.runamicghent.Activities.MainScreen.CustomSettings;

import android.content.Context;
import android.preference.CheckBoxPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by beheerder on 19/07/2017.
 */

public class CustomCbPref extends CheckBoxPreference
{
    public CustomCbPref(Context context, AttributeSet attrs){
        super(context, attrs);
    }
    protected void onBindView( View view){
        super.onBindView(view);
        makeMultiline(view);
    }
    protected void makeMultiline( View view)
    {
        if ( view instanceof ViewGroup){

            ViewGroup grp=(ViewGroup)view;

            for ( int index = 0; index < grp.getChildCount(); index++)
            {
                makeMultiline(grp.getChildAt(index));
            }
        } else if (view instanceof TextView){
            TextView t = (TextView)view;
            t.setSingleLine(false);
            t.setEllipsize(null);
        }
    }
}

