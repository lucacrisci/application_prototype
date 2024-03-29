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
 *  Initial developer(s):               The ActiveEon Team
 *                        http://www.activeeon.com/
 *  Contributor(s):
 *
 * ################################################################
 * $$ACTIVEEON_INITIAL_DEV$$
 */
package functionaltests.workflow;

import org.ow2.proactive.scheduler.common.task.flow.FlowActionType;


/**
 * Tests the correctness of workflow-controlled jobs including {@link FlowActionType#IF} actions
 * 
 * 
 * @author The ProActive Team
 * @since ProActive Scheduling 2.2
 */
public class TestWorkflowIfJobs extends TWorkflowJobs {

    @Override
    public final String[][] getJobs() {
        return new String[][] {
        // 1: if/else: single task; no continuation
                { "T 0", "T1 1", "T2 -1" },

                // 2 : if/else: task block; continuation
                { "T 0", "T1 1", "T3 2", "T5 3", "T2 -1", "T4 -1" },

                // 3 : if: task block w/ nested if/else single task; no continuation
                { "T 0", "T1 1", "T2 -1", "T3 -1", "T4 2", "T5 -1", "T6 3", "T7 4" },

                // 4 : if: task block; else: task w/ nested if: task block; continuation
                { "T 0", "T1 -1", "T2 -1", "T3 1", "T4 2", "T5 3", "T6 4", "T7 -1", "T8 5", "T9 6" },

                // 5 : nested if depth 3 with continuation
                { "T 0", "T2 -1", "T4 -1", "T1 1", "T6 2", "T8 -1", "T10 -1", "T9 3", "T12 4", "T17 -1",
                        "T14 5", "T16 6", "T15 12", "T13 13", "T11 14", "T7 15", "T3 16", "T5 17" }, };
    }

    @Override
    public final String getJobPrefix() {
        return "/functionaltests/workflow/descriptors/flow_if_";
    }

    @org.junit.Test
    public void run() throws Throwable {
        internalRun();
    }

}
