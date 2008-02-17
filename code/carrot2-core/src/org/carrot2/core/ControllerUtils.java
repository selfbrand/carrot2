package org.carrot2.core;

import java.util.Map;

import org.carrot2.core.attribute.*;

/**
 * Static life cycle and controller utilities (for use within the core package).
 * <p>
 * This code is refactored to make sure the tests can perform exactly the same sequence of
 * actions without using the controller as a whole.
 */
final class ControllerUtils
{
    /**
     * 
     */
    private ControllerUtils()
    {
        // no instances.
    }

    /**
     * Performs all life cycle actions required upon initialization.
     */
    public static void init(ProcessingComponent processingComponent,
        Map<String, Object> attributes) throws ProcessingException
    {
        try
        {
            AttributeBinder
                .bind(processingComponent, attributes, Init.class, Input.class);
            
            processingComponent.init();

            AttributeBinder.bind(processingComponent, attributes, Init.class,
                Output.class);
        }
        catch (InstantiationException e)
        {
            throw new ProcessingException("Attribute binding failed", e);
        }

    }

    /**
     * Performs all life cycle actions required before processing starts.
     */
    public static void beforeProcessing(ProcessingComponent processingComponent,
        Map<String, Object> attributes) throws ProcessingException
    {
        try
        {
            AttributeBinder.bind(processingComponent, attributes, Processing.class,
                Input.class);

            processingComponent.beforeProcessing();
        }
        catch (InstantiationException e)
        {
            throw new ProcessingException("Attribute binding failed", e);
        }

    }

    /**
     * Perform all life cycle required to do processing.
     */
    public static void performProcessing(ProcessingComponent processingComponent,
        Map<String, Object> attributes) throws ProcessingException
    {
        processingComponent.process();
    }

    /**
     * Perform all life cycle actions after processing is completed.
     */
    public static void afterProcessing(ProcessingComponent processingComponent,
        Map<String, Object> attributes)
    {
        try
        {
            processingComponent.afterProcessing();

            AttributeBinder.bind(processingComponent, attributes, Processing.class,
                Output.class);
        }
        catch (InstantiationException e)
        {
            throw new ProcessingException("Attribute binding failed", e);
        }
    }
}
