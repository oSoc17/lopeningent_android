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

package com.dp16.eventbroker;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Threadsafe Event broker. Listeners must implement {@link EventListener}. Publishers must implement {@link EventPublisher}.
 * Publishers can also extend the {@link EventPublisherClass} which offers the {@link EventPublisherClass#publishEvent(String, Object)} convenience method.
 * The event type can be any string.
 * <p>
 * Normally a listener registers for events by implementing {@link EventListener} and calling {@link #addEventListener(String, EventListener, int)} with <b>this</b> as second argument.
 * A listener can stop receiving events by calling {@link #removeEventListener(String, EventListener)} with <b>this</b> as last argument.
 * <br>
 * A publisher implements {@link EventPublisher} and calls {@link #addEvent(String, Object, EventPublisher)} with <b>this</b> as the last argument.
 * Alternatively a publisher extends {@link EventPublisherClass} and calls {@link EventPublisherClass#publishEvent(String, Object)}.
 * <br>
 * The {@link #start()} and {@link #stop()} methods are usually called from the setup and teardown of the application respectively.
 * </p>
 * A few examples can be found in the unit tests in the EventBrokerUnitTests.java file.
 *
 * @see EventListener
 * @see EventPublisher
 * @see EventPublisherClass
 * Created by Nick on 25-2-2017.
 */
public final class EventBroker implements Runnable {
    private static final EventBroker ourInstance = new EventBroker();

    private final Map<String, List<ListenerItem>> listeners = new HashMap<>();
    private final LinkedList<QueueItem> queue = new LinkedList<>();
    private AtomicBoolean startSignalReceived = new AtomicBoolean();
    private AtomicBoolean stopSignalReceived = new AtomicBoolean();
    private Thread thread;
    private final Object lock = new Object();


    /**
     * Private constructor to enforce singleton.
     */
    private EventBroker() {
        startSignalReceived.set(false);
        stopSignalReceived.set(false);
    }

    /**
     * Data class for items in the queue of the event broker.
     */
    private class QueueItem {
        private String eventType;
        private Object message;
        private EventPublisher source;

        public QueueItem(String eventType, Object message, EventPublisher source) {
            this.eventType = eventType;
            this.message = message;
            this.source = source;
        }
    }

    /**
     * Data class for the registration of an event listener
     */
    private class ListenerItem {
        private EventListener listener;
        private long requestedInterval;
        private long lastServed;

        public ListenerItem(EventListener listener, long requestedInterval) {
            this.listener = listener;
            this.requestedInterval = requestedInterval;
            this.lastServed = 0;
        }
    }


    /**
     * Singleton access method.
     *
     * @return The singleton instance of this class.
     */
    public static EventBroker getInstance() {
        return ourInstance;
    }

    /**
     * Starts the event broker.
     * Does nothing if called on a running event broker.
     */
    public void start() {
        // if false, set to true in a threadsafe manner
        boolean wasFalse = startSignalReceived.compareAndSet(false, true);
        Log.d(this.getClass().getName() + ">start()", "Trying to start Thread");
        // start the thread if this is the first time the start signal is received
        if (wasFalse) {
            Log.d(this.getClass().getName() + ">start()", "Starting Thread");
            thread = new Thread(this);
            thread.start();
        }
    }


    /**
     * Stops the event broker. No new events are accepted after this method is called.
     * Events already in the broker are still processed.
     * This deletes all internal state.
     * A call to start() after this method returns will behave the same as the first call to start().
     * Important: this method does not return until the event broker has fully stopped.
     * This may take a considerable amount of time if the event queue has a lot of items.
     */
    public void stop() {
        // set stop signal -> no more events are accepted, thread will finish when empty
        stopSignalReceived.set(true);

        // wake up thread in case it was sleeping
        synchronized (lock) {
            lock.notify();
        }

        // wait for thread to finish
        try {
            thread.join();
        } catch (InterruptedException e) {
            // someone wants us to stop executing as soon as possible
            // we do as they ask, but signal the EventBroker is in an inconsistent state
            Log.e(this.getClass().getName() + ">stop()", "Interrupt triggered during stopping of EventBroker. Internal state is an inconsistent state.", e);
            Thread.currentThread().interrupt();
        }

        // clean up internal state
        synchronized (listeners) {
            listeners.clear();
        }
        synchronized (queue) {
            queue.clear();
        }
        stopSignalReceived.set(false);
        startSignalReceived.set(false);
    }


    /**
     * Registers an EventListener for one event type.
     * Does nothing if the EventListener already listens for the given event type.
     * This method is equivalent to {@link #addEventListener(String, EventListener, int)}.
     *
     * @param type     Event type to listen to.
     * @param listener EventListener to be registered.
     * @return 1 when listener is added, -1 when listener is not added
     */
    public int addEventListener(String type, EventListener listener) {
        return addEventListener(type, listener, 0);
    }


    /**
     * Registers an EventListener for a collection of event types.
     * Does nothing if the EventListener already listens for the given event type.
     * This method is equivalent to {@link #addEventListener(Collection, EventListener, int)} }
     *
     * @param types    Event type to listen to.
     * @param listener EventListener to be registered.
     * @return 1 when the listener is added for all types, -x if the listener could not be added for all types (where x is the amount of types that failed)
     */
    public int addEventListener(Collection<String> types, EventListener listener) {
        return addEventListener(types, listener, 0);
    }


    /**
     * Registers an EventListener for an event type.
     * An event will only be delivered when at least 'interval' ms have passed since the last delivered event.
     * Any event published during the interval will be lost for this listener.
     * <br>
     * If a new interval is required, the listener has to be removed first (see {@link #removeEventListener(String, EventListener)}).
     *
     * @param type     Event type to listen to
     * @param listener EventListener to be registered
     * @param interval Minimum amount of milliseconds (ms) in between updates
     * @return 1 when listener is added, -1 when listener is not added
     */
    public int addEventListener(String type, EventListener listener, int interval) {
        synchronized (listeners) {
            if (listeners.containsKey(type)) {
                for (ListenerItem item : listeners.get(type)) {
                    if (item.listener.equals(listener)) {
                        return -1;
                    }
                }
                ListenerItem listenerItem = new ListenerItem(listener, interval);
                listeners.get(type).add(listenerItem);
                return 1;
            } else {
                // first listener for this type, so we need a new list in the hashmap
                listeners.put(type, new LinkedList<ListenerItem>());
                ListenerItem listenerItem = new ListenerItem(listener, interval);
                listeners.get(type).add(listenerItem);
                return 1;
            }
        }
    }


    /**
     * Registers an EventListener for a collection of types.
     * An event will only be delivered when at least 'interval' ms have passed since the last delivered event.
     * Any event published during the interval will be lost for this listener.
     * <br>
     * If a new interval is required, the listener has to be removed first (see {@link #removeEventListener(String, EventListener)}).
     *
     * @param types    Event type to listen to
     * @param listener EventListener to be registered
     * @param interval Minimum amount of milliseconds in between updates
     * @return 1 when the listener is added for all types, -x if the listener could not be added for all types (where x is the amount of types that failed)
     */
    public int addEventListener(Collection<String> types, EventListener listener, int interval) {
        int result = 0;
        for (String type : types) {
            if (addEventListener(type, listener, interval) < 0) {
                result--;
            }
        }
        if (result == 0) {
            result++;
        }
        return result;
    }


    /**
     * Unregisters the EventListener for one event type. Potential other event types are not affected.
     * Does nothing if the EventListener is not registered for the event type.
     *
     * @param type     Event type to unregister from.
     * @param listener EventListener to be unregistered.
     * @return 1 when listener is removed, -1 when listener is not removed
     */
    public int removeEventListener(String type, EventListener listener) {
        synchronized (listeners) {
            // the remove method does the 'isPresent' test internally
            List<ListenerItem> itemList = listeners.get(type);
            if (itemList == null) {
                return -1;
            }
            for (int i = 0; i < itemList.size(); i++) {
                if (itemList.get(i).listener.equals(listener)) {
                    itemList.remove(i);
                    return 1;
                }
            }
            return -1;
        }
    }


    /**
     * Unregisters the EventListener for all event types.
     * Calls method above for every type of event.
     * Does nothing if the EventListener is not registered.
     *
     * @param listener EventListener to be unregistered.
     * @return number of times EventListener is removed, -1 if not removed
     */
    public int removeEventListener(EventListener listener) {
        int timesRemoved = 0;
        synchronized (listeners) {
            for (String key : listeners.keySet()) {
                int code = this.removeEventListener(key, listener);
                if (code > 0) {
                    timesRemoved++;
                }
            }
        }
        if (timesRemoved == 0) {
            timesRemoved = -1;
        }
        return timesRemoved;
    }


    /**
     * For testing purposes.
     * Listeners that listen for multiple events are only counted once.
     *
     * @return Amount of listeners that listen for a specified event type.
     */
    public int getAmountOfListeners() {
        synchronized (listeners) {
            List<EventListener> allListeners = new ArrayList<>();
            for (List<ListenerItem> listenersOneType : listeners.values()) {
                for (ListenerItem i : listenersOneType) {
                    if (!allListeners.contains(i.listener)) {
                        allListeners.add(i.listener);
                    }
                }
            }
            return allListeners.size();
        }
    }


    /**
     * Publishes an event to the event broker.
     *
     * @param eventType This event type can be any String.
     * @param message   Message to send to all listeners.
     * @param source    The EventPublisher that generated the event.
     */
    public void addEvent(String eventType, Object message, EventPublisher source) {
        if (stopSignalReceived.get()) {
            // we don't accept new events when the stop signal is received
            return;
        }
        synchronized (queue) {
            QueueItem item = new QueueItem(eventType, message, source);
            queue.add(item);
            Log.d(this.getClass().getName() + ">addEvent()", "Added " + eventType + " to queue");

            // wake up the processing thread in case it was sleeping
            synchronized (lock) {
                lock.notify();
            }
        }
    }


    /**
     * Private method that calls the callbacks on listener for the event type.
     *
     * @param item QueueItem to process
     */
    private void processEvent(QueueItem item) {
        Log.d(this.getClass().getName() + ">processEvent()", "Processing " + item.eventType + "," + item.message);
        List<ListenerItem> listenersForType = listeners.get(item.eventType);
        if (listenersForType == null) {
            Log.d(this.getClass().getName() + ">processEvent()", "No listeners for " + item.eventType);
            // nobody is listening for this event
            return;
        }

        // make a copy of the listeners for this type
        // this way the 'synchronized' part is very short, speeding up parallel execution
        // at the cost of some memory space and sequential running time
        List<ListenerItem> copyListenersForType = new ArrayList<>();
        synchronized (listeners) {
            copyListenersForType.addAll(listenersForType);
        }

        for (ListenerItem listenerItem : copyListenersForType) {
            // don't send event to the sender itself.
            if (!listenerItem.listener.equals(item.source)) {
                // only send when the requested interval has expired
                long currentTime = System.currentTimeMillis();
                if (currentTime >= listenerItem.lastServed + listenerItem.requestedInterval) {
                    listenerItem.listener.handleEvent(item.eventType, item.message);
                    listenerItem.lastServed = currentTime;
                }
            }
        }

    }


    /**
     * Thread code. Takes an item from the queue to process in each iteration.
     * Waits when the queue is empty. Stops when the queue is empty and stopSignalReceived is set
     */
    @Override
    public void run() {
        while (true) {
            QueueItem item;
            synchronized (queue) {
                item = queue.poll();
            }

            // when the queue is empty
            if (item == null) {
                if (stopSignalReceived.get()) {
                    // stop the loop as the queue is empty and the stop flag is set
                    break;
                }

                // sleep until awoken (probably when a new item enters the queue)
                synchronized (lock) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        Log.d(this.getClass().getName() + ">run()", "Interrupt triggered during the waiting on new queue items.", e);
                        Thread.currentThread().interrupt();
                    }
                }
            } else {
                processEvent(item);
            }
        }

    }


}
