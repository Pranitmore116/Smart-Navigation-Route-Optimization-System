package model;

import java.util.Objects;

public class Road {

    public enum TrafficLevel {
        LOW(1.0),
        MEDIUM(1.35),
        HIGH(1.8);

        private final double multiplier;

        TrafficLevel(double multiplier) {
            this.multiplier = multiplier;
        }

        public double multiplier() {
            return multiplier;
        }

        @Override
        public String toString() {
            return name().charAt(0) + name().substring(1).toLowerCase();
        }
    }

    public enum RoadType {
        CITY("City Road"),
        HIGHWAY("Highway"),
        EXPRESSWAY("Expressway"),
        SERVICE("Service Road");

        private final String label;

        RoadType(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public enum Status {
        OPEN,
        CLOSED;

        @Override
        public String toString() {
            return name().charAt(0) + name().substring(1).toLowerCase();
        }
    }

    private int id;

    private final Location from;
    private final Location to;

    private double distance;
    private double speedLimit;

    private TrafficLevel trafficLevel;
    private RoadType roadType;
    private Status status;

    // Backward-compatible constructor
    public Road(int id,
                Location from,
                Location to,
                double distance,
                double speedLimit,
                TrafficLevel trafficLevel,
                Status status) {

        this(id,
                from,
                to,
                distance,
                speedLimit,
                trafficLevel,
                RoadType.CITY,
                status);
    }

    // Full constructor
    public Road(int id,
                Location from,
                Location to,
                double distance,
                double speedLimit,
                TrafficLevel trafficLevel,
                RoadType roadType,
                Status status) {

        if (from == null || to == null || from.equals(to)) {
            throw new IllegalArgumentException("A road needs two different locations");
        }

        this.id = id;
        this.from = from;
        this.to = to;

        setDistance(distance);
        setSpeedLimit(speedLimit);

        this.trafficLevel = Objects.requireNonNull(trafficLevel);
        this.roadType = Objects.requireNonNull(roadType);
        this.status = Objects.requireNonNull(status);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Location getFrom() {
        return from;
    }

    public Location getTo() {
        return to;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        if (distance <= 0) {
            throw new IllegalArgumentException("Distance must be greater than zero");
        }
        this.distance = distance;
    }

    public double getSpeedLimit() {
        return speedLimit;
    }

    public void setSpeedLimit(double speedLimit) {
        if (speedLimit <= 0) {
            throw new IllegalArgumentException("Speed must be greater than zero");
        }
        this.speedLimit = speedLimit;
    }

    public TrafficLevel getTrafficLevel() {
        return trafficLevel;
    }

    public void setTrafficLevel(TrafficLevel trafficLevel) {
        this.trafficLevel = Objects.requireNonNull(trafficLevel);
    }

    public RoadType getRoadType() {
        return roadType;
    }

    public void setRoadType(RoadType roadType) {
        this.roadType = Objects.requireNonNull(roadType);
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = Objects.requireNonNull(status);
    }

    public boolean isOpen() {
        return status == Status.OPEN;
    }

    public Location other(Location location) {
        if (from.equals(location)) {
            return to;
        }
        if (to.equals(location)) {
            return from;
        }
        throw new IllegalArgumentException("Location is not connected to this road");
    }

    public double travelTimeHours() {
        return distance / speedLimit * trafficLevel.multiplier();
    }

    @Override
    public String toString() {
        return from + " ↔ " + to;
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof Road road && id == road.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}