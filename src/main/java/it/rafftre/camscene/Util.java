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

import com.sleepingdumpling.jvideoinput.VideoFrame;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.WritableRaster;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

final class Util {

    static final String RESOURCE_BUNDLE_NAME = "Messages";
    static final String MAIN_WINDOW_FXML = "/ui/MainWindow.fxml";
    static final String START_IMAGE = "/images/play.png";
    static final String STOP_IMAGE = "/images/pause.png";
    static final String DISPOSE_IMAGE = "/images/stop.png";
    static final long WINDOW_RESIZE_DELAY_MS = 500;
    static final long ONE_SECOND_IN_NANOS = 1000000000L;
    static final Integer DEFAULT_VIDEO_WIDTH = 640;
    static final Integer DEFAULT_VIDEO_HEIGHT = 480;
    static final Integer DEFAULT_VIDEO_FPS = 25;

    private static ResourceBundle resourceBundle;
    private static ScheduledExecutorService scheduler;

    static {
        scheduler = Executors.newSingleThreadScheduledExecutor();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                scheduler.shutdownNow();
            }
        });
    }

    public static ResourceBundle getResourceBundle() {
        if (resourceBundle == null) {
            resourceBundle = ResourceBundle.getBundle(Util.RESOURCE_BUNDLE_NAME);
        }

        return resourceBundle;
    }

    public static String getString(String key, Object... params) {
        return MessageFormat.format(getResourceBundle().getString(key), params);
    }

    public static ScheduledExecutorService getScheduler() {
        return scheduler;
    }

    public static void startDaemonTask(Task<Void> task) {
        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
    }

    public static void showExceptionDialog(Throwable e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Exception Dialog");
        alert.setHeaderText("Errore inaspettato");
        alert.setContentText(e.getLocalizedMessage());

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String exceptionText = sw.toString();

        Label label = new Label("The exception stacktrace was:");

        TextArea textArea = new TextArea(exceptionText);
        textArea.setEditable(false);
        textArea.setWrapText(true);

        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(label, 0, 0);
        expContent.add(textArea, 0, 1);

        alert.getDialogPane().setExpandableContent(expContent);

        alert.showAndWait();
    }

    public static BufferedImage getRenderingBufferedImage(VideoFrame videoFrame) {
        GraphicsConfiguration gc =
                GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        BufferedImage img =
                gc.createCompatibleImage(videoFrame.getWidth(), videoFrame.getHeight(), Transparency.TRANSLUCENT);
        if (img.getType() == BufferedImage.TYPE_INT_ARGB
                || img.getType() == BufferedImage.TYPE_INT_ARGB_PRE
                || img.getType() == BufferedImage.TYPE_INT_RGB) {
            WritableRaster raster = img.getRaster();
            DataBufferInt dataBuffer = (DataBufferInt) raster.getDataBuffer();

            byte[] data = videoFrame.getRawData();
            addAlphaChannel(data, data.length, dataBuffer.getData());
            return img; //convert the data ourselves, the performance is much better
        } else {
            return videoFrame.getBufferedImage(); //much slower when drawing it on the screen.
        }
    }

    private static void addAlphaChannel(byte[] rgbBytes, int bytesLen, int[] argbInts) {
        for (int i = 0, j = 0; i < bytesLen; i += 3, j++) {
            argbInts[j] = ((byte) 0xff) << 24 |        // Alpha
                    (rgbBytes[i] << 16) & (0xff0000) |        // Red
                    (rgbBytes[i + 1] << 8) & (0xff00) |        // Green
                    (rgbBytes[i + 2]) & (0xff);                // Blue
        }
    }
}
