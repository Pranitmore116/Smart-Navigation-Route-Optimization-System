package model;

import java.util.Objects;

public class Location {
    private int id;
    private String name;
    private double x;
    private double y;

    public Location(int id, String name, double x, double y) {
        this.id = id;
        setName(name);
        this.x = x;
        this.y = y;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Location name is required");
        this.name = name.trim();
    }
    public double getX() { return x; }
    public void setX(double x) { this.x = x; }
    public double getY() { return y; }
    public void setY(double y) { this.y = y; }

    public double distanceTo(Location other) {
        return Math.hypot(x - other.x, y - other.y);
    }

    @Override public String toString() { return name; }
    @Override public boolean equals(Object o) {
        return o instanceof Location other && id == other.id;
    }
    @Override public int hashCode() { return Objects.hash(id); }
}