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
 *  Contributor(s): ActiveEon Team - http://www.activeeon.com
 *
 * ################################################################
 * $$ACTIVEEON_CONTRIBUTOR$$
 */
package org.ow2.proactive.scheduler.core;

import java.util.TimerTask;

import org.apache.log4j.Logger;
import org.objectweb.proactive.core.util.log.ProActiveLogger;
import org.ow2.proactive.scheduler.job.InternalJob;
import org.ow2.proactive.scheduler.task.internal.InternalTask;
import org.ow2.proactive.scheduler.util.SchedulerDevLoggers;


/**
 * RestartJobTimerTask is used to manage the timeout from which a task can be restarted after an error.
 * This timeout will increase according to a function defined in the job.
 *
 * @author The ProActiveTeam
 * @since ProActiveScheduling 0.9
 *
 * $Id$
 */
public class RestartJobTimerTask extends TimerTask {

    private static final Logger logger_dev = ProActiveLogger.getLogger(SchedulerDevLoggers.SCHEDULE);
    /** The job on which to restart the task */
    private InternalJob job;
    /** The task that have to be restarted */
    private InternalTask task;

    /**
     * Create a new instance of RestartJobTimerTask using given job and task.
     *
     * @param job The job on which to restart the task
     * @param task The task that have to be restarted
     */
    public RestartJobTimerTask(InternalJob job, InternalTask task) {
        super();
        this.job = job;
        this.task = task;
    }

    /**
     * @see java.util.TimerTask#run()
     */
    @Override
    public void run() {
        try {
            job.reStartTask(task);
        } catch (Throwable t) {
            logger_dev.fatal("Error while restarting task ! Job '" + job.getId() +
                "' should be killed manually !", t);
        }
    }

}
