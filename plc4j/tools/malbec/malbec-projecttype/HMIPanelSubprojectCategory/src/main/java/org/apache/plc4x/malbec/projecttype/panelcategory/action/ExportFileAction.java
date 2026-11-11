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
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.StatusDisplayer;
import org.openide.filesystems.FileChooserBuilder;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import org.openide.util.RequestProcessor;
import org.openide.windows.WindowManager;

public class ExportFileAction extends AbstractAction {

    private final FileObject source;

    public ExportFileAction(FileObject source) {
        super("Export (ZIP)...");
        this.source = source;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (source == null || !source.isData()) {
            showError("The selected file could not be exported.");
            return;
        }

        File destination = chooseDestination();
        if (destination == null) {
            return;
        }

        RequestProcessor.getDefault().post(() -> export(source, destination));
    }

    private File chooseDestination() {
        JFileChooser chooser = new FileChooserBuilder("ExportFileKey")
                .setTitle("Export " + source.getNameExt() + " as ZIP")
                .setApproveText("Export")
                .setFileFilter(new FileNameExtensionFilter("ZIP files (*.zip)", "zip"))
                .setAcceptAllFileFilterUsed(false)
                .createFileChooser();
        chooser.setSelectedFile(new File(source.getName() + ".zip"));

        if (chooser.showSaveDialog(WindowManager.getDefault().getMainWindow())
                != JFileChooser.APPROVE_OPTION) {
            return null;
        }

        File selected = chooser.getSelectedFile();
        if (selected == null) {
            return null;
        }
        if (!selected.getName().toLowerCase(Locale.ROOT).endsWith(".zip")) {
            selected = new File(selected.getAbsolutePath() + ".zip");
        }
        if (selected.exists()) {
            NotifyDescriptor.Confirmation confirmation = new NotifyDescriptor.Confirmation(
                    "The file '" + selected.getName() + "' already exists. Overwrite it?",
                    "Export (ZIP)", NotifyDescriptor.YES_NO_OPTION);
            if (DialogDisplayer.getDefault().notify(confirmation) != NotifyDescriptor.YES_OPTION) {
                return null;
            }
        }
        return selected;
    }

    private void export(FileObject file, File destination) {
        try {
            writeZip(file, destination);
            StatusDisplayer.getDefault().setStatusText(
                    "File exported: " + destination.getAbsolutePath());
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
            showError("The file could not be exported: " + ex.getMessage());
        }
    }

    private static void writeZip(FileObject source, File destination) throws IOException {
        try (OutputStream output = Files.newOutputStream(destination.toPath());
                ZipOutputStream zip = new ZipOutputStream(output);
                InputStream input = source.getInputStream()) {
            zip.putNextEntry(new ZipEntry(source.getNameExt()));
            input.transferTo(zip);
            zip.closeEntry();
        }
    }

    private static void showError(String message) {
        DialogDisplayer.getDefault().notify(
                new NotifyDescriptor.Message(message, NotifyDescriptor.ERROR_MESSAGE));
    }
}
