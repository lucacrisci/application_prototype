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
package org.ow2.proactive.scheduler.ext.matsci.worker.util;

import org.objectweb.proactive.core.node.Node;
import org.ow2.proactive.scheduler.ext.common.util.IOTools;
import org.ow2.proactive.scheduler.ext.matsci.worker.MatSciWorker;


/**
 * MatSciJVMInfo
 *
 * @author The ProActive Team
 */
public class MatSciJVMInfo<W extends MatSciWorker, C extends MatSciEngineConfig> implements
        org.ow2.proactive.scheduler.ext.matsci.common.ProcessListener {
    IOTools.LoggingThread isLogger = null;
    IOTools.RedirectionThread ioThread = null;
    Node node = null;
    Integer deployID = null;
    Process process = null;
    W worker = null;

    public C getConfig() {
        return config;
    }

    public void setConfig(C config) {
        this.config = config;
    }

    C config;

    public IOTools.LoggingThread getLogger() {
        return isLogger;
    }

    public void setLogger(IOTools.LoggingThread logger) {
        isLogger = logger;
    }

    public IOTools.RedirectionThread getIoThread() {
        return ioThread;
    }

    public void setIoThread(IOTools.RedirectionThread ioThread) {
        this.ioThread = ioThread;
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    public Integer getDeployID() {
        return deployID;
    }

    public void setDeployID(Integer deployID) {
        this.deployID = deployID;
    }

    public Process getProcess() {
        return process;
    }

    public void setProcess(Process process) {
        this.process = process;
    }

    public W getWorker() {
        return worker;
    }

    public void setWorker(W worker) {
        this.worker = worker;
    }
}
