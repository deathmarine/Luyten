/*
 * 12/01/2008
 *
 * AbstractCodeTemplate.java - Base class for code templates.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea.templates;


/**
 * A base class to build code templates on top of.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public abstract class AbstractCodeTemplate implements CodeTemplate {

	/**
	 * The ID of this template.
	 */
	private String id;


	/**
	 * This no-arg constructor is required for serialization purposes.
	 */
	public AbstractCodeTemplate() {
	}


	/**
	 * Creates a new template.
	 *
	 * @param id The ID for this template.
	 * @throws IllegalArgumentException If <code>id</code> is <code>null</code>.
	 */
	public AbstractCodeTemplate(String id) {
		setID(id);
	}


	/**
	 * Creates a deep copy of this template.
	 *
	 * @return A deep copy of this template.
	 */
	public Object clone() {
		// This method can't be abstract as compilers don't like concrete
		// subclassses calling super.clone() on  an abstract super.
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			throw new InternalError(
				"CodeTemplate implementation not Cloneable: " +
							getClass().getName());
		}
	}



	/**
	 * Compares the <code>StaticCodeTemplate</code> to another.
	 *
	 * @param o Another <code>StaticCodeTemplate</code> object.
	 * @return A negative integer, zero, or a positive integer as this
	 *         object is less than, equal-to, or greater than the passed-in
	 *         object.
	 * @throws ClassCastException If <code>o</code> is
	 *         not an instance of <code>CodeTemplate</code>.
	 */
	public int compareTo(Object o) {
		if (!(o instanceof CodeTemplate)) {
			return -1;
		}
		CodeTemplate t2 = (CodeTemplate)o;
		return getID().compareTo(t2.getID());
	}


	/**
	 * Overridden to return "<code>true</code>" iff {@link #compareTo(Object)}
	 * returns <code>0</code>.
	 *
	 * @return Whether this code template is equal to another.
	 */
	public boolean equals(Object obj) {
		if (obj instanceof CodeTemplate) {
			return compareTo(obj)==0;
		}
		return false;
	}


	/**
	 * Returns the ID of this code template.
	 *
	 * @return The template's ID.
	 * @see #setID(String)
	 */
	public String getID() {
		return id;
	}


	/**
	 * Returns the hash code for this template.
	 *
	 * @return The hash code for this template.
	 */
	public int hashCode() {
		return id.hashCode();
	}


	/**
	 * Sets the ID for this template.
	 *
	 * @param id The ID for this template.
	 * @throws IllegalArgumentException If <code>id</code> is <code>null</code>.
	 * @see #getID()
	 */
	public void setID(String id) {
		if (id==null) {
			throw new IllegalArgumentException("id cannot be null");
		}
		this.id = id;
	}


}