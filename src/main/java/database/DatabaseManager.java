package database;

import model.*;

import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDateTime;

public class DatabaseManager implements AutoCloseable {
    private final Connection connection;

    public DatabaseManager() {
        try {
            Path databasePath = Path.of(System.getProperty("user.home"), ".smart-navigation.db");
            connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");
                statement.execute("PRAGMA journal_mode = WAL");
            }
            createTables();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not open the navigation database", exception);
        }
    }

    private void createTables() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS locations (
                        id INTEGER PRIMARY KEY, name TEXT NOT NULL, x REAL NOT NULL, y REAL NOT NULL
                    )""");
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS roads (
                        id INTEGER PRIMARY KEY, from_id INTEGER NOT NULL, to_id INTEGER NOT NULL,
                        distance REAL NOT NULL, speed REAL NOT NULL, traffic TEXT NOT NULL, status TEXT NOT NULL,
                        FOREIGN KEY(from_id) REFERENCES locations(id) ON DELETE CASCADE,
                        FOREIGN KEY(to_id) REFERENCES locations(id) ON DELETE CASCADE
                    )""");
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS saved_routes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT, source_id INTEGER, destination_id INTEGER,
                        path TEXT NOT NULL, distance REAL NOT NULL, travel_time REAL NOT NULL, created_at TEXT NOT NULL
                    )""");
        }
    }

    public void loadGraph(Graph graph) {
        graph.clear();
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT id, name, x, y FROM locations ORDER BY id")) {
            while (rows.next()) {
                graph.addLocation(new Location(rows.getInt("id"), rows.getString("name"),
                        rows.getDouble("x"), rows.getDouble("y")));
            }
            try (ResultSet roads = statement.executeQuery("SELECT * FROM roads ORDER BY id")) {
                while (roads.next()) {
                    Location from = graph.getLocation(roads.getInt("from_id"));
                    Location to = graph.getLocation(roads.getInt("to_id"));
                    if (from != null && to != null) {
                        graph.addRoad(new Road(roads.getInt("id"), from, to, roads.getDouble("distance"),
                                roads.getDouble("speed"), Road.TrafficLevel.valueOf(roads.getString("traffic")),
                                Road.Status.valueOf(roads.getString("status"))));
                    }
                }
            }
        } catch (SQLException exception) {
            throw databaseError("load map", exception);
        }
    }

    public void saveLocation(Location location) {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO locations(id, name, x, y) VALUES(?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET name=excluded.name, x=excluded.x, y=excluded.y""")) {
            statement.setInt(1, location.getId());
            statement.setString(2, location.getName());
            statement.setDouble(3, location.getX());
            statement.setDouble(4, location.getY());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw databaseError("save location", exception);
        }
    }

    public void deleteLocation(Location location) {
        executeDelete("DELETE FROM locations WHERE id=?", location.getId());
    }

    public void saveRoad(Road road) {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO roads(id, from_id, to_id, distance, speed, traffic, status)
                VALUES(?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET distance=excluded.distance, speed=excluded.speed,
                traffic=excluded.traffic, status=excluded.status""")) {
            statement.setInt(1, road.getId());
            statement.setInt(2, road.getFrom().getId());
            statement.setInt(3, road.getTo().getId());
            statement.setDouble(4, road.getDistance());
            statement.setDouble(5, road.getSpeedLimit());
            statement.setString(6, road.getTrafficLevel().name());
            statement.setString(7, road.getStatus().name());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw databaseError("save road", exception);
        }
    }

    public void deleteRoad(Road road) {
        executeDelete("DELETE FROM roads WHERE id=?", road.getId());
    }

    public void saveRoute(Route route) {
        if (!route.isFound()) return;
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO saved_routes(source_id, destination_id, path, distance, travel_time, created_at)
                VALUES(?, ?, ?, ?, ?, ?)""")) {
            statement.setInt(1, route.getPath().get(0).getId());
            statement.setInt(2, route.getPath().get(route.getPath().size() - 1).getId());
            statement.setString(3, route.pathText());
            statement.setDouble(4, route.getDistance());
            statement.setDouble(5, route.getTravelTimeHours());
            statement.setString(6, LocalDateTime.now().toString());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw databaseError("save route", exception);
        }
    }

    private void executeDelete(String sql, int id) {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw databaseError("delete item", exception);
        }
    }

    private IllegalStateException databaseError(String action, SQLException exception) {
        return new IllegalStateException("Could not " + action + ": " + exception.getMessage(), exception);
    }


    public void clearAll() {
    try (Statement statement = connection.createStatement()) {
        statement.executeUpdate("DELETE FROM saved_routes");
        statement.executeUpdate("DELETE FROM roads");
        statement.executeUpdate("DELETE FROM locations");
    } catch (SQLException exception) {
        throw databaseError("clear database", exception);
    }
}
    @Override public void close() {
        try {
            connection.close();
        } catch (SQLException ignored) {
        }
    }
}