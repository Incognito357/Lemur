/*
 * $Id$
 *
 * Copyright (c) 2016, Simsilica, LLC
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.simsilica.lemur.core;

import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.simsilica.lemur.focus.FocusManagerState;
import com.simsilica.lemur.focus.FocusNavigationState;
import com.simsilica.lemur.focus.FocusTarget;
import com.simsilica.lemur.focus.FocusTraversal;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Wraps a standard GuiLayout to provide basic default focus
 * navigation support.  Essentially, things like Left, Right, Up, Down
 * are directly wired to Next, Previous and the GuiLayout's child
 * list is used for ordering.
 *
 * @author Paul Speed
 */
public class FocusTraversalAdapter implements FocusTraversal {

    private GuiLayout layout;
    private boolean focusRoot;

    public FocusTraversalAdapter(GuiLayout layout) {
        this.layout = layout;
    }

    public GuiLayout getLayout() {
        return layout;
    }

    public void setLayout(GuiLayout layout) {
        this.layout = layout;
    }

    @Override
    public Spatial getDefaultFocus() {
        // Check each child in order to see if it is either
        // focusable or has a default focus child
        for (Spatial child : layout.getChildren()) {
            FocusTarget target = FocusManagerState.findFocusTarget(child);
            if (target != null && target.isFocusable()) {
                return child;  // we found it
            }

            // Check to see if it is a container itself
            FocusTraversal ft = FocusNavigationState.getFocusTraversal(child);
            if (ft != null) {
                Spatial defaultFocus = ft.getDefaultFocus();
                if (defaultFocus != null) {
                    return defaultFocus;
                }
            }
        }
        return null;
    }

    @Override
    public Spatial getRelativeFocus(Spatial from, TraversalDirection direction) {
        switch (direction) {
            case Up:
            case Left:
            case Previous:
                return getPrevious(from);
            default:
            case Down:
            case Right:
            case Next:
                return getNext(from);
            case Home:
            case PageHome:
                return getFirst();
            case End:
            case PageEnd:
                return getLast();
        }
    }

    @Override
    public boolean isFocusRoot() {
        return focusRoot;
    }

    public void setFocusRoot(boolean b) {
        this.focusRoot = b;
    }

    protected Spatial getFirst() {
        ChildIterator it = new ChildIterator(TraversalDirection.PageHome);
        if (it.hasNext()) {
            return it.next();
        }
        return null;
    }

    protected Spatial getLast() {
        ChildIterator it = new ChildIterator(TraversalDirection.PageEnd);
        Spatial cur = null;
        while (it.hasNext()) {
            cur = it.next();
        }
        return cur;
    }

    protected Spatial getNext(Spatial from) {
        ChildIterator it = new ChildIterator(from, null, TraversalDirection.PageHome);
        Spatial next = null;
        if (it.hasNext()) {
            next = it.next();
        }
        if (next == null && focusRoot) {
            next = getFirst();
        }
        return next;
    }

    protected Spatial getPrevious(Spatial from) {
        ChildIterator it = new ChildIterator(null, from, TraversalDirection.PageEnd);
        Spatial previous = null;
        while (it.hasNext()) {
            previous = it.next();
        }
        if (previous == null && focusRoot) {
            previous = getLast();
        }
        return previous;
    }

    /**
     * Provides an in-order traversal over all focusable children from
     * this component's perspective, potentially drilling into child
     * focus roots using a relative direction.
     */
    private class ChildIterator implements Iterator<Spatial> {

        private final Iterator<Node> delegate;
        private final Spatial start;
        private final Spatial end;
        private final TraversalDirection entryDir;

        private boolean started;
        private Spatial next;

        public ChildIterator(TraversalDirection entryDir) {
            this(null, null, entryDir);
        }

        public ChildIterator(Spatial start, Spatial end, TraversalDirection entryDir) {
            this.delegate = layout.getChildren().iterator();
            this.start = start;
            this.end = end;
            this.entryDir = entryDir;
            this.started = start == null;
            fetch();
        }

        protected void fetch() {
            this.next = null;

            while (delegate.hasNext()) {
                Spatial child = delegate.next();
                if (start != null && child == start) {
                    started = true;
                    continue;
                }
                if (end != null && child == end) {
                    // No more elements
                    break;
                }
                if (!started) {
                    continue;
                }
                FocusTarget target = FocusManagerState.findFocusTarget(child);
                if (target != null && target.isFocusable()) {
                    this.next = child;
                    break;
                } else {
                    // Check to see if it is a container itself
                    FocusTraversal ft = FocusNavigationState.getFocusTraversal(child);
                    if (ft != null) {
                        Spatial newFocus = ft.getRelativeFocus(null, entryDir);
                        if (newFocus != null) {
                            this.next = newFocus;
                            break;
                        }
                    }
                }
            }
        }

        public boolean hasNext() {
            return next != null;
        }

        public Spatial next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            Spatial result = next;
            fetch();
            return result;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}

