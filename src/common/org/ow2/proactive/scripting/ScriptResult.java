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
package org.ow2.proactive.scripting;

import java.io.Serializable;

import org.objectweb.proactive.annotation.PublicAPI;


/**
 *
 * The class implements a script result container.
 * The script result is an object typed with the template class, or
 * an exception raised by the script execution.
 * @author ProActive team
 *
 * @param <E> template class for the result.
 */
@PublicAPI
public class ScriptResult<E> implements Serializable {

    /**  */
    private static final long serialVersionUID = 31L;
    /** Result of the script */
    private E result = null;
    /** Exception in the result if so */
    private Throwable exception = null;

    /**
     * ProActive empty constructor
     */
    public ScriptResult() {
    }

    /** Constructor
     * @param result result to store
     * @param exception eventual exception representing the result
     */
    public ScriptResult(E result, Throwable exception) {
        this.result = result;
        this.exception = exception;
    }

    /**
     * Create a new instance of ScriptResult
     * 
     * @param result result to store
     */
    public ScriptResult(E result) {
        this(result, null);
    }

    /** Constructor
     * @param exception to store as a script result
     */
    public ScriptResult(Throwable exception) {
        this(null, exception);
    }

    /** tell if an exception has been raised during
     * script execution.
     * @return true if an exception occured, false otherwise.
     */
    public boolean errorOccured() {
        return exception != null;
    }

    /**
     * Return the eventual exception of the script's execution.
     * @return Throwable representing the exception.
     */
    public Throwable getException() {
        return exception;
    }

    /**
     * Set an exception qs result.
     * @param exception exception to set.
     */
    public void setException(Throwable exception) {
        this.exception = exception;
    }

    /**
     * Return the result's object
     * @return result object.
     */
    public E getResult() {
        return result;
    }

    /**
     * Set the result
     * @param result result to set
     */
    public void setResult(E result) {
        this.result = result;
    }
}
