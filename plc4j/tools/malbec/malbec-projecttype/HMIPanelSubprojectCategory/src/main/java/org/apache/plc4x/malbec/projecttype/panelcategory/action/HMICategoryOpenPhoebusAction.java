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
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.swing.AbstractAction;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.RequestProcessor;
import org.apache.plc4x.malbec.projecttype.configphoebus.PhoebusOptionsPanelController;

public class HMICategoryOpenPhoebusAction extends AbstractAction {


    private final FileObject bobFile;

    public HMICategoryOpenPhoebusAction(FileObject bobFile) {
        this.bobFile = bobFile;
        putValue(NAME, "Open in Phoebus");
    }

    public void actionPerformed(ActionEvent e) {
        RequestProcessor.getDefault().post(() -> openNativeBob(this.bobFile));
    }

    private void openNativeBob(FileObject nuevoArchivoBob) {
        File file = FileUtil.toFile(nuevoArchivoBob);
        if (file == null) {
            return;
        }

        java.util.prefs.Preferences prefs = org.openide.util.NbPreferences.forModule(PhoebusOptionsPanelController.class);
        String rutaPhoebus = prefs.get("phoebus.path", "").trim();

        if (rutaPhoebus.isEmpty()) {
            org.openide.DialogDisplayer.getDefault().notify(
                    new org.openide.NotifyDescriptor.Message("Please configure the Phoebus path in Tools -> Options.",
                            org.openide.NotifyDescriptor.INFORMATION_MESSAGE)
            );
            return;
        }

        boolean esWindows = System.getProperty("os.name").toLowerCase().contains("win");
        File ejecutablePhoebus = new File(rutaPhoebus);

        if (esWindows) {
            if (!rutaPhoebus.toLowerCase().endsWith(".bat")) {
                org.openide.DialogDisplayer.getDefault().notify(
                        new org.openide.NotifyDescriptor.Message(
                                "Configuration error: In Windows, the Phoebus external "
                                        + "tool must point to a ‘.bat’ executable file."
                                        + "\nPlease correct this in Tools -> Options.",
                                org.openide.NotifyDescriptor.ERROR_MESSAGE)
                );
                return;
            }
        } else {
            if (!rutaPhoebus.toLowerCase().endsWith(".sh")) {
                org.openide.DialogDisplayer.getDefault().notify(
                        new org.openide.NotifyDescriptor.Message(
                                "Configuration error: On Unix/Linux systems, the "
                                        + "Phoebus external tool must point to a "
                                        + "‘.sh’ script.\nPlease correct this in "
                                        + "Tools -> Options.",
                                org.openide.NotifyDescriptor.ERROR_MESSAGE)
                );
                return;
            }
        }

        if (!ejecutablePhoebus.exists()) {
            org.openide.DialogDisplayer.getDefault().notify(
                    new org.openide.NotifyDescriptor.Message(
                            "The file configured for Phoebus does not exist in the specified path:\n" + rutaPhoebus,
                            org.openide.NotifyDescriptor.ERROR_MESSAGE)
            );
            return;
        }

        try {
            Path memento = PhoebusLauncher.rutaMemento();
            Path respaldo = PhoebusLauncher.backupPath(
                    Paths.get(System.getProperty("user.home", ".")));
            String content = PhoebusLauncher.contentFor(file.toPath(),
                    System.getProperty("user.name", "user"));
            PhoebusLauncher.writeMemento(memento, respaldo, content);

            ProcessBuilder pb = PhoebusLauncher.command(rutaPhoebus);
            Process proceso = pb.start();
            PhoebusLauncher.waitAndRestore(proceso, memento, respaldo, content);

        } catch (Exception ex) {
            org.openide.DialogDisplayer.getDefault().notify(
                    new org.openide.NotifyDescriptor.Message("Error while trying to run Phoebus: " + ex.getMessage(),
                            org.openide.NotifyDescriptor.ERROR_MESSAGE)
            );
            Exceptions.printStackTrace(ex);
        }
    }

}
