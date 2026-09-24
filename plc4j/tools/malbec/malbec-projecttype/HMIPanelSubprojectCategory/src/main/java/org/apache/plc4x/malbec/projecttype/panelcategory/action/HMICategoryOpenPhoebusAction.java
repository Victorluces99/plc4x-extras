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
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.swing.AbstractAction;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.RequestProcessor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;
import org.apache.plc4x.malbec.projecttype.configphoebus.PhoebusOptionsPanelController;

public class HMICategoryOpenPhoebusAction extends AbstractAction {

    private final FileObject bobFile;
    private static final String MEMENTO_TEMPLATE
            = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
            + "<memento default_application=\"Display Editor\" last_opened_file=\"\" show_menu=\"false\" show_statusbar=\"false\" show_tabs=\"false\" show_toolbar=\"false\">\n"
            + "    <DockStage_MAIN height=\"1000.0\" maximized=\"true\" width=\"1296.0\" x=\"-8.0\" y=\"-8.0\">\n"
            + "        <pane selected=\"0\">\n"
            + "            <DockItem_%ID% application=\"display_editor\" user_name=\"%USER%\" input_uri=\"%URI%\"/>\n"
            + "        </pane>\n"
            + "    </DockStage_MAIN>\n"
            + "</memento>\n";

    public HMICategoryOpenPhoebusAction(FileObject bobFile) {
        this.bobFile = bobFile;
        putValue(NAME, "Abrir en Phoebus");
    }

    public void actionPerformed(ActionEvent e) {
        RequestProcessor.getDefault().post(() -> OpenNativeBob(this.bobFile));
        System.out.println("Acción abrir ejecutada sobre: " + bobFile.getNameExt());
    }

    private void OpenNativeBob(FileObject nuevoArchivoBob) {
        File file = FileUtil.toFile(nuevoArchivoBob);
        if (file == null) {
            return;
        }

        java.util.prefs.Preferences prefs = org.openide.util.NbPreferences.forModule(PhoebusOptionsPanelController.class);
        String rutaPhoebus = prefs.get("phoebus.path", "").trim();

        if (rutaPhoebus.isEmpty()) {
            org.openide.DialogDisplayer.getDefault().notify(
                    new org.openide.NotifyDescriptor.Message("Por favor, configure la ruta de Phoebus en Tools -> Options.",
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
                                "Error de configuración: En Windows, la herramienta externa de Phoebus debe apuntar a un archivo ejecutable '.bat'.\nPor favor, corríjalo en Tools -> Options.",
                                org.openide.NotifyDescriptor.ERROR_MESSAGE)
                );
                return;
            }
        } else {
            if (!rutaPhoebus.toLowerCase().endsWith(".sh")) {
                org.openide.DialogDisplayer.getDefault().notify(
                        new org.openide.NotifyDescriptor.Message(
                                "Error de configuración: En sistemas Unix/Linux, la herramienta externa de Phoebus debe apuntar a un script '.sh'.\nPor favor, corríjalo en Tools -> Options.",
                                org.openide.NotifyDescriptor.ERROR_MESSAGE)
                );
                return;
            }
        }

        if (!ejecutablePhoebus.exists()) {
            org.openide.DialogDisplayer.getDefault().notify(
                    new org.openide.NotifyDescriptor.Message(
                            "El archivo configurado para Phoebus no existe en la ruta especificada:\n" + rutaPhoebus,
                            org.openide.NotifyDescriptor.ERROR_MESSAGE)
            );
            return;
        }

        try {
            OverwriteMemento(file);
            ProcessBuilder pb = new ProcessBuilder(
                    rutaPhoebus,
                    "-nosplash",
                    "-edit",
                    ""
            );

            pb.redirectErrorStream(true);
            pb.start();

        } catch (Exception ex) {
            org.openide.DialogDisplayer.getDefault().notify(
                    new org.openide.NotifyDescriptor.Message("Error al intentar ejecutar Phoebus: " + ex.getMessage(),
                            org.openide.NotifyDescriptor.ERROR_MESSAGE)
            );
            Exceptions.printStackTrace(ex);
        }
    }

    private void OverwriteMemento(File file) throws IOException {

        String userHome = System.getProperty("user.home");
        Path rutaMemento = Paths.get(userHome, ".phoebus", "memento");
        //Construir la URI del archivo (siempre con / y la funcion XmlEscaping)
        String nuevaUri = file.toURI().toString();
        nuevaUri = XmlEscaping(nuevaUri);
        String user = System.getProperty("user.name", "user");
        String uid = "DockItem_" + UUID.randomUUID().toString().replace("-", "_");
        String memento = MEMENTO_TEMPLATE
                .replace("%ID%", uid)
                .replace("%USER%", XmlEscaping(user))
                .replace("%URI%", nuevaUri);
        Files.createDirectories(rutaMemento.getParent());
        Files.write(rutaMemento, memento.getBytes(StandardCharsets.UTF_8));
        System.out.println("[abrirBobNativo] Memento creado en: " + rutaMemento
                + " contenido: " + memento);
    }

    private static String XmlEscaping(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

}
