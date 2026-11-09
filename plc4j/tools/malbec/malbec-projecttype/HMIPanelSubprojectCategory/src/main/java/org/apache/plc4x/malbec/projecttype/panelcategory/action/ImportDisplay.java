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
package org.apache.plc4x.malbec.projecttype.panelcategory.action;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.netbeans.api.project.Project;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.windows.WindowManager;

public class ImportDisplay extends AbstractAction {

    private static final String BOB_EXTENSION = "bob";

    private final Project project;

    public ImportDisplay(Project project) {
        super("Import Display");
        this.project = project;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        importBob();
    }

    private void importBob() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Display");
        chooser.setCurrentDirectory(new File(System.getProperty("user.home", ".")));
        chooser.setFileFilter(new FileNameExtensionFilter(
                "Phoebus displays (*.bob)", BOB_EXTENSION));
        chooser.setAcceptAllFileFilterUsed(false);

        if (chooser.showOpenDialog(WindowManager.getDefault().getMainWindow())
                != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File selectedFile = chooser.getSelectedFile();
        if (selectedFile == null
                || !BOB_EXTENSION.equalsIgnoreCase(FileUtil.getExtension(selectedFile.getName()))) {
            showError("Please select a .bob file.");
            return;
        }

        FileObject source = FileUtil.toFileObject(selectedFile);
        if (source == null || !source.isData()) {
            showError("The selected file could not be read.");
            return;
        }

        FileObject targetFolder = project.getProjectDirectory();
        if (targetFolder == null || !targetFolder.isFolder()) {
            showError("The destination folder could not be found.");
            return;
        }

        if (targetFolder.getFileObject(source.getName(), source.getExt()) != null) {
            showError("A file named '" + source.getNameExt()
                    + "' already exists in this folder.");
            return;
        }

        try {
            source.copy(targetFolder, source.getName(), source.getExt());
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
            showError("The display could not be imported: " + ex.getMessage());
        }
    }

    private static void showError(String message) {
        DialogDisplayer.getDefault().notify(
                new NotifyDescriptor.Message(message, NotifyDescriptor.ERROR_MESSAGE));
    }

}
