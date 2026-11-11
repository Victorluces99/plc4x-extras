/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.plc4x.malbec.projecttype.panelcategory;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.apache.plc4x.malbec.projecttype.panelcategory.ProjectIds;

final class DeteccionCategory {

    static final List<String> BOOKMARK_FILES = List.of(
            "template.bob", ProjectIds.COMMUNICATION_CONFIG_FILE, "template.oppc",
            "template.record", "template.rcp", "template.rpt", "template.scp",
            "template.wmg", "template.tlc", "template.rtum", "template.icon");

    private DeteccionCategory() {
    }

    static Optional<String> detect(ProjectTree root) {
        if (root == null) {
            return Optional.empty();
        }
        Set<String> names = new HashSet<>();
        iterate(root, names);
        Optional<String> bymarker = BOOKMARK_FILES.stream()
                .filter(marker -> names.contains(normalize(marker)))
                .findFirst();
        if (bymarker.isPresent()) {
            return bymarker;
        }
        return HMICategoryDefinition.byFolderName(root.name())
                .map(HMICategoryDefinition::detectionKey)
                .filter(key -> key != null);
    }

    private static void iterate(ProjectTree root, Set<String> names) {
        Deque<ProjectTree> pendings = new ArrayDeque<>();
        pendings.push(root);
        while (!pendings.isEmpty()) {
            ProjectTree current = pendings.pop();
            if (current.isFolder()) {
                for (ProjectTree child : current.children()) {
                    if (child != null) {
                        pendings.push(child);
                    }
                }
            } else {
                names.add(normalize(current.name()));
            }
        }
    }

    private static String normalize(String name) {
        return name == null ? "" : name.toLowerCase(Locale.ROOT);
    }
}
