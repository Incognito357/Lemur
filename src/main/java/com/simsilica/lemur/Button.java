/*
 * $Id$
 *
 * Copyright (c) 2012-2012 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.simsilica.lemur;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Spatial;

import com.simsilica.lemur.core.CommandMap;
import com.simsilica.lemur.core.GuiControl;
import com.simsilica.lemur.component.QuadBackgroundComponent;
import com.simsilica.lemur.focus.FocusChangeEvent;
import com.simsilica.lemur.focus.FocusChangeListener;
import com.simsilica.lemur.focus.FocusNavigationFunctions;
import com.simsilica.lemur.input.FunctionId;
import com.simsilica.lemur.input.InputState;
import com.simsilica.lemur.input.StateFunctionListener;
import com.simsilica.lemur.style.StyleDefaults;
import com.simsilica.lemur.style.Attributes;
import com.simsilica.lemur.style.ElementId;
import com.simsilica.lemur.style.StyleAttribute;
import com.simsilica.lemur.style.Styles;
import com.simsilica.lemur.event.DefaultMouseListener;
import com.simsilica.lemur.event.FocusMouseListener;
import com.simsilica.lemur.event.MouseEventControl;


/**
 * A standard Button GUI element that can be clicked to
 * perform an action or set of actions.
 *
 * @author Paul Speed
 */
public class Button extends Label {

    public static final String ELEMENT_ID = "button";

    public static final String EFFECT_PRESS = "press";
    public static final String EFFECT_RELEASE = "release";
    public static final String EFFECT_CLICK = "click";
    public static final String EFFECT_ACTIVATE = "activate";
    public static final String EFFECT_DEACTIVATE = "deactivate";
    public static final String EFFECT_FOCUS = "focus";
    public static final String EFFECT_UNFOCUS = "unfocus";
    public static final String EFFECT_ENABLE = "enable";
    public static final String EFFECT_DISABLE = "disable";

    private boolean enabled = true;
    private ColorRGBA color;
    private ColorRGBA shadowColor;
    private ColorRGBA highlightColor;
    private ColorRGBA highlightShadowColor;
    private ColorRGBA focusColor;
    private ColorRGBA focusShadowColor;
    private boolean highlightOn;
    private boolean focusOn;
    private boolean pressed;
    private final CommandMap<Button, ButtonAction> commandMap = new CommandMap<>(this);

    public enum ButtonAction {
        Down,
        Up,
        Click,
        HighlightOn,
        HighlightOff,
        FocusGained,
        FocusLost,
        Hover,
        Enabled,
        Disabled
    }

    public Button(String s) {
        this(s, true, new ElementId(ELEMENT_ID), null);
    }

    public Button(String s, String style) {
        this(s, true, new ElementId(ELEMENT_ID), style);
    }

    public Button(String s, ElementId elementId) {
        this(s, true, elementId, null);
    }

    public Button(String s, ElementId elementId, String style) {
        this(s, true, elementId, style);
    }

    protected Button(String s, boolean applyStyles, ElementId elementId, String style) {
        super(s, false, elementId, style);

        addControl(new MouseEventControl(FocusMouseListener.INSTANCE, new ButtonMouseHandler()));
        getControl(GuiControl.class).addFocusChangeListener(new FocusObserver());
        getControl(GuiControl.class).setFocusable(true);

        Styles styles = GuiGlobals.getInstance().getStyles();
        if (applyStyles) {
            styles.applyStyles(this, elementId, style);
        }
    }

    @StyleDefaults(ELEMENT_ID)
    public static void initializeDefaultStyles(Attributes attrs) {
        GuiGlobals globals = GuiGlobals.getInstance();
        attrs.set("background", new QuadBackgroundComponent(new ColorRGBA(0, 0, 0, 0)), false);
        attrs.set("highlightColor", ColorRGBA.Yellow, false);  // yellow should not need srgb conversion
        attrs.set("focusColor", ColorRGBA.Green, false);       // green should not need srgb conversion
        attrs.set("shadowColor", globals.srgbaColor(new ColorRGBA(0, 0, 0, 0.5f)), false);
    }

    private static ColorRGBA mix(ColorRGBA c1, ColorRGBA c2) {
        if (c1 == null && c2 == null) {
            return null;
        }
        if (c1 == null) {
            return c2;
        }
        if (c2 == null) {
            return c1;
        }
        return c1.clone().interpolateLocal(c2, 0.5f);
    }

    public void addCommand(ButtonAction a, Command<Button> command) {
        commandMap.addCommand(a, command);
    }

    public void addCommands(ButtonAction a, List<Command<Button>> commands) {
        commandMap.addCommands(a, commands);
    }

    public List<Command<Button>> getCommands(ButtonAction a) {
        return commandMap.get(a);
    }

    public void addClickCommand(Command<Button> command) {
        commandMap.addCommand(ButtonAction.Click, command);
    }

    public void addClickCommands(List<Command<Button>> commands) {
        commandMap.addCommands(ButtonAction.Click, commands);
    }

    public void removeClickCommand(Command<Button> command) {
        getClickCommands().remove(command);
    }

    public void removeClickCommands(List<Command<Button>> commands) {
        getClickCommands().removeAll(commands);
    }

    public List<Command<Button>> getClickCommands() {
        return commandMap.get(ButtonAction.Click);
    }

    @StyleAttribute("buttonCommands")
    public void setButtonCommands(Map<ButtonAction, List<Command<Button>>> map) {
        commandMap.clear();
        // We don't use putAll() because (right now) it would potentially
        // put the wrong list implementations into the command map.
        for (Map.Entry<ButtonAction, List<Command<Button>>> e : map.entrySet()) {
            commandMap.addCommands(e.getKey(), e.getValue());
        }
    }

    @Override
    public ColorRGBA getColor() {
        return color;
    }

    @StyleAttribute("color")
    @Override
    public void setColor(ColorRGBA color) {
        this.color = color;
        super.setColor(color);
    }

    @Override
    public ColorRGBA getShadowColor() {
        return shadowColor;
    }

    @StyleAttribute(value = "shadowColor", lookupDefault = false)
    @Override
    public void setShadowColor(ColorRGBA color) {
        this.shadowColor = color;
        super.setShadowColor(shadowColor);
    }

    public ColorRGBA getHighlightColor() {
        return highlightColor;
    }

    @StyleAttribute(value = "highlightColor", lookupDefault = false)
    public void setHighlightColor(ColorRGBA color) {
        this.highlightColor = color;
        if (isHighlightOn()) {
            resetColors();
        }
    }

    public ColorRGBA getHighlightShadowColor() {
        return highlightShadowColor;
    }

    @StyleAttribute(value = "highlightShadowColor", lookupDefault = false)
    public void setHighlightShadowColor(ColorRGBA color) {
        this.highlightShadowColor = color;
        if (isHighlightOn()) {
            resetColors();
        }
    }

    public ColorRGBA getFocusColor() {
        return focusColor;
    }

    @StyleAttribute(value = "focusColor", lookupDefault = false)
    public void setFocusColor(ColorRGBA color) {
        this.focusColor = color;
        if (isFocusHighlightOn()) {
            resetColors();
        }
    }

    public ColorRGBA getFocusShadowColor() {
        return focusShadowColor;
    }

    @StyleAttribute(value = "focusShadowColor", lookupDefault = false)
    public void setFocusShadowColor(ColorRGBA color) {
        this.focusShadowColor = color;
        if (isFocusHighlightOn()) {
            resetColors();
        }
    }

    /**
     * Can be called by application code to simulate a click on a button.
     * Note: this will run the click effects/actions but not the press/release
     * actions.
     */
    public void click() {
        runClick();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean b) {
        if (this.enabled == b)
            return;
        this.enabled = b;

        if (isEnabled()) {
            commandMap.runCommands(ButtonAction.Enabled);
            runEffect(EFFECT_ENABLE);
        } else {
            commandMap.runCommands(ButtonAction.Disabled);
            runEffect(EFFECT_DISABLE);
        }
    }

    public boolean isPressed() {
        return pressed;
    }

    protected void setPressed(boolean f) {
        if (pressed == f) {
            return;
        }
        this.pressed = f;
        if (pressed) {
            commandMap.runCommands(ButtonAction.Down);
            runEffect(EFFECT_PRESS);
        } else {
            commandMap.runCommands(ButtonAction.Up);
            runEffect(EFFECT_RELEASE);
        }
    }

    public boolean isHighlightOn() {
        return highlightOn;
    }

    public boolean isFocusHighlightOn() {
        return focusOn;
    }

    public boolean isFocused() {
        return getControl(GuiControl.class).isFocused();
    }

    protected void showHighlight(boolean f) {
        highlightOn = f;
        resetColors();
    }

    protected void showFocus(boolean f) {
        focusOn = f;
        resetColors();
    }

    protected void resetColors() {
        if (focusOn && highlightOn) {
            // Mix them
            ColorRGBA col = mix(getHighlightColor(), getFocusColor());
            if (col != null) {
                super.setColor(col);
            }
            ColorRGBA shadow = mix(getHighlightShadowColor(), getFocusShadowColor());
            if (shadow != null) {
                super.setShadowColor(shadow);
            }
        } else if (highlightOn) {
            if (getHighlightColor() != null) {
                super.setColor(getHighlightColor());
            }
            if (getHighlightShadowColor() != null) {
                super.setShadowColor(getHighlightShadowColor());
            }
        } else if (focusOn) {
            if (getFocusColor() != null) {
                super.setColor(getFocusColor());
            }
            if (getFocusShadowColor() != null) {
                super.setShadowColor(getFocusShadowColor());
            }
        } else {
            // Just the plain color
            super.setColor(getColor());
            super.setShadowColor(getShadowColor());
        }
    }

    protected void runClick() {
        if (!isEnabled())
            return;
        commandMap.runCommands(ButtonAction.Click);
        runEffect(EFFECT_CLICK);
    }

    @Override
    public String toString() {
        return getClass().getName() + "[text=" + getText() + ", color=" + getColor() + ", elementId=" + getElementId() + "]";
    }

    protected class FocusObserver implements FocusChangeListener, StateFunctionListener {

        public void focusGained(FocusChangeEvent event) {
            if (!isEnabled()) {
                return;
            }
            showFocus(true);
            commandMap.runCommands(ButtonAction.FocusGained);
            runEffect(EFFECT_FOCUS);

            GuiGlobals.getInstance().getInputMapper().addStateListener(this, FocusNavigationFunctions.F_ACTIVATE);
        }

        public void focusLost(FocusChangeEvent event) {
            if (!isFocusHighlightOn()) {
                // No reason to run the 'off' effects if we were never on.
                return;
            }
            GuiGlobals.getInstance().getInputMapper().removeStateListener(this, FocusNavigationFunctions.F_ACTIVATE);

            // If the button is pressed then unpress it
            if (isPressed()) {
                setPressed(false);
            }

            showFocus(false);
            commandMap.runCommands(ButtonAction.FocusLost);
            runEffect(EFFECT_UNFOCUS);
        }

        public void valueChanged(FunctionId func, InputState value, double tpf) {
            if (pressed && value == InputState.Off) {
                // Do click processing... the mouse does click processing before
                // up processing so we will too
                runClick();
            }
            // Only mapped to one function so no need to distinguish
            setPressed(isEnabled() && value == InputState.Positive);
        }
    }

    protected class ButtonMouseHandler extends DefaultMouseListener {
        @Override
        public void mouseButtonEvent(MouseButtonEvent event, Spatial target, Spatial capture) {

            // Buttons always consume their click events
            event.setConsumed();

            // Do our own better handling of 'click' now
            if (!isEnabled())
                return;

            if (event.isPressed()) {
                setPressed(event.isPressed());
            } else if (isPressed()) {
                // Only run the up processing if we were already pressed
                // This also handles the case where we weren't enabled before
                // but are now, etc.

                if (target == capture) {
                    // Then we are still over the button and we should run the
                    // click
                    runClick();
                }
                // If we run the up without checking properly then we
                // potentially get up events with no down event.  This messes
                // up listeners that are (correctly) expecting an up for every
                // down and no ups without downs.
                // So, any time the capture is us then we will run, else not
                if (capture == Button.this) {
                    setPressed(false);
                }
            }
        }

        @Override
        public void mouseEntered(MouseMotionEvent event, Spatial target, Spatial capture) {
            if (!isEnabled())
                return;
            if (capture == Button.this || (target == Button.this && capture == null)) {
                showHighlight(true);
                commandMap.runCommands(ButtonAction.HighlightOn);
                runEffect(EFFECT_ACTIVATE);
            }
        }

        @Override
        public void mouseExited(MouseMotionEvent event, Spatial target, Spatial capture) {
            if (!isHighlightOn()) {
                // If the highlight is on then we need to run through
                // the events regardless of enabled state... and if it's 
                // not on then there is no reason to run events. 
            }
            showHighlight(false);
            commandMap.runCommands(ButtonAction.HighlightOff);
            runEffect(EFFECT_DEACTIVATE);
        }

        @Override
        public void mouseMoved(MouseMotionEvent event, Spatial target, Spatial capture) {
            commandMap.runCommands(ButtonAction.Hover);
        }
    }
}
