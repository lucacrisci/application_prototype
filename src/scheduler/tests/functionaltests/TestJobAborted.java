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
package functionaltests;

import java.io.File;
import java.net.URL;
import java.util.Map;

import org.junit.Assert;
import org.ow2.proactive.scheduler.common.job.Job;
import org.ow2.proactive.scheduler.common.job.JobId;
import org.ow2.proactive.scheduler.common.job.JobInfo;
import org.ow2.proactive.scheduler.common.job.JobResult;
import org.ow2.proactive.scheduler.common.job.JobStatus;
import org.ow2.proactive.scheduler.common.task.TaskResult;

import org.ow2.tests.FunctionalTest;


/**
 * This class tests the cancelJobOnError feature of a task.
 * It will start 2 tasks.
 * The first task last 10s, the second one will throw an exception after 3 seconds.
 * This last task has its flag 'cancelJobOnError' as true.
 * The goal is to check that this job will terminate with the exception.
 * The first task must be canceled.
 * The number of finish task will be 1 at the end.
 *
 * @author The ProActive Team
 * @date 2 jun 08
 * @since ProActive Scheduling 1.0
 */
public class TestJobAborted extends FunctionalTest {

    private static URL jobDescriptor = TestJobAborted.class
            .getResource("/functionaltests/descriptors/Job_Aborted.xml");

    /**
     * Tests start here.
     *
     * @throws Throwable any exception that can be thrown during the test.
     */
    @org.junit.Test
    public void run() throws Throwable {

        String task1Name = "task1";
        String task2Name = "task2";

        SchedulerTHelper.log("Test 1 : Submitting job...");

        //job submission
        JobId id = SchedulerTHelper.submitJob(new File(jobDescriptor.toURI()).getAbsolutePath());

        //check events reception
        SchedulerTHelper.log("Job submitted, id " + id.toString());

        SchedulerTHelper.log("Waiting for jobSubmitted");

        Job receivedJob = SchedulerTHelper.waitForEventJobSubmitted(id);
        Assert.assertEquals(receivedJob.getId(), id);

        SchedulerTHelper.log("Waiting for job running");
        JobInfo jInfo = SchedulerTHelper.waitForEventJobRunning(id);
        Assert.assertEquals(jInfo.getJobId(), id);

        SchedulerTHelper.log("Waiting for task running : " + task1Name);
        SchedulerTHelper.waitForEventTaskRunning(id, task1Name);
        SchedulerTHelper.log("Waiting for task running : " + task2Name);
        SchedulerTHelper.waitForEventTaskRunning(id, task2Name);

        SchedulerTHelper.log("Waiting for task finished : " + task2Name);
        SchedulerTHelper.waitForEventTaskFinished(id, task2Name);

        SchedulerTHelper.log("Waiting for job finished");
        jInfo = SchedulerTHelper.waitForEventJobFinished(id);
        Assert.assertEquals(JobStatus.CANCELED, jInfo.getStatus());

        SchedulerTHelper.log("Test 7 : Getting job result...");
        JobResult res = SchedulerTHelper.getJobResult(id);

        Map<String, TaskResult> results = res.getAllResults();

        //check that number of results correspond to 1
        Assert.assertEquals(1, results.size());
        Assert.assertNotNull(results.get("task2").getException());

        //remove jobs and check its event
        SchedulerTHelper.removeJob(id);
        SchedulerTHelper.log("Waiting for job removed");
        jInfo = SchedulerTHelper.waitForEventJobRemoved(id);
        Assert.assertEquals(JobStatus.CANCELED, jInfo.getStatus());
        Assert.assertEquals(jInfo.getJobId(), id);
    }
}
