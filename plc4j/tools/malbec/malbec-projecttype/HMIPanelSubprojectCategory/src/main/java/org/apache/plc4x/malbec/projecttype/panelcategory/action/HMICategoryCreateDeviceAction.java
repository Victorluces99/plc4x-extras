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
import javax.swing.AbstractAction;
import javax.swing.SwingUtilities;
import org.apache.plc4x.malbec.projecttype.panelcategory.panel.CreateDeviceDialog;
import org.netbeans.api.project.Project;
import org.openide.windows.WindowManager;

/**
 * Abre el alta de dispositivo de comunicación como diálogo modal.
 *
 * <p>Antes esto buscaba un TopComponent por su id de string y, si la pestaña no
 * estaba abierta, no pasaba nada sin avisar. Un diálogo nuevo por invocación no
 * tiene ese problema y tampoco deja estado de una alta en la siguiente.
 */
public class HMICategoryCreateDeviceAction extends AbstractAction {

    private final Project project;

    public HMICategoryCreateDeviceAction(Project project) {
        putValue(NAME, "Crear Dispositivo");
        this.project = project;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        SwingUtilities.invokeLater(() -> new CreateDeviceDialog(
                WindowManager.getDefault().getMainWindow(), project).setVisible(true));
    }
}