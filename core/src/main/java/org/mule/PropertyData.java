/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule;

import org.mule.api.transformer.DataType;

import java.io.Serializable;

/**
 *
 */
public class PropertyData implements Serializable
{

    private final Object value;

    private final DataType<Object> dataType;

    public PropertyData(Object value, DataType<Object> dataType)
    {
        this.value = value;
        this.dataType = dataType;
    }

    public DataType<Object> getDataType()
    {
        return dataType;
    }

    public Object getValue()
    {
        return value;
    }


}
