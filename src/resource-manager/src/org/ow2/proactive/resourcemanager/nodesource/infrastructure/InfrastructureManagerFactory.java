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
package org.ow2.proactive.resourcemanager.nodesource.infrastructure;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

import org.objectweb.proactive.core.util.log.ProActiveLogger;
import org.ow2.proactive.resourcemanager.core.properties.PAResourceManagerProperties;
import org.ow2.proactive.resourcemanager.exception.RMException;
import org.ow2.proactive.resourcemanager.utils.RMLoggers;


/**
 *
 * Provides a generic way to create an infrastructure manager.
 * Loads all supported infrastructure names from a config file. Checks that required
 * infrastructure is supported at creation time.
 *
 */
public class InfrastructureManagerFactory {

    /** list of supported infrastructures */
    private static Collection<Class<?>> supportedInfrastructures;

    /**
     * Creates new infrastructure manager using reflection mechanism.
     *
     * @param infrastructureType a full class name of an infrastructure manager
     * @param infrastructureParameters parameters for nodes acquisition
     * @return new infrastructure manager
     * @throws RMException if any problems occurred
     */
    public static InfrastructureManager create(String infrastructureType, Object[] infrastructureParameters) {

        InfrastructureManager im = null;
        try {

            boolean supported = false;
            for (Class<?> cls : getSupportedInfrastructures()) {
                if (cls.getName().equals(infrastructureType)) {
                    supported = true;
                    break;
                }
            }
            if (!supported) {
                throw new IllegalArgumentException(infrastructureType + " is not supported");
            }

            Class<?> imClass = Class.forName(infrastructureType);
            im = (InfrastructureManager) imClass.newInstance();
            im.internalConfigure(infrastructureParameters);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return im;
    }

    /**
     * Loads a list of supported infrastructures from a configuration file
     * @return list of supported infrastructures
     */
    public static Collection<Class<?>> getSupportedInfrastructures() {
        // reload file each time as it can be updated while the rm is running
        supportedInfrastructures = new ArrayList<Class<?>>();
        Properties properties = new Properties();
        try {
            String propFileName = PAResourceManagerProperties.RM_NODESOURCE_INFRASTRUCTURE_FILE
                    .getValueAsString();
            if (!(new File(propFileName).isAbsolute())) {
                // file path is relative, so we complete the path with the prefix RM_Home constant
                propFileName = PAResourceManagerProperties.RM_HOME.getValueAsString() + File.separator +
                    propFileName;
            }

            properties.load(new FileInputStream(propFileName));
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (Object className : properties.keySet()) {
            try {
                Class<?> cls = Class.forName(className.toString());
                supportedInfrastructures.add(cls);
            } catch (ClassNotFoundException e) {
                ProActiveLogger.getLogger(RMLoggers.NODESOURCE).warn(
                        "Cannot find class " + className.toString());
            }
        }
        return supportedInfrastructures;
    }
}
