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

import android.app.Activity;

import com.dp16.runamicghent.GuiController.GuiController;
import com.dp16.runamicghent.GuiController.NoSuchTypeException;
import com.dp16.runamicghent.GuiController.TypeAlreadyExistsException;

import org.junit.After;
import org.junit.Test;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Tests for the GuiController Package
 * Created by Nick on 7-3-2017.
 */

public class GuiControllerTests {
    private GuiController controller = GuiController.getInstance();

    @After
    public void emptyGuiController() {
        controller.emptyState();
    }


    @Test
    public void registerTwice_ThrowsException() {
        String type = "testRegisterTwice";
        try {
            controller.register(type, Activity.class);
        } catch (TypeAlreadyExistsException e) {
            fail("First register of type throws an exception");
        }
        try {
            controller.register(type, Activity.class);
            fail("Second register of type throws no exception");
        } catch (TypeAlreadyExistsException e) {
            // should happen
        }
    }

    @Test
    public void basicRegisterChangeUnregisterScenario_NoExceptionsThrown() {
        String type = "basicScenario";
        try {
            controller.register(type, Activity.class);
        } catch (TypeAlreadyExistsException e) {
            fail("First register of type throws an exception");
        }
        try {
            controller.changeActivity(type, Activity.class);
        } catch (NoSuchTypeException e) {
            fail("Change of registered type throws an exception");
        }
        try {
            controller.unregister(type);
        } catch (NoSuchTypeException e) {
            fail("Unregister of registered type throws an exception");
        }
    }

    @Test
    public void changeActivityOnNonRegisteredType_ThrowsException() {
        String type = "changeWithoutRegister";
        try {
            controller.changeActivity(type, Activity.class);
            fail("Change without register throws no exception");
        } catch (NoSuchTypeException e) {
            // should happen
        }
    }

    @Test
    public void unRegisterNonRegisteredType_ThrowsException() {
        String type = "unregisterWithoutRegister";
        try {
            controller.unregister(type);
            fail("Unregister without register throws no exception");
        } catch (NoSuchTypeException e) {
            // should happen
        }
    }

    @Test
    public void startActivity_AllSupportedTypes_ThrowNoException() {
        String type = "startActivityAllSupportedTypes";
        try {
            controller.register(type, Activity.class);
        } catch (TypeAlreadyExistsException e) {
            fail("First register of type throws an exception");
        }

        Activity dummyContext = new Activity();
        Map<String, Object> extras = new HashMap<String, Object>();

        extras.put("1", "String");
        extras.put("2", 0.0);
        extras.put("3", 1);
        extras.put("4", true);
        byte message5 = 1;
        extras.put("5", message5);
        extras.put("6", 1.0f);
        extras.put("7", 1L);
        short message8 = 1;
        extras.put("8", message8);
        Serializable message9 = new Serializable() {  };
        extras.put("9", message9);

        try {
            int returnValue = controller.startActivity(dummyContext, type, extras);
            assertEquals("startActivity does not return 0 (succes) on valid scenario", 0, returnValue);
        } catch (ClassCastException e) {
            fail("startActivity throws ClassCastException on supported types");
        }
    }

    @Test(expected = ClassCastException.class)
    public void startActivity_NotSupportedType_ThrowsException() {
        String type = "startActivityNoSupportedType";
        try {
            controller.register(type, Activity.class);
        } catch (TypeAlreadyExistsException e) {
            fail("First register of type throws an exception");
        }

        Activity dummyContext = new Activity();
        Map<String, Object> extras = new HashMap<String, Object>();

        Object object = new Object();
        extras.put("1", object);

        controller.startActivity(dummyContext, type, extras);
    }

    @Test
    public void startActivity_startOnUnregisteredType_returnsMinusOne() {
        String type = "startActivityUnregisteredType";
        Activity dummyContext = new Activity();
        assertEquals("", -1, controller.startActivity(dummyContext, type, null));
    }

    @Test
    public void startActivity_NullAsExtra_ThrowsNoException() {
        String type = "startActivityNoSupportedType";
        try {
            controller.register(type, Activity.class);
        } catch (TypeAlreadyExistsException e) {
            fail("First register of type throws an exception");
        }

        Activity dummyContext = new Activity();
        controller.startActivity(dummyContext, type, null);
    }

    @Test
    public void exitActivity_DoesNotThrowException() {
        try {
            Activity activity = new Activity();
            controller.exitActivity(activity);
        } catch (Throwable e){
            fail("exitActivity threw an exception");
        }
    }
}
