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
package org.ow2.proactive.scheduler.task;

import java.io.File;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.Serializable;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Hashtable;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.swing.JPanel;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.log4j.Logger;
import org.hibernate.annotations.AccessType;
import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyMetaDef;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.MetaValue;
import org.hibernate.annotations.Proxy;
import org.hibernate.annotations.Type;
import org.objectweb.proactive.core.util.converter.ByteToObjectConverter;
import org.objectweb.proactive.core.util.converter.ObjectToByteConverter;
import org.objectweb.proactive.core.util.log.ProActiveLogger;
import org.ow2.proactive.db.annotation.Unloadable;
import org.ow2.proactive.db.types.BigString;
import org.ow2.proactive.scheduler.common.exception.InternalSchedulerException;
import org.ow2.proactive.scheduler.common.task.Log4JTaskLogs;
import org.ow2.proactive.scheduler.common.task.ResultPreview;
import org.ow2.proactive.scheduler.common.task.SimpleTaskLogs;
import org.ow2.proactive.scheduler.common.task.TaskId;
import org.ow2.proactive.scheduler.common.task.TaskLogs;
import org.ow2.proactive.scheduler.common.task.TaskResult;
import org.ow2.proactive.scheduler.common.task.flow.FlowAction;
import org.ow2.proactive.scheduler.common.task.util.ResultPreviewTool.SimpleTextPanel;
import org.ow2.proactive.scheduler.util.SchedulerDevLoggers;
import org.ow2.proactive.scheduler.util.classloading.TaskClassLoader;
import org.ow2.proactive.utils.Formatter;


/**
 * Class representing the task result.
 * A task result can be an exception or a serializable object that you have to cast into your own type.
 * Before getting the object it is recommended that you call the hadException() method.
 * It will tell you if an exception occurred in the task that generate this result.
 *
 * @author The ProActive Team
 * @since ProActive Scheduling 0.9
 */
@Entity
@Table(name = "TASK_RESULT_IMPL")
@AccessType("field")
@Proxy(lazy = false)
@XmlAccessorType(XmlAccessType.FIELD)
public class TaskResultImpl implements TaskResult {
    /**  */
    private static final long serialVersionUID = 31L;

    public static final Logger logger_dev = ProActiveLogger.getLogger(SchedulerDevLoggers.CORE);

    @Id
    @GeneratedValue
    @SuppressWarnings("unused")
    @XmlTransient
    private long hId;

    /** The task identification of the result */
    @Cascade(CascadeType.ALL)
    @OneToOne(fetch = FetchType.EAGER, targetEntity = TaskIdImpl.class)
    private TaskId id = null;

    /** The value of the result if no exception occurred as a byte array */
    @Unloadable
    @Column(name = "SERIALIZED_VALUE", updatable = false, length = Integer.MAX_VALUE)
    @Type(type = "org.ow2.proactive.scheduler.core.db.schedulerType.BinaryLargeOBject")
    @Lob
    private byte[] serializedValue = null;
    /** The value of the result if no exception occurred */
    @Transient
    private transient Serializable value = null;

    /** The exception thrown by the task as a byte array */
    @Unloadable
    @Column(name = "SERIALIZED_EXCEPTION", updatable = false, length = Integer.MAX_VALUE)
    @Type(type = "org.ow2.proactive.scheduler.core.db.schedulerType.BinaryLargeOBject")
    @Lob
    private byte[] serializedException = null;
    /** The exception thrown by the task */
    @Transient
    private transient Throwable exception = null;

    /** Task output */
    @Unloadable
    @Any(fetch = FetchType.EAGER, metaColumn = @Column(name = "OUTPUT_TYPE", updatable = false, length = 5))
    @AnyMetaDef(idType = "long", metaType = "string", metaValues = {
            @MetaValue(targetEntity = Log4JTaskLogs.class, value = "LTL"),
            @MetaValue(targetEntity = SimpleTaskLogs.class, value = "STL") })
    @JoinColumn(name = "OUTPUT_ID", updatable = false)
    @Cascade(CascadeType.ALL)
    private TaskLogs output = null;

    /** Description definition of this result */
    @Unloadable
    @Column(name = "PREVIEWER_CLASSNAME", updatable = false)
    private String previewerClassName = null;

    /** An instance that describe how to display the result of this task. */
    @Transient
    private transient ResultPreview descriptor = null;

    // this classpath is used on client side to instantiate the previewer (can be null)
    @Unloadable
    @Column(name = "JOB_CLASSPATH", updatable = false, columnDefinition = "BLOB")
    @Type(type = "org.ow2.proactive.scheduler.core.db.schedulerType.CharacterLargeOBject")
    private String[] jobClasspath;

    /** result of the FlowScript if there was one, or null */
    @Cascade(CascadeType.ALL)
    @OneToOne(fetch = FetchType.EAGER, targetEntity = FlowAction.class)
    private FlowAction flowAction = null;

    //Managed by taskInfo, this field is here only to bring taskDuration to core AO
    @Transient
    private long taskDuration = -1;

    /** All the properties propagated through Exporter.propagateProperty() */
    // @Unloadable // BUG ? DON't WORK WITH UNLOADABLE
    @OneToMany(cascade = javax.persistence.CascadeType.ALL)
    @Cascade(CascadeType.ALL)
    @LazyCollection(value = LazyCollectionOption.FALSE)
    private Map<String, BigString> propagatedProperties;

    /** ProActive empty constructor. */
    public TaskResultImpl() {
    }

    private TaskResultImpl(TaskId id, TaskLogs output) {
        this.id = id;
        this.output = output;
    }

    /**
     * Return a new instance of task result represented by a task id, its result and its output.
     *
     * @param id the identification of the task that send this result.
     * @param value the result of the task.
     * @param output the output of the task.
     * @param execDuration the execution duration of the task itself
     * @throws IOException
     */
    public TaskResultImpl(TaskId id, Serializable value, TaskLogs output, long execDuration) {
        this(id, value, output, execDuration, null);
    }

    /**
     * Return a new instance of task result represented by a task id and its exception.
     *
     * @param id the identification of the task that send this result.
     * @param exception the exception that occurred in the task.
     * @param output the output of the task.
     * @param execDuration the execution duration of the task itself
     */
    public TaskResultImpl(TaskId id, Throwable value, TaskLogs output, long execDuration) {
        this(id, value, output, execDuration, null);
    }

    /**
     * Return a new instance of task result represented by a task id, its result and its output.
     *
     * @param id the identification of the task that send this result.
     * @param value the result of the task.
     * @param output the output of the task.
     * @param execDuration the execution duration of the task itself
     * @param propagatedProperties the map of all properties that has been propagated through Exporter.propagateProperty
     * @throws IOException
     */
    public TaskResultImpl(TaskId id, Serializable value, TaskLogs output, long execDuration,
            Map<String, BigString> propagatedProperties) {
        this(id, output);
        this.taskDuration = execDuration;
        this.value = value;
        this.propagatedProperties = propagatedProperties;
        try {
            //try to serialize user result
            this.serializedValue = ObjectToByteConverter.ObjectStream.convert(value);
        } catch (IOException ioe1) {
            //error while serializing
            logger_dev.error("", ioe1);
            try {
                //try to serialize the cause as an exception
                this.serializedException = ObjectToByteConverter.ObjectStream.convert(ioe1);
            } catch (IOException ioe2) {
                //cannot serialize the cause
                logger_dev.error("", ioe2);
                try {
                    //serialize a NotSerializableException with the cause message
                    this.serializedException = ObjectToByteConverter.ObjectStream
                            .convert(new NotSerializableException(ioe2.getMessage()));
                } catch (IOException ioe3) {
                    //this should not append as the NotSerializableException is serializable and
                    //the given argument is a string (also serializable)
                    logger_dev.error("", ioe3);
                }
            }
        }
    }

    /**
     * Return a new instance of task result represented by a task id and its exception.
     *
     * @param id the identification of the task that send this result.
     * @param exception the exception that occurred in the task.
     * @param output the output of the task.
     * @param execDuration the execution duration of the task itself
     * @param propagatedProperties the map of all properties that has been propagated through Exporter.propagateProperty
     */
    public TaskResultImpl(TaskId id, Throwable exception, TaskLogs output, long execDuration,
            Map<String, BigString> propagatedProperties) {
        this(id, output);
        this.taskDuration = execDuration;
        this.exception = exception;
        this.propagatedProperties = propagatedProperties;
        try {
            //try to serialize the user exception
            this.serializedException = ObjectToByteConverter.ObjectStream.convert(exception);
        } catch (IOException ioe2) {
            //cannot serialize the exception
            logger_dev.error("", ioe2);
            try {
                //serialize a NotSerializableException with the cause message
                this.serializedException = ObjectToByteConverter.ObjectStream
                        .convert(new NotSerializableException(ioe2.getMessage()));
            } catch (IOException ioe3) {
                //this should not append as the NotSerializableException is serializable and
                //the given argument is a string (also serializable)
                logger_dev.error("", ioe3);
            }
        }
    }

    /**
     * If a FlowScript was executed on this task, its result
     * is stored so that the action can be performed later when
     * processed by the core.
     * 
     * return the Action to perform for this task
     */
    public FlowAction getAction() {
        return this.flowAction;
    }

    /**
     * If a FlowScript was executed on this task, its result
     * is stored so that the action can be performed later when
     * processed by the core.
     * 
     * @param act an Control Flow action to embed in this TaskResult
     */
    public void setAction(FlowAction act) {
        this.flowAction = act;
    }

    /**
     * @param the output of the task
     */
    public void setLogs(TaskLogs l) {
        this.output = l;
    }

    /**
     * @param propagatedProperties the map of all properties that has been propagated through Exporter.propagateProperty
     */
    public void setPropagatedProperties(Map<String, BigString> props) {
        this.propagatedProperties = props;
    }

    /**
     * @see org.ow2.proactive.scheduler.common.task.TaskResult#hadException()
     */
    public boolean hadException() {
        return serializedException != null;
    }

    /**
     * @see org.ow2.proactive.scheduler.common.task.TaskResult#getTaskId()
     */
    public TaskId getTaskId() {
        return id;
    }

    /**
     * @see org.ow2.proactive.scheduler.common.task.TaskResult#value()
     */
    public Serializable value() throws Throwable {
        if (hadException()) {
            Throwable thrown = null;
            try {
                thrown = this.instanciateException(this.getTaskClassLoader());
            } catch (IOException e) {
                throw new InternalSchedulerException("Cannot instanciate exception thrown by the task " +
                    this.id + " : " + e.getMessage());
            } catch (ClassNotFoundException e) {
                throw new InternalSchedulerException("Cannot instanciate exception thrown by the task " +
                    this.id + " : " + e.getMessage());
            }
            throw thrown;
        } else {
            try {
                return this.instanciateValue(this.getTaskClassLoader());
            } catch (IOException e) {
                logger_dev.error("", e);
                throw new InternalSchedulerException("Cannot instanciate result of the task " + this.id +
                    " : " + e.getMessage());
            } catch (ClassNotFoundException e) {
                logger_dev.error("", e);
                throw new InternalSchedulerException("Cannot instanciate result of the task " + this.id +
                    " : " + e.getMessage());
            }
        }
    }

    /**
     * @see org.ow2.proactive.scheduler.common.task.TaskResult#getException()
     */
    public Throwable getException() {
        if (hadException()) {
            try {
                return this.instanciateException(this.getTaskClassLoader());
            } catch (IOException e) {
                return new InternalSchedulerException("Cannot instanciate exception thrown by the task " +
                    this.id + " : " + e.getMessage());
            } catch (ClassNotFoundException e) {
                return new InternalSchedulerException("Cannot instanciate exception thrown by the task " +
                    this.id + " : " + e.getMessage());
            }
        } else {
            return null;
        }
    }

    /**
     * Set the class that is able to describe this result. See ResultPreview.
     *
     * @param descClass the name of the class that is able to describe this result.
     */
    public void setPreviewerClassName(String descClass) {
        if (this.previewerClassName != null) {
            throw new RuntimeException("Previewer class cannot be changed");
        } else {
            this.previewerClassName = descClass;
        }
    }

    /**
     * Set the classpath of the job that contained the corresponding task.
     *
     * @param jcp the classpath of the job
     */
    public void setJobClasspath(String[] jcp) {
        this.jobClasspath = jcp;
    }

    /**
     * @see org.ow2.proactive.scheduler.common.task.TaskResult#getGraphicalDescription()
     */
    @XmlTransient
    public JPanel getGraphicalDescription() {
        boolean instanciation = false;
        try {
            instanciation = this.instanciateDescriptor();
        } catch (ClassNotFoundException e) {
            return new SimpleTextPanel(
                "[ERROR] Previewer classes cannot be found. Cannot create graphical previewer: " +
                    System.getProperty("line.separator") + e);
        } catch (Exception e) {
            return new SimpleTextPanel("[ERROR] Cannot create graphical previewer: " +
                System.getProperty("line.separator") + e);
        }
        if (instanciation) {
            JPanel ret = this.descriptor.getGraphicalDescription(this);
            return ret != null ? ret : new SimpleTextPanel(
                "[ERROR] Graphical preview returned by previewer " + this.descriptor.getClass().getName() +
                    " is null");
        } else {
            return new SimpleTextPanel(this.getTextualDescription());
        }
    }

    /**
     * @see org.ow2.proactive.scheduler.common.task.TaskResult#getTextualDescription()
     */
    public String getTextualDescription() {
        boolean instanciation = false;
        try {
            instanciation = this.instanciateDescriptor();
        } catch (ClassNotFoundException e) {
            return "[ERROR] Previewer classes cannot be found. Cannot create textual previewer: " +
                System.getProperty("line.separator") + e;
        } catch (Exception e) {
            return "[ERROR] Cannot create textual previewer: " + System.getProperty("line.separator") + e;
        }
        if (instanciation) {
            String ret = this.descriptor.getTextualDescription(this);
            return ret != null ? ret : "[ERROR] Textual preview returned by previewer " +
                this.descriptor.getClass().getName() + " is null";
        } else if (!this.hadException()) {
            return "[DEFAULT DESCRIPTION] " + value;
        } else {
            // yes, Guillaume, I know...
            return "[DEFAULT DESCRIPTION] " + Formatter.stackTraceToString(exception);
        }
    }

    /**
     * Create the descriptor instance if descriptor class is available.
     * This descriptor is instanciated in a dedicated URLClassloader build on
     * the job classpath.
     * @return true if the creation occurs, false otherwise
     */
    private boolean instanciateDescriptor() throws InstantiationException, IllegalAccessException,
            IOException, ClassNotFoundException {
        if (this.descriptor == null) {
            ClassLoader cl = this.getTaskClassLoader();
            boolean isInstanciated = false;
            // if a specific previewer is defined, instanciate it
            if (this.previewerClassName != null) {
                Class<?> previewClass = Class.forName(this.previewerClassName, true, cl);
                this.descriptor = (ResultPreview) (previewClass.newInstance());
                isInstanciated = true;
            }
            // in any case, instanciate value and exception
            if (this.serializedException != null) {
                this.exception = this.instanciateException(cl);
            } else {
                this.value = this.instanciateValue(cl);
            }
            return isInstanciated;
        } else {
            return true;
        }
    }

    /**
     * Instanciate thrown exception if any from the serialized version.
     * This instanciation must not be performed in a dedicated classloader to
     * avoid ClassCastException with the user's code.
     * @param cl the classloader where to instanciate the object. Can be null : object is instanciated 
     * in the default caller classloader. 
     * @return the exception that has been thrown if any.
     * @throws ClassNotFoundException 
     * @throws IOException 
     */
    private Throwable instanciateException(ClassLoader cl) throws IOException, ClassNotFoundException {
        if (this.serializedException != null && this.exception == null) {
            this.exception = (Throwable) ByteToObjectConverter.ObjectStream.convert(this.serializedException,
                    cl);
        }
        return this.exception;
    }

    /**
     * Instanciate value if any from the serialized version.
     * This instanciation must not be performed in a dedicated classloader to
     * avoid ClassCastException with the user's code.
     * @param cl the classloader where to instanciate the object. Can be null : object is instanciated 
     * in the default caller classloader. 
     * @return the value if no exception has been thown.
     * @throws ClassNotFoundException 
     * @throws IOException 
     */
    private Serializable instanciateValue(ClassLoader cl) throws IOException, ClassNotFoundException {
        if (this.serializedValue != null && this.value == null) {
            this.value = (Serializable) ByteToObjectConverter.ObjectStream.convert(this.serializedValue, cl);
        }
        return this.value;
    }

    /**
     * Return the classloader for the given jobclasspath if any.
     * @return on worker node, the taskClassLoader. On client side, an URL classloader created from the jobclasspath,
     * or the ClassLoader that has loaded the current class if no jobclasspath is set.
     * @throws IOException if the classloader cannot be created.
     */
    private ClassLoader getTaskClassLoader() throws IOException {
        ClassLoader currentCCL = Thread.currentThread().getContextClassLoader();
        if (!(currentCCL instanceof TaskClassLoader)) {
            ClassLoader thisClassLoader = this.getClass().getClassLoader();
            if (this.jobClasspath != null) {
                //we are not on a worker and jcp is set...
                URL[] urls = new URL[this.jobClasspath.length];
                for (int i = 0; i < this.jobClasspath.length; i++) {
                    urls[i] = new File(this.jobClasspath[i]).toURI().toURL();
                }
                return new URLClassLoader(urls, thisClassLoader);
            } else {
                //we are not on a worker and jcp is set...
                return thisClassLoader;
            }
        } else {
            //we are on a worker, use taskclassloader
            return currentCCL;
        }
    }

    /**
     * Get the serializedValue.
     *
     * @return the serializedValue.
     */
    public byte[] getSerializedValue() {
        return serializedValue;
    }

    /**
     * Get the serializedException.
     *
     * @return the serializedException.
     */
    public byte[] getSerializedException() {
        return serializedException;
    }

    /**
     * @see org.ow2.proactive.scheduler.common.task.TaskResult#getOutput()
     */
    public TaskLogs getOutput() {
        return this.output;
    }

    /**
     * Get the previewerClassName.
     *
     * @return the previewerClassName.
     */
    public String getPreviewerClassName() {
        return previewerClassName;
    }

    /**
     * Get the jobClasspath.
     *
     * @return the jobClasspath.
     */
    public String[] getJobClasspath() {
        return jobClasspath;
    }

    /**
     * Get the real task duration. This duration is the CPU time usage of the associated executable.
     * 
     * @return the real task duration.
     */
    public long getTaskDuration() {
        return taskDuration;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        try {
            if (hadException()) {
                return getException().toString();
            } else {
                return value().toString();
            }
        } catch (Throwable t) {
            return "Result not available";
        }
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, String> getPropagatedProperties() {
        // null if no prop has been propagated
        if (this.propagatedProperties == null) {
            return null;
        } else {
            Map<String, String> convertedProperties = new Hashtable<String, String>(this.propagatedProperties
                    .size());
            for (String k : this.propagatedProperties.keySet()) {
                convertedProperties.put(k, this.propagatedProperties.get(k).getValue());
            }
            return convertedProperties;
        }
    }
}
