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

import com.sleepingdumpling.jvideoinput.Device;
import com.sleepingdumpling.jvideoinput.VideoInput;
import com.sleepingdumpling.jvideoinput.VideoInputException;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.locks.LockSupport;

public final class MainWindowController {

    private static final Logger logger = LoggerFactory.getLogger(MainWindowController.class);

    public FlowPane topPane;
    public BorderPane cameraPane;
    public ComboBox<CameraInfo> selectBox;
    public Button playButton;
    public Button disposeButton;
    public ImageView startImage;
    public ImageView stopImage;
    public ImageView cameraImage;

    private CameraInfo currentCamera = null;
    private boolean stopCamera = false;
    private Dimension viewSize;
    private int frameRate;

    private ObjectProperty<Image> imageProperty = new SimpleObjectProperty<>();
    private StringProperty imageSize = new SimpleStringProperty();

    public StringProperty imageSizeProperty() {
        return imageSize;
    }

    public void setViewSize(Dimension viewSize) {
        if (viewSize != null) {
            this.viewSize = viewSize;
        } else {
            this.viewSize = new Dimension(Util.DEFAULT_VIDEO_WIDTH, Util.DEFAULT_VIDEO_HEIGHT);
        }
    }

    public void setFrameRate(int frameRate) {
        if (frameRate > 0) {
            this.frameRate = frameRate;
        } else {
            this.frameRate = Util.DEFAULT_VIDEO_FPS;
        }
    }

    public void initialize() {
        startImage = new ImageView(new Image(getClass().getResourceAsStream(Util.START_IMAGE)));
        stopImage = new ImageView(new Image(getClass().getResourceAsStream(Util.STOP_IMAGE)));

        loadCameraOptions();

        playButton.setText("");
        playButton.setGraphic(startImage);
        playButton.setDisable(true);
        disposeButton.setText("");
        disposeButton.setGraphic(new ImageView(new Image(getClass().getResourceAsStream(Util.DISPOSE_IMAGE))));
        disposeButton.setDisable(true);

        imageSize.setValue(Util.getString("app.imageSizeEmptyDescr"));

        cameraImage.imageProperty().bind(imageProperty);

        Platform.runLater(new Runnable() {

            @Override
            public void run() {
                adjustSize(0, 0);
            }
        });

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                disposeCamera();
            }
        });
    }

    public void adjustSize(double windowWidth, double windowHeight) {
        double height = cameraPane.getHeight();
        double width = cameraPane.getWidth();
        if (windowHeight > 0 && cameraPane.getHeight() > windowHeight) {
            height = windowHeight;
        }
        if (windowWidth > 0 && cameraPane.getWidth() > windowWidth) {
            width = windowWidth;
        }
        logger.debug("Adjusting size to {}x{}, window: {}x{}", width, height, windowWidth, windowHeight);

        cameraImage.setFitHeight(height);
        cameraImage.setFitWidth(width);
        cameraImage.prefHeight(height);
        cameraImage.prefWidth(width);
    }

    public void selectAction(ActionEvent e) {
        final CameraInfo cameraInfo = selectBox.getSelectionModel().getSelectedItem();

        if (cameraInfo == null) {
            return;
        }

        logger.debug("Selected camera '{}'", cameraInfo.getName());
        Util.startDaemonTask(new SwitchCameraTask(cameraInfo));

        playButton.setGraphic(stopImage);
        playButton.setDisable(false);
        disposeButton.setDisable(false);
    }

    public void playAction(ActionEvent e) {
        if (startImage.equals(playButton.getGraphic())) {
            startCamera();
        } else {
            stopCamera();
        }
    }

    public void disposeAction(ActionEvent e) {
        if (currentCamera == null) {
            return;
        }

        disposeCamera();

        selectBox.getSelectionModel().clearSelection();

        // le due proprietà andrebbero resettate alla fine del StartCameraStreamTask, ma solo in questo caso
        imageProperty.setValue(null);
        imageSize.setValue(Util.getString("app.imageSizeEmptyDescr"));

        playButton.setGraphic(startImage);
        playButton.setDisable(true);
        disposeButton.setDisable(true);
    }

    private void loadCameraOptions() {
        ObservableList<CameraInfo> options = FXCollections.observableArrayList();

        for (Device device : VideoInput.getVideoDevices()) {
            logger.debug("Found camera '{}'.", device.getNameStr());
            options.add(new CameraInfo(device));
        }

        selectBox.setItems(options);
    }

    private void startCamera() {
        logger.debug("Starting camera '{}'.", currentCamera);

        Util.startDaemonTask(new RetrieveAndDisplayTask());

        playButton.setGraphic(stopImage);
    }

    private void stopCamera() {
        logger.debug("Stopping camera '{}'.", currentCamera);

        stopCamera = true;

        playButton.setGraphic(startImage);
    }

    private void disposeCamera() {
        stopCamera = true;
    }

    private class SwitchCameraTask extends Task<Void> {
        private CameraInfo cameraInfo;

        public SwitchCameraTask(CameraInfo cameraInfo) {
            this.cameraInfo = cameraInfo;
        }

        @Override
        protected Void call() throws Exception {
            if (currentCamera != null) {
                disposeCamera();
            }

            currentCamera = cameraInfo;
            logger.debug("Connecting camera '{}'.", currentCamera);

            Platform.runLater(new Runnable() {

                @Override
                public void run() {
                    adjustSize(0, 0);
                    Util.startDaemonTask(new RetrieveAndDisplayTask());
                }
            });

            return null;
        }
    }

    private class RetrieveAndDisplayTask extends Task<Void> {

        @Override
        protected Void call() throws Exception {
            stopCamera = false;

            if (currentCamera == null) {
                return null;
            }
            currentCamera.setViewSize(viewSize);
            currentCamera.setFrameRate(frameRate);

            logger.debug("Starting stream on camera '{}'.", currentCamera);

            try {
                currentCamera.open();
            } catch (VideoInputException e) {
                logger.warn(null, e);
                return null;
            }

            final long interval = Util.ONE_SECOND_IN_NANOS / currentCamera.getFrameRate();
            long lastReportTime = -1;
            long imgCnt = 0;

            while (!stopCamera) {
                long start = System.nanoTime();
                try {
                    final BufferedImage grabbedImage = currentCamera.grabFrame();
                    if (grabbedImage != null) {
                        imgCnt++;

                        long now = System.nanoTime();
                        if (lastReportTime != -1 && now - lastReportTime >= Util.ONE_SECOND_IN_NANOS) {
                            final double videoFps =
                                    ((double) imgCnt * Util.ONE_SECOND_IN_NANOS) / (now - lastReportTime);

                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    imageSize.setValue(String.format(
                                            Util.getString("app.imageSizeDescr"),
                                            currentCamera.getViewSize().width,
                                            currentCamera.getViewSize().height,
                                            videoFps));
                                }
                            });

                            imgCnt = 0;
                            lastReportTime = now;
                        } else if (lastReportTime == -1) {
                            lastReportTime = now;
                        }

                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                imageProperty.set(SwingFXUtils.toFXImage(grabbedImage, null));
                            }
                        });
                    }
                } catch (Exception e) {
                    logger.warn(null, e);
                } finally {
                    long end = System.nanoTime();
                    long waitTime = interval - (end - start);
                    if (waitTime > 0) {
                        LockSupport.parkNanos(waitTime);
                    }
                }
            }

            logger.debug("Stopping stream on camera '{}'.", currentCamera);

            currentCamera.close();

            return null;
        }
    }
}
