package org.fife.ui.rsyntaxtextarea;

//import java.awt.Graphics;
//import java.awt.Rectangle;
//import java.awt.Shape;

//import javax.swing.text.CompositeView;
import javax.swing.text.Element;
//import javax.swing.text.View;


/**
 * Replacement for the old <code>WrappedSyntaxView</code> class, designed to
 * be faster with large wrapped documents.  Heavily based off of
 * <code>BoxView</code>, but streamlined to only care about the y-axis, and
 * takes code folding into account.<p>
 *
 * This class is not currently used.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class WrappedSyntaxView2 {//extends CompositeView {

//	private Rectangle tempRect;
//	private int[] cachedOffsets;
//	private int[] cachedSpans;
//	private boolean sizeRequirementsValid;


	public WrappedSyntaxView2(Element root) {
		//super(root);
//		tempRect = new Rectangle();
//		cachedOffsets = new int[0];
//		cachedSpans = new int[0];
//		sizeRequirementsValid = false;
	}


//	protected void childAllocation(int index, Rectangle alloc) {
//		alloc.y += getOffset(index);
//		alloc.height = getHeight(index);
//	}
//
//
//	private int getHeight(int childIndex) {
//		return cachedSpans[childIndex];
//	}
//
//
//	private int getOffset(int childIndex) {
//		return cachedOffsets[childIndex]; 
//	}
//
//
//	protected View getViewAtPoint(int x, int y, Rectangle alloc) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//
//	/**
//	 * @param alloc The allocated region; this is the area inside of the insets
//	 * @return Whether the point lies after the region.
//	 */
//	protected boolean isAfter(int x, int y, Rectangle alloc) {
//		return y > (alloc.y + alloc.height);
//	}
//
//
//	/**
//	 * @param alloc The allocated region; this is the area inside of the insets
//	 * @return Whether the point lies before the region.
//	 */
//	protected boolean isBefore(int x, int y, Rectangle alloc) {
//		return y < alloc.y;
//	}
//
//
//	public float getPreferredSpan(int axis) {
//		if (axis==X_AXIS) {
//			return preferredWidth + getLeftInset() + getRightInset();
//		}
//		else {
//			return preferredHeight + getTopInset() + getBottomInset();
//		}
//	}
//
//
//	public void paint(Graphics g, Shape allocation) {
//
//		Rectangle alloc = (allocation instanceof Rectangle) ?
//				(Rectangle)allocation : allocation.getBounds();
//		int n = getViewCount();
//
//		int x = alloc.x + getLeftInset();
//		int y = alloc.y + getTopInset();
//		Rectangle clip = g.getClipBounds();
//		int preferredWidth = (int)getPreferredSpan(X_AXIS);
//
//		for (int i = 0; i < n; i++) {
//			tempRect.x = x;
//			tempRect.y = y + getOffset(i);
//			tempRect.width = preferredWidth;
//			tempRect.height = getHeight(i);
//			if (tempRect.intersects(clip)) {
//				paintChild(g, tempRect, i);
//			}
//		}
//
//	}
//
//
//	/**
//	 * Called when a child view's preferred span changes.  This invalidates
//	 * our layout cache and calls the super implementation.
//	 */
//	public void preferenceChanged(View child, boolean widthPreferenceChanged,
//								boolean heightPreferenceChanged) {
//
//		if (heightPreferenceChanged) {
//			sizeRequirementsValid = false;
////			majorAllocValid = false;
//		}
////		if (width) {
////			minorReqValid = false;
////			minorAllocValid = false;
////		}
//
//		super.preferenceChanged(child, widthPreferenceChanged, heightPreferenceChanged);
//
//	}
//
//	public void replace(int index, int length, View[] elems) {
//
//		super.replace(index, length, elems);
//
//		// Invalidate cache
//		int insertCount = elems==null ? 0 : elems.length;
//		cachedOffsets = updateLayoutArray(cachedOffsets, index, insertCount);
//		majorReqValid = false;
//		majorAllocValid = false;
//
//	}
//
//
//	private int[] updateLayoutArray(int[] oldArray, int offset, int nInserted) {
//		int n = getViewCount(); // Called after super.replace() so this is accurate
//		int[] newArray = new int[n];
//		System.arraycopy(oldArray, 0, newArray, 0, offset);
//		System.arraycopy(oldArray, offset, 
//				newArray, offset + nInserted, n - nInserted - offset);
//		return newArray;
//	}
//

}