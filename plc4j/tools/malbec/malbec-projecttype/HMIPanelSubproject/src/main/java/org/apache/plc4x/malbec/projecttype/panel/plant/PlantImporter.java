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
package org.apache.plc4x.malbec.projecttype.panel.plant;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.plc4x.malbec.projecttype.panel.plant.ImportResult.Success;
import org.apache.plc4x.malbec.projecttype.panel.plant.ImportResult.fail;
import org.apache.plc4x.malbec.projecttype.panel.plant.PlantSnapshot.AreaNode;
import org.apache.plc4x.malbec.projecttype.panelcategory.ProjectIds;
import org.apache.plc4x.malbec.s88.api.S88Element;
import org.apache.plc4x.malbec.s88.api.S88PlantModel;
import org.apache.plc4x.malbec.s88.api.S88Repository;
import org.apache.plc4x.malbec.s88.api.S88Storage;
import org.apache.plc4x.malbec.s88.plant.services.S88ProjectServices;
import org.netbeans.api.project.ProjectManager;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

public final class PlantImporter {

    private static final Logger LOGGER = Logger.getLogger(PlantImporter.class.getName());

    private PlantImporter() {
    }

    private static final String INVALID = "/\\:*?\"<>|";

    public static ImportResult importPlant(File file, FileObject panel) {
        S88Repository repository;
        try {
            repository = S88ProjectServices.createRepository(
                    extensionOf(file), new Storage(file));
        } catch (RuntimeException ex) {
            LOGGER.log(Level.WARNING, "There is no repository for " + file.getName(), ex);
            return new fail(file, "the format " + extensionOf(file) + " is not supported.");
        }

        S88PlantModel model;
        try {
            model = repository.loadPlant();
        } catch (RuntimeException ex) {
            LOGGER.log(Level.WARNING, "It could not be interpreted " + file.getName(), ex);
            return new fail(file, describes(ex));
        }

        S88Element root = model == null ? null : model.getRoot();
        String areaId = root == null || root.getId() == null ? "" : root.getId().trim();
        if (areaId.isEmpty()) {
            return new fail(file, " The file does not define a plant area.");
        }

        File panelFile = FileUtil.toFile(panel);
        if (panelFile != null) {
            FileUtil.refreshFor(panelFile);
        }

        FileObject imageFo = panel.getFileObject("image");
        String problem = targetProblem(imageFo);
        if (problem != null) {
            LOGGER.log(Level.WARNING, "Failed to import {0}: {1}",
                    new Object[]{file.getName(), problem});
            return new fail(file, problem);
        }

        int failed = 0;

        String invalidName = firstInvalidName(root);
        if (invalidName != null) {
            LOGGER.log(Level.WARNING, "Failed to import {0}: {1}",
                    new Object[]{file.getName(), invalidName});
            return new fail(file, invalidName);
        }

        List<FileObject> created = new ArrayList<>();
        if (root.getChildren() != null) {
            for (S88Element child : root.getChildren()) {
                failed += reverseChildStack(child, imageFo, created);
            }
        }
        if (failed > 0) {
            undo(created);
            return new fail(file, failed + " Model node(s) could not be created;"
                    + " The import is incomplete.");
        }

        try {
            File dir = FileUtil.toFile(panel);
            if (dir == null) {
                undo(created);
                return new fail(file, "It could not be written " + PlantModelXml.FILE_NAME
                        + " in the project.");
            }
            AreaNode snapshot = PlantSnapshot.collectElement(root);
            File target = new File(dir, PlantModelXml.FILE_NAME);
            PlantModelXml.export(snapshot, target);
            FileUtil.refreshFor(target);
            LOGGER.log(Level.INFO, "Snapshot of the plant model at: {0}",
                    target.getAbsolutePath());
        } catch (IOException ex) {
            undo(created);
            LOGGER.log(Level.WARNING, "It could not be written " + PlantModelXml.FILE_NAME, ex);
            return new fail(file, describes(ex));
        }

        if (panelFile != null) {
            FileUtil.refreshAll();
            ProjectManager.getDefault().clearNonProjectCache();
        }
        return new Success(file, areaId);
    }

    public static int reverseChildStack(S88Element root, FileObject baseDir) {
        return reverseChildStack(root, baseDir, new ArrayList<>());
    }

    public static int reverseChildStack(S88Element root, FileObject baseDir,
            List<FileObject> created) {
        if (root == null || baseDir == null) {
            return 0;
        }

        Deque<S88Element> stack = new ArrayDeque<>();
        Map<S88Element, FileObject> dirMap = new HashMap<>();
        int failed = 0;

        stack.push(root);
        dirMap.put(root, baseDir);

        while (!stack.isEmpty()) {
            S88Element node = stack.pop();
            FileObject parentDir = dirMap.remove(node);

            if (parentDir == null) {
                continue;
            }

            try {

                String name = validFolderName(node.getId());
                if (name == null) {
                    failed++;
                    LOGGER.log(Level.WARNING, "Node rejected: {0}",
                            nameProblem(node.getId()));
                    continue;
                }
                FileObject currentDir = createFolder(name, parentDir, created);
                createCfgFile(currentDir, created);

                LOGGER.fine(() -> "Properties in " + node.getId() + ": " + node.getProperties());
                LOGGER.fine(() -> "check: " + node.isCheck());
                if (node.isCheck()) {
                    LOGGER.fine(() -> "Creating BOB for: " + node.getId());
                    createBobFile(name, currentDir, created);
                }

                List<S88Element> children = node.getChildren();
                if (children != null) {
                    for (int i = children.size() - 1; i >= 0; i--) {
                        S88Element child = children.get(i);
                        dirMap.put(child, currentDir);
                        stack.push(child);
                    }
                }
            } catch (IOException ex) {
                failed++;
                LOGGER.log(Level.WARNING, "The node could not be created '" + node.getId() + "'", ex);
            }
        }
        return failed;
    }

    static String validFolderName(String id) {
        return nameProblem(id) == null ? id.trim() : null;
    }

    static String nameProblem(String id) {
        if (id == null || id.isBlank()) {
            return "a model node has no identifier";
        }
        String name = id.trim();
        if (name.isEmpty()) {
            return "a model node has an empty identifier";
        }

        if (name.contains("..")) {
            return "identifier '" + name + "' contains '..' and could create folders outside the panel";
        }
        for (int i = 0; i < INVALID.length(); i++) {
            char c = INVALID.charAt(i);
            if (name.indexOf(c) >= 0) {
                return "identifier '" + name + "' contains character '" + c + "' which cannot be used in a folder name";
            }
        }
        if (Character.isISOControl(name.charAt(0))
                || Character.isISOControl(name.charAt(name.length() - 1))) {
            return "identifier '" + name + "' starts or ends with a control character";
        }
        return null;
    }

    public static String firstInvalidName(S88Element root) {
        Deque<S88Element> pending = new ArrayDeque<>();
        if (root != null) {
            pending.push(root);
        }
        while (!pending.isEmpty()) {
            S88Element node = pending.pop();
            String problem = nameProblem(node.getId());
            if (problem != null) {
                return problem;
            }
            List<S88Element> children = node.getChildren();
            if (children != null) {
                for (int i = children.size() - 1; i >= 0; i--) {
                    S88Element child = children.get(i);
                    if (child != null) {
                        pending.push(child);
                    }
                }
            }
        }
        return null;
    }

    public static String targetProblem(FileObject imageFo) {
        if (imageFo == null) {
            return "the panel does not have the 'image' folder where the structure is created; nothing was created.";
        }
        if (!imageFo.isFolder()) {
            return "'image' exists but is not a folder; nothing was created.";
        }
        return null;
    }

    static String extensionOf(File file) {
        String name = file.getName();
        int point = name.lastIndexOf('.');
        return point < 0 || point == name.length() - 1
                ? "" : name.substring(point + 1).toLowerCase(java.util.Locale.ROOT);
    }

    private static String describes(Throwable ex) {
        String message = ex.getMessage();
        return message == null || message.isBlank() ? ex.getClass().getSimpleName() : message;
    }

    private static FileObject createFolder(String name, FileObject parentDir,
            List<FileObject> created) throws IOException {
        FileObject folder = parentDir.getFileObject(name);
        if (folder != null) {
            return folder;
        }
        FileObject news = parentDir.createFolder(name);
        created.add(news);
        return news;
    }

    private static void createCfgFile(FileObject folder, List<FileObject> created) throws IOException {
        if (folder.getFileObject(ProjectIds.CATEGORY_MARKER_FILE) == null) {
            created.add(folder.createData(ProjectIds.CATEGORY_MARKER_FILE));
        }
    }

    private static void createBobFile(String name, FileObject folder,
            List<FileObject> created) throws IOException {
        if (folder.getFileObject(name + ".bob") == null) {
            created.add(folder.createData(name, "bob"));
        }
    }

    static void undo(List<FileObject> created) {
        for (int i = created.size() - 1; i >= 0; i--) {
            FileObject fo = created.get(i);
            try {
                if (fo.isValid()) {
                    fo.delete();
                }
            } catch (IOException ex) {
                LOGGER.log(Level.WARNING, "It couldn't be undone " + fo.getName(), ex);
            }
        }
        created.clear();
    }

    private record Storage(File file) implements S88Storage {

        @Override
        public InputStream openInput() throws IOException, FileNotFoundException {
            return new FileInputStream(file);
        }

        @Override
        public OutputStream openOutput() throws IOException {
            return new FileOutputStream(file);
        }
    }
}
