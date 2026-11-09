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
import java.util.Locale;
import java.util.Set;
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


public class ImportIcon extends AbstractAction{

    private static final String[] ICON_EXTENSIONS = {"png", "jpg", "svg", "bmp", "ico"};
    private static final Set<String> ICON_EXTENSION_SET = Set.of(ICON_EXTENSIONS);
    
    private final Project project;
    
    public ImportIcon(Project project){
        super("Import Icon");
        this.project = project;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        importIcon();
    }
    
    private void importIcon(){
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Icon");
        chooser.setCurrentDirectory(new File(System.getProperty("user.home", ".")));
        chooser.setFileFilter(new FileNameExtensionFilter(
                "Icon image files (PNG, JPG, SVG, BMP, ICO)", ICON_EXTENSIONS));
        chooser.setAcceptAllFileFilterUsed(false);
        if (chooser.showOpenDialog(WindowManager.getDefault().getMainWindow())
                != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File selectedFile = chooser.getSelectedFile();
        if (selectedFile == null) {
            showError("Please select a file in one of the following formats: png, jpg, svg, BMP, ICO");
            return;
        }
        String extension = FileUtil.getExtension(selectedFile.getName()).toLowerCase(Locale.ROOT);
        if (!ICON_EXTENSION_SET.contains(extension)) {
            showError("Please select a file in one of the following formats: png, jpg, svg, BMP, ICO");
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
            showError("The icon could not be imported: " + ex.getMessage());
        }
    }

    private static void showError(String message) {
        DialogDisplayer.getDefault().notify(
                new NotifyDescriptor.Message(message, NotifyDescriptor.ERROR_MESSAGE));
    }
    
}
