

/*
 * Carrot2 Project
 * Copyright (C) 2002-2004, Dawid Weiss
 * Portions (C) Contributors listen in carrot2.CONTRIBUTORS file.
 * All rights reserved.
 * 
 * Refer to full text of the license "carrot2.LICENSE" in the root folder
 * of CVS checkout or at: 
 * http://www.cs.put.poznan.pl/dweiss/carrot2.LICENSE
 */


package com.dawidweiss.carrot.util.tokenizer.parser;


import java.io.Reader;

import com.dawidweiss.carrot.core.local.linguistic.LanguageTokenizer;
import com.dawidweiss.carrot.core.local.linguistic.tokens.Token;
import com.dawidweiss.carrot.core.local.linguistic.tokens.TypedToken;
import com.dawidweiss.carrot.util.common.pools.*;


/**
 * Tokenizer class splits a string into tokens like word, e-mail address, web page address and
 * such. Instances may be quite large (memory consumption-wise) so they should be reused rather
 * than recreated.
 * 
 * @author Dawid Weiss
 * @version $Revision$
 */
public final class WordBasedParser
    implements LanguageTokenizer
{
    /**
     * An instance of JavaCC-generated parser that is used
     * for input stream tokenization.
     */
    private WordBasedParserImpl tokenizer;

    /** 
     * Temporary variable for holding token type passed
     * from the {@link WordBasedParserImpl}. 
     */
    private final short [] tokenTypeHolder = new short[1];

    
    /**
     * A reusable, unbounded pool of token objects.
     */
    private final ReusableObjectsPool pool;

    /**
     * Minimal size for the 'hard' part of the reusable
     * pool, if empty constructor is used.
     */
    private final static int DEFAULT_POOL_MIN_HARD_SIZE = 500;

    /**
     * Pool block increment size, if empty constructor
     * is used.
     */
    private final static int DEFAULT_POOL_SOFT_INCREMENT = 1000;
    
    /**
     * Public constructor creates a new instance
     * of the parser. <b>Reuse tokenizer objects</b> instead
     * of recreating them.
     * 
     * <p>This constructor creates a default
     * {@link SoftReusableObjectsPool} 
     * that 
     * produces and pools objects of type {@link TypedToken}. If a more
     * specific token type is needed, pass an instance
     * of your own token factory object.
     */
    public WordBasedParser()
    {
        this( new SoftReusableObjectsPool(new ReusableObjectsFactory() {
            public void createNewObjects( Object [] objects ) {
                final int max = objects.length;
                for (int i=0;i<max;i++) {
                    objects[i] = new StringTypedToken();
                }
            }
        }, DEFAULT_POOL_MIN_HARD_SIZE, DEFAULT_POOL_SOFT_INCREMENT));
    }
    
    /**
     * Creates an instance of the parser that uses a custom
     * pool of token objects. The pool <b>must</b> return
     * objects subclassing {@link StringTypedToken} class.  
     * 
     * @param pool An unbounded pool of objects implementing at least
     * {@link StringTypedToken} interface.
     */
    public WordBasedParser(ReusableObjectsPool pool) {
        this.pool = pool;
    }

    /** 
     * Reuses the tokens pool assigned to this parser. All
     * tokens returned from this parser become invalid and should
     * be no longer referenced after this call.
     */
    public void reuse() {
        pool.reuse();
    }

    /**
     * Returns the next token from the parsing data. The token value is returned from the method,
     * while the type of the token, as defined in
     * {@link com.dawidweiss.carrot.core.local.linguistic.tokens.TypedToken}, 
     *  is stored in the zero index of the input parameter
     * <code>tokenTypeHolder</code>. If tokenTypeHolder is <code>null</code>, token type is not
     * saved anywhere. <code>null</code> is returned when end of the input data has been reached.
     * This method is <em>not</em> synchronized.
     * 
     * @param tokenTypeHolder A holder where the next token's type is saved (at index 0).
     * @return Returns the next token's value as a String object, or <code>null</code>
     * if end of the input data has been reached.
     * 
     * @see com.dawidweiss.carrot.core.local.linguistic.tokens.TypedToken
     */     
    private final String getNextToken(short [] tokenTypeHolder)
    {
        try
        {
            com.dawidweiss.carrot.util.tokenizer.parser.Token t 
                = tokenizer.getNextToken();

            if (t.kind == WordBasedParserImplConstants.EOF)
            {
                return null;
            }

            if (tokenTypeHolder != null)
            {
                switch (t.kind)
                {
                    case WordBasedParserImplConstants.URL:
                    case WordBasedParserImplConstants.EMAIL:
                        tokenTypeHolder[0] = TypedToken.TOKEN_TYPE_SYMBOL;
                        break;

                    case WordBasedParserImplConstants.TERM:
                    case WordBasedParserImplConstants.HYPHTERM:
                    case WordBasedParserImplConstants.ACRONYM:
                        tokenTypeHolder[0] = TypedToken.TOKEN_TYPE_TERM;
                        break;

                    case WordBasedParserImplConstants.SENTENCEMARKER:
                        tokenTypeHolder[0] = TypedToken.TOKEN_TYPE_PUNCTUATION | TypedToken.TOKEN_FLAG_SENTENCE_DELIM;
                        break;

                    case WordBasedParserImplConstants.PUNCTUATION:
                        tokenTypeHolder[0] = TypedToken.TOKEN_TYPE_PUNCTUATION;
                        break;
                        
                    case WordBasedParserImplConstants.NUMERIC:
                        tokenTypeHolder[0] = TypedToken.TOKEN_TYPE_NUMERIC;
                        break;

                    default:
                        throw new RuntimeException(
                            "Unexpected token type: "
                        	+ WordBasedParserImplConstants.tokenImage[t.kind] + " (" + t.image + ")"
                        );
                }
            }

            return t.image;
        }
        catch (NullPointerException e)
        {
            // catching exception costs nothing
            if (tokenizer == null)
            {
                throw new RuntimeException("Initialize tokenizer first.");
            }

            throw e;
        }
    }


	/**
     * Restarts tokenization on another stream of characters.
     * The tokens pool is <b>not</b> reused at this point;
     * an explicit call to {@link #reuse()} is needed to achieve
     * this.
     * 
     * @param stream A character stream to restart tokenization on.
	 */
	public void restartTokenizationOn(Reader stream) {
        if (tokenizer != null)
        {
            tokenizer.ReInit(stream);
        }
        else
        {
            tokenizer = new WordBasedParserImpl(stream);
        }
	}


	/**
     * Parses the input and returns a new chunk of tokens.
     * 
     * @param array An array where new tokens will be stored.
     * @param startAt The first index in <code>array</code> to use.
     * @return the number of tokens placed in the array, or 0 if
     * no more tokens are available. 
	 */
	public int getNextTokens(Token[] array, int startAt) {

        try {
            int count = 0;
            short tokenType;
            String image;
            StringTypedToken token; 
            while (startAt < array.length) {
                image = getNextToken(tokenTypeHolder);
                if (image == null) {
                    break;
                }

                token = (StringTypedToken) pool.acquireObject();
                token.assign(image, tokenTypeHolder[0]);
                array[startAt] = token;
                count++;
                startAt++;
            }
            
            return count;
            
        } catch (ClassCastException e) {
            throw new RuntimeException("Class cast exception: invalid object type returned from the pool?",e);
        } catch (NullPointerException e) {
            if (array == null) {
                throw new IllegalArgumentException("Array must not be null.");
            } else
                throw e;
        }
	}
}
