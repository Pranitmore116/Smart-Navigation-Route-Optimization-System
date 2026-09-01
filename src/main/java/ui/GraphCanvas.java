package ui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import model.*;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class GraphCanvas extends Region {
    private static final double NODE_RADIUS = 19;
    private static final double VIEW_PADDING = 54;
    private static final double LABEL_SPACE = 44;

    private final Canvas canvas = new Canvas();
    private final Graph graph;
    private final Set<Location> visited = new LinkedHashSet<>();
    private final Set<Road> animatedEdges = new LinkedHashSet<>();
    private final Map<Location, String> badges = new HashMap<>();
    private Route highlightedRoute;
    private Location selected;
    private Location dragged;
    private Location active;
    private Location source;
    private Location destination;
    private double viewScale = 1.0;
    private double viewOffsetX = 0.0;
    private double viewOffsetY = 0.0;
    private Consumer<Location> selectionListener = ignored -> {};
    private Consumer<Location> moveListener = ignored -> {};
    private BiConsumer<Double, Double> mouseListener = (x, y) -> {};

    public GraphCanvas(Graph graph) {
        this.graph = graph;
        getChildren().add(canvas);
        getStyleClass().add("graph-canvas");
        widthProperty().addListener(ignored -> resizeCanvas());
        heightProperty().addListener(ignored -> resizeCanvas());
        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, this::mousePressed);
        canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::mouseDragged);
        canvas.addEventHandler(MouseEvent.MOUSE_RELEASED, this::mouseReleased);
        canvas.addEventHandler(MouseEvent.MOUSE_MOVED, this::mouseMoved);
    }

    public void setSelectionListener(Consumer<Location> listener) { selectionListener = listener; }
    public void setMoveListener(Consumer<Location> listener) { moveListener = listener; }
    public void setMouseListener(BiConsumer<Double, Double> listener) { mouseListener = listener; }
    public Location getSelected() { return selected; }
    public double getZoomPercent() { return viewScale * 100.0; }

    public void setSelected(Location location) {
        selected = location;
        draw();
    }

    public void setRouteEndpoints(Location source, Location destination) {
        this.source = source;
        this.destination = destination;
        draw();
    }

    public void setBadges(Map<Location, String> values) {
        badges.clear();
        badges.putAll(values);
        draw();
    }

    public void clearVisualization() {
        visited.clear();
        animatedEdges.clear();
        badges.clear();
        active = null;
        highlightedRoute = null;
        draw();
    }

    public void showVisit(Location location) {
        active = location;
        visited.add(location);
        draw();
    }

    public void showEdge(Location from, Location to) {
        graph.findRoad(from, to).ifPresent(animatedEdges::add);
        draw();
    }

    public void showRoute(Route route) {
        highlightedRoute = route;
        active = null;
        draw();
    }

    public void refresh() { draw(); }

    private void resizeCanvas() {
        canvas.setWidth(getWidth());
        canvas.setHeight(getHeight());
        draw();
    }

    private void mousePressed(MouseEvent event) {
        dragged = findLocation(event.getX(), event.getY());
        selected = dragged;
        selectionListener.accept(selected);
        draw();
    }

    private void mouseDragged(MouseEvent event) {
        if (dragged == null) return;
        double safeX = clamp(event.getX(), NODE_RADIUS + 12, getWidth() - NODE_RADIUS - 12);
        double safeY = clamp(event.getY(), NODE_RADIUS + 12, getHeight() - NODE_RADIUS - LABEL_SPACE);
        dragged.setX(screenToModelX(safeX));
        dragged.setY(screenToModelY(safeY));
        mouseMoved(event);
        draw();
    }

    private void mouseReleased(MouseEvent ignored) {
        if (dragged != null) moveListener.accept(dragged);
        dragged = null;
    }

    private void mouseMoved(MouseEvent event) {
        updateViewport();
        mouseListener.accept(screenToModelX(event.getX()), screenToModelY(event.getY()));
    }

    private Location findLocation(double x, double y) {
        updateViewport();
        Location best = null;
        for (Location location : graph.getLocations()) {
            if (Math.hypot(screenX(location) - x, screenY(location) - y) <= NODE_RADIUS + 6) {
                best = location;
            }
        }
        return best;
    }

    private void draw() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.web("#17191d"));
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        updateViewport();
        drawGrid(gc);
        for (Road road : graph.getRoads()) drawRoad(gc, road);
        for (Location location : graph.getLocations()) drawNode(gc, location);
    }

    private void updateViewport() {
        if (graph.getLocations().isEmpty() || canvas.getWidth() <= 0 || canvas.getHeight() <= 0) {
            viewScale = 1.0;
            viewOffsetX = 0.0;
            viewOffsetY = 0.0;
            return;
        }

        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        for (Location location : graph.getLocations()) {
            minX = Math.min(minX, location.getX());
            minY = Math.min(minY, location.getY());
            maxX = Math.max(maxX, location.getX());
            maxY = Math.max(maxY, location.getY());
        }

        double graphWidth = Math.max(1, maxX - minX);
        double graphHeight = Math.max(1, maxY - minY);
        double availableWidth = Math.max(1, canvas.getWidth() - VIEW_PADDING * 2);
        double availableHeight = Math.max(1, canvas.getHeight() - VIEW_PADDING * 2 - LABEL_SPACE);

        viewScale = Math.min(availableWidth / graphWidth, availableHeight / graphHeight);
        viewScale = clamp(viewScale, 0.55, 1.08);

        double fittedWidth = graphWidth * viewScale;
        double fittedHeight = graphHeight * viewScale;
        viewOffsetX = (canvas.getWidth() - fittedWidth) / 2 - minX * viewScale;
        viewOffsetY = (canvas.getHeight() - LABEL_SPACE - fittedHeight) / 2 - minY * viewScale;
    }

    private void drawGrid(GraphicsContext gc) {
        gc.setStroke(Color.web("#202329"));
        gc.setLineWidth(1);
        for (double x = 0; x < canvas.getWidth(); x += 32) gc.strokeLine(x, 0, x, canvas.getHeight());
        for (double y = 0; y < canvas.getHeight(); y += 32) gc.strokeLine(0, y, canvas.getWidth(), y);
    }

    private void drawRoad(GraphicsContext gc, Road road) {
        Location a = road.getFrom();
        Location b = road.getTo();
        double ax = screenX(a);
        double ay = screenY(a);
        double bx = screenX(b);
        double by = screenY(b);
        boolean inRoute = isRouteSegment(a, b);
        boolean animated = animatedEdges.contains(road);

        Color color = !road.isOpen() ? Color.web("#8f1f2a") :
                inRoute ? Color.web("#58d889") :
                animated ? Color.web("#f0b95b") : Color.web("#59616e");
        gc.setStroke(color);
        gc.setLineWidth(inRoute ? 5 : animated ? 3.5 : 2.4);
        gc.strokeLine(ax, ay, bx, by);

        double midX = (ax + bx) / 2;
        double midY = (ay + by) / 2;
        String text = String.format("%.1f km | %.0f km/h | %s", road.getDistance(), road.getSpeedLimit(),
                road.getTrafficLevel());
        gc.setFont(Font.font("System", FontWeight.SEMI_BOLD, 10.5));
        double width = text.length() * 5.7;
        gc.setFill(Color.web("#111317", 0.88));
        gc.fillRoundRect(midX - width / 2 - 6, midY - 11, width + 12, 21, 8, 8);
        gc.setStroke(Color.web("#333944"));
        gc.strokeRoundRect(midX - width / 2 - 6, midY - 11, width + 12, 21, 8, 8);
        gc.setFill(Color.web("#d9dee7"));
        gc.fillText(text, midX - width / 2, midY + 4);
    }

    private void drawNode(GraphicsContext gc, Location location) {
        double x = screenX(location);
        double y = screenY(location);
        Color fill = Color.web("#4f91ff");
        if (visited.contains(location)) fill = Color.web("#8a93a3");
        if (location.equals(source)) fill = Color.web("#50d889");
        if (location.equals(destination)) fill = Color.web("#ef5b64");
        if (location.equals(active)) fill = Color.web("#c996ff");
        if (location.equals(selected)) fill = Color.web("#f0b95b");

        gc.setFill(Color.web("#000000", 0.38));
        gc.fillOval(x - NODE_RADIUS + 4, y - NODE_RADIUS + 7, NODE_RADIUS * 2 + 2, NODE_RADIUS * 2 + 2);
        gc.setFill(Color.web("#ffffff", 0.10));
        gc.fillOval(x - NODE_RADIUS - 4, y - NODE_RADIUS - 4, NODE_RADIUS * 2 + 8, NODE_RADIUS * 2 + 8);
        gc.setFill(fill);
        gc.fillOval(x - NODE_RADIUS, y - NODE_RADIUS, NODE_RADIUS * 2, NODE_RADIUS * 2);
        gc.setStroke(Color.web("#edf4ff"));
        gc.setLineWidth(2);
        gc.strokeOval(x - NODE_RADIUS, y - NODE_RADIUS, NODE_RADIUS * 2, NODE_RADIUS * 2);

        gc.setFont(Font.font("System", FontWeight.BOLD, 12));
        double nameWidth = location.getName().length() * 6.8;
        gc.setFill(Color.web("#f2f4f8"));
        gc.fillText(location.getName(), x - nameWidth / 2, y + 36);

        String badge = badges.get(location);
        if (badge != null && !badge.isBlank()) drawBadge(gc, badge, x, y - 34);
    }

    private void drawBadge(GraphicsContext gc, String text, double x, double y) {
        gc.setFont(Font.font("System", FontWeight.SEMI_BOLD, 10));
        double width = Math.max(34, text.length() * 5.8);
        gc.setFill(Color.web("#101216", 0.92));
        gc.fillRoundRect(x - width / 2 - 6, y - 10, width + 12, 19, 8, 8);
        gc.setFill(Color.web("#a9c7ff"));
        gc.fillText(text, x - width / 2, y + 3);
    }

    private boolean isRouteSegment(Location a, Location b) {
        if (highlightedRoute == null) return false;
        List<Location> path = highlightedRoute.getPath();
        for (int i = 0; i + 1 < path.size(); i++) {
            if ((path.get(i).equals(a) && path.get(i + 1).equals(b)) ||
                    (path.get(i).equals(b) && path.get(i + 1).equals(a))) return true;
        }
        return false;
    }

    private double clamp(double value, double min, double max) {
        if (max < min) return (min + max) / 2;
        return Math.max(min, Math.min(max, value));
    }

    private double screenX(Location location) { return location.getX() * viewScale + viewOffsetX; }
    private double screenY(Location location) { return location.getY() * viewScale + viewOffsetY; }
    private double screenToModelX(double x) { return (x - viewOffsetX) / viewScale; }
    private double screenToModelY(double y) { return (y - viewOffsetY) / viewScale; }

    @Override protected void layoutChildren() {
        canvas.relocate(0, 0);
        resizeCanvas();
    }
}