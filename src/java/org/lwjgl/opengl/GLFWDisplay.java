package org.lwjgl.opengl;

import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.input.Cursor;

import javax.swing.*;
import java.awt.*;
import java.awt.EventQueue;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.security.Key;
import java.lang.reflect.Constructor;


public class GLFWDisplay implements DisplayImplementation {
    static {
        sInit(Keyboard.keyDownBuffer);
    }

    private static boolean focused, visible, closeRequested, resized, hovered;
    private static int width, height, x, y;

    private static final ByteBuffer mouseEventBuffer = nGetMouseEventBuffer().order(ByteOrder.nativeOrder());
    private static final ByteBuffer keyboardEventBuffer = nGetKeyEventBuffer().order(ByteOrder.nativeOrder());
    private static final ByteBuffer mouseKeyBuffer = nGetMouseKeyBuffer();
    private static final IntBuffer mouseBuffer = nGetMouseBuffer().order(ByteOrder.nativeOrder()).asIntBuffer();

    private static Canvas attachedCanvas = null;
    private static EmptyCursorGrabListener grabListener = null;

    public interface EmptyCursorGrabListener {
        void onGrab(boolean grabbing);
    }

    static {
        try {
            Class infdevMouse = Class.forName("org.lwjgl.input.InfdevMouse");
            Constructor constructor = infdevMouse.getConstructor();
            grabListener = (EmptyCursorGrabListener) constructor.newInstance();
        } catch (NoClassDefFoundError ignored) {
	  // Cacio17 doesn't have this hack
	} catch (Exception e) {
	    System.out.printf("Unable to setup Infdev mouse handler: %s\n", e.getMessage());
	    e.printStackTrace();
	  }
    }

    public void createWindow(DrawableLWJGL drawable, DisplayMode mode, Canvas parent, int x, int y) throws LWJGLException {
        // In GLFW we don't actually have contexts. Instead we have windows, always with a context.
        ContextGL primaryContext = (ContextGL) drawable.getContext();
        long primaryWindowHandle = GLFWContextImplementation.getTrueHandle(primaryContext.getHandle());
        boolean resizable = Display.isResizable();

        if(parent != null) {
            // HACK for Minecraft: set Display settings based on the parent window of Canvas and hide it
            attachedCanvas = parent;

            resizable = true;
            Point screenLocation = attachedCanvas.getLocationOnScreen();

            x = screenLocation.x;
            y = screenLocation.y;
        }

        nAttachWindow(primaryWindowHandle, resizable);
        visible = true;
        if(Display.isFullscreen()) {
            nGoFullscreen( mode.getWidth(), mode.getHeight(), mode.getFrequency());
        } else {
            nReshape(x, y, mode.getWidth(), mode.getHeight());
            GLFWDisplay.x = x;
            GLFWDisplay.y = y;
        }
        width = mode.getWidth();
        height = mode.getHeight();
        nSetTitle(Display.getTitle());
    }

    public void destroyWindow() {
        nDetachWindow();
        attachedCanvas = null;
    }

    public void switchDisplayMode(DisplayMode mode) throws LWJGLException {
        nGoFullscreen(mode.getWidth(), mode.getHeight(), mode.getFrequency());
    }

    public void resetDisplayMode() {
        nReshape(0, 0, Display.getWidth(), Display.getHeight());
    }

    public int getGammaRampLength() {
        return 0;
    }

    public void setGammaRamp(FloatBuffer gammaRamp) throws LWJGLException {

    }

    public String getAdapter() {
        return null;
    }

    public String getVersion() {
        return null;
    }

    public DisplayMode init() throws LWJGLException {
        assert mouseEventBuffer.capacity() == Mouse.EVENT_SIZE * 49;
        assert keyboardEventBuffer.capacity() == Keyboard.EVENT_SIZE * 49;
        return nInit();
    }

    public void setTitle(String title) {
        nSetTitle(title);
    }

    public boolean isCloseRequested() {
        return closeRequested;
    }

    public boolean isVisible() {
        return visible;
    }

    public boolean isActive() {
        return focused;
    }

    public boolean isDirty() {
        return false;
    }

    public PeerInfo createPeerInfo(PixelFormat pixel_format, ContextAttribs attribs) throws LWJGLException {
        return new GLFWPeerInfo(attribs, pixel_format);
    }

    public void update() {
        long comboEvents = nPollEvents();
        int mouseEvents = (int) (comboEvents & 0x0FFFFFFF);
        int keyEvents = (int) ((comboEvents >> 32) & 0x0FFFFFFF);
        mouseEventBuffer.position(0).limit(mouseEvents * Mouse.EVENT_SIZE);
        keyboardEventBuffer.position(0).limit(keyEvents * Keyboard.EVENT_SIZE);
    }

    public void reshape(int x, int y, int width, int height) {
        if(attachedCanvas != null) {
            x = GLFWDisplay.x;
            y = GLFWDisplay.y;
        }
        nReshape(x, y, width, height);
    }

    public DisplayMode[] getAvailableDisplayModes() throws LWJGLException {
        return nGetVideoModes();
    }

    public int getPbufferCapabilities() {
        return 0;
    }

    public boolean isBufferLost(PeerInfo handle) {
        throw new UnsupportedOperationException("Not implemented. Use FBOs!");
    }

    public PeerInfo createPbuffer(int width, int height, PixelFormat pixel_format, ContextAttribs attribs, IntBuffer pixelFormatCaps, IntBuffer pBufferAttribs) throws LWJGLException {
        throw new UnsupportedOperationException("Not implemented. Use FBOs!");
    }

    public void setPbufferAttrib(PeerInfo handle, int attrib, int value) {
        throw new UnsupportedOperationException("Not implemented. Use FBOs!");
    }

    public void bindTexImageToPbuffer(PeerInfo handle, int buffer) {
        throw new UnsupportedOperationException("Not implemented. Use FBOs!");
    }

    public void releaseTexImageFromPbuffer(PeerInfo handle, int buffer) {
        throw new UnsupportedOperationException("Not implemented. Use FBOs!");
    }

    public int setIcon(ByteBuffer[] icons) {
        return 0;
    }

    public void setResizable(boolean resizable) {

    }

    public boolean wasResized() {
        try {
            return resized;
        } finally {
            resized = false;
        }
    }

    public boolean hasWheel() {
        return true;
    }

    public int getButtonCount() {
        return 8;
    }

    public void createMouse() throws LWJGLException {
    }

    public void destroyMouse() {

    }

    public void pollMouse(IntBuffer coord_buffer, ByteBuffer buttons) {
        coord_buffer.position(0);
        coord_buffer.put(mouseBuffer);
        mouseBuffer.position(0);
        buttons.position(0);
        buttons.put(mouseKeyBuffer);
        mouseKeyBuffer.position(0);
    }

    public void readMouse(ByteBuffer buffer) {
        buffer.put(mouseEventBuffer);
    }

    public void grabMouse(boolean grab) {
        nEnableGrab(grab);
    }

    public int getNativeCursorCapabilities() {
        return Cursor.CURSOR_ONE_BIT_TRANSPARENCY;
    }

    public void setCursorPosition(int x, int y) {

    }

    private void updateGrabbing(Object handle){
        //dummy
        if(handle == null) {
            grabMouse(false);
            if(grabListener != null) grabListener.onGrab(false);
        }
        else {
            grabMouse(true);
            if(grabListener != null) grabListener.onGrab(true);
        }
    }

    public void setNativeCursor(Object handle) throws LWJGLException {
        updateGrabbing(handle);
    }

    public int getMinCursorSize() {
        return 0;
    }

    public int getMaxCursorSize() {
        return 0;
    }

    public void createKeyboard() throws LWJGLException {

    }

    public void destroyKeyboard() {

    }

    public void pollKeyboard(ByteBuffer keyDownBuffer) {

    }

    public void readKeyboard(ByteBuffer buffer) {
        buffer.put(keyboardEventBuffer);
    }

    public Object createCursor(int width, int height, int xHotspot, int yHotspot, int numImages, IntBuffer images, IntBuffer delays) throws LWJGLException {
        return new Object();
    }

    public void destroyCursor(Object cursor_handle) {

    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isInsideWindow() {
        return hovered;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public float getPixelScaleFactor() {
        return 1;
    }

    private static native void sInit(ByteBuffer keyDownBuffer);
    private static native DisplayMode nInit();
    private static native DisplayMode[] nGetVideoModes();
    private static native void nGoFullscreen(int width, int height, int refresh);
    private static native void nReshape(int x, int y, int width, int height);
    private static native void nAttachWindow(long handle, boolean resizable);
    private static native void nDetachWindow();
    private static native void nSetTitle(String title);
    private static native long nPollEvents();
    private static native void nEnableGrab(boolean grabbing);
    private static native ByteBuffer nGetMouseBuffer();
    private static native ByteBuffer nGetMouseKeyBuffer();
    private static native ByteBuffer nGetKeyEventBuffer();
    private static native ByteBuffer nGetMouseEventBuffer();

    @SuppressWarnings("unused")
    private static void cUpdateWindowSize(final int width, final int height) {
        GLFWDisplay.width = width;
        GLFWDisplay.height = height;
        GLFWDisplay.resized = true;
        if(attachedCanvas != null) EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                // Annoying, but we need to make a roundtrip through AWT to correctly notify Display of a window resize when
                // it has a parent
                attachedCanvas.setSize(width, height);
            }
        });
    }

    @SuppressWarnings("unused")
    private static void cUpdateWindowPos(int x, int y) {
        GLFWDisplay.x = x;
        GLFWDisplay.y = y;
    }

    @SuppressWarnings("unused")
    private static void cUpdateWindowFocus(boolean focused) {
        GLFWDisplay.focused = focused;
    }

    @SuppressWarnings("unused")
    private static void cUpdateWindowVisible(boolean visible) {
        GLFWDisplay.visible = visible;
    }

    @SuppressWarnings("unused")
    private static void cUpdateWindowHovered(boolean hovered) {
        GLFWDisplay.hovered = hovered;
    }

    @SuppressWarnings("unused")
    private static void cUpdateWindowCloseRequested(boolean closeRequested) {
        GLFWDisplay.closeRequested = closeRequested;
    }

    @SuppressWarnings("unused")
    private static void cPushInputEvent() {

    }
}
