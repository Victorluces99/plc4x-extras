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
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.apache.plc4x.malbec.projecttype.panel.action.PanelArchiveImporter;
import org.netbeans.api.progress.BaseProgressUtils;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.StatusDisplayer;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.windows.WindowManager;

public class ImportPanel extends AbstractAction {

    private static final String ZIP_EXTENSION = "zip";

    private final FileObject hmiDirectory;

    public ImportPanel(FileObject hmiDirectory) {
        super("Import Panel");
        this.hmiDirectory = hmiDirectory;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (hmiDirectory == null || !hmiDirectory.isFolder()) {
            showError("The HMI directory could not be found.");
            return;
        }

        File selectedFile = choosePanelZip();
        if (selectedFile == null) {
            return;
        }

        try {
            runWithProgress("Validating panel...", () -> {
                PanelArchiveImporter.validatePanelArchive(selectedFile);
                return null;
            });
        } catch (IOException ex) {
            showError(ex.getMessage());
            return;
        }

        String panelName = askForPanelName(defaultPanelName(selectedFile));
        if (panelName == null) {
            return;
        }

        try {
            FileObject imported = runWithProgress("Importing panel...",
                    () -> PanelArchiveImporter.importPanel(hmiDirectory, panelName, selectedFile));
            StatusDisplayer.getDefault().setStatusText("Panel imported: " + imported.getName());
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
            showError("The panel could not be imported: " + ex.getMessage());
        }
    }

    private static <T> T runWithProgress(String title, ProgressTask<T> task) throws IOException {
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<IOException> failure = new AtomicReference<>();
        BaseProgressUtils.showProgressDialogAndRun(handle -> {
            handle.setDisplayName(title);
            handle.start(400);
            try {
                result.set(task.run());
            } catch (IOException ex) {
                failure.set(ex);
            } finally {
                handle.finish();
            }
            return null;
        }, title, false);

        IOException error = failure.get();
        if (error != null) {
            throw error;
        }
        return result.get();
    }

    @FunctionalInterface
    private interface ProgressTask<T> {

        T run() throws IOException;
    }

    private File choosePanelZip() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Import Panel");
        chooser.setCurrentDirectory(new File(System.getProperty("user.home", ".")));
        chooser.setFileFilter(new FileNameExtensionFilter("Panel ZIP (*.zip)", ZIP_EXTENSION));
        chooser.setAcceptAllFileFilterUsed(false);

        if (chooser.showOpenDialog(WindowManager.getDefault().getMainWindow())
                != JFileChooser.APPROVE_OPTION) {
            return null;
        }

        File selected = chooser.getSelectedFile();
        if (selected == null) {
            return null;
        }
        if (!ZIP_EXTENSION.equalsIgnoreCase(FileUtil.getExtension(selected.getName()))) {
            showError("Please select a .zip file.");
            return null;
        }
        return selected;
    }

    private static String askForPanelName(String defaultName) {
        NotifyDescriptor.InputLine input = new NotifyDescriptor.InputLine(
                "Panel name:", "Import Panel");
        input.setInputText(defaultName);
        Object result = DialogDisplayer.getDefault().notify(input);
        return result == NotifyDescriptor.OK_OPTION ? input.getInputText() : null;
    }

    private static String defaultPanelName(File zipFile) {
        String name = zipFile.getName();
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            name = name.substring(0, dot);
        }
        name = name.trim();
        return name.isEmpty() ? "Panel" : name;
    }

    private static void showError(String message) {
        DialogDisplayer.getDefault().notify(
                new NotifyDescriptor.Message(message, NotifyDescriptor.ERROR_MESSAGE));
    }
}
