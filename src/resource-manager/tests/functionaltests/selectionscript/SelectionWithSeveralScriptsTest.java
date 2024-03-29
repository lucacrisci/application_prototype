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
package functionaltests.selectionscript;

import static junit.framework.Assert.assertTrue;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import junit.framework.Assert;

import org.objectweb.proactive.api.PAFuture;
import org.ow2.proactive.resourcemanager.common.NodeState;
import org.ow2.proactive.resourcemanager.common.event.RMEventType;
import org.ow2.proactive.resourcemanager.common.event.RMNodeEvent;
import org.ow2.proactive.resourcemanager.frontend.ResourceManager;
import org.ow2.proactive.resourcemanager.nodesource.NodeSource;
import org.ow2.proactive.scripting.SelectionScript;
import org.ow2.proactive.utils.NodeSet;

import org.ow2.tests.FunctionalTest;
import functionaltests.RMTHelper;


/**
 *
 * This class tests RM's mechanism of resource selection with static
 * scripts.
 *
 * -launch nodes with a specified JVM environment variable => verify script
 * -launch 5 nodes without this specified JVM environment variable => not verify script
 *
 *  1/ ask 1 node with a selection that check this environment variable,
 *  and verify that a node can be provided by RM
 *  2/ ask 3 nodes with specific environment var, and check that just one node can be provided
 *  3/ add a second node with specific JVM env var and ask 3 nodes with specific environment var,
 *   and check that two nodes can be provided with one getAtMostNodes method call.
 *  4/ remove the node with specified environment var, end check that no node
 *  can be provided
 *  5/ ask a node with a selection script that provides execution error,
 *  and check that error handling is correct.
 *  6/ ask a node with a selection script that doesn't return
 *  the 'selected' return value
 *
 * @author ProActive team
 *
 */
public class SelectionWithSeveralScriptsTest extends FunctionalTest {

    private URL vmPropSelectionScriptpath = this.getClass().getResource("vmPropertySelectionScript.js");

    private String vmPropKey1 = "myProperty1";
    private String vmPropValue1 = "myValue1";

    private String vmPropKey2 = "myProperty2";
    private String vmPropValue2 = "myValue2";

    /** Actions to be Perform by this test.
    * The method is called automatically by Junit framework.
    * @throws Exception If the test fails.
    */
    @org.junit.Test
    public void action() throws Exception {

        RMTHelper.log("Deployment");
        RMTHelper.createDefaultNodeSource();
        RMTHelper.waitForNodeSourceEvent(RMEventType.NODESOURCE_CREATED, NodeSource.DEFAULT);

        ResourceManager resourceManager = RMTHelper.getResourceManager();

        for (int i = 0; i < RMTHelper.defaultNodesNumber; i++) {
            RMTHelper.waitForAnyNodeEvent(RMEventType.NODE_ADDED);
            //wait for the nodes to be in free state
            RMTHelper.waitForAnyNodeEvent(RMEventType.NODE_STATE_CHANGED);
        }

        String node1Name = "node1";
        String node2Name = "node2";
        String node3Name = "node3";
        //---------------------------------------------------
        //create a first node with the two VM properties
        //---------------------------------------------------

        HashMap<String, String> vmTwoProperties = new HashMap<String, String>();
        vmTwoProperties.put(this.vmPropKey1, this.vmPropValue1);
        vmTwoProperties.put(this.vmPropKey2, this.vmPropValue2);

        String node1URL = RMTHelper.createNode(node1Name, vmTwoProperties).getNodeInformation().getURL();
        resourceManager.addNode(node1URL);

        //wait node adding event
        RMTHelper.waitForNodeEvent(RMEventType.NODE_ADDED, node1URL);
        //wait for the nodes to be in free state
        RMTHelper.waitForAnyNodeEvent(RMEventType.NODE_STATE_CHANGED);

        //--------------------------------------------------
        //create a second node with only the first VM property
        //---------------------------------------------------

        HashMap<String, String> vmProp1 = new HashMap<String, String>();
        vmProp1.put(this.vmPropKey1, this.vmPropValue1);

        String node2URL = RMTHelper.createNode(node2Name, vmProp1).getNodeInformation().getURL();
        resourceManager.addNode(node2URL);

        //wait node adding event
        RMTHelper.waitForNodeEvent(RMEventType.NODE_ADDED, node2URL);
        //wait for the nodes to be in free state
        RMTHelper.waitForAnyNodeEvent(RMEventType.NODE_STATE_CHANGED);

        //--------------------------------------------------
        //create a third node with only the second VM property
        //---------------------------------------------------

        HashMap<String, String> vmProp2 = new HashMap<String, String>();
        vmProp1.put(this.vmPropKey2, this.vmPropValue2);

        String node3URL = RMTHelper.createNode(node3Name, vmProp2).getNodeInformation().getURL();
        resourceManager.addNode(node3URL);

        //wait node adding event
        RMTHelper.waitForNodeEvent(RMEventType.NODE_ADDED, node3URL);
        //wait for the nodes to be in free state
        RMTHelper.waitForAnyNodeEvent(RMEventType.NODE_STATE_CHANGED);

        //create the static selection script object that check vm prop1
        SelectionScript sScript1 = new SelectionScript(new File(vmPropSelectionScriptpath.toURI()),
            new String[] { this.vmPropKey1, this.vmPropValue1 }, true);

        //create the static selection script object prop2
        SelectionScript sScript2 = new SelectionScript(new File(vmPropSelectionScriptpath.toURI()),
            new String[] { this.vmPropKey2, this.vmPropValue2 }, false);

        RMTHelper.log("Test 1");

        ArrayList<SelectionScript> scriptsList = new ArrayList<SelectionScript>();

        scriptsList.add(sScript1);
        scriptsList.add(sScript2);

        NodeSet nodes = resourceManager.getAtMostNodes(RMTHelper.defaultNodesNumber, scriptsList, null);

        //wait node selection
        PAFuture.waitFor(nodes);

        assertTrue(nodes.size() == 1);
        assertTrue(nodes.get(0).getNodeInformation().getURL().equals(node1URL));

        //wait for node busy event
        RMNodeEvent evt = RMTHelper.waitForNodeEvent(RMEventType.NODE_STATE_CHANGED, node1URL);
        Assert.assertEquals(evt.getNodeState(), NodeState.BUSY);

        assertTrue(resourceManager.getState().getFreeNodesNumber() == RMTHelper.defaultNodesNumber + 2);

        resourceManager.releaseNodes(nodes);
        //wait for node free event
        evt = RMTHelper.waitForNodeEvent(RMEventType.NODE_STATE_CHANGED, node1URL);
        Assert.assertEquals(evt.getNodeState(), NodeState.FREE);

        assertTrue(resourceManager.getState().getFreeNodesNumber() == RMTHelper.defaultNodesNumber + 3);

        RMTHelper.log("Test 2");

        nodes = resourceManager.getAtMostNodes(RMTHelper.defaultNodesNumber, scriptsList, nodes);

        //wait node selection
        PAFuture.waitFor(nodes);

        assertTrue(nodes.size() == 0);
        assertTrue(resourceManager.getState().getFreeNodesNumber() == RMTHelper.defaultNodesNumber + 3);

    }
}
