package com.dawidweiss.carrot.filter.langguesser;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.dawidweiss.carrot.core.local.LocalComponent;
import com.dawidweiss.carrot.core.local.LocalFilterComponentBase;
import com.dawidweiss.carrot.core.local.ProcessingException;
import com.dawidweiss.carrot.core.local.clustering.RawDocument;
import com.dawidweiss.carrot.core.local.clustering.RawDocumentsConsumer;
import com.dawidweiss.carrot.core.local.clustering.RawDocumentsProducer;
import com.dawidweiss.carrot.core.local.linguistic.LanguageGuesser;

/**
 * An local component that adds a language code
 * to the properties of a document. 
 * 
 * <p>The component implements {@link RawDocumentConsumer}
 * and {@link RawDocumentProducer}, so basically, it acts
 * as a filter between two other components. 
 * 
 * @author Dawid Weiss
 * @version $Revision$
 */
public class RawDocumentLanguageDetection extends LocalFilterComponentBase
    implements RawDocumentsProducer, RawDocumentsConsumer {
    
    /**
     * Capabilities exposed by this component.
     */
    private static final Set CAPABILITIES_COMPONENT = new HashSet(Arrays.asList(
                new Object[] { RawDocumentsProducer.class, RawDocumentsConsumer.class }));

    /**
     * Capabilities required of the successor of this component.
     */
    private static final Set CAPABILITIES_SUCCESSOR = new HashSet(Arrays.asList(
                new Object[] { RawDocumentsConsumer.class, }));

    /**
     * Capabilities required of the predecessor of this component.
     */
    private static final Set CAPABILITIES_PREDECESSOR = new HashSet(Arrays.asList(
                new Object[] { RawDocumentsProducer.class, }));

    /**
     * A constant with the maximum size of the detection buffer.
     */
    public final static int MAX_DETECTION_BUFFER_SIZE = 500;
    
    /**
     * The successor component, consumer of
     * documents accepted by this component.
     */
	private RawDocumentsConsumer rawDocumentConsumer;
    
    /**
     * Language guesser used for language detection.
     */
    private final LanguageGuesser guesser;
    
    /**
     * Public contructor requires an instance of {@link LanguageGuesser}
     * interface that is used for language detection.
     * @param languageGuesser A language guesser instance.
     */
    public RawDocumentLanguageDetection(LanguageGuesser languageGuesser) {
    	this.guesser = languageGuesser;
    }
    
    /**
     * A buffer which is used to concatenate
     * input document's title and snippet. 
     */
    private char [] buffer = new char [MAX_DETECTION_BUFFER_SIZE];
    
    /**
     * Length of the current text in the buffer.
     */
    private int bufferLength;
    
	/**
     * New document to process: identify language and
     * add a new property to the document.
     * 
	 * @see com.dawidweiss.carrot.core.local.clustering.RawDocumentsConsumer#addDocument(com.dawidweiss.carrot.core.local.clustering.RawDocument)
	 */
	public void addDocument(RawDocument doc) throws ProcessingException {
        resetBuffer();
        // concatenate title and snippet first.

        String title = doc.getTitle();
        if (title != null) {
            append( title );
        }
        String snippet = (String) doc.getProperty(RawDocument.PROPERTY_SNIPPET);
        if (snippet != null) {
            if (title != null)
                append(". ");
            append(snippet);
        }
        
        if (bufferLength > 0) {
        	// attempt to identify the language of this document.
            String lang = guesser.guessLanguage(buffer, 0, bufferLength);
            if (lang != null) {
                doc.setProperty(RawDocument.PROPERTY_LANGUAGE, lang);
            }
        }

        // pass the document reference...
        this.rawDocumentConsumer.addDocument(doc);
	}
    
    /**
     * Resets the internal language identification buffer.
     */
    protected final void resetBuffer() {
        this.bufferLength = 0;
    }
    
    /**
     * Appends a string to the internal array. If the buffer
     * is shorter than the string, the string is truncated.
     * 
     * @param string The string to append.
     */
    protected final void append(String string) {
        int charsLeft = Math.min( buffer.length - bufferLength, string.length()); 
        string.getChars(0, charsLeft, buffer, bufferLength);
        bufferLength += charsLeft;
    }
    
    /**
     * Sets the successor component for the duration of the current request.
     * The component should implement <code>RawDocumentsConsumer</code>
     * interface.
     *
     * @param next The successor component.
     */
    public void setNext(LocalComponent next) {
        super.setNext(next);

        if (next instanceof RawDocumentsConsumer) {
            this.rawDocumentConsumer = (RawDocumentsConsumer) next;
        } else {
            throw new IllegalArgumentException("Successor should implement: "
                        + RawDocumentsConsumer.class.getName());
        }
    }

   /**
    * Performs a cleanup before the object is reused.
    *
    * @see com.dawidweiss.carrot.core.local.LocalComponent.flushResources()
    */
   public void flushResources() {
       super.flushResources();
       this.rawDocumentConsumer = null;
   }
    
	/*
	 * @see com.dawidweiss.carrot.core.local.LocalComponent#getComponentCapabilities()
	 */
	public Set getComponentCapabilities() {
        return CAPABILITIES_COMPONENT;
	}
	
    /*
	 * @see com.dawidweiss.carrot.core.local.LocalComponent#getRequiredPredecessorCapabilities()
	 */
	public Set getRequiredPredecessorCapabilities() {
        return CAPABILITIES_PREDECESSOR;
	}
	
    /* 
	 * @see com.dawidweiss.carrot.core.local.LocalComponent#getRequiredSuccessorCapabilities()
	 */
	public Set getRequiredSuccessorCapabilities() {
        return CAPABILITIES_SUCCESSOR;
	}
}
