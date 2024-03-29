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
package org.ow2.proactive.scheduler.common;

import java.io.Serializable;

import org.objectweb.proactive.annotation.PublicAPI;


/**
 * NotificationData is used by the Scheduler Core to notify the front-end of any changes.
 * It is sent through notification methods in core interface.
 *
 * @param T the type of the data to be sent in the notification.
 *
 * @author The ProActive Team
 * @since ProActive Scheduling 0.9.1
 */
@PublicAPI
public class NotificationData<T extends Object> implements Serializable {

    /**  */
    private static final long serialVersionUID = 31L;
    /** Event type of the update */
    private SchedulerEvent eventType;
    /** The data to be sent in the update */
    private T data;

    /**
     * Create a new instance of NotificationData.
     *
     * @param eventType the Type of the event.
     * @param data the data contained in the notification
     */
    public NotificationData(SchedulerEvent eventType, T data) {
        this.eventType = eventType;
        this.data = data;
    }

    /**
     * Get the Type of the event.
     *
     * @return the Type of the event.
     */
    public SchedulerEvent getEventType() {
        return eventType;
    }

    /**
     * Get the data.
     *
     * @return the data.
     */
    public T getData() {
        return data;
    }

}
