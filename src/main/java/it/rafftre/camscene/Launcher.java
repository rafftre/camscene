/*
 * Copyright (c) 2015 Raffaele Tretola
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package it.rafftre.camscene;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class Launcher extends Application {

    private static final Logger logger = LoggerFactory.getLogger(Launcher.class);

    @Override
    public void start(Stage primaryStage) throws Exception {
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());

        Map<String, String> params = getParameters().getNamed();
        int width = Integer.parseInt(params.getOrDefault("width", Util.DEFAULT_VIDEO_WIDTH.toString()));
        int height = Integer.parseInt(params.getOrDefault("height", Util.DEFAULT_VIDEO_HEIGHT.toString()));
        int fps = Integer.parseInt(params.getOrDefault("rate", Util.DEFAULT_VIDEO_FPS.toString()));

        primaryStage.setTitle(Util.getString("app.name"));

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(Util.MAIN_WINDOW_FXML), Util.getResourceBundle());
        Parent root = fxmlLoader.load();

        primaryStage.setScene(new Scene(
                root,
                Integer.parseInt(Util.getString("app.defaultSize.width")),
                Integer.parseInt(Util.getString("app.defaultSize.height"))));
        primaryStage.setMinWidth(Integer.parseInt(Util.getString("app.minSize.width")));
        primaryStage.setMinHeight(Integer.parseInt(Util.getString("app.minSize.height")));

        MainWindowController mainWindowController = fxmlLoader.getController();
        mainWindowController.setViewSize(new Dimension(width, height));
        mainWindowController.setFrameRate(fps);

        primaryStage.titleProperty().bind(
                new SimpleStringProperty(primaryStage.getTitle())
                        .concat(Util.getString("app.imageSizePrefix"))
                        .concat(mainWindowController.imageSizeProperty())
                        .concat(Util.getString("app.imageSizeSuffix")));

        WindowResizeListener listener = new WindowResizeListener(mainWindowController, primaryStage);
        primaryStage.widthProperty().addListener(listener);
        primaryStage.heightProperty().addListener(listener);

        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {

            @Override
            public void handle(WindowEvent event) {
                Platform.exit();
                System.exit(0);
            }
        });

        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

    private static class UncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

        @Override
        public void uncaughtException(Thread t, Throwable e) {
            if (Platform.isFxApplicationThread()) {
                Util.showExceptionDialog(e);
            } else {
                logger.error("An unexpected error occurred.", e);
            }
        }
    }

    private class WindowResizeListener implements ChangeListener<Number> {

        private ScheduledFuture scheduledTask = null;
        private MainWindowController mainWindowController;
        private Stage stage;

        public WindowResizeListener(MainWindowController mainWindowController, Stage stage) {
            this.mainWindowController = mainWindowController;
            this.stage = stage;
        }

        @Override
        public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
            if (scheduledTask != null && !scheduledTask.isDone()) {
                scheduledTask.cancel(false);
            }

            scheduledTask = Util.getScheduler().schedule(
                    new ResizeMainWindowTask(mainWindowController, stage),
                    Util.WINDOW_RESIZE_DELAY_MS,
                    TimeUnit.MILLISECONDS);
        }
    }

    private class ResizeMainWindowTask implements Runnable {

        private MainWindowController mainWindowController;
        private Stage stage;

        public ResizeMainWindowTask(MainWindowController mainWindowController, Stage stage) {
            this.mainWindowController = mainWindowController;
            this.stage = stage;
        }

        @Override
        public void run() {
            Platform.runLater(new Runnable() {

                @Override
                public void run() {
                    mainWindowController.adjustSize(stage.getWidth(), stage.getHeight());
                }
            });
        }
    }
}
