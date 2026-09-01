package model;

import java.util.*;

public class Graph {

    private final Map<Integer, Location> locations = new LinkedHashMap<>();
    private final Map<Location, List<Road>> adjacencyList = new LinkedHashMap<>();
    private final List<Road> roads = new ArrayList<>();

    private int nextLocationId = 1;
    private int nextRoadId = 1;

    public Location addLocation(String name, double x, double y) {
        Location location = new Location(nextLocationId++, name, x, y);
        addLocation(location);
        return location;
    }

    public void addLocation(Location location) {
        locations.put(location.getId(), location);
        adjacencyList.computeIfAbsent(location, k -> new ArrayList<>());
        nextLocationId = Math.max(nextLocationId, location.getId() + 1);
    }

    // Backward-compatible method
    public Road addRoad(Location from,
                        Location to,
                        double distance,
                        double speed,
                        Road.TrafficLevel traffic,
                        Road.Status status) {

        return addRoad(
                from,
                to,
                distance,
                speed,
                traffic,
                Road.RoadType.CITY,
                status
        );
    }

    // New method with RoadType
    public Road addRoad(Location from,
                        Location to,
                        double distance,
                        double speed,
                        Road.TrafficLevel traffic,
                        Road.RoadType roadType,
                        Road.Status status) {

        Road road = new Road(
                nextRoadId++,
                from,
                to,
                distance,
                speed,
                traffic,
                roadType,
                status
        );

        addRoad(road);
        return road;
    }

    public void addRoad(Road road) {

        requireLocation(road.getFrom());
        requireLocation(road.getTo());

        if (findRoad(road.getFrom(), road.getTo()).isPresent()) {
            throw new IllegalArgumentException("A road already connects these locations");
        }

        roads.add(road);

        adjacencyList.get(road.getFrom()).add(road);
        adjacencyList.get(road.getTo()).add(road);

        nextRoadId = Math.max(nextRoadId, road.getId() + 1);
    }

    public void removeLocation(Location location) {
        new ArrayList<>(getRoads(location)).forEach(this::removeRoad);
        adjacencyList.remove(location);
        locations.remove(location.getId());
    }

    public void removeRoad(Road road) {
        roads.remove(road);

        List<Road> fromList = adjacencyList.get(road.getFrom());
        List<Road> toList = adjacencyList.get(road.getTo());

        if (fromList != null) fromList.remove(road);
        if (toList != null) toList.remove(road);
    }

    public Collection<Location> getLocations() {
        return Collections.unmodifiableCollection(locations.values());
    }

    public List<Road> getRoads() {
        return Collections.unmodifiableList(roads);
    }

    public List<Road> getRoads(Location location) {
        return Collections.unmodifiableList(
                adjacencyList.getOrDefault(location, List.of())
        );
    }

    public Location getLocation(int id) {
        return locations.get(id);
    }

    public Optional<Road> findRoad(Location a, Location b) {

        return adjacencyList
                .getOrDefault(a, List.of())
                .stream()
                .filter(road -> road.other(a).equals(b))
                .findFirst();
    }

    public void clear() {
        roads.clear();
        adjacencyList.clear();
        locations.clear();
        nextLocationId = 1;
        nextRoadId = 1;
    }

    private void requireLocation(Location location) {

        if (!locations.containsKey(location.getId())) {
            throw new IllegalArgumentException(
                    "Location is not part of the graph"
            );
        }
    }
}