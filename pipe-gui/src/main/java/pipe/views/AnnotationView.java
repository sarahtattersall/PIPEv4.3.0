/*
 * Created on 04-Mar-2004
 * Author is Michael Camacho
 */
package pipe.views;

import pipe.constants.GUIConstants;
import pipe.controllers.PetriNetController;
import uk.ac.imperial.pipe.models.petrinet.Annotation;
import uk.ac.imperial.pipe.models.petrinet.AnnotationImpl;

import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;


public final class AnnotationView extends Note {

    private static final long serialVersionUID = 1L;

    public static final int NO_DRAG_POINTS = 8;

    private final List<ResizePoint> dragPoints = new ArrayList<>(NO_DRAG_POINTS);

    private boolean fillNote = true;

    private AffineTransform prova = new AffineTransform();

    public AnnotationView(Annotation annotation, PetriNetController controller, Container parent, MouseInputAdapter handler) {
        super(annotation, controller, parent);
        addChangeListener(annotation);
        setDragPoints();
        setMouseHandler(handler);
        updateBounds();
    }

    private void setMouseHandler(MouseInputAdapter handler) {
        addMouseListener(handler);
        addMouseMotionListener(handler);
        noteText.addMouseListener(handler);
        noteText.addMouseMotionListener(handler);
    }

    @Override
    public void updateBounds() {
        super.updateBounds();
        // TOP-LEFT
        dragPoints.get(0).setLocation(noteRect.getMinX(), noteRect.getMinY());
        // TOP-MIDDLE
        dragPoints.get(1).setLocation(noteRect.getCenterX(), noteRect.getMinY());
        // TOP-RIGHT
        dragPoints.get(2).setLocation(noteRect.getMaxX(), noteRect.getMinY());
        // MIDDLE-RIGHT
        dragPoints.get(3).setLocation(noteRect.getMaxX(), noteRect.getCenterY());
        // BOTTOM-RIGHT
        dragPoints.get(4).setLocation(noteRect.getMaxX(), noteRect.getMaxY());
        // BOTTOM-MIDDLE
        dragPoints.get(5).setLocation(noteRect.getCenterX(), noteRect.getMaxY());
        // BOTTOM-LEFT
        dragPoints.get(6).setLocation(noteRect.getMinX(), noteRect.getMaxY());
        // MIDDLE-LEFT
        dragPoints.get(7).setLocation(noteRect.getMinX(), noteRect.getCenterY());
    }



    /**
     * @param x
     * @param y
     * @return true if (x, y) intersect annotation location
     */
    @Override
    public boolean contains(int x, int y) {
        boolean pointContains = false;

        for (ResizePoint dragPoint : dragPoints) {
            pointContains |= dragPoint.contains(x - dragPoint.getX(), y - dragPoint.getY());
        }

        return super.contains(x, y) || pointContains;
    }

    /**
     * Listens for changes to the annotation model
     *
     * @param annotation model to register changes to
     */
    private void addChangeListener(Annotation annotation) {
        annotation.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                String name = propertyChangeEvent.getPropertyName();
                if (name.equals(AnnotationImpl.TEXT_CHANGE_MESSAGE)) {
                    String text = (String) propertyChangeEvent.getNewValue();
                    setText(text);
                } else if (name.equals(Annotation.X_CHANGE_MESSAGE) || name.equals(Annotation.Y_CHANGE_MESSAGE)) {
                    updateBounds();
                }
            }
        });
    }

    /**
     * Creates drag points for all the corners and half way along
     * each edge
     */
    private void setDragPoints() {
        dragPoints.add(new ResizePoint(ResizePoint.TOP | ResizePoint.LEFT));
        dragPoints.add(new ResizePoint(ResizePoint.TOP));
        dragPoints.add(new ResizePoint(ResizePoint.TOP | ResizePoint.RIGHT));
        dragPoints.add(new ResizePoint(ResizePoint.RIGHT));
        dragPoints.add(new ResizePoint(ResizePoint.BOTTOM | ResizePoint.RIGHT));
        dragPoints.add(new ResizePoint(ResizePoint.BOTTOM));
        dragPoints.add(new ResizePoint(ResizePoint.BOTTOM | ResizePoint.LEFT));
        dragPoints.add(new ResizePoint(ResizePoint.LEFT));

        for (ResizePoint dragPoint : dragPoints) {
            ResizePointHandler handler = new ResizePointHandler(dragPoint);
            dragPoint.addMouseListener(handler);
            dragPoint.addMouseMotionListener(handler);
            add(dragPoint);
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;
        prova = g2.getTransform();

        g2.setStroke(new BasicStroke(1.0f));
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);

        if (isSelected() && !ignoreSelection) {
            g2.setPaint(GUIConstants.SELECTION_FILL_COLOUR);
            g2.fill(noteRect);
            if (drawBorder) {
                g2.setPaint(GUIConstants.SELECTION_LINE_COLOUR);
                g2.draw(noteRect);
            }
        } else {
            g2.setPaint(GUIConstants.ELEMENT_FILL_COLOUR);
            if (fillNote) {
                g2.fill(noteRect);
            }
            if (drawBorder) {
                g2.setPaint(GUIConstants.ELEMENT_LINE_COLOUR);
                g2.draw(noteRect);
            }
        }
        for (ResizePoint dragPoint : dragPoints) {
            dragPoint.paintOnCanvas(g);
        }
    }

    @Override
    public void addToContainer(Container container) {
        // Do nothing on add
    }

    @Override
    public void componentSpecificDelete() {
        //Nothing to do
    }

    /**
     * Deals with resizing of the annotation by handling mouse events
     */
    private class ResizePointHandler extends javax.swing.event.MouseInputAdapter {

        /**
         * Point to perform actions to
         */
        private final ResizePoint point;

        private Point start;


        public ResizePointHandler(ResizePoint point) {
            this.point = point;
        }

        @Override
        public void mousePressed(MouseEvent e) {
            point.isPressed = true;
            point.repaint();
            start = e.getPoint();
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            point.isPressed = false;
            updateBounds();
            point.repaint();
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            point.drag(e.getX() - start.x, e.getY() - start.y);
            updateBounds();
            point.repaint();
        }

    }

    /**
     * Resizable point for changing the size of the annotation
     * These appear on the boarder of the annotation
     */
    public class ResizePoint extends javax.swing.JComponent {

        private static final int TOP = 1;

        private static final int BOTTOM = 2;

        private static final int LEFT = 4;

        private static final int RIGHT = 8;

        public final int typeMask;

        private static final int SIZE = 3;

        private Rectangle shape;

        private boolean isPressed = false;

        public ResizePoint(int type) {
            setOpaque(false);
            setBounds(-SIZE - 1, -SIZE - 1, 2 * SIZE + GUIConstants.ANNOTATION_SIZE_OFFSET + 1,
                    2 * SIZE + GUIConstants.ANNOTATION_SIZE_OFFSET + 1);
            typeMask = type;
        }

        public void setLocation(double x, double y) {
            super.setLocation((int) (x - SIZE), (int) (y - SIZE));
        }

        private void drag(int x, int y) {
            if ((typeMask & TOP) == TOP) {
                adjustTop(y);
            }
            if ((typeMask & BOTTOM) == BOTTOM) {
                adjustBottom(y);
            }
            if ((typeMask & LEFT) == LEFT) {
                adjustLeft(x);
            }
            if ((typeMask & RIGHT) == RIGHT) {
                adjustRight(x);
            }
        }

        public void paintOnCanvas(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setTransform(prova);
            if (isSelected() && !AbstractPetriNetViewComponent.ignoreSelection) {
                g2.translate(this.getLocation().x, this.getLocation().y);
                shape = new Rectangle(0, 0, 2 * SIZE, 2 * SIZE);
                g2.fill(shape);

                g2.setStroke(new BasicStroke(1.0f));
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (isPressed) {
                    g2.setPaint(GUIConstants.RESIZE_POINT_DOWN_COLOUR);
                } else {
                    g2.setPaint(GUIConstants.ELEMENT_FILL_COLOUR);
                }
                g2.fill(shape);
                g2.setPaint(GUIConstants.ELEMENT_LINE_COLOUR);
                g2.draw(shape);
                g2.setTransform(prova);
            }
        }
    }


}