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
package org.ow2.proactive.scheduler.ext.matlab.common;

import org.ow2.proactive.scheduler.ext.matsci.common.PASolveMatSciGlobalConfig;


/**
 * PASolveMatlabGlobalConfig configuration of a Matlab PAsolve Job (a job represents a call to PAsolve)
 *
 * @author The ProActive Team
 */
public class PASolveMatlabGlobalConfig extends PASolveMatSciGlobalConfig {

    /**  */
    private static final long serialVersionUID = 31L;
    /**
     * URL of the script used to check licenses
     */
    private String checkLicenceScriptUrl = null;

    /**
     * Options used when writing MAT files (binary Matlab files)
     */
    private String matFileOptions = null;

    /**
     * URL of the license server proxy
     */
    private String licenseServerUrl = null;

    /**
     * Name of the matlab function used to keep checked out tokens alive. This function is executed periodically by matlab.
     */
    private String keepaliveCallbackFunctionName = null;

    /**
     * Name of the matlab function used to check out toolboxes tokens right after matlab starts.
     */
    private String checktoolboxesFunctionName = null;

    /**
     * Do we use Matlab Control or standard matlab batch mode
     */
    private boolean useMatlabControl = false;

    public PASolveMatlabGlobalConfig() {
        transferSource = true;
        transferVariables = true;
    }

    public String getCheckLicenceScriptUrl() {
        return checkLicenceScriptUrl;
    }

    public void setCheckLicenceScriptUrl(String checkLicenceScript) {
        this.checkLicenceScriptUrl = checkLicenceScript;
    }

    public String getMatFileOptions() {
        return matFileOptions;
    }

    public void setMatFileOptions(String matFileOptions) {
        this.matFileOptions = matFileOptions;
    }

    public String getLicenseServerUrl() {
        return licenseServerUrl;
    }

    public void setLicenseServerUrl(String licenseServerUrl) {
        this.licenseServerUrl = licenseServerUrl;
    }

    public String getKeepaliveCallbackFunctionName() {
        return keepaliveCallbackFunctionName;
    }

    public void setKeepaliveCallbackFunctionName(String keepaliveCallbackFunctionName) {
        this.keepaliveCallbackFunctionName = keepaliveCallbackFunctionName;
    }

    public String getChecktoolboxesFunctionName() {
        return checktoolboxesFunctionName;
    }

    public void setChecktoolboxesFunctionName(String checktoolboxesFunctionName) {
        this.checktoolboxesFunctionName = checktoolboxesFunctionName;
    }

    public boolean isUseMatlabControl() {
        return useMatlabControl;
    }

    public void setUseMatlabControl(boolean useMatlabControl) {
        this.useMatlabControl = useMatlabControl;
    }
}
