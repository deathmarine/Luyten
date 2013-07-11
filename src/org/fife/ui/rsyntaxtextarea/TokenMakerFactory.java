/*
 * 12/12/2008
 *
 * TokenMakerFactory.java - A factory for TokenMakers.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea;

import java.util.Set;

import org.fife.ui.rsyntaxtextarea.modes.PlainTextTokenMaker;


/**
 * A factory that maps syntax styles to {@link TokenMaker}s capable of splitting
 * text into tokens for those syntax styles.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public abstract class TokenMakerFactory {

	/**
	 * If this system property is set, a custom <code>TokenMakerFactory</code>
	 * of the specified class will be used as the default token maker factory.
	 */
	public static final String PROPERTY_DEFAULT_TOKEN_MAKER_FACTORY	=
														"TokenMakerFactory";

	/**
	 * The singleton default <code>TokenMakerFactory</code> instance.
	 */
	private static TokenMakerFactory DEFAULT_INSTANCE;


	/**
	 * Returns the default <code>TokenMakerFactory</code> instance.  This is
	 * the factory used by all {@link RSyntaxDocument}s by default.
	 *
	 * @return The factory.
	 * @see #setDefaultInstance(TokenMakerFactory)
	 */
	public static synchronized TokenMakerFactory getDefaultInstance() {
		if (DEFAULT_INSTANCE==null) {
			String clazz = null;
			try {
				clazz= System.getProperty(PROPERTY_DEFAULT_TOKEN_MAKER_FACTORY);
			} catch (java.security.AccessControlException ace) {
				clazz = null; // We're in an applet; take default.
			}
			if (clazz==null) {
				clazz = "org.fife.ui.rsyntaxtextarea.DefaultTokenMakerFactory";
			}
			try {
				DEFAULT_INSTANCE = (TokenMakerFactory)Class.forName(clazz).
													newInstance();
			} catch (RuntimeException re) { // FindBugs
				throw re;
			} catch (Exception e) {
				e.printStackTrace();
				throw new InternalError("Cannot find TokenMakerFactory: " +
											clazz);
			}
		}
		return DEFAULT_INSTANCE;
	}


	/**
	 * Returns a {@link TokenMaker} for the specified key.
	 *
	 * @param key The key.
	 * @return The corresponding <code>TokenMaker</code>, or
	 *         {@link PlainTextTokenMaker} if none matches the specified key.
	 */
	public final TokenMaker getTokenMaker(String key) {
		TokenMaker tm = getTokenMakerImpl(key);
		if (tm==null) {
			tm = new PlainTextTokenMaker();
		}
		return tm;
	}


	/**
	 * Returns a {@link TokenMaker} for the specified key.
	 *
	 * @param key The key.
	 * @return The corresponding <code>TokenMaker</code>, or <code>null</code>
	 *         if none matches the specified key.
	 */
	protected abstract TokenMaker getTokenMakerImpl(String key);


	/**
	 * Returns the set of keys that this factory maps to token makers.
	 *
	 * @return The set of keys.
	 */
	public abstract Set keySet();


	/**
	 * Sets the default <code>TokenMakerFactory</code> instance.  This is
	 * the factory used by all future {@link RSyntaxDocument}s by default.
	 * <code>RSyntaxDocument</code>s that have already been created are not
	 * affected.
	 *
	 * @param tmf The factory.
	 * @throws IllegalArgumentException If <code>tmf</code> is
	 *         <code>null</code>.
	 * @see #getDefaultInstance()
	 */
	public static synchronized void setDefaultInstance(TokenMakerFactory tmf) {
		if (tmf==null) {
			throw new IllegalArgumentException("tmf cannot be null");
		}
		DEFAULT_INSTANCE = tmf;
	}


}