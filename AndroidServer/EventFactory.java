package com.example.server;

import android.os.SystemClock;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;

import androidx.constraintlayout.widget.ConstraintSet;

/*
 * Key Event:
 * 1. downtime: long: The time (in ms) when the user originally pressed down.
 * 2. eventtime: long: The the time (in ms) when this specific event was generated.
 * 3. action: int: The kind of action being performed, such as ACTION_DOWN.
 * 4. code: int: The key code.
 * 5. repeat: int: A repeat count for down events or event count for multiple events.
 * 6. metaState: int: Flags indicating which meta keys are currently pressed.
 * 7. deviceId: int: The device ID that generated the key event.
 * 8. scancode: int: Raw device scan code of the event.
 * 9. flags: int: The flags for this key event
 * 10. source: int: The input source such as InputDevice#SOURCE_KEYBOARD.
 */

/*
 * Motion Event:
 * 1. downtime: long: The time (in ms) when the user originally pressed down.
 * 2. eventtime: long: The the time (in ms) when this specific event was generated.
 * 3. action: int: The kind of action being performed, such as ACTION_DOWN.
 * 4. pointerCount: int: The number of pointers that will be in this event.
 * 5. pointerProperties: PointerProperties: An array of pointerCount values providing PointerProperties property
 * 6. pointerCoords: PointerCoords: An array of pointerCount values providing PointerCoords coordinate
 * 7. metaState: int: The state of any meta / modifier keys that were in effect when the event was generated.
 * 8. buttonState: int: The state of buttons that are pressed.
 * 9. xPrecision: float: The precision of the X coordinate being reported.
 * 10. yPrecision: float: The precision of the Y coordinate being reported.
 * 11. deviceId: int: The id for the device that this event came from.
 * 12. edgeFlags: int: A bitfield indicating which edges, if any, were touched by this MotionEvent.
 * 13. source: 	int: The source of this event.
 * 14. flags: int: The motion event flags.
 */

public class EventFactory {
    // KeyEvent部分
    // 处理键盘输入事件
    public static KeyEvent keyEvent(int action, int keyCode, int repeat, int metaState) {
        long now = SystemClock.uptimeMillis();
        KeyEvent event = new KeyEvent(now, now, action, keyCode, repeat, metaState,
                KeyCharacterMap.VIRTUAL_KEYBOARD,
                0,
                0,
                InputDevice.SOURCE_KEYBOARD);
        return event;
    }

    // 通过送入一个ACTION_DOWN 和ACTION_UP 来模拟一次点击的事件，用于开屏中
    public static KeyEvent[] clickEvent(int keyCode) {
        return new KeyEvent[]{keyEvent(KeyEvent.ACTION_DOWN, keyCode, 0, 0)
                , keyEvent(KeyEvent.ACTION_UP, keyCode, 0, 0)};
    }

    // MotionEvent部分
    private static long lastMouseDown;
    private static int lastX;
    private static int lastY;
    private static final MotionEvent.PointerCoords[] pointerCoords = {new MotionEvent.PointerCoords()};
    private static final MotionEvent.PointerProperties[] pointerProperties = {new MotionEvent
            .PointerProperties()};

    // 点击事件
    public static MotionEvent createClickEvent(int type, int x, int y) {
        long now = SystemClock.uptimeMillis();
        int action;
        if (type == 1) {
            lastMouseDown = now;
            lastX = x;
            lastY = y;
            action = MotionEvent.ACTION_DOWN;
        }
        else {
            action = MotionEvent.ACTION_UP;
        }
        MotionEvent.PointerCoords[] pointerCoords = {new MotionEvent.PointerCoords()};
        MotionEvent.PointerCoords coords = pointerCoords[0];
        coords.x = 2 * x;
        coords.y = 2 * y;
        MotionEvent.PointerProperties[] pointerProperties = {new MotionEvent.PointerProperties()};
        MotionEvent.PointerProperties props = pointerProperties[0];
        props.id = 0;
        props.toolType = MotionEvent.TOOL_TYPE_FINGER;

        coords = pointerCoords[0];
        coords.orientation = 0;
        coords.pressure = 1;
        coords.size = 1;

        return MotionEvent.obtain(
                lastMouseDown, now, action,
                1, pointerProperties, pointerCoords,
                0, 1,
                1f, 1f,
                0, 0,
                InputDevice.SOURCE_TOUCHSCREEN, 0);
    }

    // 滑动事件
    public static MotionEvent creatMoveEvent(int x, int y) {
        long now = SystemClock.uptimeMillis();
        int action = MotionEvent.ACTION_MOVE;
        double tan = (double)(x - lastX) / (double)(y - lastY);
        float orient = (float)Math.atan(tan);
        lastX = x;
        lastY = y;

        MotionEvent.PointerCoords[] pointerCoords = {new MotionEvent.PointerCoords()};
        MotionEvent.PointerCoords coords = pointerCoords[0];
        coords.x = 2 * x;
        coords.y = 2 * y;
        coords.orientation = orient;
        coords.pressure = 1;
        coords.size = 1;

        MotionEvent.PointerProperties[] pointerProperties = {new MotionEvent.PointerProperties()};
        MotionEvent.PointerProperties props = pointerProperties[0];
        props.id = 0;
        props.toolType = MotionEvent.TOOL_TYPE_FINGER;

        return MotionEvent.obtain(
                lastMouseDown, now, action,
                1, pointerProperties, pointerCoords,
                0, 1,
                1f, 1f,
                0, 0,
                InputDevice.SOURCE_TOUCHSCREEN, 0);
    }

    // 滚动事件
    public static MotionEvent createScrollEvent(int x, int y, int hScroll, int vScroll) {
        long now = SystemClock.uptimeMillis();

        MotionEvent.PointerCoords[] pointerCoords = {new MotionEvent.PointerCoords()};
        MotionEvent.PointerCoords coords = pointerCoords[0];
        coords.x = 2 * x;
        coords.y = 2 * y;
        coords.orientation = 0;
        coords.pressure = 1;
        coords.size = 1;
        coords.setAxisValue(MotionEvent.AXIS_HSCROLL, hScroll);
        coords.setAxisValue(MotionEvent.AXIS_VSCROLL, vScroll);

        MotionEvent.PointerProperties[] pointerProperties = {new MotionEvent.PointerProperties()};
        MotionEvent.PointerProperties props = pointerProperties[0];
        props.id = 0;
        props.toolType = MotionEvent.TOOL_TYPE_FINGER;

        return MotionEvent.obtain(lastMouseDown, now,
                MotionEvent.ACTION_SCROLL,
                1, pointerProperties, pointerCoords,
                0, 0,
                1f, 1f,
                0, 0,
                InputDevice.SOURCE_MOUSE, 0);
    }
}
