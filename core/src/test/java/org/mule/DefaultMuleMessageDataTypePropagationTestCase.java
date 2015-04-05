/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.IsNot.not;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.transformer.types.MimeTypes.APPLICATION_XML;
import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.transformer.DataType;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.transformer.types.MimeTypes;

import java.util.Collections;

import javax.activation.DataHandler;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

@SmallTest
public class DefaultMuleMessageDataTypePropagationTestCase extends AbstractMuleTestCase
{

    public static final String DEFAULT_ENCODING = "UTF-8";
    public static final String CUSTOM_ENCODING = "UTF-16";
    private MuleContext muleContext = mock(MuleContext.class, RETURNS_DEEP_STUBS);

    @Before
    public void setUp() throws Exception
    {
        when(muleContext.getConfiguration().getDefaultEncoding()).thenReturn(DEFAULT_ENCODING);
    }

    @Test
    public void usesDefaultEncodingWithNoProperty() throws Exception
    {
        DefaultMuleMessage muleMessage = new DefaultMuleMessage("test", muleContext);

        assertThat(muleMessage.getEncoding(), equalTo(DEFAULT_ENCODING));
        assertThat(muleMessage.getPropertyNames(), not(hasItem(MuleProperties.MULE_ENCODING_PROPERTY)));
    }

    @Test
    public void usesCustomEncodingWithProperty() throws Exception
    {
        DefaultMuleMessage muleMessage = new DefaultMuleMessage("test", muleContext);
        muleMessage.setEncoding(CUSTOM_ENCODING);

        assertThat(muleMessage.getEncoding(), equalTo(CUSTOM_ENCODING));
        assertThat(muleMessage.getPropertyNames(), hasItem(MuleProperties.MULE_ENCODING_PROPERTY));
        assertThat(muleMessage.getProperty(MuleProperties.MULE_ENCODING_PROPERTY), Matchers.<Object>equalTo(CUSTOM_ENCODING));
    }

    @Test
    public void usesNoDefaultMimeTypeWithProperty() throws Exception
    {
        DefaultMuleMessage muleMessage = new DefaultMuleMessage("test", muleContext);

        assertThat(muleMessage.getPropertyNames(), not(hasItem(MuleProperties.CONTENT_TYPE_PROPERTY)));
    }

    @Test
    public void usesCustomMimeTypeWithProperty() throws Exception
    {
        DefaultMuleMessage muleMessage = new DefaultMuleMessage("test", muleContext);
        muleMessage.setEncoding(CUSTOM_ENCODING);
        muleMessage.setMimeType(MimeTypes.APPLICATION_XML);

        assertThat(muleMessage.getPropertyNames(), hasItem(MuleProperties.CONTENT_TYPE_PROPERTY));
        assertThat(muleMessage.getProperty(MuleProperties.CONTENT_TYPE_PROPERTY), Matchers.<Object>equalTo(MimeTypes.APPLICATION_XML + ";charset=" + CUSTOM_ENCODING));
    }

    //TODO(pablo.kraan): DFL - mimeType property: setting enconding after mime type causes wrong mimetye property value

    @Test
    public void setsDefaultDataTypeOnCreation() throws Exception
    {
        assertDefaultDataType(new DefaultMuleMessage("test", muleContext));
        assertDefaultDataType(new DefaultMuleMessage("test", Collections.<String, Object>emptyMap(), muleContext));
        assertDefaultDataType(new DefaultMuleMessage("test", Collections.<String, Object>emptyMap(), Collections.<String, DataHandler>emptyMap(), muleContext));
        assertDefaultDataType(new DefaultMuleMessage("test", Collections.<String, Object>emptyMap(), Collections.<String, Object>emptyMap(), Collections.<String, DataHandler>emptyMap(), muleContext));
    }

    @Test
    public void setsDataTypeFromPreviousMessageOnCreation() throws Exception
    {
        MuleMessage previousMuleMessage = new DefaultMuleMessage(1, muleContext);
        previousMuleMessage.setEncoding("UTF-16");

        assertDataType(new DefaultMuleMessage(previousMuleMessage), previousMuleMessage.getDataType());
    }

    @Test
    public void overridesDataTypeFromPreviousMessageOnCreation() throws Exception
    {
        DefaultMuleMessage previousMuleMessage = new DefaultMuleMessage(1, muleContext);
        previousMuleMessage.setEncoding(CUSTOM_ENCODING);
        //TODO(pablo.kraan): DFL - why setMimeType is not part of the MuleMessage API?
        previousMuleMessage.setMimeType(APPLICATION_XML);

        assertDataType(new DefaultMuleMessage("test", previousMuleMessage, muleContext), String.class, APPLICATION_XML, CUSTOM_ENCODING);
    }

    private void assertDefaultDataType(MuleMessage muleMessage)
    {
        assertDataType(muleMessage, String.class, DataType.ANY_MIME_TYPE, DEFAULT_ENCODING);
    }

    private void assertDataType(MuleMessage muleMessage, Class type, String mimeType, String encoding)
    {
        assertThat(muleMessage.getDataType(), DataTypeMatcher.like(type, mimeType, encoding));
    }

    private void assertDataType(MuleMessage muleMessage, DataType<?> dataType)
    {
        assertThat(muleMessage.getDataType(), DataTypeMatcher.like(dataType));
    }
}
