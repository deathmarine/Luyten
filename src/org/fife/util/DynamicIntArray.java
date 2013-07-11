/*
 * 03/26/2004
 *
 * DynamicIntArray.java - Similar to an ArrayList, but holds ints instead
 * of Objects.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.util;

import java.io.Serializable;
import java.util.Arrays;


/**
 * Similar to a <code>java.util.ArrayList</code>, but specifically for
 * <code>int</code>s.  This is basically an array of integers that resizes
 * itself (if necessary) when adding new elements.
 *
 * @author Robert Futrell
 * @version 0.8
 */
public class DynamicIntArray implements Serializable {

	/**
	 * The actual data.
	 */
	private int[] data;

	/**
	 * The number of values in the array.  Note that this is NOT the
	 * capacity of the array; rather, <code>size &lt;= capacity</code>.
	 */
	private int size;


	/**
	 * Constructs a new array object with an initial capacity of 10.
	 */
	public DynamicIntArray() {
		this(10);
	}


	/**
	 * Constructs a new array object with a given initial capacity.
	 *
	 * @param initialCapacity The initial capacity.
	 * @throws IllegalArgumentException If <code>initialCapacity</code> is
	 *         negative.
	 */
	public DynamicIntArray(int initialCapacity) {
		if (initialCapacity<0) {
			throw new IllegalArgumentException("Illegal initialCapacity: "
												+ initialCapacity);
		}
		data = new int[initialCapacity];
		size = 0;
	}


	/**
	 * Constructs a new array object from the given int array.  The resulting
	 * <code>DynamicIntArray</code> will have an initial capacity of 110%
	 * the size of the array.
	 *
	 * @param intArray Initial data for the array object.
	 * @throws NullPointerException If <code>intArray</code> is
	 *         <code>null</code>.
	 */
	public DynamicIntArray(int[] intArray) {
		size = intArray.length;
		int capacity = (int)Math.min(size*110L/100, Integer.MAX_VALUE);
		data = new int[capacity];
		System.arraycopy(intArray,0, data,0, size); // source, dest, length.
	}


	/**
	 * Appends the specified <code>int</code> to the end of this array.
	 *
	 * @param value The <code>int</code> to be appended to this array.
	 */
	public void add(int value) {
		ensureCapacity(size + 1);
		data[size++] = value;
	}


	/**
	 * Inserts all <code>int</code>s in the specified array into this array
	 * object at the specified location.  Shifts the <code>int</code>
	 * currently at that position (if any) and any subsequent
	 * <code>int</code>s to the right (adds one to their indices).
	 *
	 * @param index The index at which the specified integer is to be
	 *        inserted.
	 * @param intArray The array of <code>int</code>s to insert.
	 * @throws IndexOutOfBoundsException If <code>index</code> is less than
	 *         zero or greater than <code>getSize()</code>.
	 * @throws NullPointerException If <code>intArray<code> is
	 *         <code>null<code>.
	 */
	public void add(int index, int[] intArray) {
		if (index>size) {
			throwException2(index);
		}
		int addCount = intArray.length;
		ensureCapacity(size+addCount);
		int moveCount = size - index;
		if (moveCount>0)
			System.arraycopy(data,index, data,index+addCount, moveCount);
		System.arraycopy(data,index, intArray,0, moveCount);
		size += addCount;
	}


	/**
	 * Inserts the specified <code>int</code> at the specified position in
	 * this array. Shifts the <code>int</code> currently at that position (if
	 * any) and any subsequent <code>int</code>s to the right (adds one to
	 * their indices).
	 *
	 * @param index The index at which the specified integer is to be
	 *        inserted.
	 * @param value The <code>int</code> to be inserted.
	 * @throws IndexOutOfBoundsException If <code>index</code> is less than
	 *         zero or greater than <code>getSize()</code>.
	 */
	public void add(int index, int value) {
		if (index>size) {
			throwException2(index);
		}
		ensureCapacity(size+1);
		System.arraycopy(data,index, data,index+1, size-index);
		data[index] = value;
		size++;
	}


	/**
	 * Removes all values from this array object.  Capacity will remain the
	 * same.
	 */
	public void clear() {
		size = 0;
	}


	/**
	 * Returns whether this array contains a given integer.  This method
	 * performs a linear search, so it is not optimized for performance.
	 *
	 * @param integer The <code>int</code> for which to search.
	 * @return Whether the given integer is contained in this array.
	 */
	public boolean contains(int integer) {
		for (int i=0; i<size; i++) {
			if (data[i]==integer)
				return true;
		}
		return false;
	}


	/**
	 * Decrements all values in the array in the specified range.
	 *
	 * @param from The range start offset (inclusive).
	 * @param to The range end offset (exclusive).
	 * @see #increment(int, int)
	 */
	public void decrement(int from, int to) {
		for (int i=from; i<to; i++) {
			data[i]--;
		}
	}


	/**
	 * Makes sure that this <code>DynamicIntArray</code> instance can hold
	 * at least the number of elements specified.  If it can't, then the
	 * capacity is increased.
	 *
	 * @param minCapacity The desired minimum capacity.
	 */
	private final void ensureCapacity(int minCapacity) {
		int oldCapacity = data.length;
		if (minCapacity > oldCapacity) {
			int[] oldData = data;
			// Ensures we don't just keep increasing capacity by some small
			// number like 1...
			int newCapacity = (oldCapacity * 3)/2 + 1;
			if (newCapacity < minCapacity)
				newCapacity = minCapacity;
			data = new int[newCapacity];
			System.arraycopy(oldData,0, data,0, size);
		}
	}


	/**
	 * Sets the value of all entries in this array to the specified value.
	 *
	 * @param value The new value for all elements in the array.
	 */
	public void fill(int value) {
		Arrays.fill(data, value);
	}


	/**
	 * Returns the <code>int</code> at the specified position in this array
	 * object.
	 *
	 * @param index The index of the <code>int</code> to return.
	 * @return The <code>int</code> at the specified position in this array.
	 * @throws IndexOutOfBoundsException If <code>index</code> is less than
	 *         zero or greater than or equal to <code>getSize()</code>.
	 */
	public int get(int index) {
		// Small enough to be inlined, and throwException() is rarely called.
		if (index>=size) {
			throwException(index);
		}
		return data[index];
	}


	/**
	 * Returns the <code>int</code> at the specified position in this array
	 * object, without doing any bounds checking.  You really should use
	 * {@link #get(int)} instead of this method.
	 *
	 * @param index The index of the <code>int</code> to return.
	 * @return The <code>int</code> at the specified position in this array.
	 */
	public int getUnsafe(int index) {
		// Small enough to be inlined.
		return data[index];
	}


	/**
	 * Returns the number of <code>int</code>s in this array object.
	 *
	 * @return The number of <code>int</code>s in this array object.
	 */
	public int getSize() {
		return size;
	}


	/**
	 * Increments all values in the array in the specified range.
	 *
	 * @param from The range start offset (inclusive).
	 * @param to The range end offset (exclusive).
	 * @see #decrement(int, int)
	 */
	public void increment(int from, int to) {
		for (int i=from; i<to; i++) {
			data[i]++;
		}
	}


	public void insertRange(int offs, int count, int value) {
		if (offs>size) {
			throwException2(offs);
		}
		ensureCapacity(size+count);
		System.arraycopy(data,offs, data,offs+count, size-offs);
		if (value!=0) {
			Arrays.fill(data, offs, offs+count, value);
		}
		size += count;
	}


	/**
	 * Returns whether or not this array object is empty.
	 *
	 * @return Whether or not this array object contains no elements.
	 */
	public boolean isEmpty() {
		return size==0;
	}


	/**
	 * Removes the <code>int</code> at the specified location from this array
	 * object.
	 *
	 * @param index The index of the <code>int</code> to remove.
	 * @throws IndexOutOfBoundsException If <code>index</code> is less than
	 *         zero or greater than or equal to <code>getSize()</code>.
	 */
	public void remove(int index) {
		if (index>=size) {
			throwException(index);
		}
		int toMove = size - index - 1;
		if (toMove>0) {
			System.arraycopy(data,index+1, data,index, toMove);
		}
		--size;
	}


	/**
	 * Removes the <code>int</code>s in the specified range from this array
	 * object.
	 *
	 * @param fromIndex The index of the first <code>int</code> to remove.
	 * @param toIndex The index AFTER the last <code>int</code> to remove.
	 * @throws IndexOutOfBoundsException If either of <code>fromIndex</code>
	 *         or <code>toIndex</code> is less than zero or greater than or
	 *         equal to <code>getSize()</code>.
	 */
	public void removeRange(int fromIndex, int toIndex) {
		if (fromIndex>=size || toIndex>size) {
			throwException3(fromIndex, toIndex);
		}
		int moveCount = size - toIndex;
		System.arraycopy(data,toIndex, data,fromIndex, moveCount);
		size -= (toIndex - fromIndex);
	}

		
	/**
	 * Sets the <code>int</code> value at the specified position in this
	 * array object.
	 *
	 * @param index The index of the <code>int</code> to set
	 * @param value The value to set it to.
	 * @throws IndexOutOfBoundsException If <code>index</code> is less than
	 *         zero or greater than or equal to <code>getSize()</code>.
	 */
	public void set(int index, int value) {
		// Small enough to be inlined, and throwException() is rarely called.
		if (index>=size) {
			throwException(index);
		}
		data[index] = value;
	}


	/**
	 * Sets the <code>int</code> value at the specified position in this
	 * array object, without doing any bounds checking.  You should use
	 * {@link #set(int, int)} instead of this method.
	 *
	 * @param index The index of the <code>int</code> to set
	 * @param value The value to set it to.
	 */
	public void setUnsafe(int index, int value) {
		// Small enough to be inlined.
		data[index] = value;
	}


	/**
	 * Throws an exception.  This method isolates error-handling code from
	 * the error-checking code, so that callers (e.g. {@link #get} and
	 * {@link #set}) can be both small enough to be inlined, as well as
	 * not usually make any expensive method calls (since their callers will
	 * usually not pass illegal arguments to them).
	 *
	 * See <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5103956">
	 * this Sun bug report</a> for more information.
	 *
	 * @param index The invalid index.
	 * @throws IndexOutOfBoundsException Always.
	 */
	private final void throwException(int index)
								throws IndexOutOfBoundsException {
		throw new IndexOutOfBoundsException("Index " + index +
						" not in valid range [0-" + (size-1) + "]");
	}


	/**
	 * Throws an exception.  This method isolates error-handling code from
	 * the error-checking code, so that callers can be both small enough to be
	 * inlined, as well as not usually make any expensive method calls (since
	 * their callers will usually not pass illegal arguments to them).
	 *
	 * See <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5103956">
	 * this Sun bug report</a> for more information.
	 *
	 * @param index The invalid index.
	 * @throws IndexOutOfBoundsException Always.
	 */
	private final void throwException2(int index)
								throws IndexOutOfBoundsException {
		throw new IndexOutOfBoundsException("Index " + index +
								", not in range [0-" + size + "]");
	}


	/**
	 * Throws an exception.  This method isolates error-handling code from
	 * the error-checking code, so that callers can be both small enough to be
	 * inlined, as well as not usually make any expensive method calls (since
	 * their callers will usually not pass illegal arguments to them).
	 *
	 * See <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5103956">
	 * this Sun bug report</a> for more information.
	 *
	 * @param index The invalid index.
	 * @throws IndexOutOfBoundsException Always.
	 */
	private final void throwException3(int fromIndex, int toIndex)
								throws IndexOutOfBoundsException {
		throw new IndexOutOfBoundsException("Index range [" +
						fromIndex + ", " + toIndex +
						"] not in valid range [0-" + (size-1) + "]");
	}


}