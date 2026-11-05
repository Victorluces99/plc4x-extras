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
package org.apache.plc4x.malbec.hmi.action;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.swing.AbstractAction;
import org.apache.plc4x.malbec.projecttype.panel.HMIPanelSubprojectFactory;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;

public class CreateProjectFolders extends AbstractAction {

    private static final String ZIP_RESOURCE = "HMIPanel.zip";
    private static final String ZIP_ROOT = "HMIPanel";

    private final FileObject hmiDirectory;

    public CreateProjectFolders(FileObject hmiDirectory) {
        super("Create Panel");
        this.hmiDirectory = hmiDirectory;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (hmiDirectory == null || !hmiDirectory.isFolder()) {
            showError("The HMI directory could not be found.");
            return;
        }

        String panelName = askForPanelName();
        if (panelName == null) {
            return;
        }

        panelName = panelName.trim();
        String validationError = validatePanelName(panelName);
        if (validationError != null) {
            showError(validationError);
            return;
        }

        try {
            createPanel(hmiDirectory, panelName);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
            showError("The panel folders could not be created: " + ex.getMessage());
        }
    }

    private static String askForPanelName() {
        NotifyDescriptor.InputLine input = new NotifyDescriptor.InputLine(
                "Panel name:", "Create Panel");
        Object result = DialogDisplayer.getDefault().notify(input);
        return result == NotifyDescriptor.OK_OPTION ? input.getInputText() : null;
    }

    private String validatePanelName(String panelName) {
        if (panelName.isEmpty()) {
            return "The panel name cannot be empty.";
        }
        if (".".equals(panelName) || "..".equals(panelName)) {
            return "The panel name is not valid.";
        }
        if (!FileUtil.isValidFileName(panelName)) {
            return "The panel name contains invalid characters.";
        }
        if (panelName.endsWith(".") || panelName.endsWith(" ")) {
            return "The panel name cannot end with a dot or a space.";
        }
        if (findChild(panelName) != null) {
            return "A panel named '" + panelName + "' already exists.";
        }
        return null;
    }

    private FileObject findChild(String panelName) {
        for (FileObject child : hmiDirectory.getChildren()) {
            if (child.getName().equalsIgnoreCase(panelName)) {
                return child;
            }
        }
        return null;
    }

    private static void createPanel(FileObject hmiDirectory, String panelName) throws IOException {
        FileObject panelDirectory = hmiDirectory.createFolder(panelName);
        boolean completed = false;
        try {
            extractPanel(panelDirectory);
            if (panelDirectory.getFileObject(HMIPanelSubprojectFactory.PROJECT_FILE) == null) {
                panelDirectory.createData(HMIPanelSubprojectFactory.PROJECT_FILE);
            }
            completed = true;
        } finally {
            if (!completed) {
                deleteRecursively(panelDirectory);
            }
        }
    }

    private static void extractPanel(FileObject panelDirectory) throws IOException {
        try (InputStream source = CreateProjectFolders.class.getResourceAsStream(ZIP_RESOURCE)) {
            if (source == null) {
                throw new IOException("The panel template was not found: " + ZIP_RESOURCE);
            }
            try (ZipInputStream zip = new ZipInputStream(source)) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    String entryName = relativeEntryName(entry.getName());
                    if (entryName == null) {
                        continue;
                    }
                    if (entry.isDirectory()) {
                        FileUtil.createFolder(panelDirectory, entryName);
                    } else {
                        FileObject file = FileUtil.createData(panelDirectory, entryName);
                        try (OutputStream out = file.getOutputStream()) {
                            zip.transferTo(out);
                        }
                    }
                }
            }
        }
    }

    private static String relativeEntryName(String entryName) throws IOException {
        if (entryName == null) {
            return null;
        }

        String name = entryName.replace('\\', '/');
        while (name.startsWith("/")) {
            name = name.substring(1);
        }

        if (name.equals(ZIP_ROOT) || name.equals(ZIP_ROOT + "/")) {
            return null;
        }
        if (name.startsWith(ZIP_ROOT + "/")) {
            name = name.substring(ZIP_ROOT.length() + 1);
        }
        if (name.isEmpty()) {
            return null;
        }

        try {
            Path path = Paths.get(name).normalize();
            if (path.isAbsolute() || path.startsWith("..")) {
                throw new IOException("Invalid entry in the panel template: " + entryName);
            }
            return path.toString().replace('\\', '/');
        } catch (InvalidPathException ex) {
            throw new IOException("Invalid entry in the panel template: " + entryName, ex);
        }
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

    private static void showError(String message) {
        DialogDisplayer.getDefault().notify(
                new NotifyDescriptor.Message(message, NotifyDescriptor.ERROR_MESSAGE));
    }
}
