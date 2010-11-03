package bd.amazed.pdfscissors.view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.Vector;

import bd.amazed.pdfscissors.model.RectChangeListener;

public class Rect {
	protected Rectangle bounds;
	protected boolean isSelected;

	protected static final int CORNERBOX_SIZE = 6;
	protected static final int CORNER_NONE = -1;
	protected static final int CORNER_TOP_LEFT = 0;
	protected static final int CORNER_BOTTOM_LEFT = 1;
	protected static final int CORNER_BUTTOM_RIGHT = 2;
	protected static final int CORNER_TOP_RIGHT = 3;
	

	protected static final Color COLOR_SELECTED_RECT = new Color(0x55000077, true);
	protected static final Color COLOR_RECT = new Color(0x55555555, true);
	private static Stroke dashStroke = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0);
	private static Font font;
	
	protected Vector<RectChangeListener> listeners = new Vector<RectChangeListener>() ;
	private UIHandler uiHandler;
	
	/**
	 * Initial width, height is zero.
	 * @param start starting point
	 */
	public Rect(Point start, UIHandler uiHandler) {
		bounds = new Rectangle(start);
		this.uiHandler = uiHandler;
	}

	public void addListener(RectChangeListener listener) {
		if (!listeners.contains(listener)) {
			listeners.add(listener);
		}
	}

	public boolean removeListener(RectChangeListener listener) {
		return listeners.remove(listener);
	}

	public void removeAllListeners() {
		listeners.removeAllElements();
	}

	protected void setBounds(Rectangle newBounds) {
		Rectangle oldBounds = bounds;
		bounds = newBounds;
		updateCanvas(oldBounds.union(bounds));
	}

	public void resize(Point anchor, Point end, int containerWidth,
			int containerHeight) {
		Rectangle newRect = new Rectangle(anchor);
		newRect.add(end); // creates smallest rectange which includes both anchor & end
		if (isRectInsideContainer(newRect, containerWidth, containerHeight)) {
			setBounds(newRect);
		}
	}

	public void translate(int dx, int dy, int containerWidth, int containerHeight) {
		Rectangle newRect = new Rectangle(bounds);
		newRect.translate(dx, dy);
		if (isRectInsideContainer(newRect, containerWidth, containerHeight)) {
			setBounds(newRect);
		}
	}

	private boolean isRectInsideContainer(Rectangle newRect, int containerWidth, int containerHeight) {
		return newRect.x >= 0 && newRect.x + newRect.width <= containerWidth
				&& newRect.y >= 0 && newRect.y + newRect.height <= containerHeight;

	}

	public void setSelected(boolean newState) {
		isSelected = newState;
		updateCanvas(bounds, true); // need to erase/add cornerboxs including extent of extended bounds
	}

	protected void updateCanvas(Rectangle areaOfChange, boolean enlargeForCornerboxes) {
		Rectangle toRedraw = new Rectangle(areaOfChange);
		if (enlargeForCornerboxes)
			toRedraw.grow(CORNERBOX_SIZE / 2, CORNERBOX_SIZE / 2);
		fireEvent(toRedraw);
	}

	/**
	 * 
	 * @param repaintArea can be null to indicate repaint whole area
	 */
	void fireEvent(Rectangle repaintArea) {
		for (RectChangeListener listener : listeners) {
			listener.rectUpdated(this, repaintArea);
		}
	}

	protected void updateCanvas(Rectangle areaOfChange) {
		updateCanvas(areaOfChange, isSelected);
	}

	
	public void draw(Graphics g, Rectangle clipRect) {
		if (!bounds.intersects(clipRect))
			return;
		if (isSelected) {
			g.setColor(COLOR_SELECTED_RECT);
		} else {
			g.setColor(COLOR_RECT);
		}
		g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
		
		//draw order number
		if (isSelected) {
			g.setColor(Color.BLUE);
		} else {
			g.setColor(Color.GRAY);
		}
		if (font == null) {
			font = new Font(g.getFont().getFamily(), g.getFont().getStyle(), g.getFont().getSize() * 2);
		}
		g.setFont(font);
		g.drawString(String.valueOf(uiHandler.getIndexOf(this) + 1), bounds.x + 5, bounds.y + g.getFont().getSize() + 2);
		
		//draw dashed border
		g.setColor(Color.BLACK);
		Graphics2D g2d = (Graphics2D) g;
		Stroke oldStroke = g2d.getStroke();
		g2d.setStroke(dashStroke);
		g.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
		g2d.setStroke(oldStroke);
		if (isSelected) {
			Rectangle[] cornerboxs = getCornerboxRects();
			for (int i = 0; i < cornerboxs.length; i++)
				g.fillRect(cornerboxs[i].x, cornerboxs[i].y, cornerboxs[i].width, cornerboxs[i].height);
		}
	}

	
	public boolean inside(Point pt) {
		return bounds.contains(pt);
	}
	
	protected Rectangle[] getCornerboxRects() {
		Rectangle[] cornerboxs = new Rectangle[4];
		cornerboxs[CORNER_TOP_LEFT] = new Rectangle(bounds.x - CORNERBOX_SIZE / 2, bounds.y
				- CORNERBOX_SIZE / 2, CORNERBOX_SIZE, CORNERBOX_SIZE);
		cornerboxs[CORNER_BOTTOM_LEFT] = new Rectangle(bounds.x - CORNERBOX_SIZE / 2, bounds.y
				+ bounds.height - CORNERBOX_SIZE / 2, CORNERBOX_SIZE, CORNERBOX_SIZE);
		cornerboxs[CORNER_BUTTOM_RIGHT] = new Rectangle(bounds.x + bounds.width - CORNERBOX_SIZE / 2,
				bounds.y + bounds.height - CORNERBOX_SIZE / 2, CORNERBOX_SIZE, CORNERBOX_SIZE);
		cornerboxs[CORNER_TOP_RIGHT] = new Rectangle(bounds.x + bounds.width - CORNERBOX_SIZE / 2,
				bounds.y - CORNERBOX_SIZE / 2, CORNERBOX_SIZE, CORNERBOX_SIZE);
		return cornerboxs;
	}

	/**
	 * Helper method to determine if a point is within one of the resize corner
	 * cornerboxs. If not selected, we have no resize cornerboxs, so it can't have been a
	 * click on one. Otherwise, we calculate the cornerbox rects and then check
	 * whether the point falls in one of them. The return value is one of NW,
	 * NE, SW, SE constants depending on which cornerbox is found, or NONE if the
	 * click doesn't fall within any cornerbox.
	 */
	protected int getCornerboxContainingPoint(Point pt) {
		if (!isSelected) // if we aren't selected, the cornerboxs aren't showing and
			// thus there are no cornerboxs to check
			return CORNER_NONE;

		Rectangle[] cornerboxs = getCornerboxRects();
		for (int i = 0; i < cornerboxs.length; i++)
			if (cornerboxs[i].contains(pt))
				return i;
		return CORNER_NONE;
	}

	/**
	 * Method used by PdfPanel to determine if a mouse click is starting a
	 * resize event. In order for it to be a resize, the click must have been
	 * within one of the cornerbox rects (checked by the helper method
	 * getCornerboxContainingPoint) and if so, we return the "anchor" ie the cornerbox
	 * opposite this corner that will remain fixed as the user drags the
	 * resizing cornerbox of the other corner around. During the drag actions of a
	 * resize, that fixed anchor point and the current mouse point will be
	 * passed to the resize method, which will reset the bounds in response to
	 * the movement. If the mouseLocation wasn't a click in a cornerbox and thus not
	 * the beginning of a resize event, null is returned.
	 */
	public Point getAnchorForResize(Point mouseLocation) {
		int whichCornerbox = getCornerboxContainingPoint(mouseLocation);

		if (whichCornerbox == CORNER_NONE) // no resize cornerbox is at this location
			return null;
		switch (whichCornerbox) {
		case CORNER_TOP_LEFT:
			return new Point(bounds.x + bounds.width, bounds.y + bounds.height);
		case CORNER_TOP_RIGHT:
			return new Point(bounds.x, bounds.y + bounds.height);
		case CORNER_BOTTOM_LEFT:
			return new Point(bounds.x + bounds.width, bounds.y);
		case CORNER_BUTTOM_RIGHT:
			return new Point(bounds.x, bounds.y);
		}
		return null;
	}
}