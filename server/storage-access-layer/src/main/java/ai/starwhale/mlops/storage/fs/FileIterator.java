/*
 * Copyright 2022 Starwhale, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.starwhale.mlops.storage.fs;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class FileIterator implements Iterator<String> {
    private final List<List<File>> stack = new ArrayList<>();
    private final File rootDir;
    private String next;

    public FileIterator(File rootDir, String prefix) {
        if (!rootDir.isDirectory()) {
            throw new IllegalArgumentException(rootDir + " not found or is not a directory");
        }
        this.rootDir = rootDir;
        var root = rootDir;
        var path = prefix.split("/", -1);
        if (path.length > 1) {
            for (int i = 0; i < path.length - 1; ++i) {
                root = new File(root, path[i]);
                if (!root.isDirectory()) {
                    return;
                }
            }
            prefix = path[path.length - 1];
        }
        var candidates = new ArrayList<File>();
        for (var fn : Objects.requireNonNull(root.list())) {
            if (fn.startsWith(prefix)) {
                candidates.add(new File(root, fn));
            }
        }
        if (candidates.isEmpty()) {
            return;
        }
        candidates.sort(FileIterator::compareFilePaths);
        this.stack.add(candidates);
        this.findNext();
    }

    @Override
    public boolean hasNext() {
        return this.next != null;
    }

    @Override
    public String next() {
        if (this.next == null) {
            return null;
        }
        var ret = this.next;
        this.next = null;
        this.findNext();
        return ret;
    }

    private void findNext() {
        while (!this.stack.isEmpty()) {
            var last = this.stack.get(this.stack.size() - 1);
            if (last.isEmpty()) {
                this.stack.remove(this.stack.size() - 1);
                continue;
            }
            var next = last.remove(last.size() - 1);
            if (!next.isDirectory()) {
                var names = new ArrayList<String>();
                for (var path : this.rootDir.toPath().relativize(next.toPath())) {
                    names.add(path.toString());
                }
                this.next = String.join("/", names);
                return;
            }
            var candidates = new ArrayList<>(Arrays.asList(Objects.requireNonNull(next.listFiles())));
            candidates.sort(FileIterator::compareFilePaths);
            this.stack.add(candidates);
        }
    }

    private static int compareFilePaths(File a, File b) {
        var x = a.getPath();
        var y = b.getPath();
        if (a.isDirectory()) {
            x += "/";
        }
        if (b.isDirectory()) {
            y += "/";
        }
        return y.compareTo(x);
    }
}
