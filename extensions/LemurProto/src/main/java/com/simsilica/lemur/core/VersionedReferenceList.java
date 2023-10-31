/*
 * $Id$
 *
 * Copyright (c) 2015, Simsilica, LLC
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

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;


/**
 * Watches a composite set of VersionedReferences and presents
 * a combined version.  The versioned references watched need
 * not be of the same type.  This is useful for watching for changes
 * in a whole form of fields, for example.
 *
 * @author Paul Speed
 */
public class VersionedReferenceList<T> extends AbstractList<VersionedReference<T>> {

    private final List<VersionedReference<T>> list = new ArrayList<>();
    private long lastVersion;

    public VersionedReferenceList() {
    }

    public static <T> VersionedReferenceList<T> create(List<VersionedObject<T>> objects) {
        VersionedReferenceList<T> result = new VersionedReferenceList<>();
        result.addReferences(objects);
        return result;
    }

    public void addReferences(List<VersionedObject<T>> objects) {
        for (VersionedObject<T> vo : objects) {
            add(vo.createReference());
        }
    }

    public void addReference(VersionedObject<T> o) {
        add(o.createReference());
    }

    public boolean removeReference(VersionedObject<T> o) {
        for (ListIterator<VersionedReference<T>> it = listIterator(); it.hasNext(); ) {
            VersionedReference<T> ref = it.next();
            if (ref.get() == o) {
                it.remove();
                return true;
            }
        }
        return false;
    }

    @Override
    public VersionedReference<T> get(int index) {
        return list.get(index);
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public VersionedReference<T> set(int index, VersionedReference<T> element) {
        return list.set(index, element);
    }

    @Override
    public void add(int index, VersionedReference<T> element) {
        list.add(index, element);
    }

    @Override
    public VersionedReference<T> remove(int index) {
        return list.remove(index);
    }

    public long getLastVersion() {
        return lastVersion;
    }

    public long getObjectVersion() {
        long v = 0;
        for (VersionedReference<T> ref : this) {
            v += ref.getObjectVersion();
        }
        return v;
    }

    public boolean needsUpdate() {
        return lastVersion != getObjectVersion();
    }

    public boolean update() {
        long version = getObjectVersion();
        if (version == lastVersion) {
            return false;
        }
        lastVersion = getObjectVersion();
        return true;
    }
}
