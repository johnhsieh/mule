package org.mule.config.spring;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorChain;
import org.mule.processor.AbstractMessageProcessorOwner;
import org.mule.processor.chain.DefaultMessageProcessorChainBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class TemplateStage extends AbstractMessageProcessorOwner implements MessageProcessor, Initialisable
{

    private String name;
    protected List<MessageProcessor> messageProcessors = new ArrayList<MessageProcessor>();
    private MessageProcessorChain messageProcessorChain;

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        if (messageProcessorChain != null)
        {
            return messageProcessorChain.process(event);
        }
        return event;
    }

    public void setMessageProcessors(List<MessageProcessor> messageProcessors)
    {
        this.messageProcessors.addAll(messageProcessors);
    }

    public List<MessageProcessor> getMessageProcessors()
    {
        return messageProcessors;
    }


    @Override
    public void initialise() throws InitialisationException
    {
        super.initialise();
        if (!this.messageProcessors.isEmpty())
        {
            try
            {
                this.messageProcessorChain = new DefaultMessageProcessorChainBuilder().chain(this.messageProcessors).build();
            }
            catch (MuleException e)
            {
                throw new InitialisationException(e,this);
            }
        }
    }

    @Override
    protected List<MessageProcessor> getOwnedMessageProcessors()
    {
        return this.messageProcessors;
    }
}