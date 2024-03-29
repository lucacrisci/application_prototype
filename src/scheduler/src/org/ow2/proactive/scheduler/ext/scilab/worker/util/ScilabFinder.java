/*
 * ################################################################
 *
 * ProActive Parallel Suite(TM): The Java(TM) library for
 *    Parallel, Distributed, Multi-Core Computing for
 *    Enterprise Grids & Clouds 
 *
 * Copyright (C) 1997-2010 INRIA/University of 
 * 				Nice-Sophia Antipolis/ActiveEon
 * Contact: proactive@ow2.org or contact@activeeon.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 3 of
 * the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 * USA
 *
 * If needed, contact us to obtain a release under GPL Version 2 
 * or a different license than the GPL.
 *
 *  Initial developer(s):               The ProActive Team
 *                        http://proactive.inria.fr/team_members.htm
 *  Contributor(s): ActiveEon Team - http://www.activeeon.com
 *
 * ################################################################
 * $$ACTIVEEON_CONTRIBUTOR$$
 */
package org.ow2.proactive.scheduler.ext.scilab.worker.util;

import java.util.ArrayList;
import java.util.HashSet;

import org.ow2.proactive.scheduler.ext.matsci.worker.util.MatSciEngineConfig;
import org.ow2.proactive.scheduler.ext.matsci.worker.util.MatSciFinder;


public class ScilabFinder extends MatSciFinder {

    private static ScilabFinder instance = null;

    protected ScilabFinder() {

    }

    public static ScilabFinder getInstance() {
        if (instance == null) {
            instance = new ScilabFinder();
        }
        return instance;
    }

    /**
     * Utility function to find Matlab
     */
    public MatSciEngineConfig findMatSci(String version_pref, String versionsRej, String versionMin,
            String versionMax) throws Exception {
        return findMatSci(version_pref, parseVersionRej(versionsRej), versionMin, versionMax);
    }

    public MatSciEngineConfig findMatSci(String version_pref, HashSet<String> versionsRej, String versionMin,
            String versionMax) throws Exception {
        ArrayList<MatSciEngineConfig> confs = ScilabConfigurationParser.getConfigs();

        if (confs == null) {
            System.out.println("No configuration found");
            return null;
        }
        for (MatSciEngineConfig conf : confs) {
            System.out.println("Found " + conf.getVersion());
        }
        MatSciEngineConfig answer = chooseMatSciConfig(confs, version_pref, versionsRej, versionMin,
                versionMax);
        if (answer == null) {
            System.out.println("No configuration accepted");
        }

        return answer;
    }

}
