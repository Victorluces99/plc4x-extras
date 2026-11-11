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
package org.apache.plc4x.malbec.projecttype.panel.action;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.plc4x.malbec.projecttype.panel.HMIPanelSubprojectFactory;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;

public final class PanelArchiveImporter {

    private static final String PANEL_FILE = HMIPanelSubprojectFactory.PROJECT_FILE;

    private PanelArchiveImporter() {
    }

    public static FileObject importPanel(FileObject hmiDirectory, String panelName, File zipFile)
            throws IOException {
        if (hmiDirectory == null || !hmiDirectory.isFolder()) {
            throw new IOException("The HMI directory could not be found.");
        }
        if (zipFile == null || !zipFile.isFile()) {
            throw new IOException("The selected panel ZIP could not be read.");
        }

        String name = panelName == null ? "" : panelName.trim();
        validatePanelName(hmiDirectory, name);

        ArchiveLayout layout = readLayout(zipFile);
        FileObject panelDirectory = hmiDirectory.createFolder(name);
        boolean completed = false;
        try {
            extract(zipFile, layout.rootPrefix, panelDirectory);
            if (panelDirectory.getFileObject(PANEL_FILE) == null) {
                throw new IOException("The selected ZIP is not a panel: panel.cfg was not found.");
            }
            completed = true;
            return panelDirectory;
        } finally {
            if (!completed) {
                deleteRecursively(panelDirectory);
            }
        }
    }

    public static void validatePanelArchive(File zipFile) throws IOException {
        if (zipFile == null || !zipFile.isFile()) {
            throw new IOException("The selected panel ZIP could not be read.");
        }
        readLayout(zipFile);
    }

    private static ArchiveLayout readLayout(File zipFile) throws IOException {
        List<EntryInfo> entries = readEntries(zipFile);
        String rootPrefix = detectRootPrefix(entries);
        validateEntriesWithinRoot(entries, rootPrefix);
        return new ArchiveLayout(rootPrefix);
    }

    private static List<EntryInfo> readEntries(File zipFile) throws IOException {
        List<EntryInfo> entries = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        try (InputStream input = new BufferedInputStream(Files.newInputStream(zipFile.toPath()));
                ZipInputStream zip = new ZipInputStream(input)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = normalizeEntryName(entry.getName());
                if (name == null) {
                    continue;
                }
                if (!seen.add(name)) {
                    throw new IOException("The ZIP contains duplicate entries: " + name);
                }
                entries.add(new EntryInfo(name, entry.isDirectory()));
            }
        }
        return entries;
    }

    private static String detectRootPrefix(List<EntryInfo> entries) throws IOException {
        for (EntryInfo entry : entries) {
            if (!entry.directory && PANEL_FILE.equals(entry.name)) {
                return "";
            }
        }

        String rootPrefix = null;
        for (EntryInfo entry : entries) {
            if (entry.directory || !entry.name.endsWith("/" + PANEL_FILE)) {
                continue;
            }
            String candidate = entry.name.substring(0, entry.name.length() - PANEL_FILE.length());
            if (candidate.indexOf('/') != candidate.length() - 1) {
                throw new IOException(
                        "panel.cfg must be at the root of the ZIP or inside a single top-level folder.");
            }
            if (rootPrefix != null && !rootPrefix.equals(candidate)) {
                throw new IOException("The ZIP contains more than one panel.");
            }
            rootPrefix = candidate;
        }

        if (rootPrefix == null) {
            throw new IOException("The selected ZIP is not a panel: panel.cfg was not found.");
        }
        return rootPrefix;
    }

    private static void validateEntriesWithinRoot(List<EntryInfo> entries, String rootPrefix)
            throws IOException {
        if (rootPrefix.isEmpty()) {
            return;
        }

        String rootDirectory = rootPrefix.substring(0, rootPrefix.length() - 1);
        for (EntryInfo entry : entries) {
            if (entry.name.equals(rootDirectory) || entry.name.startsWith(rootPrefix)) {
                continue;
            }
            throw new IOException("The ZIP contains files outside the panel folder: " + entry.name);
        }
    }

    private static void extract(File zipFile, String rootPrefix, FileObject panelDirectory)
            throws IOException {
        try (InputStream input = new BufferedInputStream(Files.newInputStream(zipFile.toPath()));
                ZipInputStream zip = new ZipInputStream(input)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = normalizeEntryName(entry.getName());
                if (name == null) {
                    continue;
                }
                String relative = relativeToRoot(name, rootPrefix);
                if (relative == null) {
                    continue;
                }
                if (entry.isDirectory()) {
                    FileUtil.createFolder(panelDirectory, relative);
                } else {
                    FileObject file = FileUtil.createData(panelDirectory, relative);
                    try (OutputStream output = file.getOutputStream()) {
                        zip.transferTo(output);
                    }
                }
            }
        }
    }

    private static String relativeToRoot(String name, String rootPrefix) throws IOException {
        if (rootPrefix.isEmpty()) {
            return name;
        }
        String rootDirectory = rootPrefix.substring(0, rootPrefix.length() - 1);
        if (name.equals(rootDirectory)) {
            return null;
        }
        if (!name.startsWith(rootPrefix)) {
            throw new IOException("The ZIP contains files outside the panel folder: " + name);
        }
        String relative = name.substring(rootPrefix.length());
        return relative.isEmpty() ? null : relative;
    }

    private static String normalizeEntryName(String entryName) throws IOException {
        if (entryName == null) {
            return null;
        }

        String name = entryName.replace('\\', '/');
        if (name.isEmpty() || name.startsWith("/") || name.matches("^[A-Za-z]:.*")) {
            throw new IOException("The ZIP contains an invalid path: " + entryName);
        }

        String[] parts = name.split("/");
        List<String> normalized = new ArrayList<>(parts.length);
        for (String part : parts) {
            if (part.isEmpty() || ".".equals(part)) {
                continue;
            }
            if ("..".equals(part) || part.indexOf(':') >= 0) {
                throw new IOException("The ZIP contains an invalid path: " + entryName);
            }
            normalized.add(part);
        }
        if (normalized.isEmpty()) {
            return null;
        }

        String result = String.join("/", normalized);
        try {
            Path path = Paths.get(result);
            if (path.isAbsolute() || path.getRoot() != null) {
                throw new IOException("The ZIP contains an invalid path: " + entryName);
            }
        } catch (InvalidPathException ex) {
            throw new IOException("The ZIP contains an invalid path: " + entryName, ex);
        }
        return result;
    }

    private static void validatePanelName(FileObject hmiDirectory, String panelName) throws IOException {
        if (panelName.isEmpty()) {
            throw new IOException("The panel name cannot be empty.");
        }
        if (".".equals(panelName) || "..".equals(panelName)) {
            throw new IOException("The panel name is not valid.");
        }
        if (!FileUtil.isValidFileName(panelName)) {
            throw new IOException("The panel name contains invalid characters.");
        }
        if (panelName.endsWith(".") || panelName.endsWith(" ")) {
            throw new IOException("The panel name cannot end with a dot or a space.");
        }
        if (findChild(hmiDirectory, panelName) != null) {
            throw new IOException("A panel named '" + panelName + "' already exists.");
        }
    }

    private static FileObject findChild(FileObject hmiDirectory, String panelName) {
        for (FileObject child : hmiDirectory.getChildren()) {
            if (child.getName().equalsIgnoreCase(panelName)) {
                return child;
            }
        }
        return null;
    }

    private static void deleteRecursively(FileObject folder) {
        try {
            for (FileObject child : folder.getChildren()) {
                if (child.isFolder()) {
                    deleteRecursively(child);
                } else {
                    child.delete();
                }
            }
            folder.delete();
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    private static final class EntryInfo {

        private final String name;
        private final boolean directory;

        EntryInfo(String name, boolean directory) {
            this.name = name;
            this.directory = directory;
        }
    }

    private static final class ArchiveLayout {

        private final String rootPrefix;

        ArchiveLayout(String rootPrefix) {
            this.rootPrefix = rootPrefix;
        }
    }
}
