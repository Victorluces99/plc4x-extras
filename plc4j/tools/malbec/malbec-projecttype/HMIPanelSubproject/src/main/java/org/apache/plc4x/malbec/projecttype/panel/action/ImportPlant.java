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

import java.awt.FileDialog;
import java.awt.event.ActionEvent;
import java.io.File;
import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import org.apache.plc4x.malbec.projecttype.panel.plant.ImportFormats;
import org.apache.plc4x.malbec.projecttype.panel.plant.ImportResult;
import org.apache.plc4x.malbec.projecttype.panel.plant.ImportResult.Success;
import org.apache.plc4x.malbec.projecttype.panel.plant.ImportResult.fail;
import org.apache.plc4x.malbec.projecttype.panel.plant.PlantImporter;
import org.netbeans.api.progress.ProgressUtils;
import org.openide.filesystems.FileObject;
import org.openide.windows.WindowManager;

public class ImportPlant extends AbstractAction {

    private static final String title = "Importing a Plant Model...";

    private final FileObject panel;

    public ImportPlant(FileObject panel) {
        super("Import plant");
        this.panel = panel;
        ImportFormats.warnMisalignedFormats();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        importPlant();
    }

    private void importPlant() {
        FileDialog chooser =
                new FileDialog(WindowManager.getDefault().getMainWindow(),
                        "Select Plant Model", FileDialog.LOAD);
        chooser.setDirectory(System.getProperty("user.home"));
        chooser.setFilenameFilter(ImportFormats.fileFilter());
        chooser.setVisible(true);

        File[] selected = chooser.getFiles();
        if (selected == null || selected.length == 0) {
            return;
        }
        File file = selected[0];
        if (!ImportFormats.extensionApproved(file)) {
            reportFailure(file, "Only files are allowed "
                    + ImportFormats.allowedFormats() + ".");
            return;
        }

        ImportResult result = ProgressUtils.showProgressDialogAndRun(
                handle -> {
                    handle.setDisplayName(title);
                    handle.start(400);
                    try {
                        return PlantImporter.importPlant(file, panel);
                    } finally {
                        handle.finish();
                    }
                },
                title,
                false);
        show(result);
    }

    private void show(ImportResult result) {
        if (result instanceof Success success) {
            reportSuccess(success.file(), success.areaId());
        } else if (result instanceof fail fail) {
            reportFailure(fail.file(), fail.reason());
        }
    }

    private void reportSuccess(File file, String areaId) {
        JOptionPane.showMessageDialog(null,
                "The floor plan was imported '" + areaId + "' from '" + file.getName() + "'.",
                "Import Successful", JOptionPane.INFORMATION_MESSAGE);
    }

    private void reportFailure(File file, String reason) {
        JOptionPane.showMessageDialog(null,
                "It could not be imported '" + file.getName() + "'.\n\n" + reason,
                "Import Error", JOptionPane.ERROR_MESSAGE);
    }
}