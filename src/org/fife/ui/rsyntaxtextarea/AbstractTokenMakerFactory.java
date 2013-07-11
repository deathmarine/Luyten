/*
 * 12/14/08
 *
 * AbstractTokenMakerFactory.java - Base class for TokenMaker implementations.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
 * Base class for {@link TokenMakerFactory} implementations.  A mapping from
 * language keys to the names of {@link TokenMaker} classes is stored.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public abstract class AbstractTokenMakerFactory extends TokenMakerFactory {

	/**
	 * A mapping from keys to the names of {@link TokenMaker} implementation
	 * class names.  When {@link #getTokenMaker(String)} is called with a key
	 * defined in this map, a <code>TokenMaker</code> of the corresponding type
	 * is returned.
	 */
	private Map tokenMakerMap;


	/**
	 * Constructor.
	 */
	protected AbstractTokenMakerFactory() {
		tokenMakerMap = new HashMap();
		initTokenMakerMap();
	}


	/**
	 * Returns a {@link TokenMaker} for the specified key.
	 *
	 * @param key The key.
	 * @return The corresponding <code>TokenMaker</code>, or <code>null</code>
	 *         if none matches the specified key.
	 */
	protected TokenMaker getTokenMakerImpl(String key) {
		TokenMakerCreator tmc = (TokenMakerCreator)tokenMakerMap.get(key);
		if (tmc!=null) {
			try {
				return tmc.create();
			} catch (RuntimeException re) { // FindBugs
				throw re;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}


	/**
	 * Populates the mapping from keys to instances of
	 * <code>TokenMakerCreator</code>s.  Subclasses should override this method
	 * and call one of the <code>putMapping</code> overloads to register
	 * {@link TokenMaker}s for syntax constants.
	 *
	 * @see #putMapping(String, String)
	 * @see #putMapping(String, String, ClassLoader)
	 */
	protected abstract void initTokenMakerMap();


	/**
	 * {@inheritDoc}
	 */
	public Set keySet() {
		return tokenMakerMap.keySet();
	}


	/**
	 * Adds a mapping from a key to a <code>TokenMaker</code> implementation
	 * class name.
	 *
	 * @param key The key.
	 * @param className The <code>TokenMaker</code> class name.
	 * @see #putMapping(String, String, ClassLoader)
	 */
	public void putMapping(String key, String className) {
		putMapping(key, className, null);
	}


	/**
	 * Adds a mapping from a key to a <code>TokenMaker</code> implementation
	 * class name.
	 *
	 * @param key The key.
	 * @param className The <code>TokenMaker</code> class name.
	 * @param cl The class loader to use when loading the class.
	 * @see #putMapping(String, String)
	 */
	public void putMapping(String key, String className, ClassLoader cl) {
		tokenMakerMap.put(key, new TokenMakerCreator(className, cl));
	}


	/**
	 * Wrapper that handles the creation of TokenMaker instances.
	 */
	private static class TokenMakerCreator {

		private String className;
		private ClassLoader cl;

		public TokenMakerCreator(String className, ClassLoader cl) {
			this.className = className;
			this.cl = cl!=null ? cl : getClass().getClassLoader();
		}

		public TokenMaker create() throws Exception {
			return (TokenMaker)Class.forName(className, true, cl).newInstance();
		}

	}


}