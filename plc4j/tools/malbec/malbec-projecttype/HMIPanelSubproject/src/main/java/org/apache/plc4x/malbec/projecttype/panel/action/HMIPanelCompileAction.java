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

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import org.netbeans.api.progress.ProgressRunnable;
import org.netbeans.api.progress.ProgressUtils;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.StatusDisplayer;
import org.openide.filesystems.FileChooserBuilder;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;

public class HMIPanelCompileAction extends AbstractAction {

    private final FileObject panel;

    public HMIPanelCompileAction(FileObject panel) {
        super("Export panel (ZIP)");
        this.panel = panel;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        compilePanel(this.panel);
    }

    private void compilePanel(FileObject panel) {
        File folderSource = FileUtil.toFile(panel);
        if (folderSource == null || !folderSource.isDirectory()) {
            StatusDisplayer.getDefault().setStatusText("The panel folder could not be obtained.");
            return;
        }
        File ziptarget = new FileChooserBuilder("ZipOutputKey")
                .setTitle("Save ZIP of the panel as...")
                .setApproveText("Save")
                .setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("ZIP File (*.zip)", "zip"))
                .showSaveDialog();
        if (ziptarget == null) {
            return;
        }
        if (!ziptarget.getName().toLowerCase().endsWith(".zip")) {
            ziptarget = new File(ziptarget.getAbsolutePath() + ".zip");
        }
        File source = ziptarget;
        try {
            AtomicReference<Exception> fail = new AtomicReference<>();
            ProgressRunnable<Void> operation = handle -> {
                try {
                    ZipUtils.zipFolder(folderSource.toPath(), source.toPath());
                } catch (Exception ex) {
                    fail.set(ex);
                }
                return null;
            };
            ProgressUtils.showProgressDialogAndRun(operation, "Compress the panel...", false);
            if (fail.get() != null) {
                throw fail.get();
            }
            StatusDisplayer.getDefault().setStatusText("Compressed panel: " + source.getName());
            JOptionPane.showMessageDialog(null,
                    "The panel was exported successfully:\n" + source.getAbsolutePath(),
                    "Export panel (ZIP)", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
            DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message(
                    "The panel could not be compressed: " + ex.getMessage(),
                    NotifyDescriptor.ERROR_MESSAGE));
        }
    }
}
