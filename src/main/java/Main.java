import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ui.MainWindow;

import java.util.Objects;

public class Main extends Application {
    @Override
    public void start(Stage stage) {
        MainWindow window = new MainWindow();
        Scene scene = new Scene(window, Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT);
        scene.getStylesheets().add(Objects.requireNonNull(
                getClass().getResource("/dark-theme.css")).toExternalForm());
        stage.setTitle(Constants.APP_TITLE);
         stage.setMinWidth(960);
        stage.setMinHeight(640);
        stage.setMaximized(true);
        stage.setScene(scene);
        stage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}