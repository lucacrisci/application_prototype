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
package functionaltests.executables;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

import org.ow2.proactive.scheduler.common.task.TaskResult;
import org.ow2.proactive.scheduler.common.task.executable.JavaExecutable;


public class ResultAsArray extends JavaExecutable {

    private int size = 10;

    //	public ResultAsArray(){}

    @Override
    public Serializable execute(TaskResult... results) throws Throwable {
        final ArrayList<RenderedBucket> renderedBucketList = new ArrayList<RenderedBucket>(size);
        for (int i = 0; i < size; i++) {
            float[] tab = new float[3 * 32 * 32]; // 3072 color component test
            Arrays.fill(tab, (float) Math.random());
            renderedBucketList.add(new RenderedBucket(i * 1, i * 2, i * 3, i * 4, tab));
        }
        return renderedBucketList;
    }

    static class RenderedBucket implements Serializable {
        /**  */
        private static final long serialVersionUID = 31L;
        private int x, y, w, h;
        private float[] color;

        public RenderedBucket(int x, int y, int w, int h, float[] color) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.color = color;
        }

        @Override
        public String toString() {
            return "" + x + y + w + h;
        }
    }
}