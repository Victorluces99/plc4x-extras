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

import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.Utilities;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;
import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import java.io.File;
import org.apache.plc4x.malbec.projecttype.panelcategory.HMIPanelDataBaseFactory;
import org.apache.plc4x.malbec.projecttype.panelcategory.panel.CommConfigData;
import org.apache.plc4x.malbec.projecttype.panelcategory.panel.CreateCommTopComponent;

public class HMICategoryCreateCommAction extends AbstractAction implements LookupListener {

    private final Project project;
    private final Lookup.Result<CommConfigData> lookupResult;

    public HMICategoryCreateCommAction(Project project) {
        super("Crear Comunicación");
        this.project = project;

        this.lookupResult = Utilities.actionsGlobalContext().lookupResult(CommConfigData.class);
        this.lookupResult.addLookupListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        createComm(project);
    }

    private void createComm(Project proj) {
        if (proj == null) {
            return;
        }

        FileObject projectDir = proj.getProjectDirectory();
        if (projectDir == null) {
            return;
        }
        FileObject merlotFile = projectDir.getFileObject("comm.merlot");

        File targetFile = (merlotFile != null) ? FileUtil.toFile(merlotFile) : FileUtil.toFile(projectDir);

        if (targetFile != null) {
            String dbFolderPath = targetFile.isDirectory() ? targetFile.getAbsolutePath() : targetFile.getParent();

            HMIPanelDataBaseFactory.createDB(dbFolderPath);

            TopComponent tc = WindowManager.getDefault().findTopComponent("DeviceManagerTopComponent");

            if (tc instanceof CreateCommTopComponent) {
                CreateCommTopComponent deviceWindow = (CreateCommTopComponent) tc;

                deviceWindow.setProjectPath(dbFolderPath);

                deviceWindow.setOnSaveListener(config -> {
                    System.out.println("Guardando datos en la base de datos de: " + dbFolderPath);
                    HMIPanelDataBaseFactory.insertDeviceConfig(dbFolderPath, config);
                });

                deviceWindow.open();
                deviceWindow.requestActive();
            }
        }
    }

    @Override
    public void resultChanged(LookupEvent ev) {
        for (CommConfigData config : lookupResult.allInstances()) {
            System.out.println("--- DATOS RECIBIDOS VÍA LOOKUP ---");
            System.out.println("Dispositivo activo: " + config.getDeviceName());
            System.out.println("Total Grupos: " + config.getGroups().size());
            System.out.println("Total Items: " + config.getItems().size());
            System.out.println("Total PVs: " + config.getPvs().size());
        }
    }
}
