/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformer.simple;

import org.mule.api.AnnotatedObject;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transformer.DataType;
import org.mule.transformer.types.SimpleDataType;
import org.mule.transport.NullPayload;
import org.mule.util.AttributeEvaluator;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.namespace.QName;

/**
 *  Modifies the payload of a {@link MuleMessage} according to the provided value.
 */
public class SetPayloadMessageProcessor implements MessageProcessor, MuleContextAware, AnnotatedObject, Initialisable
{

    /**
     * The name that identifies this transformer. If none is set the class name of
     * the transformer is used
     */
    //TODO(pablo.kraan): DFL - deprecate or remove this attribute
    protected String name = null;
    protected String mimeType;
    protected String encoding;
    private final Map<QName, Object> annotations = new ConcurrentHashMap<>();
    private AttributeEvaluator valueEvaluator;
    private MuleContext muleContext;
    private Class<?> returnClass;


    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        Object value;
        if (valueEvaluator.getRawValue() == null)
        {
            value = NullPayload.getInstance();
        }
        else
        {
            value = valueEvaluator.resolveValue(event.getMessage());
        }

        DataType dataType = createDataType(value);

        event.getMessage().setPayload(value, dataType);

        return event;
    }

    private DataType createDataType(Object value)
    {
        Class type;
        if (returnClass != null)
        {
            type = returnClass;
        }
        else
        {
            type = (value == null || value instanceof NullPayload) ? Object.class : value.getClass();
        }

        SimpleDataType simpleDataType = new SimpleDataType(type, mimeType);
        simpleDataType.setEncoding(encoding);

        return simpleDataType;

    }

    @Override
    public final Object getAnnotation(QName qName)
    {
        return annotations.get(qName);
    }

    @Override
    public final Map<QName, Object> getAnnotations()
    {
        return Collections.unmodifiableMap(annotations);
    }

    @Override
    public synchronized final void setAnnotations(Map<QName, Object> newAnnotations)
    {
        annotations.clear();
        annotations.putAll(newAnnotations);
    }

    public void setName(String name)
    {
        this.name = name;
    }

    @Deprecated
    public void setIgnoreBadInput(boolean ignoreBadInput)
    {
        // Do nothing
    }

    public void setMimeType(String mimeType)
    {
        this.mimeType = mimeType;
    }

    public void setEncoding(String encoding)
    {
        this.encoding = encoding;
    }

    public void setReturnClass(Class<?> returnClass)
    {
        this.returnClass = returnClass;
    }

    public void setValue(String value)
    {
        valueEvaluator = new AttributeEvaluator(value);
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        muleContext = context;
    }

    @Override
    public void initialise() throws InitialisationException
    {
        valueEvaluator.initialize(muleContext.getExpressionManager());
    }
}
