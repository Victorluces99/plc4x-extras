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
import org.apache.plc4x.malbec.projecttype.panelcategory.comms.xml.HMICommunicationModel;
import org.apache.plc4x.malbec.projecttype.panelcategory.panel.CreateDeviceTopComponent;
import org.apache.plc4x.malbec.projecttype.panelcategory.panel.DeviceConfigData;
import org.netbeans.api.project.Project;
import org.openide.windows.WindowManager;


public class HMICategoryCreateDeviceAction extends AbstractAction {
    private final Project project;

    public HMICategoryCreateDeviceAction(Project project) {
        putValue(NAME, "Crear Dispositivo");
        this.project = project;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        // Obtenemos o creamos la instancia del TopComponent
        CreateDeviceTopComponent tc = (CreateDeviceTopComponent) WindowManager.getDefault()
                .findTopComponent("CommunicationsTopComponent");

        if (tc != null) {
            tc.setProject(project); // Asociamos el proyecto actual
            tc.resetForm();          // Limpiamos la información previa
            tc.open();              // Abre la pestaña en el editor
            tc.requestActive();     // Le da el foco activo
        }
    }
    
    public static void createDeviceFileInProject(Project project, DeviceConfigData data) {
        if (project == null || data == null) {
            return;
        }
        HMICommunicationModel model = project.getLookup().lookup(HMICommunicationModel.class);
        if (model == null) {
            System.err.println("No se encontró HMICommunicationModel en el Lookup del proyecto");
            return;
        }
        model.upsertDevice(data);
        model.save();
        System.out.println("Dispositivo guardado en comunicacion.xml: " + data.getDeviceName());
    }
}
