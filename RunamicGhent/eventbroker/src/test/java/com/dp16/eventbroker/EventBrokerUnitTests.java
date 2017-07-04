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

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * Tests for the EventBroker package
 * Created by Nick on 26-2-2017.
 */

public class EventBrokerUnitTests {
    private EventBroker broker = EventBroker.getInstance();

    /*
     * Note: JUnit launches all tests in parallel. As EventBroker is a singleton, this means all tests can interfere with each other.
     * This does not seem to be much of a problem if a different eventType is used for each test.
     */

    @Test
    public void basicOneListenerOnePublisherTwoEventsScenario() {
        final AtomicInteger messageReceived = new AtomicInteger(0);
        final String eventType = "test1";

        // make an event listener with callback
        EventListener listener = new EventListener() {
            @Override
            public void handleEvent(String eventType, Object message) {
                messageReceived.getAndIncrement();
            }
        };

        // make an event publisher
        EventPublisherClass publisher = new EventPublisherClass();

        // add the listener (no interval)
        broker.addEventListener(eventType, listener);

        // start the event broker
        broker.start();

        // publish some events
        publisher.publishEvent(eventType, "message");
        publisher.publishEvent(eventType, "message2");

        // stop the broker: this blocks future incoming events and handles the events in flight before returning
        broker.stop();

        // this event should not be received anymore
        publisher.publishEvent(eventType, "dropppedMessage");

        assertEquals("Events were not received in a one-listener, one-publisher, two-events scenario", 2, messageReceived.get());
    }

    @Test
    public void basicScenarioWithFrequency() throws Exception {
        final AtomicInteger messageReceived = new AtomicInteger(0);
        final String eventType = "testFrequency1";
        EventListener listener = new EventListener() {
            @Override
            public void handleEvent(String eventType, Object message) {
                messageReceived.getAndIncrement();
            }
        };
        EventPublisherClass publisher = new EventPublisherClass();
        broker.addEventListener(eventType, listener, 100);
        broker.start();
        publisher.publishEvent(eventType, "message");
        publisher.publishEvent(eventType, "messageToFast");
        publisher.publishEvent(eventType, "message2ToFast");
        broker.stop();
        // 'message' should be received, 'messageToFast' and 'message2ToFast should be dropped
        assertEquals("Events were not received in a one-listener, one-publisher, one-event with frequency scenario", 1, messageReceived.get());
    }

    @Test
    public void basicTwoListenersOnePublisherOneEventScenario() {
        final AtomicBoolean messageReceivedListenerOne = new AtomicBoolean(false);
        final AtomicBoolean messageReceivedListenerTwo = new AtomicBoolean(false);
        EventListener listenerOne = new EventListener() {
            @Override
            public void handleEvent(String eventType, Object message) {
                messageReceivedListenerOne.set(true);
            }
        };
        EventListener listenerTwo = new EventListener() {
            @Override
            public void handleEvent(String eventType, Object message) {
                messageReceivedListenerTwo.set(true);
            }
        };
        EventPublisher publisher = new EventPublisher() {
            // the EventPublisher interface has no methods, it is just a marker interface
        };
        broker.addEventListener("testOne", listenerOne);
        broker.addEventListener("testTwo", listenerTwo);
        broker.start();

        // when using the EventPublisher interface instead of the EventPublisherClass, one must call the event broker explicitly
        broker.addEvent("testTwo", "message", publisher);

        broker.stop();
        assertFalse("Message received of the wrong type", messageReceivedListenerOne.get());
        assertTrue("No message received when the right type was published", messageReceivedListenerTwo.get());
    }

    @Test
    public void addingAndRemovingAListener_isIdempotent() {
        EventListener listener = new EventListener() {
            @Override
            public void handleEvent(String eventType, Object message) {

            }
        };
        broker.addEventListener("test2", listener);
        assertEquals("Adding one listener does not return 1 present listener", 1, broker.getAmountOfListeners());
        broker.removeEventListener("test2", listener);
        assertEquals("Adding and removing a listener does not result in an empty listener list", 0, broker.getAmountOfListeners());
    }

    @Test
    public void callingStartOrStopTwice_isSafe() {
        final AtomicBoolean messageReceived = new AtomicBoolean(false);
        EventListener listener = new EventListener() {
            @Override
            public void handleEvent(String eventType, Object message) {
                messageReceived.set(true);
            }
        };
        EventPublisherClass publisher = new EventPublisherClass();
        broker.addEventListener("test3", listener);
        broker.start();
        broker.start();
        publisher.publishEvent("test3", "message");
        broker.stop();
        broker.stop();
        assertTrue("Calling start or stop twice is not safe", messageReceived.get());
    }

    @Test
    public void callingStop_CleansUpInternalState() {
        final AtomicBoolean messageReceived = new AtomicBoolean(false);
        EventListener listener = new EventListener() {
            @Override
            public void handleEvent(String eventType, Object message) {
                messageReceived.set(true);
            }
        };
        EventPublisherClass publisher = new EventPublisherClass();
        broker.addEventListener("test4", listener);
        broker.start();
        publisher.publishEvent("test4", "message");
        broker.stop();
        assertEquals("Calling stop does not remove old listeners", broker.getAmountOfListeners(), 0);

        messageReceived.set(false);
        broker.start();
        publisher.publishEvent("test4", "message2");
        assertFalse("Calling stop does not unregister old listeners", messageReceived.get());

        EventListener listener2 = new EventListener() {
            @Override
            public void handleEvent(String eventType, Object message) {
                messageReceived.set(true);
            }
        };
        broker.addEventListener("test5", listener2);
        assertEquals("Can't add a new listener while the event broker is running", broker.getAmountOfListeners(), 1);

        publisher.publishEvent("test5", "message");
        broker.stop();
        assertTrue("Event is not delivered when restarting from a stop() start() sequence", messageReceived.get());
    }

    @Test
    public void eventBroker_isThreadSafe_v1() throws Exception {
        final AtomicInteger amountMessagesReceived = new AtomicInteger(0);

        // make and register listener in one thread
        Thread listenerThread = new Thread() {
            @Override
            public void run() {
                EventListener listener = new EventListener() {
                    @Override
                    public void handleEvent(String eventType, Object message) {
                        amountMessagesReceived.getAndIncrement();
                    }
                };
                broker.addEventListener("testMulti", listener);
            }
        };

        // make and launch an event from a publisher in another thread
        Thread publisherThread = new Thread() {
            @Override
            public void run() {
                EventPublisherClass publisher = new EventPublisherClass();
                publisher.publishEvent("testMulti", "message");
            }
        };


        listenerThread.start();
        broker.start();
        listenerThread.join();

        // add an event from the 'main' thread
        EventPublisher publisherTwo = new EventPublisher() {
        };
        broker.addEvent("testMulti", "message", publisherTwo);
        broker.addEvent("testMulti", "message2", publisherTwo);
        broker.addEvent("otherTypeMulti", "message", publisherTwo);
        publisherThread.start();
        broker.stop();
        int amountReceived = amountMessagesReceived.get();

        // should receive event from 'main' thread, might receive message from 'publisherThread'
        assertTrue("Event broker does not work with multiple threads", amountReceived == 2 || amountReceived == 3);
        publisherThread.join();
    }

    @Test
    public void eventBroker_isThreadSafe_v2() {
        final AtomicInteger amountMessagesReceivedListenerOne = new AtomicInteger(0);
        final AtomicInteger amountMessagesReceivedListenerTwo = new AtomicInteger(0);
        final int amountThreads = 16;
        final int amountEventsInThread = 1000;
        final String eventType = "testMultiV2";

        EventListener listenerOne = new EventListener() {
            @Override
            public void handleEvent(String eventType, Object message) {
                amountMessagesReceivedListenerOne.incrementAndGet();
            }
        };
        broker.addEventListener(eventType, listenerOne);
        assertEquals("No messages before start of broker", 0, amountMessagesReceivedListenerOne.get());
        broker.start();

        List<Thread> threads = new ArrayList<Thread>();
        for (int i = 0; i < amountThreads; i++) {
            Thread thread = new Thread() {
                @Override
                public void run() {
                    EventPublisherClass publisher = new EventPublisherClass();
                    for (int j = 0; j < amountEventsInThread; j++) {
                        publisher.publishEvent(eventType, "message");
                    }
                }
            };
            threads.add(thread);
            thread.start();
        }

        Thread listenerTwoThread = new Thread() {
            @Override
            public void run() {
                EventListener listenerTwo = new EventListener() {
                    @Override
                    public void handleEvent(String eventType, Object message) {
                        amountMessagesReceivedListenerTwo.getAndIncrement();
                    }
                };
                broker.addEventListener(eventType, listenerTwo);
            }
        };
        listenerTwoThread.start();

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        try {
            listenerTwoThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        broker.stop();

        int messagesOne = amountMessagesReceivedListenerOne.get();
        int messagesTwo = amountMessagesReceivedListenerTwo.get();

        assertEquals("Always running listener doesn't get all messages", amountThreads * amountEventsInThread, messagesOne);
        assertTrue("Intermediate started listener doesn't get some messagess", messagesTwo >= 0 && messagesTwo <= amountThreads * amountEventsInThread);
    }

    @Test
    public void eventBroker_supports2ListenersOfTheSameTime() {
        final String eventType = "2ListenersSameType";

        EventListener listener1 = new EventListener() {
            @Override
            public void handleEvent(String eventType, Object message) {
                // not relevant for this test
            }
        };

        EventListener listener2 = new EventListener() {
            @Override
            public void handleEvent(String eventType, Object message) {
                // not relevant for this test
            }
        };

        broker.addEventListener(eventType, listener1);
        broker.addEventListener(eventType, listener2);

        assertEquals("Two listeners were added, but there are not exactly 2 listeners present", 2, broker.getAmountOfListeners());

        broker.removeEventListener(eventType, listener1);
        broker.removeEventListener(eventType, listener2);
    }

    @Test
    public void removeListener_removeFromEmptyBroker_doesNotThrowException() {
        try {
            final String eventType = "emptyBrokerRemoveListener";
            EventListener listener = new EventListener() {
                @Override
                public void handleEvent(String eventType, Object message) {
                    // not relevant for this test
                }
            };
            broker.removeEventListener(eventType, listener);
        } catch (Exception e) {
            fail("Removing a listener from an empty broker throws: " + e.getClass());
        }
    }

    @Test
    public void addListener_removeListener_returnValues() {
        final String eventType = "addRemoveListenerReturnValues";

        EventListener listener = new EventListener() {
            @Override
            public void handleEvent(String eventType, Object message) {
                // not relevant for this test
            }
        };

        assertEquals("Adding first listener does not return 1", 1, broker.addEventListener(eventType, listener));
        assertEquals("Adding listener twice does not return -1", -1, broker.addEventListener(eventType, listener));
        assertEquals("Removing added listener does not return 1", 1, broker.removeEventListener(eventType, listener));
        assertEquals("Removing added listener twice does not return -1", -1, broker.removeEventListener(eventType, listener));
    }

    @Test
    public void addListenerMultipleTypes_receivesBothEvents() {
        final String eventType1 = "addListenerMultipletypes1";
        final String eventType2 = "addListenerMultipletypes2";
        final AtomicInteger amountMessagesReceivedTypeOne = new AtomicInteger(0);
        final AtomicInteger amountMessagesReceivedTypeTwo = new AtomicInteger(0);

        EventListener listener = new EventListener() {
            @Override
            public void handleEvent(String eventType, Object message) {
                if (eventType.equals(eventType1)) {
                    amountMessagesReceivedTypeOne.incrementAndGet();
                    return;
                }
                if (eventType.equals(eventType2)) {
                    amountMessagesReceivedTypeTwo.incrementAndGet();
                    return;
                }
                fail("Unregistered event type received");
            }
        };

        EventPublisherClass publisher = new EventPublisherClass();

        Set<String> types = new HashSet<>();
        types.add(eventType1);
        types.add(eventType2);
        broker.addEventListener(types, listener);
        broker.start();
        publisher.publishEvent(eventType1, "bla");
        publisher.publishEvent(eventType2, "bla");
        broker.stop();
        broker.removeEventListener(listener);
        assertEquals("Did not receive one message for a type that was added with a collection", 1, amountMessagesReceivedTypeOne.get());
        assertEquals("Did not receive one message for a type that was added with a collection", 1, amountMessagesReceivedTypeTwo.get());
    }

    @Test
    public void removingListenerForAllTypes_LeavesBrokerEmpty() {
        final String eventType1 = "removeListenerMultipletypes1";
        final String eventType2 = "removeListenerMultipletypes2";

        EventListener listener = new EventListener() {
            @Override
            public void handleEvent(String eventType, Object message) {
                // does not matter for this test
            }
        };

        List<String> types = new ArrayList<>();
        types.add(eventType1);
        types.add(eventType2);
        broker.addEventListener(types, listener);
        assertEquals("Adding one listener for multiple types does not return one present listener", 1, broker.getAmountOfListeners());
        broker.removeEventListener(listener);
        assertEquals("Removing all types for a listener does not leave the broker empty", 0, broker.getAmountOfListeners());
    }
}
