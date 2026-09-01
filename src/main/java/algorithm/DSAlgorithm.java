package algorithm;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import model.Location;
import model.Route;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class DSAlgorithm {
    private Timeline timeline;

    public void animate(Route route, Consumer<Location> onVisit, Runnable onComplete) {
        animate(route, onVisit, (from, to) -> {}, onComplete);
    }

    public void animate(Route route, Consumer<Location> onVisit, BiConsumer<Location, Location> onEdge,
                        Runnable onComplete) {
        stop();
        timeline = new Timeline();
        int index = 0;
        List<Location> visited = route.getVisited();
        for (int i = 0; i < visited.size(); i++) {
            Location location = visited.get(i);
            Location current = location;
            timeline.getKeyFrames().add(new KeyFrame(Duration.millis(index++ * 170L),
                    event -> onVisit.accept(current)));
            if (i > 0) {
                Location previous = visited.get(i - 1);
                timeline.getKeyFrames().add(new KeyFrame(Duration.millis(index++ * 120L),
                        event -> onEdge.accept(previous, current)));
            }
        }
        timeline.getKeyFrames().add(new KeyFrame(Duration.millis(Math.max(1, index) * 170L),
                event -> onComplete.run()));
        timeline.play();
    }

    public void stop() {
        if (timeline != null) timeline.stop();
    }
}
