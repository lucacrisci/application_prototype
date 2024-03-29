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
package org.ow2.proactive.resourcemanager.core;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.objectweb.proactive.ActiveObjectCreationException;
import org.objectweb.proactive.Body;
import org.objectweb.proactive.InitActive;
import org.objectweb.proactive.RunActive;
import org.objectweb.proactive.Service;
import org.objectweb.proactive.api.PAActiveObject;
import org.objectweb.proactive.api.PAFuture;
import org.objectweb.proactive.core.ProActiveException;
import org.objectweb.proactive.core.UniqueID;
import org.objectweb.proactive.core.body.request.Request;
import org.objectweb.proactive.core.node.Node;
import org.objectweb.proactive.core.node.NodeException;
import org.objectweb.proactive.core.util.log.ProActiveLogger;
import org.objectweb.proactive.core.util.wrapper.BooleanWrapper;
import org.objectweb.proactive.core.util.wrapper.IntWrapper;
import org.objectweb.proactive.extensions.annotation.ActiveObject;
import org.ow2.proactive.authentication.principals.IdentityPrincipal;
import org.ow2.proactive.authentication.principals.UserNamePrincipal;
import org.ow2.proactive.permissions.MethodCallPermission;
import org.ow2.proactive.permissions.PrincipalPermission;
import org.ow2.proactive.policy.ClientsPolicy;
import org.ow2.proactive.resourcemanager.authentication.Client;
import org.ow2.proactive.resourcemanager.authentication.RMAuthenticationImpl;
import org.ow2.proactive.resourcemanager.cleaning.NodesCleaner;
import org.ow2.proactive.resourcemanager.common.NodeState;
import org.ow2.proactive.resourcemanager.common.RMConstants;
import org.ow2.proactive.resourcemanager.common.RMState;
import org.ow2.proactive.resourcemanager.common.event.RMEvent;
import org.ow2.proactive.resourcemanager.common.event.RMEventType;
import org.ow2.proactive.resourcemanager.common.event.RMInitialState;
import org.ow2.proactive.resourcemanager.common.event.RMNodeEvent;
import org.ow2.proactive.resourcemanager.common.event.RMNodeSourceEvent;
import org.ow2.proactive.resourcemanager.core.account.RMAccountsManager;
import org.ow2.proactive.resourcemanager.core.history.Alive;
import org.ow2.proactive.resourcemanager.core.history.NodeHistory;
import org.ow2.proactive.resourcemanager.core.history.UserHistory;
import org.ow2.proactive.resourcemanager.core.jmx.RMJMXHelper;
import org.ow2.proactive.resourcemanager.core.properties.PAResourceManagerProperties;
import org.ow2.proactive.resourcemanager.db.DatabaseManager;
import org.ow2.proactive.resourcemanager.exception.AddingNodesException;
import org.ow2.proactive.resourcemanager.exception.NotConnectedException;
import org.ow2.proactive.resourcemanager.frontend.RMMonitoring;
import org.ow2.proactive.resourcemanager.frontend.RMMonitoringImpl;
import org.ow2.proactive.resourcemanager.frontend.ResourceManager;
import org.ow2.proactive.resourcemanager.frontend.topology.Topology;
import org.ow2.proactive.resourcemanager.frontend.topology.TopologyException;
import org.ow2.proactive.resourcemanager.nodesource.NodeSource;
import org.ow2.proactive.resourcemanager.nodesource.RMNodeConfigurator;
import org.ow2.proactive.resourcemanager.nodesource.common.PluginDescriptor;
import org.ow2.proactive.resourcemanager.nodesource.infrastructure.DefaultInfrastructureManager;
import org.ow2.proactive.resourcemanager.nodesource.infrastructure.InfrastructureManager;
import org.ow2.proactive.resourcemanager.nodesource.infrastructure.InfrastructureManagerFactory;
import org.ow2.proactive.resourcemanager.nodesource.policy.NodeSourcePolicy;
import org.ow2.proactive.resourcemanager.nodesource.policy.NodeSourcePolicyFactory;
import org.ow2.proactive.resourcemanager.nodesource.policy.StaticPolicy;
import org.ow2.proactive.resourcemanager.rmnode.RMDeployingNode;
import org.ow2.proactive.resourcemanager.rmnode.RMNode;
import org.ow2.proactive.resourcemanager.selection.SelectionManager;
import org.ow2.proactive.resourcemanager.selection.statistics.ProbablisticSelectionManager;
import org.ow2.proactive.resourcemanager.selection.topology.TopologyManager;
import org.ow2.proactive.resourcemanager.utils.ClientPinger;
import org.ow2.proactive.resourcemanager.utils.RMLoggers;
import org.ow2.proactive.scripting.SelectionScript;
import org.ow2.proactive.topology.descriptor.TopologyDescriptor;
import org.ow2.proactive.utils.NodeSet;


/**
 * The main active object of the Resource Manager (RM), the RMCore has to
 * provide nodes to clients.
 * 
 * The RMCore functions are :<BR>
 * - Create Resource Manager's active objects at its initialization ;
 * {@link RMAdmin}, {@link RMUser}, {@link RMMonitoring}.<BR>
 * - keep an up-to-date list of nodes able to perform scheduler's tasks.<BR>
 * - give nodes to the Scheduler asked by {@link RMUser} object, with a node
 * selection mechanism performed by {@link SelectionScript}.<BR>
 * - dialog with node sources which add and remove nodes to the Core. - perform
 * creation and removal of NodeSource objects. <BR>
 * - treat removing nodes and adding nodes request coming from {@link RMAdmin}.
 * - create and launch RMEvents concerning nodes and nodes Sources To
 * RMMonitoring active object.<BR>
 * <BR>
 * 
 * Nodes in Resource Manager are represented by {@link RMNode objects}. RMcore
 * has to manage different states of nodes : -free : node is ready to perform a
 * task.<BR>
 * -busy : node is executing a task.<BR>
 * -to be removed : node is busy and have to be removed at the end of the its
 * current task.<BR>
 * -down : node is broken, and not anymore able to perform tasks.<BR>
 * <BR>
 * 
 * RMCore is not responsible of creation, acquisition and monitoring of nodes,
 * these points are performed by {@link NodeSource} objects.<BR>
 * <BR>
 * 
 * WARNING : you must instantiate this class as an Active Object !
 * 
 * RmCore should be non-blocking which means <BR>
 * - no direct access to nodes <BR>
 * - all method calls to other active objects should be either asynchronous or non-blocking immediate services <BR>
 * - methods which have to return something depending on another active objects should use an automatic continuation <BR>
 *
 * @author The ProActive Team
 * @since ProActive Scheduling 0.9
 */
@ActiveObject
public class RMCore implements ResourceManager, InitActive, RunActive {

    /** Log4J logger name for RMCore */
    private final static Logger logger = ProActiveLogger.getLogger(RMLoggers.CORE);

    /** If RMCore Active object */
    private String id;

    /** ProActive Node containing the RMCore */
    private Node nodeRM;

    /** stub of RMMonitoring active object of the RM */
    private RMMonitoringImpl monitoring;

    /** authentication active object */
    private RMAuthenticationImpl authentication;

    /** HashMap of NodeSource active objects */
    private HashMap<String, NodeSource> nodeSources;

    /** HashMaps of nodes known by the RMCore */
    private HashMap<String, RMNode> allNodes;

    /** list of all free nodes */
    private ArrayList<RMNode> freeNodes;

    private SelectionManager selectionManager;

    /** indicates that RMCore must shutdown */
    private boolean toShutDown = false;

    private boolean shutedDown = false;

    private Client caller = null;

    /**
     * Map of connected clients and internal services that have an access to the core.
     * It is statically used due to drawbacks in the client pinger functionality
     * @see Client
     */
    public static Map<UniqueID, Client> clients = Collections
            .synchronizedMap(new HashMap<UniqueID, Client>());

    /** nodes topology */
    public static TopologyManager topologyManager;

    /** Client pinger */
    private ClientPinger clientPinger;

    /** an active object used to clean nodes when they are released after computations */
    private NodesCleaner nodesCleaner;

    private RMAccountsManager accountsManager;

    private RMJMXHelper jmxHelper;

    /** utility ao used to configure nodes (compute topology, configure dataspaces...) */
    private RMNodeConfigurator nodeConfigurator;

    /** the last time when RMCore seemed to be alive */
    private Alive alive;

    /**
     * ProActive Empty constructor
     */
    public RMCore() {
    }

    /**
     * Creates the RMCore object.
     * 
     * @param id
     *            Name for RMCOre.
     * @param nodeRM
     *            Name of the ProActive Node object containing RM active
     *            objects.
     * @throws ActiveObjectCreationException
     *             if creation of the active object failed.
     * @throws NodeException
     *             if a problem occurs on the target node.
     */
    public RMCore(String id, Node nodeRM) throws ActiveObjectCreationException, NodeException {
        this.id = id;
        this.nodeRM = nodeRM;

        nodeSources = new HashMap<String, NodeSource>();
        allNodes = new HashMap<String, RMNode>();
        freeNodes = new ArrayList<RMNode>();

        this.accountsManager = new RMAccountsManager();
        this.jmxHelper = new RMJMXHelper(this.accountsManager);
    }

    /**
     * Initialization part of the RMCore active object. <BR>
     * Create RM's active objects :<BR>
     * -{@link RMAdmin},<BR>
     * -{@link RMUser},<BR>
     * -{@link RMMonitoring},<BR>
     * and creates the default static Node Source named
     * {@link RMConstants#DEFAULT_STATIC_SOURCE_NAME}. Finally throws the RM
     * started event.
     * 
     * @param body
     *            the active object's body.
     * 
     */
    public void initActivity(Body body) {

        if (logger.isDebugEnabled()) {
            logger.debug("RMCore start : initActivity");
        }
        try {
            // setting up the policy
            logger.info("Setting up the resource manager security policy");
            ClientsPolicy.init();

            PAActiveObject.registerByName(PAActiveObject.getStubOnThis(),
                    RMConstants.NAME_ACTIVE_OBJECT_RMCORE);

            logger.info("Starting Hibernate...");
            boolean drop = PAResourceManagerProperties.RM_DB_HIBERNATE_DROPDB.getValueAsBoolean();
            logger.info("Drop DB : " + drop);
            if (drop) {
                DatabaseManager.getInstance().setProperty("hibernate.hbm2ddl.auto", "create");
                // dropping RRD data base as well
                File ddrDB = new File(PAResourceManagerProperties.RM_HOME.getValueAsString() +
                    System.getProperty("file.separator") +
                    PAResourceManagerProperties.RM_RRD_DATABASE_NAME.getValueAsString());
                if (ddrDB.exists()) {
                    ddrDB.delete();
                }

            }
            DatabaseManager.getInstance().build();
            logger.info("Hibernate successfully started !");

            if (!drop) {

                List<Alive> selected = DatabaseManager.getInstance().recover(Alive.class);
                if (selected.size() == 1) {
                    alive = selected.get(0);

                    // to keep the data base consistency updating all nodes history with
                    // the last alive time stamp
                    NodeHistory.recover(alive);
                    // updating end times of user connections
                    UserHistory.recover(alive);

                    // updating alive time stamp
                    alive.setTime(System.currentTimeMillis());
                    alive.update();

                } else if (selected.size() > 1) {
                    logger
                            .error("Inconsistency in the data base (Alive table). Cannot be more than one record.");
                }
            }

            if (alive == null) {
                alive = new Alive();
                alive.setTime(System.currentTimeMillis());
                alive.save();
            }

            if (logger.isDebugEnabled()) {
                logger.debug("Creating RMAuthentication active object");
            }

            authentication = (RMAuthenticationImpl) PAActiveObject.newActive(RMAuthenticationImpl.class
                    .getName(), new Object[] { PAActiveObject.getStubOnThis() }, nodeRM);

            if (logger.isDebugEnabled()) {
                logger.debug("Creating RMMonitoring active object");
            }

            // Boot the JMX infrastructure
            this.jmxHelper.boot(authentication);

            monitoring = (RMMonitoringImpl) PAActiveObject.newActive(RMMonitoringImpl.class.getName(),
                    new Object[] { PAActiveObject.getStubOnThis() }, nodeRM);

            if (logger.isDebugEnabled()) {
                logger.debug("Creating SelectionManager active object");
            }
            selectionManager = (SelectionManager) PAActiveObject.newActive(ProbablisticSelectionManager.class
                    .getName(), new Object[] { PAActiveObject.getStubOnThis() }, nodeRM);

            if (logger.isDebugEnabled()) {
                logger.debug("Creating ClientPinger active object");
            }
            clientPinger = (ClientPinger) PAActiveObject.newActive(ClientPinger.class.getName(),
                    new Object[] { PAActiveObject.getStubOnThis() }, nodeRM);

            if (logger.isDebugEnabled()) {
                logger.debug("Creating NodeCleaner active object");
            }
            nodesCleaner = (NodesCleaner) PAActiveObject.newActive(NodesCleaner.class.getName(),
                    new Object[] { PAActiveObject.getStubOnThis() }, nodeRM);

            topologyManager = new TopologyManager();

            nodeConfigurator = (RMNodeConfigurator) PAActiveObject.newActive(RMNodeConfigurator.class
                    .getName(), new Object[] { PAActiveObject.getStubOnThis() }, nodeRM);

            // adding shutdown hook
            final RMCore rmcoreStub = (RMCore) PAActiveObject.getStubOnThis();
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    if (!toShutDown) {
                        RMCore.clients.put(PAActiveObject.getBodyOnThis().getID(), new Client(null, false));
                        rmcoreStub.shutdown(true);
                    }

                    synchronized (nodeRM) {
                        if (!shutedDown) {
                            try {
                                // wait for rmcore shutdown (5 min at most)
                                nodeRM.wait(5 * 60 * 60 * 1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            });

            // Creating RM started event
            this.monitoring.rmEvent(new RMEvent(RMEventType.STARTED));

            // registering internal clients of the core
            clients.put(Client.getId(nodeConfigurator), new Client(null, false));
            clients.put(Client.getId(nodesCleaner), new Client(null, false));
            clients.put(Client.getId(authentication), new Client(null, false));
            clients.put(Client.getId(monitoring), new Client(null, false));
            clients.put(Client.getId(selectionManager), new Client(null, false));
            clients.put(Client.getId(clientPinger), new Client(null, false));

            authentication.setActivated(true);
            clientPinger.ping();

        } catch (ActiveObjectCreationException e) {
            logger.error("", e);
        } catch (NodeException e) {
            logger.error("", e);
        } catch (ProActiveException e) {
            logger.error("", e);
        } catch (ClassNotFoundException e) {
            logger.error("", e);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("RMCore end : initActivity");
        }
    }

    /**
     * RunActivity periodically send "alive" event to listeners
     */
    public void runActivity(Body body) {
        Service service = new Service(body);

        long timeStamp = System.currentTimeMillis();
        long delta = 0;

        // recalculating nodes number only once per policy period
        while (body.isActive()) {

            Request request = service
                    .blockingRemoveOldest(PAResourceManagerProperties.RM_ALIVE_EVENT_FREQUENCY
                            .getValueAsInt());
            if (request != null) {
                try {
                    try {
                        caller = checkMethodCallPermission(request.getMethodName(), request.getSourceBodyID());
                        service.serve(request);
                    } catch (SecurityException ex) {
                        logger.warn("Cannot serve request: " + request, ex);
                        service.serve(new ThrowExceptionRequest(request, ex));
                    }
                } catch (Throwable e) {
                    logger.error("Cannot serve request: " + request, e);
                }
            }

            delta += System.currentTimeMillis() - timeStamp;
            timeStamp = System.currentTimeMillis();

            if (delta > PAResourceManagerProperties.RM_ALIVE_EVENT_FREQUENCY.getValueAsInt()) {
                alive.setTime(timeStamp);
                alive.update();

                delta = 0;
            }
        }
    }

    /**
     * Returns a node object to a corresponding URL.
     * 
     * @param url
     *            url of the node asked.
     * @return RMNode object containing the node.
     */
    private RMNode getNodebyUrl(String url) {
        assert allNodes.containsKey(url);
        return allNodes.get(url);
    }

    /**
     * Set a node's state to free, after a completed task by it. Set the to free
     * and move the node to the internal free nodes list. An event informing the
     * node state's change is thrown to RMMonitoring.
     * 
     * @param rmNode node to set free.
     * @return true if the node successfully set as free, false if it was down before.
     */
    private BooleanWrapper internalSetFree(final RMNode rmNode) {
        // If the node is already free no need to go further
        if (rmNode.isFree()) {
            return new BooleanWrapper(true);
        }
        // Get the previous state of the node needed for the event
        final NodeState previousNodeState = rmNode.getState();
        try {
            logger.debug("The node " + rmNode.getNodeURL() + " owned by " + rmNode.getOwner() + " is free");
            Client client = rmNode.getOwner();
            if (client == null) {
                // node has been just configured, so the user initiated this action is the node provider
                client = rmNode.getProvider();
            }
            // reseting owner here
            rmNode.setFree();
            this.freeNodes.add(rmNode);
            // create the event
            this.registerAndEmitNodeEvent(new RMNodeEvent(rmNode, RMEventType.NODE_STATE_CHANGED,
                previousNodeState, client.getName()));
        } catch (NodeException e) {
            // the node is down
            logger.debug("", e);
            return new BooleanWrapper(false);
        }
        return new BooleanWrapper(true);
    }

    /**
     * Mark nodes as free after cleaning procedure.
     *
     * @param nodes to be free
     * @return true if all successful, false if there is a down node among nodes
     */
    public BooleanWrapper setFreeNodes(List<RMNode> nodes) {
        boolean result = true;
        for (RMNode node : nodes) {
            // getting the correct instance
            RMNode rmnode = this.getNodebyUrl(node.getNodeURL());
            // freeing it
            result &= internalSetFree(rmnode).getBooleanValue();
        }
        return new BooleanWrapper(result);
    }

    /**
     * Mark node to be removed after releasing.
     * 
     * @param rmNode
     *            node to be removed after node is released.
     */
    private void internalSetToRemove(final RMNode rmNode, Client initiator) {
        // If the node is already marked to be removed, so no need to go further
        if (rmNode.isToRemove()) {
            return;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Prepare to removing the node " + rmNode.getNodeURL());
        }
        // Get the previous state of the node needed for the event
        final NodeState previousNodeState = rmNode.getState();
        try {
            rmNode.setToRemove();
            // create the event
            this.registerAndEmitNodeEvent(new RMNodeEvent(rmNode, RMEventType.NODE_STATE_CHANGED,
                previousNodeState, initiator.getName()));
        } catch (NodeException e1) {
            // A down node shouldn't be busied...
            logger.debug("", e1);
        }
    }

    /**
     * Performs an RMNode release from the Core At this point the node is at
     * free or 'to be released' state. do the release, and confirm to NodeSource
     * the removal.
     * 
     * @param rmnode
     *            the node to release
     */
    private void removeNodeFromCoreAndSource(RMNode rmnode, Client initiator) {
        if (logger.isInfoEnabled()) {
            logger.info("Removing node " + rmnode.getNodeURL());
        }
        removeNodeFromCore(rmnode, initiator);
        rmnode.getNodeSource().removeNode(rmnode.getNodeURL(), initiator);
    }

    /**
     * Internal operations to remove the node from Core. RMNode object is
     * removed from {@link RMCore#allNodes}, removal Node event is thrown to
     * RMMonitoring Active object.
     * 
     * @param rmnode
     *            the node to remove.
     */
    private void removeNodeFromCore(RMNode rmnode, Client initiator) {
        logger.debug("Removing node " + rmnode.getNodeURL() + " provided by " + rmnode.getProvider());
        // removing the node from the HM list
        if (rmnode.isFree()) {
            freeNodes.remove(rmnode);
        }
        this.allNodes.remove(rmnode.getNodeURL());
        // create the event
        this.registerAndEmitNodeEvent(new RMNodeEvent(rmnode, RMEventType.NODE_REMOVED, rmnode.getState(),
            initiator.getName()));
    }

    /**
     * Internal operation of registering a new node in the Core
     * This step is done after node configuration ran by {@link RMNodeConfigurator} active object.
     * 
     * @param nodeURL the node's url of the node that is going to be added
     */
    public void internalAddNodeToCore(String nodeURL) {
        if (!this.allNodes.containsKey(nodeURL)) {
            //does nothing, the node has been removed preemptively
            //during its configuration
            logger.debug("internalAddNodeToCore returned immediately because the node " + nodeURL +
                " was not known");
            return;
        }
        //was added during internalRegisterConfiguringNode
        RMNode rmnode = this.allNodes.get(nodeURL);

        if (toShutDown) {
            logger.warn("Node " + rmnode.getNodeURL() +
                " will not be added to the core as the resource manager is shutting down");
            removeNodeFromCoreAndSource(rmnode, rmnode.getProvider());
            return;
        }

        //during the configuration process, the rmnode can be removed. Its state would be toRemove
        if (rmnode.isToRemove()) {
            removeNodeFromCoreAndSource(rmnode, rmnode.getProvider());
            return;
        }

        //during the configuration process, the node has been detected down by the nodesource.
        //discarding the registration
        if (rmnode.isDown()) {
            logger.debug("internalAddNodeToCore returned immediately because the node " + nodeURL +
                " is already down");
            return;
        }

        internalSetFree(rmnode);
    }

    /**
     * Internal operation of configuring a node. The node is not useable by a final user
     * ( not eligible thanks to getNode methods ) if it is in configuration state.
     * This method is called by {@link RMNodeConfigurator} to notify the core that the
     * process of configuring the rmnode has started. The end of the process will be
     * notified thanks to the method internalAddNodeToCore(RMNode)
     * @param rmnode the node in the configuration state
     */
    public void internalRegisterConfiguringNode(RMNode rmnode) {
        if (toShutDown) {
            logger.warn("The RM core is shutting down, cannot configure the node");
            rmnode.getNodeSource().removeNode(rmnode.getNodeURL(), rmnode.getProvider());
            return;
        }

        rmnode.setConfiguring(rmnode.getProvider());
        //we add the configuring node to the collection to be able to ping it
        this.allNodes.put(rmnode.getNodeURL(), rmnode);

        // create the event
        this.registerAndEmitNodeEvent(new RMNodeEvent(rmnode, RMEventType.NODE_ADDED, null, rmnode
                .getProvider().getName()));
        if (logger.isDebugEnabled()) {
            logger.debug("Configuring node " + rmnode.getNodeURL());
        }

        //now configuring the newly looked up node
        nodeConfigurator.configureNode(rmnode);
    }

    public String getId() {
        return this.id;
    }

    /**
     * Returns the url of the node where the rm core is running
     * @return the url of the node where the rm core is running
     */
    private String getUrl() {
        try {
            String tmp = PAActiveObject.getActiveObjectNodeUrl(PAActiveObject.getStubOnThis());
            if (tmp != null) {
                return tmp.replaceAll(PAResourceManagerProperties.RM_NODE_NAME.getValueAsString(), "");
            } else {
                return "No default RM URL";
            }
        } catch (Throwable e) {
            logger.error("Unable to get RM URL", e);
            return "No default RM URL";
        }
    }

    // ----------------------------------------------------------------------
    // Methods called by RMAdmin
    // ----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    public BooleanWrapper addNode(String nodeUrl) {
        return addNode(nodeUrl, NodeSource.DEFAULT);
    }

    /**
     * {@inheritDoc}
     */
    public BooleanWrapper addNode(String nodeUrl, String sourceName) {

        if (toShutDown) {
            throw new AddingNodesException("The resource manager is shutting down");
        }

        boolean existingNodeSource = nodeSources.containsKey(sourceName);

        if (!existingNodeSource && sourceName.equals(NodeSource.DEFAULT)) {
            // creating the default node source
            createNodeSource(NodeSource.DEFAULT, DefaultInfrastructureManager.class.getName(), null,
                    StaticPolicy.class.getName(), null).getBooleanValue();
        }

        if (nodeSources.containsKey(sourceName)) {
            NodeSource nodeSource = this.nodeSources.get(sourceName);

            // Known URL, so do some cleanup before replacing it
            if (allNodes.containsKey(nodeUrl)) {

                if (!allNodes.get(nodeUrl).getNodeSourceName().equals(sourceName)) {
                    // trying to already registered node to another node source
                    // do nothing in this case
                    throw new AddingNodesException("An attempt to add a node " + nodeUrl +
                        " registered in one node source to another one");
                }
            }
            return nodeSource.acquireNode(nodeUrl, caller);
        } else {
            throw new AddingNodesException("Unknown node source " + sourceName);
        }
    }

    /**
     * Removes a node from the RM. This method also handles deploying node removal ( deploying node's url
     * follow the scheme deploying:// ). In such a case, the preempt parameter is not used.
     *
     * @param nodeUrl URL of the node to remove.
     * @param preempt if true remove the node immediately without waiting while it will be freed. ( ignored if deploying node )
     */
    public BooleanWrapper removeNode(String nodeUrl, boolean preempt) {

        //waiting for better integration of deploying node
        //if we get a "deploying node url" we change the flow
        if (isDeployingNodeURL(nodeUrl)) {
            return new BooleanWrapper(removeDeployingNode(nodeUrl));
        }

        if (this.allNodes.containsKey(nodeUrl)) {
            RMNode rmnode = this.allNodes.get(nodeUrl);
            logger.debug("Request to remove node " + rmnode);

            // checking if the caller is the node administrator
            checkNodeAdminPermission(rmnode, caller);

            if (rmnode.isDown() || preempt || rmnode.isFree() || rmnode.isLocked()) {
                removeNodeFromCoreAndSource(rmnode, caller);
            } else if (rmnode.isBusy() || rmnode.isConfiguring()) {
                internalSetToRemove(rmnode, caller);
            }
        } else {
            logger.warn("An attempt to remove a non existing node: " + nodeUrl + " was made. Ignoring it");
            return new BooleanWrapper(false);
        }
        return new BooleanWrapper(true);
    }

    /**
     * Removes "number" of nodes from the node source.
     *
     * @param number amount of nodes to be released
     * @param name a node source name
     * @param preemptive if true remove nodes immediately without waiting while they will be freed
     */
    public void removeNodes(int number, String nodeSourceName, boolean preemptive) {
        int numberOfRemovedNodes = 0;

        // temporary list to avoid concurrent modification
        List<RMNode> nodelList = new LinkedList<RMNode>();
        nodelList.addAll(freeNodes);

        logger.debug("Free nodes size " + nodelList.size());
        for (RMNode node : nodelList) {

            if (numberOfRemovedNodes == number) {
                break;
            }

            if (node.getNodeSource().getName().equals(nodeSourceName)) {
                removeNode(node.getNodeURL(), preemptive);
                numberOfRemovedNodes++;
            }
        }

        nodelList.clear();
        nodelList.addAll(allNodes.values());
        logger.debug("All nodes size " + nodelList.size());
        if (numberOfRemovedNodes < number) {
            for (RMNode node : nodelList) {

                if (numberOfRemovedNodes == number) {
                    break;
                }

                if (node.isBusy() && node.getNodeSource().getName().equals(nodeSourceName)) {
                    removeNode(node.getNodeURL(), preemptive);
                    numberOfRemovedNodes++;
                }
            }
        }

        if (numberOfRemovedNodes < number) {
            logger.warn("Cannot remove " + number + " nodes from node source " + nodeSourceName);
        }
    }

    /**
     * Removes all nodes from the specified node source.
     *
     * @param nodeSourceName a name of the node source
     * @param preemptive if true remove nodes immediately without waiting while they will be freed
     */
    public void removeAllNodes(String nodeSourceName, boolean preemptive) {

        for (RMDeployingNode pn : nodeSources.get(nodeSourceName).getDeployingNodes()) {
            removeNode(pn.getNodeURL(), preemptive);
        }
        for (Node node : nodeSources.get(nodeSourceName).getAliveNodes()) {
            removeNode(node.getNodeInformation().getURL(), preemptive);
        }
        for (Node node : nodeSources.get(nodeSourceName).getDownNodes()) {
            removeNode(node.getNodeInformation().getURL(), preemptive);
        }
    }

    /**
     * Returns true if the node nodeUrl is registered (i.e. known by the RM). Note that
     * true is returned even if the node is down.
     *
     * @param nodeUrl the tested node.
     * @return true if the node nodeUrl is registered.
     */
    public BooleanWrapper nodeIsAvailable(String nodeUrl) {
        final RMNode checked = this.allNodes.get(nodeUrl);
        return new BooleanWrapper(checked != null && !checked.isDown());
    }

    public NodeState getNodeState(String nodeUrl) {
        RMNode node = this.allNodes.get(nodeUrl);
        if (node == null) {
            throw new IllegalArgumentException("Unknown node " + nodeUrl);
        }
        return node.getState();
    }

    /**
     * Creates a new node source with specified name, infrastructure manages {@link InfrastructureManager}
     * and acquisition policy {@link NodeSourcePolicy}.
     *
     * @param nodeSourceName the name of the node source
     * @param infrastructureType type of the underlying infrastructure {@link InfrastructureType}
     * @param infrastructureParameters parameters for infrastructure creation
     * @param policyType name of the policy type. It passed as a string due to pluggable approach {@link NodeSourcePolicyFactory}
     * @param policyParameters parameters for policy creation
     */
    public BooleanWrapper createNodeSource(String nodeSourceName, String infrastructureType,
            Object[] infrastructureParameters, String policyType, Object[] policyParameters) {

        //checking that nsname doesn't contain invalid characters and doesn't exist yet
        if (nodeSourceName == null) {
            throw new IllegalArgumentException("Node Source name cannot be null");
        }
        nodeSourceName = nodeSourceName.trim();
        checkNodeSourceName(nodeSourceName);

        logger.info("Creating a node source : " + nodeSourceName);

        InfrastructureManager im = InfrastructureManagerFactory.create(infrastructureType,
                infrastructureParameters);
        NodeSourcePolicy policy = NodeSourcePolicyFactory.create(policyType, infrastructureType,
                policyParameters);

        NodeSource nodeSource;
        try {
            nodeSource = (NodeSource) PAActiveObject.newActive(NodeSource.class.getName(), new Object[] {
                    this.getUrl(), nodeSourceName, caller, im, policy, PAActiveObject.getStubOnThis(),
                    this.monitoring }, nodeRM);
        } catch (Exception e) {
            throw new RuntimeException("Cannot create node source " + nodeSourceName, e);
        }

        // Adding access to the core for node source and policy.
        // In order to do it node source and policy active objects are added to the clients list.
        // They will be removed from this list when node source is unregistered.
        UniqueID nsId = Client.getId(nodeSource);
        UniqueID policyId = Client.getId(policy);
        if (nsId == null || policyId == null) {
            throw new IllegalStateException("Cannot register the node source");
        }

        BooleanWrapper result = nodeSource.activate();
        if (!result.getBooleanValue()) {
            logger.error("Node source " + nodeSourceName + " cannot be activated");
        }

        Client nsService = new Client(caller.getSubject(), false);
        Client policyService = new Client(caller.getSubject(), false);

        nsService.setId(nsId);
        policyService.setId(policyId);

        RMCore.clients.put(nsId, nsService);
        RMCore.clients.put(policyId, policyService);

        this.nodeSources.put(nodeSourceName, nodeSource);
        // generate the event of node source creation
        this.monitoring.nodeSourceEvent(new RMNodeSourceEvent(nodeSource, RMEventType.NODESOURCE_CREATED,
            caller.getName()));

        logger.info("Node source " + nodeSourceName + " has been successfully created by " + caller);

        return new BooleanWrapper(true);
    }

    /**
     * Shutdown the resource manager
     */
    public BooleanWrapper shutdown(boolean preempt) {
        // this method could be called twice from shutdown hook and user action
        if (toShutDown)
            return new BooleanWrapper(false);

        logger.info("RMCore shutdown request");
        this.monitoring.rmEvent(new RMEvent(RMEventType.SHUTTING_DOWN));
        this.toShutDown = true;

        if (nodeSources.size() == 0) {
            finalizeShutdown();
        } else {
            for (Entry<String, NodeSource> entry : this.nodeSources.entrySet()) {
                removeAllNodes(entry.getKey(), preempt);
                entry.getValue().shutdown(caller);
            }
        }
        return new BooleanWrapper(true);
    }

    // ----------------------------------------------------------------------
    // Methods called by RMUser, override RMCoreInterface
    // ----------------------------------------------------------------------

    /**
     * Gives total number of alive nodes handled by RM
     * @return total number of alive nodes
     */
    private int getTotalAliveNodesNumber() {
        // TODO get the number of alive nodes in a more effective way
        int count = 0;
        for (RMNode node : allNodes.values()) {
            if (!node.isDown())
                count++;
        }
        return count;
    }

    /**
     * {@inheritDoc}
     */
    public BooleanWrapper releaseNode(Node node) {
        NodeSet nodes = new NodeSet();
        nodes.add(node);
        return releaseNodes(nodes);
    }

    /**
     * {@inheritDoc}
     */
    public BooleanWrapper releaseNodes(NodeSet nodes) {

        if (nodes.getExtraNodes() != null) {
            // do not forget to release extra nodes
            nodes.addAll(nodes.getExtraNodes());
        }

        // exception to throw in case of problems
        RuntimeException exception = null;

        for (Node node : nodes) {
            String nodeURL = null;
            try {
                nodeURL = node.getNodeInformation().getURL();
            } catch (RuntimeException e) {
                logger.debug("A Runtime exception occured while obtaining information on the node,"
                    + "the node must be down (it will be detected later)", e);
                // node is down, will be detected by pinger
                exception = new IllegalStateException(e.getMessage(), e);
            }

            // verify whether the node has not been removed from the RM
            if (this.allNodes.containsKey(nodeURL)) {
                RMNode rmnode = this.getNodebyUrl(nodeURL);

                // prevent Scheduler Error : Scheduler try to render anode already
                // free
                if (rmnode.isFree()) {
                    logger.warn("Client " + caller + " tries to release the already free node " + nodeURL);
                } else {
                    // verify that scheduler don't try to render a node detected down
                    if (!rmnode.isDown()) {
                        Set<? extends IdentityPrincipal> userPrincipal = rmnode.getOwner().getSubject()
                                .getPrincipals(UserNamePrincipal.class);
                        Permission ownerPermission = new PrincipalPermission(rmnode.getOwner().getName(),
                            userPrincipal);
                        try {
                            caller.checkPermission(ownerPermission, caller +
                                " is not authorized to free node " + node.getNodeInformation().getURL());

                            if (rmnode.isToRemove()) {
                                removeNodeFromCoreAndSource(rmnode, caller);
                            } else {
                                internalSetFree(rmnode);
                            }
                        } catch (SecurityException ex) {
                            exception = ex;
                        }
                    } else {
                        // down node, just ignore
                        logger.warn("Down node " + rmnode.getNodeURL() + " will not be released");
                    }
                }
            } else {
                logger.warn("Cannot release unknown node " + nodeURL);
                exception = new IllegalArgumentException("Cannot release unknown node " + nodeURL);
            }
        }

        if (exception != null) {
            // throwing the latest exception we had
            throw exception;
        }

        return new BooleanWrapper(true);
    }

    /**
     * {@inheritDoc}
     */
    public NodeSet getAtMostNodes(int nbNodes, SelectionScript selectionScript) {
        List<SelectionScript> selectionScriptList = selectionScript == null ? null : Collections
                .singletonList(selectionScript);
        return getAtMostNodes(nbNodes, TopologyDescriptor.ARBITRARY, selectionScriptList, null);
    }

    /**
     * {@inheritDoc}
     */
    public NodeSet getAtMostNodes(int number, SelectionScript selectionScript, NodeSet exclusion) {
        List<SelectionScript> selectionScriptList = selectionScript == null ? null : Collections
                .singletonList(selectionScript);
        return getAtMostNodes(number, TopologyDescriptor.ARBITRARY, selectionScriptList, exclusion);
    }

    /**
     * {@inheritDoc}
     */
    public NodeSet getAtMostNodes(int number, List<SelectionScript> scripts, NodeSet exclusion) {
        return getAtMostNodes(number, TopologyDescriptor.ARBITRARY, scripts, exclusion);
    }

    public NodeSet getAtMostNodes(int number, TopologyDescriptor descriptor,
            List<SelectionScript> selectionScrips, NodeSet exclusion) {

        if (number <= 0) {
            throw new IllegalArgumentException("Illegal node number " + number);
        } else if (this.toShutDown) {
            // if the resource manager is about to shutdown, do not provide any node
            return new NodeSet();
        } else {
            logger.info(caller + " requested " + number + " nodes");
            if (descriptor == null) {
                descriptor = TopologyDescriptor.ARBITRARY;
            }
            return selectionManager.selectNodes(number, descriptor, selectionScrips, exclusion, caller);
        }
    }

    /**
     * {@inheritDoc}
     */
    public NodeSet getExactlyNodes(int nb, SelectionScript selectionScript) {
        throw new RuntimeException("Not supported");
    }

    /**
     * Builds and returns a snapshot of RMCore's current state. Initial state
     * must be understood as a new Monitor point of view. A new monitor start to
     * receive RMCore events, so must be informed of the current state of the
     * Core at the beginning of monitoring.
     * 
     * @return RMInitialState containing nodes and nodeSources of the RMCore.
     */
    public RMInitialState getRMInitialState() {

        ArrayList<RMNodeEvent> nodesList = new ArrayList<RMNodeEvent>();
        for (RMNode rmnode : this.allNodes.values()) {
            nodesList.add(new RMNodeEvent(rmnode));
        }

        ArrayList<RMNodeSourceEvent> nodeSourcesList = new ArrayList<RMNodeSourceEvent>();
        for (NodeSource s : this.nodeSources.values()) {
            nodeSourcesList.add(new RMNodeSourceEvent(s));
            for (RMDeployingNode pn : s.getDeployingNodes()) {
                nodesList.add(new RMNodeEvent(pn));
            }
        }

        return new RMInitialState(nodesList, nodeSourcesList);
    }

    /**
     * Gets RM monitoring stub
     */
    public RMMonitoring getMonitoring() {
        return this.monitoring;
    }

    /**
     * Unregisters node source from the resource manager core.
     */
    public BooleanWrapper nodeSourceUnregister(String sourceName, RMNodeSourceEvent evt) {
        NodeSource nodeSource = this.nodeSources.remove(sourceName);

        if (nodeSource == null) {
            logger.warn("Attempt to remove non-existing node source " + sourceName);
            new BooleanWrapper(false);
        }

        // remove node source from clients list
        // policy has been already already removed
        UniqueID id = Client.getId(nodeSource);
        if (id != null) {
            disconnect(id);
        } else {
            logger.error("Cannot extract the body id of the node source " + sourceName);
        }
        logger.info("Node Source removed : " + sourceName);
        // create the event
        this.monitoring.nodeSourceEvent(evt);

        if ((this.nodeSources.size() == 0) && this.toShutDown) {
            finalizeShutdown();
        }

        return new BooleanWrapper(true);
    }

    private void finalizeShutdown() {
        // all nodes sources has been removed and RMCore in shutdown state,
        // finish the shutdown
        this.selectionManager.shutdown();
        this.clientPinger.shutdown();
        // waiting while all events will be dispatched to listeners
        PAFuture.waitFor(this.monitoring.shutdown());

        PAActiveObject.terminateActiveObject(false);
        try {
            Thread.sleep(2000);
            synchronized (nodeRM) {
                nodeRM.notifyAll();
                shutedDown = true;
            }

            this.nodeRM.getProActiveRuntime().killRT(true);
        } catch (Exception e) {
            logger.debug("", e);
        }
    }

    /**
     * Set a node state to busy. Set the node to busy, and move the node to the
     * internal busy nodes list. An event informing the node state's change is
     * thrown to RMMonitoring.
     * @param client
     *
     * @param rmnode
     *            node to set
     * @throws NodeException if the node can't be set busy
     */
    public void setBusyNode(final String nodeUrl, Client owner) throws NodeException {
        final RMNode rmNode = this.allNodes.get(nodeUrl);
        if (rmNode == null) {
            logger.error("Unknown node " + nodeUrl);
            return;
        }
        // If the node is already busy no need to go further
        if (rmNode.isBusy()) {
            return;
        }
        // Get the previous state of the node needed for the event
        final NodeState previousNodeState = rmNode.getState();
        try {
            rmNode.setBusy(owner);
        } catch (NodeException e) {
            // A down node shouldn't be busied...
            logger.error("Unable to set the node " + rmNode.getNodeURL() + " busy", e);
            // Since this method throws a NodeException re-throw e to inform the caller 
            throw e;
        }
        this.freeNodes.remove(rmNode);
        // create the event
        this.registerAndEmitNodeEvent(new RMNodeEvent(rmNode, RMEventType.NODE_STATE_CHANGED,
            previousNodeState, owner.getName()));
    }

    /**
     * Sets a node state to down and updates all internal structures of rm core
     * accordingly. Sends an event indicating that the node is down.
     */
    public void setDownNode(String nodeUrl) {
        RMNode rmNode = getNodebyUrl(nodeUrl);
        if (rmNode != null) {
            // If the node is already down no need to go further
            if (rmNode.isDown()) {
                return;
            }
            logger.info("The node " + rmNode.getNodeURL() + " provided by " + rmNode.getProvider() +
                " is down");
            // Get the previous state of the node needed for the event
            final NodeState previousNodeState = rmNode.getState();
            if (rmNode.isFree()) {
                freeNodes.remove(rmNode);
            }
            rmNode.setDown();
            // create the event
            this.registerAndEmitNodeEvent(new RMNodeEvent(rmNode, RMEventType.NODE_STATE_CHANGED,
                previousNodeState, rmNode.getProvider().getName()));
        } else {
            // the nodes has been removed from core asynchronously
            // when pinger of selection manager tried to access it
            // do nothing in this case
            logger.debug("setDownNode returned immediately because the node " + nodeUrl + " was not known");
        }
    }

    private void registerAndEmitNodeEvent(final RMNodeEvent event) {
        new NodeHistory(event).save();
        this.monitoring.nodeEvent(event);
    }

    /**
     * Removed a node with given url from the internal structures of the core.
     *
     * @param nodeUrl down node to be removed
     * @return true if the nodes was successfully removed, false otherwise
     */
    public BooleanWrapper removeNodeFromCore(String nodeUrl) {
        RMNode rmnode = getNodebyUrl(nodeUrl);
        if (rmnode != null) {
            removeNodeFromCore(rmnode, caller);
            return new BooleanWrapper(true);
        } else {
            return new BooleanWrapper(false);
        }
    }

    public ArrayList<RMNode> getFreeNodes() {
        return freeNodes;
    }

    /**
     * {@inheritDoc}
     */
    public IntWrapper getNodeSourcePingFrequency(String sourceName) {
        if (this.nodeSources.containsKey(sourceName)) {
            return this.nodeSources.get(sourceName).getPingFrequency();
        } else {
            throw new IllegalArgumentException("Unknown node source " + sourceName);
        }
    }

    /**
     * {@inheritDoc}
     */
    public BooleanWrapper setNodeSourcePingFrequency(int frequency, String sourceName) {
        if (this.nodeSources.containsKey(sourceName)) {
            this.nodeSources.get(sourceName).setPingFrequency(frequency);
        } else {
            throw new IllegalArgumentException("Unknown node source " + sourceName);
        }
        return new BooleanWrapper(true);
    }

    /**
     * {@inheritDoc}
     */
    @Deprecated
    public List<RMNodeSourceEvent> getNodeSourcesList() {
        return getRMInitialState().getNodeSource();
    }

    /**
     * {@inheritDoc}
     */
    @Deprecated
    public List<RMNodeEvent> getNodesList() {
        return getRMInitialState().getNodesEvents();
    }

    /**
     * {@inheritDoc}
     */
    public BooleanWrapper removeNodeSource(String sourceName, boolean preempt) {
        if (nodeSources.containsKey(sourceName)) {
            // need to have an admin permission to remove the node source
            NodeSource nodeSource = nodeSources.get(sourceName);
            caller.checkPermission(nodeSource.getAdminPermission(), caller + " is not authorized to remove " +
                sourceName);

            logger.info(caller + " requested removal of the " + sourceName + " node source");

            //remove down nodes handled by the source
            //because node source doesn't know anymore its down nodes
            removeAllNodes(sourceName, preempt);
            nodeSource.shutdown(caller);
            return new BooleanWrapper(true);
        } else {
            throw new IllegalArgumentException("Unknown node source " + sourceName);
        }
    }

    /**
     * {@inheritDoc}
     */
    public RMState getState() {
        RMState state = new RMState(freeNodes.size(), getTotalAliveNodesNumber(), allNodes.size());
        return state;
    }

    /**
     * {@inheritDoc}
     */
    public BooleanWrapper isActive() {
        return new BooleanWrapper(!toShutDown);
    }

    /**
     * {@inheritDoc}
     */
    public BooleanWrapper disconnect() {
        disconnect(PAActiveObject.getContext().getCurrentRequest().getSender().getID());
        return new BooleanWrapper(true);
    }

    /**
     * Disconnects the client and releases all nodes held by him
     */
    public void disconnect(UniqueID clientId) {
        Client client = RMCore.clients.remove(clientId);
        if (client != null) {
            List<RMNode> nodesToRelease = new LinkedList<RMNode>();
            // expensive but relatively rare operation
            for (RMNode rmnode : allNodes.values()) {
                // checking that it is not only the same client but also 
                // the same connection
                if (client.equals(rmnode.getOwner()) && clientId.equals(rmnode.getOwner().getId())) {
                    if (rmnode.isToRemove()) {
                        removeNodeFromCoreAndSource(rmnode, client);
                    } else if (rmnode.isBusy()) {
                        nodesToRelease.add(rmnode);
                    }
                }
            }
            // Force the nodes cleaning here to avoid the situation
            // when the disconnected client still uses nodes.
            // In the future we may clean nodes for any release request
            nodesCleaner.cleanAndRelease(nodesToRelease);
            // update the connection info in the DB
            if (client.getHistory() != null) {
                client.getHistory().update();
            }
            logger.info(client + " disconnected");
        } else {
            logger.warn("Trying to disconnect unknown client with id " + clientId);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Collection<PluginDescriptor> getSupportedNodeSourceInfrastructures() {
        return getPluginsDescriptor(InfrastructureManagerFactory.getSupportedInfrastructures());
    }

    /**
     * {@inheritDoc}
     */
    public Collection<PluginDescriptor> getSupportedNodeSourcePolicies() {
        return getPluginsDescriptor(NodeSourcePolicyFactory.getSupportedPolicies());
    }

    private Collection<PluginDescriptor> getPluginsDescriptor(Collection<Class<?>> plugins) {
        Collection<PluginDescriptor> descriptors = new ArrayList<PluginDescriptor>();
        for (Class<?> cls : plugins) {
            Map<String, String> defaultValues = new HashMap<String, String>();
            defaultValues.put(InfrastructureManager.RM_URL_FIELD_NAME, this.getUrl());
            descriptors.add(new PluginDescriptor(cls, defaultValues));
        }
        return descriptors;
    }

    /**
     * Checks if the caller thread has permissions to call particular method name
     * @return client object corresponding to the caller thread
     */
    private Client checkMethodCallPermission(final String methodName, UniqueID clientId) {
        Client client = RMCore.clients.get(clientId);

        if (client == null) {
            throw new NotConnectedException("Client " + clientId +
                " is not connected to the resource manager");
        }

        final String fullMethodName = RMCore.class.getName() + "." + methodName;
        final MethodCallPermission methodCallPermission = new MethodCallPermission(fullMethodName);

        client.checkPermission(methodCallPermission, client + " is not authorized to call " + fullMethodName);
        return client;
    }

    public Topology getTopology() {
        if (!PAResourceManagerProperties.RM_TOPOLOGY_ENABLED.getValueAsBoolean()) {
            throw new TopologyException("Topology is disabled");
        }
        return topologyManager.getTopology();
    }

    /**
     * Returns true if the given parameter is the representation of
     * a deploying node ( starts with deploying://nsName/nodeName )
     * @param url
     * @return true if the parameter is a deploying node's url, false otherwise
     */
    private boolean isDeployingNodeURL(String url) {
        if (url != null && url.startsWith(RMDeployingNode.PROTOCOL_ID + "://")) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * To handle the deploying node removal
     * @param url the url of the deploying node to remove
     * @return true if successful, false otherwise
     */
    private boolean removeDeployingNode(String url) {
        String nsName = "";
        try {
            URI urlObj = new URI(url);
            nsName = urlObj.getHost();
        } catch (URISyntaxException e) {
            logger.warn("No such deploying node: " + url);
            return false;
        }
        if (nsName == null) {
            //cannot compute the nsName using URI, try using Pattern
            Matcher matcher = Pattern.compile(RMDeployingNode.PROTOCOL_ID + "://([-\\w]+)/.+").matcher(url);
            if (matcher.find()) {
                try {
                    nsName = matcher.group(1);
                } catch (IndexOutOfBoundsException e) {
                    logger.debug("Was not able to determine nodesource's name for url " + url);
                }
            }
        }
        NodeSource ns = this.nodeSources.get(nsName);
        if (ns == null) {
            logger.warn("No such nodesource: " + nsName + ", cannot remove the deploying node with url: " +
                url);
            return false;
        }
        return ns.removeDeployingNode(url);
    }

    /**
     * Checks if the string parameter is a valid nodesource name.
     * Throws an IllegalArgumentException if it doesn't
     * @param nodeSourceName the name to test
     */
    private void checkNodeSourceName(String nodeSourceName) {
        //we are sure that the parameter isn't null
        if (nodeSourceName.length() == 0) {
            throw new IllegalArgumentException("Node Source Name cannot be empty");
        }
        if (this.nodeSources.containsKey(nodeSourceName)) {
            throw new IllegalArgumentException("Node Source name " + nodeSourceName + " already exist");
        }
        Pattern pattern = Pattern.compile("[^-\\w]");//letters,digits,_and-
        Matcher matcher = pattern.matcher(nodeSourceName);
        if (matcher.find()) {
            throw new IllegalArgumentException("Node Source name \"" + nodeSourceName +
                "\" is invalid because it contains invalid characters. Only [-a-zA-Z_0-9] are valid.");
        }
    }

    /**
     * Checks if the client is the node admin.
     *
     * @param rmnode is a node to be checked
     * @param client is a client to be checked
     *
     * @return true if the client is an admin, SecurityException otherwise
     */
    private boolean checkNodeAdminPermission(RMNode rmnode, Client client) {
        NodeSource nodeSource = rmnode.getNodeSource();
        // in order to be the node administrator a client has to be either
        // an administrator of the RM (with AllPermissions) or
        // an administrator of the node source (creator) or
        // a node provider
        try {
            // checking if the caller is an administrator
            caller.checkPermission(nodeSource.getAdminPermission(), caller +
                " is not authorized to remove node " + rmnode.getNodeURL() + " from " +
                rmnode.getNodeSourceName());
        } catch (SecurityException ex) {
            // the caller is not an administrator, so checking if it is a node provider
            caller.checkPermission(rmnode.getAdminPermission(), caller +
                " is not authorized to remove node " + rmnode.getNodeURL() + " from " +
                rmnode.getNodeSourceName());
        }

        return true;
    }

    /**
     * {@inheritDoc}
     */
    public BooleanWrapper lockNodes(Set<String> urls) {
        for (String url : urls) {
            RMNode rmnode = getNodebyUrl(url);
            if (rmnode.isFree()) {
                // throws a security exception if caller is not an admin
                checkNodeAdminPermission(rmnode, caller);
                rmnode.lock(caller);
                freeNodes.remove(rmnode);
                this.registerAndEmitNodeEvent(new RMNodeEvent(rmnode, RMEventType.NODE_STATE_CHANGED,
                    NodeState.FREE, caller.getName()));
            } else {
                throw new IllegalArgumentException("Cannot lock the node " + rmnode.getNodeURL() +
                    " which is not free [ state is " + rmnode.getState() + " ]");
            }
        }
        return new BooleanWrapper(true);
    }

    /**
     * {@inheritDoc}
     */
    public BooleanWrapper unlockNodes(Set<String> urls) {
        for (String url : urls) {
            RMNode rmnode = getNodebyUrl(url);
            if (rmnode.isLocked()) {
                // throws a security exception if caller is not an admin
                checkNodeAdminPermission(rmnode, caller);
                try {
                    rmnode.setFree();
                    freeNodes.add(rmnode);
                } catch (NodeException e) {
                    throw new RuntimeException(e);
                }
                this.registerAndEmitNodeEvent(new RMNodeEvent(rmnode, RMEventType.NODE_STATE_CHANGED,
                    NodeState.LOCKED, caller.getName()));
            } else {
                throw new IllegalArgumentException("Cannot unlock the node " + rmnode.getNodeURL() +
                    " which is not locked [ state is " + rmnode.getState() + " ]");
            }
        }
        return new BooleanWrapper(true);
    }
}
