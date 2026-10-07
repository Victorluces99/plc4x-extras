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
package org.apache.plc4x.malbec.projecttype.panelcategory.panel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.KeyEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import org.apache.plc4x.malbec.projecttype.panelcategory.comms.xml.HMICommunicationModel;
import org.netbeans.api.project.Project;

public final class CreateDeviceDialog extends JDialog {

    private final transient Project project;
    private final transient CreateDevicePanel panel;
    private final boolean editando;
    private final JButton btnOk = new JButton("Guardar");
    private final JButton btnCancel = new JButton("Cancelar");

    public static CreateDeviceDialog forCreate(Window owner, Project project) {
        return new CreateDeviceDialog(owner, project, null);
    }

    public static CreateDeviceDialog forEdit(Window owner, Project project, DeviceConfigData device) {
        return new CreateDeviceDialog(owner, project, device);
    }

    private CreateDeviceDialog(Window owner, Project project, DeviceConfigData device) {
        super(owner, device == null
                ? "Nuevo dispositivo de comunicación"
                : "Modificar dispositivo", ModalityType.APPLICATION_MODAL);
        this.project = project;
        this.panel = new CreateDevicePanel(project, device);
        this.editando = device != null;

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        botones.add(btnCancel);
        botones.add(btnOk);
        JScrollPane scroll = new JScrollPane(panel);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setMinimumSize(new Dimension(420, 260));

        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.add(scroll, BorderLayout.CENTER);
        content.add(botones, BorderLayout.SOUTH);
        setContentPane(content);


        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().setDefaultButton(btnOk);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        btnOk.addActionListener(e -> guardar());
        btnCancel.addActionListener(e -> dispose());

        setResizable(false);
        pack();
        Rectangle pantalla = getGraphicsConfiguration() != null
                ? getGraphicsConfiguration().getBounds()
                : new Rectangle(0, 0, 1024, 768);
        Dimension pref = getPreferredSize();
        setSize(Math.min(pref.width, pantalla.width - 80),
                Math.min(pref.height, pantalla.height - 80));
        setLocationRelativeTo(owner);
    }

    private void guardar() {
        DeviceConfigData data = panel.getDeviceConfigData();
        if (data == null) {
            return;
        }
        if (project == null) {
            JOptionPane.showMessageDialog(this,
                    "No hay un proyecto asociado.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        HMICommunicationModel model = project.getLookup().lookup(HMICommunicationModel.class);
        if (model == null) {
            JOptionPane.showMessageDialog(this,
                    "No se encontró el modelo de comunicación del proyecto.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        model.upsertDevice(data);
        if (!model.save()) {
            JOptionPane.showMessageDialog(this,
                    "El dispositivo se guardó en comunicacion.xml pero no se pudo\n"
                    + "actualizar la base de datos. Revisá la consola.",
                    "Guardado parcial", JOptionPane.WARNING_MESSAGE);
            return;
        }
        avisarGuardado(data);
        dispose();
    }

    private void avisarGuardado(DeviceConfigData data) {
        String nombre = data.getDeviceName();
        if (nombre == null || nombre.isBlank()) {
            nombre = "(sin nombre)";
        }
        JOptionPane.showMessageDialog(this,
                "El dispositivo '" + nombre + "' fue "
                + (editando ? "modificado" : "creado") + " correctamente.",
                editando ? "Modificación exitosa" : "Dispositivo creado",
                JOptionPane.INFORMATION_MESSAGE);
    }
}
