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
package functionaltests.nodestate;

import static junit.framework.Assert.assertTrue;
import junit.framework.Assert;

import org.objectweb.proactive.api.PAFuture;
import org.objectweb.proactive.core.ProActiveTimeoutException;
import org.objectweb.proactive.core.node.NodeFactory;
import org.objectweb.proactive.core.util.ProActiveInet;
import org.objectweb.proactive.core.util.wrapper.BooleanWrapper;
import org.ow2.proactive.resourcemanager.common.NodeState;
import org.ow2.proactive.resourcemanager.common.event.RMEventType;
import org.ow2.proactive.resourcemanager.common.event.RMNodeEvent;
import org.ow2.proactive.resourcemanager.core.properties.PAResourceManagerProperties;
import org.ow2.proactive.resourcemanager.frontend.ResourceManager;
import org.ow2.proactive.resourcemanager.nodesource.NodeSource;
import org.ow2.proactive.resourcemanager.nodesource.infrastructure.DefaultInfrastructureManager;
import org.ow2.proactive.resourcemanager.nodesource.policy.StaticPolicy;
import org.ow2.proactive.utils.NodeSet;

import org.ow2.tests.FunctionalTest;
import functionaltests.RMTHelper;


/**
 * This class tests different cases of adding an already deployed
 * (i.e. not deployed by Resource Manager) node to the resource Manager
 * and removal of these already deployed nodes
 *
 * simply add a node (test 1)
 * simply remove an already deployed node (test 2)
 * add a node, kill this node, node is detected down, and add a node that has the same URL (test 3).
 *
 * For the next tests, we put a big ping frequency in order to avoid detection of failed nodes,
 * in order to test the replacement of a node by another with the same URL.
 *
 * add a node, keep this node free, kill this node, and add a node that has the same URL (test 4).
 * add a node, put this node busy, kill this node, and add a node that has the same URL (test 5).
 * add a node, put this node toRelease, kill this node, and add a node that has the same URL (test 6).
 *
 * @author ProActive team
 *
 */
public class TestAdminAddingNodes extends FunctionalTest {

    /** Actions to be Perform by this test.
     * The method is called automatically by Junit framework.
     * @throws Exception If the test fails.
     */
    @org.junit.Test
    public void action() throws Exception {

        String hostName = ProActiveInet.getInstance().getHostname();

        int pingFrequency = 5000;
        ResourceManager resourceManager = RMTHelper.getResourceManager();

        resourceManager.createNodeSource(NodeSource.DEFAULT, DefaultInfrastructureManager.class.getName(),
                null, StaticPolicy.class.getName(), null);
        RMTHelper.waitForNodeSourceEvent(RMEventType.NODESOURCE_CREATED, NodeSource.DEFAULT);

        resourceManager.setNodeSourcePingFrequency(pingFrequency, NodeSource.DEFAULT);

        RMTHelper.log("Test 1");
        String node1Name = "node1";
        String node1URL = "//" + hostName + "/" + node1Name;
        RMTHelper.createNode(node1Name);

        resourceManager.addNode(node1URL, NodeSource.DEFAULT);

        RMTHelper.waitForNodeEvent(RMEventType.NODE_ADDED, node1URL);
        //wait for the node to be in free state
        RMTHelper.waitForAnyNodeEvent(RMEventType.NODE_STATE_CHANGED);

        assertTrue(resourceManager.getState().getTotalNodesNumber() == 1);
        assertTrue(resourceManager.getState().getTotalAliveNodesNumber() == 1);
        assertTrue(resourceManager.getState().getFreeNodesNumber() == 1);

        RMTHelper.log("Test 2");

        //preemptive removal is useless for this case, because node is free
        resourceManager.removeNode(node1URL, false);

        RMTHelper.waitForNodeEvent(RMEventType.NODE_REMOVED, node1URL);

        assertTrue(resourceManager.getState().getTotalNodesNumber() == 0);
        assertTrue(resourceManager.getState().getTotalAliveNodesNumber() == 0);
        assertTrue(resourceManager.getState().getFreeNodesNumber() == 0);

        RMTHelper.log("Test 3");
        String node2Name = "node2";
        String node2URL = "//" + hostName + "/" + node2Name;
        RMTHelper.createNode(node2Name);

        resourceManager.addNode(node2URL, NodeSource.DEFAULT);

        //wait the node added event
        RMTHelper.waitForNodeEvent(RMEventType.NODE_ADDED, node2URL);
        //wait for the node to be in free state
        RMTHelper.waitForAnyNodeEvent(RMEventType.NODE_STATE_CHANGED);
        assertTrue(resourceManager.getState().getTotalNodesNumber() == 1);
        assertTrue(resourceManager.getState().getFreeNodesNumber() == 1);
        assertTrue(resourceManager.getState().getTotalAliveNodesNumber() == 1);

        //kill the node
        RMTHelper.killNode(node2URL);

        RMNodeEvent evt = RMTHelper.waitForNodeEvent(RMEventType.NODE_STATE_CHANGED, node2URL);

        Assert.assertEquals(evt.getNodeState(), NodeState.DOWN);
        //wait the node down event
        Assert.assertEquals(resourceManager.getState().getTotalNodesNumber(), 1);
        assertTrue(resourceManager.getState().getFreeNodesNumber() == 0);
        assertTrue(resourceManager.getState().getTotalAliveNodesNumber() == 0);

        //create another node with the same URL, and add it to Resource manager
        RMTHelper.createNode(node2Name);
        resourceManager.addNode(node2URL, NodeSource.DEFAULT);

        //wait for removal of the previous down node with the same URL
        RMTHelper.waitForNodeEvent(RMEventType.NODE_REMOVED, node2URL);

        //wait the node added event
        RMTHelper.waitForNodeEvent(RMEventType.NODE_ADDED, node2URL);
        //wait for the node to be in free state
        RMTHelper.waitForAnyNodeEvent(RMEventType.NODE_STATE_CHANGED);

        Assert.assertEquals(resourceManager.getState().getTotalNodesNumber(), 1);
        Assert.assertEquals(resourceManager.getState().getFreeNodesNumber(), 1);
        Assert.assertEquals(resourceManager.getState().getTotalAliveNodesNumber(), 1);

        RMTHelper.log("Test 4");

        //put a large ping frequency in order to avoid down nodes detection
        resourceManager.setNodeSourcePingFrequency(Integer.MAX_VALUE, NodeSource.DEFAULT);

        //wait the end of last ping sequence
        Thread.sleep(PAResourceManagerProperties.RM_NODE_SOURCE_PING_FREQUENCY.getValueAsInt() + 500);

        //node2 is free, kill the node
        RMTHelper.killNode(node2URL);

        //create another node with the same URL, and add it to Resource manager
        RMTHelper.createNode(node2Name);
        resourceManager.addNode(node2URL, NodeSource.DEFAULT);

        //wait for removal of the previous free node with the same URL
        RMTHelper.waitForNodeEvent(RMEventType.NODE_REMOVED, node2URL);

        try {
            NodeFactory.getNode(node2URL);
        } catch (Exception e) {
            assertTrue("Runtime of the new node was killed", false);
        }

        //wait the node added event, node added is configuring
        RMTHelper.waitForNodeEvent(RMEventType.NODE_ADDED, node2URL);
        //wait for the node to be in free state
        RMTHelper.waitForAnyNodeEvent(RMEventType.NODE_STATE_CHANGED);
        Assert.assertEquals(resourceManager.getState().getTotalNodesNumber(), 1);
        Assert.assertEquals(resourceManager.getState().getFreeNodesNumber(), 1);

        RMTHelper.log("Test 5");

        //put the the node to busy state
        NodeSet nodes = resourceManager.getAtMostNodes(1, null);
        PAFuture.waitFor(nodes);

        //wait the node busy event
        evt = RMTHelper.waitForNodeEvent(RMEventType.NODE_STATE_CHANGED, node2URL);
        Assert.assertEquals(evt.getNodeState(), NodeState.BUSY);

        Assert.assertEquals(resourceManager.getState().getTotalNodesNumber(), 1);
        Assert.assertEquals(resourceManager.getState().getFreeNodesNumber(), 0);

        //node2 is busy, kill the node
        RMTHelper.killNode(node2URL);

        //create another node with the same URL, and add it to Resource manager
        RMTHelper.createNode(node2Name);
        resourceManager.addNode(node2URL, NodeSource.DEFAULT);

        //wait for removal of the previous free node with the same URL
        RMTHelper.waitForNodeEvent(RMEventType.NODE_REMOVED, node2URL);
        try {
            NodeFactory.getNode(node2URL);
        } catch (Exception e) {
            assertTrue("Runtime of the new node was killed", false);
        }

        //wait the node added event, node added is configuring
        RMTHelper.waitForNodeEvent(RMEventType.NODE_ADDED, node2URL);
        //wait for the node to be in free state
        RMTHelper.waitForAnyNodeEvent(RMEventType.NODE_STATE_CHANGED);
        assertTrue(resourceManager.getState().getTotalNodesNumber() == 1);
        assertTrue(resourceManager.getState().getFreeNodesNumber() == 1);

        RMTHelper.log("Test 6");

        //put the the node to busy state
        nodes = resourceManager.getAtMostNodes(1, null);
        PAFuture.waitFor(nodes);

        //wait the node busy event
        evt = RMTHelper.waitForNodeEvent(RMEventType.NODE_STATE_CHANGED, node2URL);
        Assert.assertEquals(evt.getNodeState(), NodeState.BUSY);

        assertTrue(resourceManager.getState().getTotalNodesNumber() == 1);
        assertTrue(resourceManager.getState().getFreeNodesNumber() == 0);

        //put the node in to Release state
        resourceManager.removeNode(node2URL, false);

        //wait the node to release event
        evt = RMTHelper.waitForNodeEvent(RMEventType.NODE_STATE_CHANGED, node2URL);
        Assert.assertEquals(evt.getNodeState(), NodeState.TO_BE_REMOVED);

        assertTrue(resourceManager.getState().getTotalNodesNumber() == 1);
        assertTrue(resourceManager.getState().getFreeNodesNumber() == 0);

        //node2 is to release, kill the node
        RMTHelper.killNode(node2URL);

        //create another node with the same URL, and add it to Resource manager
        RMTHelper.createNode(node2Name);
        resourceManager.addNode(node2URL, NodeSource.DEFAULT);

        //wait for removal of the previous down node with the same URL
        RMTHelper.waitForNodeEvent(RMEventType.NODE_REMOVED, node2URL);
        try {
            NodeFactory.getNode(node2URL);
        } catch (Exception e) {
            assertTrue("Runtime of the new node was killed", false);
        }

        //wait the node added event, node added is configuring
        RMTHelper.waitForNodeEvent(RMEventType.NODE_ADDED, node2URL);
        //wait for the node to be in free state
        RMTHelper.waitForAnyNodeEvent(RMEventType.NODE_STATE_CHANGED);
        assertTrue(resourceManager.getState().getTotalNodesNumber() == 1);
        assertTrue(resourceManager.getState().getFreeNodesNumber() == 1);

        RMTHelper.log("Test 7");

        //add the same node twice and check that RM will not kill the node. If it does
        //second attempt will fail
        BooleanWrapper result = resourceManager.addNode(node2URL, NodeSource.DEFAULT);
        if (result.getBooleanValue()) {
            assertTrue("Successfully added the same node twice - incorrect", false);
        }

        boolean timeouted = false;
        try {
            RMTHelper.waitForNodeEvent(RMEventType.NODE_ADDED, node2URL, 5000);
        } catch (ProActiveTimeoutException e) {
            timeouted = true;
        }

        assertTrue(timeouted);
    }
}
