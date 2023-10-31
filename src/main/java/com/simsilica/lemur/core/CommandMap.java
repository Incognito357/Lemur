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

package com.simsilica.lemur.core;

import com.simsilica.lemur.Command;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A general mapping of source to some list of Command objects.  This
 * can be useful for things like action maps and so forth, where some
 * action type gets mapped to caller configured commands.
 *
 * @author Paul Speed
 */
public class CommandMap<S, K> extends HashMap<K, List<Command<S>>> {
    private final transient S source;

    public CommandMap(S source) {
        this.source = source;
    }

    public void runCommands(K key) {
        List<Command<S>> list = computeIfAbsent(key, k -> new CopyOnWriteArrayList<>());
        if (list == null)
            return;
        for (Command<S> c : list) {
            c.execute(source);
        }
    }

    public void addCommand(K key, Command<S> command) {
        computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(command);
    }

    public void addCommands(K key, Collection<Command<S>> commands) {
        if (commands == null) {
            computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).clear();
            return;
        }
        computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).addAll(commands);
    }
}
