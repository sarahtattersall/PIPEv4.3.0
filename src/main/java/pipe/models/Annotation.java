package pipe.models;

public class Annotation {
    public Annotation(double x, double y, String text,
                      double width,
                      double height, boolean border) {
        this.border = border;
        this.x = x;
        this.y = y;
        this.text = text;
        this.width = width;
        this.height = height;
    }

    private boolean border;
    private double x;
    private double y;
    private String text;
    private double width;
    private double height;

    public boolean hasBoarder() {
        return border;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public String getText() {
        return text;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }
}