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
import com.sleepingdumpling.jvideoinput.VideoFrame;
import com.sleepingdumpling.jvideoinput.VideoInput;
import com.sleepingdumpling.jvideoinput.VideoInputException;

import java.awt.*;
import java.awt.image.BufferedImage;

final class CameraInfo {

    private Device device;
    private Dimension viewSize;
    private int frameRate;
    private VideoInput videoInput;

    public CameraInfo(Device device) {
        this.device = device;
        this.viewSize = new Dimension(Util.DEFAULT_VIDEO_WIDTH, Util.DEFAULT_VIDEO_HEIGHT);
        this.frameRate = Util.DEFAULT_VIDEO_FPS;
    }

    public CameraInfo() {
    }

    public String getName() {
        return device.getNameStr();
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

    public Dimension getViewSize() {
        return viewSize;
    }

    public void setViewSize(Dimension viewSize) {
        this.viewSize = viewSize;
    }

    public int getFrameRate() {
        return frameRate;
    }

    public void setFrameRate(int frameRate) {
        this.frameRate = frameRate;
    }

    public void open() throws VideoInputException {
        if (videoInput != null) {
            return;
        }

        videoInput = new VideoInput(viewSize.width, viewSize.height, frameRate, device);
    }

    public void close() {
        if (videoInput == null) {
            return;
        }

        videoInput.stopSession();
        videoInput = null;
    }

    public BufferedImage grabFrame() {
        if (videoInput == null) {
            return null;
        }

        VideoFrame vf = videoInput.getNextFrame(null);
        if (vf == null) {
            return null;
        }

        return Util.getRenderingBufferedImage(vf);
    }

    @Override
    public String toString() {
        return device.getNameStr();
    }
}
