/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.session;

import org.mule.PropertyData;
import org.mule.api.MuleContext;
import org.mule.api.MuleSession;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.security.SecurityContext;
import org.mule.api.transformer.DataType;
import org.mule.config.i18n.CoreMessages;
import org.mule.transformer.types.SimpleDataType;
import org.mule.util.CaseInsensitiveHashMap;
import org.mule.util.UUID;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>DefaultMuleSession</code> manages the interaction and distribution of events for Mule Services.
 */

public final class DefaultMuleSession implements MuleSession
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 3380926585676521866L;

    /**
     * logger used by this class
     */
    private static Log logger = LogFactory.getLog(DefaultMuleSession.class);

    /**
     * Determines if the service is valid
     */
    private boolean valid = true;

    private String id;

    /**
     * The security context associated with the session. Note that this context will only be serialized if the
     * SecurityContext object is Serializable.
     */
    private SecurityContext securityContext;

    private Map<String, PropertyData> properties;

    @Deprecated
    private FlowConstruct flowConstruct;

    public DefaultMuleSession()
    {
        id = UUID.getUUID();
        properties = Collections.synchronizedMap(new CaseInsensitiveHashMap/* <String, Object> */());
    }

    public DefaultMuleSession(MuleSession session)
    {
        this.id = session.getId();
        this.securityContext = session.getSecurityContext();
        this.valid = session.isValid();

        this.properties = Collections.synchronizedMap(new CaseInsensitiveHashMap/* <String, Object> */());
        for (String key : session.getPropertyNamesAsSet())
        {
            this.properties.put(key, session.getPropertyData(key));
        }
    }

    // Deprecated constructor

    @Deprecated
    public DefaultMuleSession(MuleContext muleContext)
    {
        this();
    }

    @Deprecated
    public DefaultMuleSession(FlowConstruct flowConstruct, MuleContext muleContext)
    {
        this();
        this.flowConstruct = flowConstruct;
    }

    @Deprecated
    public DefaultMuleSession(MuleSession source, MuleContext muleContext)
    {
        this(source);
    }

    @Deprecated
    public DefaultMuleSession(MuleSession source, FlowConstruct flowConstruct)
    {
        this(source);
        this.flowConstruct = flowConstruct;
    }

    @Override
    public String getId()
    {
        return id;
    }

    @Override
    public boolean isValid()
    {
        return valid;
    }

    @Override
    public void setValid(boolean value)
    {
        valid = value;
    }

    /**
     * The security context for this session. If not null outbound, inbound and/or method invocations will be
     * authenticated using this context
     *
     * @param context the context for this session or null if the request is not secure.
     */
    @Override
    public void setSecurityContext(SecurityContext context)
    {
        securityContext = context;
    }

    /**
     * The security context for this session. If not null outbound, inbound and/or method invocations will be
     * authenticated using this context
     *
     * @return the context for this session or null if the request is not secure.
     */
    @Override
    public SecurityContext getSecurityContext()
    {
        return securityContext;
    }

    /**
     * Will set a session level property. These will either be stored and retrieved using the underlying
     * transport mechanism of stored using a default mechanism
     *
     * @param key the key for the object data being stored on the session
     * @param value the value of the session data
     */
    @Override
    public void setProperty(String key, Object value)
    {
        if (!(value instanceof Serializable))
        {
            logger.warn(CoreMessages.sessionPropertyNotSerializableWarning(key));
        }

        //TODO(pablo.kraan): DFL - check datatype construction arguments
        DataType dataType = new SimpleDataType(value == null ? Object.class : value.getClass(), null);
        properties.put(key, new PropertyData(value, dataType));
    }

    /**
     * Will retrieve a session level property.
     *
     * @param key the key for the object data being stored on the session
     * @return the value of the session data or null if the property does not exist
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getProperty(Object key)
    {
        PropertyData propertyData = properties.get(key);
        return propertyData == null ? null : (T) propertyData.getValue();
    }

    /**
     * Will retrieve a session level property and remove it from the session
     *
     * @param key the key for the object data being stored on the session
     * @return the value of the session data or null if the property does not exist
     */
    @Override
    public Object removeProperty(Object key)
    {
        return properties.remove(key);
    }

    /**
     * Returns an iterater of property keys for the session properties on this session
     *
     * @return an iterater of property keys for the session properties on this session
     * @deprecated Use getPropertyNamesAsSet() instead
     */
    @Override
    @Deprecated
    public Iterator<String> getPropertyNames()
    {
        return properties.keySet().iterator();
    }

    @Override
    public Set<String> getPropertyNamesAsSet()
    {
        return Collections.unmodifiableSet(properties.keySet());
    }

    public void merge(MuleSession updatedSession)
    {
        if (updatedSession == null)
        {
            return;
        }
        Iterator<Entry<String, PropertyData>> propertyIterator = properties.entrySet().iterator();
        while (propertyIterator.hasNext())
        {
            final Entry<String, PropertyData> entry = propertyIterator.next();
            if (entry.getValue().getValue() instanceof Serializable)
            {
                propertyIterator.remove();
            }
        }
        for (String updatedPropertyKey : updatedSession.getPropertyNamesAsSet())
        {
            this.properties.put(updatedPropertyKey, updatedSession.getPropertyData(updatedPropertyKey));
        }
    }

    public Map<String, Object> getProperties()
    {
        Map<String, Object> result = new HashMap<>();
        for (String key : properties.keySet())
        {
            PropertyData propertyData = properties.get(key);
            result.put(key, propertyData.getValue());
        }

        return result;
    }

    public Map<String, PropertyData> getExtendedProperties()
    {
        return properties;
    }

    void removeNonSerializableProperties()
    {
        Iterator<Entry<String, PropertyData>> propertyIterator = properties.entrySet().iterator();
        while (propertyIterator.hasNext())
        {
            final Entry<String, PropertyData> entry = propertyIterator.next();
            if (!(entry.getValue().getValue() instanceof Serializable))
            {
                logger.warn(CoreMessages.propertyNotSerializableWasDropped(entry.getKey()));
                propertyIterator.remove();
            }
        }
    }

    @Override
    public void setProperty(String key, Serializable value)
    {
        setProperty(key, (Object) value);
    }

    @Override
    public <T> T getProperty(String key)
    {
        return this.<T> getProperty((Object) key);
    }

    @Override
    public Object removeProperty(String key)
    {
        return removeProperty((Object) key);
    }

    // //////////////////////////
    // Serialization methods
    // //////////////////////////

    private void writeObject(ObjectOutputStream out) throws IOException
    {
        // Temporally replaces the properties to write only serializable values into the stream
        DefaultMuleSession copy = new DefaultMuleSession(this);
        copy.removeNonSerializableProperties();
        Map<String, PropertyData> backupProperties = properties;
        try
        {
            properties = copy.properties;
            out.defaultWriteObject();
        }
        finally
        {
            properties = backupProperties;
        }
    }

    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
    }

    @Override
    public void clearProperties()
    {
        properties.clear();
    }

    @Override
    public FlowConstruct getFlowConstruct()
    {
        return flowConstruct;
    }

    @Override
    public void setFlowConstruct(FlowConstruct flowConstruct)
    {
        this.flowConstruct = flowConstruct;
    }

    @Override
    public PropertyData getPropertyData(String key)
    {
        return properties.get(key);
    }
}
