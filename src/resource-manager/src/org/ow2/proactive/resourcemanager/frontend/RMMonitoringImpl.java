/*
 * ################################################################
 *
 * ProActive Parallel Suite(TM): The Java(TM) library for
 *    Parallel, Distributed, Multi-Core Computing for
 *    Enterprise Grids & Clouds
 *
 * Copyright (C) 1997-2011 INRIA/University of
 *                 Nice-Sophia Antipolis/ActiveEon
 * Contact: proactive@ow2.org or contact@activeeon.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; version 3 of
 * the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 * USA
 *
 * If needed, contact us to obtain a release under GPL Version 2 or 3
 * or a different license than the AGPL.
 *
 *  Initial developer(s):               The ProActive Team
 *                        http://proactive.inria.fr/team_members.htm
 *  Contributor(s):
 *
 * ################################################################
 * $$PROACTIVE_INITIAL_DEV$$
 */
package org.ow2.proactive.resourcemanager.frontend;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;
import org.objectweb.proactive.Body;
import org.objectweb.proactive.InitActive;
import org.objectweb.proactive.api.PAActiveObject;
import org.objectweb.proactive.core.ProActiveException;
import org.objectweb.proactive.core.UniqueID;
import org.objectweb.proactive.core.util.log.ProActiveLogger;
import org.objectweb.proactive.core.util.wrapper.BooleanWrapper;
import org.objectweb.proactive.extensions.annotation.ActiveObject;
import org.ow2.proactive.resourcemanager.common.RMConstants;
import org.ow2.proactive.resourcemanager.common.event.RMEvent;
import org.ow2.proactive.resourcemanager.common.event.RMEventType;
import org.ow2.proactive.resourcemanager.common.event.RMInitialState;
import org.ow2.proactive.resourcemanager.common.event.RMNodeEvent;
import org.ow2.proactive.resourcemanager.common.event.RMNodeSourceEvent;
import org.ow2.proactive.resourcemanager.core.RMCore;
import org.ow2.proactive.resourcemanager.core.jmx.RMJMXHelper;
import org.ow2.proactive.resourcemanager.core.properties.PAResourceManagerProperties;
import org.ow2.proactive.resourcemanager.exception.RMException;
import org.ow2.proactive.resourcemanager.utils.AtomicRMStatisticsHolder;
import org.ow2.proactive.resourcemanager.utils.RMLoggers;


/**
 * Active object designed for the Monitoring of the Resource Manager.
 * This class provides a way for a monitor to ask at
 * Resource Manager to throw events
 * generated by nodes and nodes sources management. RMMonitoring dispatch
 * events thrown by {@link RMCore} to all its monitors.
 *
 *
 * @see org.ow2.proactive.resourcemanager.frontend.RMEventListener
 *
 * @author The ProActive Team
 * @since ProActive Scheduling 0.9
 */
@ActiveObject
public class RMMonitoringImpl implements RMMonitoring, RMEventListener, InitActive {
    private static final Logger logger = ProActiveLogger.getLogger(RMLoggers.MONITORING);

    // Attributes
    private RMCore rmcore;
    private Map<UniqueID, RMEventListener> listeners;
    private HashMap<EventDispatcher, LinkedList<RMEvent>> pendingEvents;
    private transient ExecutorService eventDispatcherThreadPool;

    /** Resource Manager's statistics */
    public static final AtomicRMStatisticsHolder rmStatistics = new AtomicRMStatisticsHolder();

    // ----------------------------------------------------------------------//
    // CONSTRUTORS

    /** ProActive empty constructor */
    public RMMonitoringImpl() {
    }

    /**
     * Creates the RMMonitoring active object.
     * @param rmcore Stub of the RMCore active object.
     */
    public RMMonitoringImpl(RMCore rmcore) {
        this.listeners = new HashMap<UniqueID, RMEventListener>();
        this.rmcore = rmcore;
        this.pendingEvents = new HashMap<EventDispatcher, LinkedList<RMEvent>>();
    }

    /**
     * @see org.objectweb.proactive.InitActive#initActivity(org.objectweb.proactive.Body)
     */
    public void initActivity(Body body) {
        try {
            PAActiveObject.registerByName(PAActiveObject.getStubOnThis(),
                    RMConstants.NAME_ACTIVE_OBJECT_RMMONITORING);
            eventDispatcherThreadPool = Executors
                    .newFixedThreadPool(PAResourceManagerProperties.RM_MONITORING_MAX_THREAD_NUMBER
                            .getValueAsInt());

        } catch (ProActiveException e) {
            logger.debug("Cannot register RMMonitoring. Aborting...", e);
            PAActiveObject.terminateActiveObject(true);
        }
    }

    private class EventDispatcher implements Runnable {

        private UniqueID listenerId;
        private ReentrantLock lock = new ReentrantLock();

        public EventDispatcher(UniqueID listenerId) {
            this.listenerId = listenerId;
        }

        public void run() {
            if (lock.isLocked()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Communication to the client " + listenerId.shortString() +
                        " is in progress in one thread of the thread pool. " +
                        "Either events come too quick or the client is slow. " +
                        "Do not initiate connection from another thread.");
                }
                return;
            }
            lock.lock();

            int numberOfEventDelivered = 0;
            long timeStamp = System.currentTimeMillis();
            if (logger.isDebugEnabled()) {
                logger.debug("Initializing " + Thread.currentThread() + " for events delivery to client '" +
                    listenerId.shortString() + "'");
            }

            LinkedList<RMEvent> events = pendingEvents.get(this);
            while (true) {
                RMEvent event = null;
                synchronized (events) {
                    if (logger.isDebugEnabled()) {
                        logger.debug(events.size() + " pending events for the client '" +
                            listenerId.shortString() + "'");
                    }
                    if (events.size() > 0) {
                        event = events.removeFirst();
                    }
                }

                if (event != null) {
                    deliverEvent(event);
                    numberOfEventDelivered++;
                } else {
                    lock.unlock();
                    break;
                }
            }

            if (logger.isDebugEnabled()) {
                logger.debug("Finnishing delivery in " + Thread.currentThread() + " to client '" +
                    listenerId.shortString() + "'. " + numberOfEventDelivered + " events were delivered in " +
                    (System.currentTimeMillis() - timeStamp) + " ms");
            }
        }

        private void deliverEvent(RMEvent event) {

            RMEventListener listener = null;

            synchronized (listeners) {
                listener = listeners.get(listenerId);
            }

            if (listener == null) {
                return;
            }

            //dispatch event
            long timeStamp = System.currentTimeMillis();

            if (logger.isDebugEnabled()) {
                logger.debug("Dispatching event '" + event.toString() + "' to client " +
                    listenerId.shortString());
            }
            try {
                if (event instanceof RMNodeEvent) {
                    RMNodeEvent nodeEvent = (RMNodeEvent) event;
                    listener.nodeEvent(nodeEvent);
                } else if (event instanceof RMNodeSourceEvent) {
                    RMNodeSourceEvent sourceEvent = (RMNodeSourceEvent) event;
                    listener.nodeSourceEvent(sourceEvent);
                } else {
                    listener.rmEvent(event);
                }

                long time = System.currentTimeMillis() - timeStamp;
                if (logger.isDebugEnabled()) {
                    logger.debug("Event '" + event.toString() + "' has been delivered to client " +
                        listenerId.shortString() + " in " + time + " ms");
                }

            } catch (Exception e) {
                // probably listener was removed or became down
                RMEventListener l = null;
                synchronized (listeners) {
                    l = listeners.remove(listenerId);
                    pendingEvents.remove(this);
                }
                if (l != null) {
                    logger.debug("", e);
                    logger.info("User known as '" + listenerId.shortString() +
                        "' has been removed from listeners");
                }
            }
        }

        public boolean equals(Object obj) {
            EventDispatcher other = (EventDispatcher) obj;
            return listenerId.equals(other.listenerId);
        }
    }

    /** Register a new Resource manager listener.
     * Way to a monitor object to ask at RMMonitoring to throw
     * RM events to it.
     * @param listener a listener object which implements {@link RMEventListener}
     * interface.
     * @param events list of wanted events that must be received.
     * @return RMInitialState snapshot of RM's current state : nodes and node sources.
     *  */
    public RMInitialState addRMEventListener(RMEventListener listener, RMEventType... events) {
        UniqueID id = PAActiveObject.getContext().getCurrentRequest().getSourceBodyID();

        synchronized (listeners) {
            this.listeners.put(id, listener);
            this.pendingEvents.put(new EventDispatcher(id), new LinkedList<RMEvent>());
        }
        return rmcore.getRMInitialState();
    }

    /**
     * Removes a listener from RMMonitoring. Only listener itself must call this method
     */
    public void removeRMEventListener() throws RMException {
        UniqueID id = PAActiveObject.getContext().getCurrentRequest().getSourceBodyID();
        synchronized (listeners) {
            if (listeners.containsKey(id)) {
                listeners.remove(id);
                pendingEvents.remove(new EventDispatcher(id));
            } else {
                throw new RMException("Listener is unknown");
            }
        }
    }

    @Deprecated
    public boolean isAlive() {
        return true;
    }

    public void queueEvent(RMEvent event) {
        //dispatch event
        if (logger.isDebugEnabled()) {
            logger.debug("Queueing event '" + event.toString() + "'");
        }

        synchronized (listeners) {
            for (Collection<RMEvent> events : pendingEvents.values()) {
                synchronized (events) {
                    events.add(event);
                }
            }
            for (Runnable eventDispatcher : pendingEvents.keySet()) {
                eventDispatcherThreadPool.submit(eventDispatcher);
            }
        }
    }

    /**
     * @see org.ow2.proactive.resourcemanager.frontend.RMEventListener#nodeEvent(org.ow2.proactive.resourcemanager.common.event.RMNodeEvent)
     */
    public void nodeEvent(RMNodeEvent event) {
        RMMonitoringImpl.rmStatistics.nodeEvent(event);
        queueEvent(event);
    }

    /**
     * @see org.ow2.proactive.resourcemanager.frontend.RMEventListener#nodeSourceEvent(org.ow2.proactive.resourcemanager.common.event.RMNodeSourceEvent)
     */
    public void nodeSourceEvent(RMNodeSourceEvent event) {
        queueEvent(event);
    }

    /**
     * @see org.ow2.proactive.resourcemanager.frontend.RMEventListener#rmEvent(org.ow2.proactive.resourcemanager.common.event.RMEvent)
     */
    public void rmEvent(RMEvent event) {
        RMMonitoringImpl.rmStatistics.rmEvent(event);
        queueEvent(event);
    }

    /** 
     * Stop and remove monitoring active object
     */
    public BooleanWrapper shutdown() {
        //throwing shutdown event
        rmEvent(new RMEvent(RMEventType.SHUTDOWN));
        PAActiveObject.terminateActiveObject(false);

        RMJMXHelper.getInstance().shutdown();
        // initiating shutdown
        eventDispatcherThreadPool.shutdown();
        try {
            // waiting until all clients will be notified
            eventDispatcherThreadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            logger.warn("", e);
        }

        return new BooleanWrapper(true);
    }

    public Logger getLogger() {
        return logger;
    }

    /**
     * Gets the current snapshot of the resource manager state providing
     * detailed nodes and node source information.
     *
     * To obtain summary of the resource manager state use {@link ResourceManager}.getState()
     *
     * @return the current state of the resource manager
     */
    public RMInitialState getState() {
        return rmcore.getRMInitialState();
    }
}
