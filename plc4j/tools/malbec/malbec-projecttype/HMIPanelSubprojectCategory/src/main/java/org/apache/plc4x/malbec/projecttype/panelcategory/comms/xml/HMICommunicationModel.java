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
package org.apache.plc4x.malbec.projecttype.panelcategory.comms.xml;

import java.io.IOException;
import java.util.List;
import javax.swing.event.ChangeListener;
import org.apache.plc4x.malbec.projecttype.panelcategory.HMIPanelDataBaseFactory;
import org.apache.plc4x.malbec.projecttype.panelcategory.comms.api.CommConfigRepository;
import org.apache.plc4x.malbec.projecttype.panelcategory.comms.api.CommunicationsConfig;
import org.apache.plc4x.malbec.projecttype.panelcategory.panel.CommConfigData;
import org.apache.plc4x.malbec.projecttype.panelcategory.panel.DeviceConfigData;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileChangeAdapter;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileObject;
import org.openide.util.ChangeSupport;

/**
 * NetBeans-aware wrapper for {@link CommunicationsConfig}. Manages the
 * {@code comunicacion.xml} lifecycle on the project directory:
 * reload on external changes, auto-materialization of the template and save.
 */
public final class HMICommunicationModel {

    public static final String CONFIG_FILE = "comunicacion.xml";

    private final Project project;
    private final ChangeSupport cs = new ChangeSupport(this);
    private final FileChangeAdapter fileListener;
    private CommConfigRepository repository;
    private CommunicationsConfig config;
    private FileObject file;

    public HMICommunicationModel(Project project) {
        this.project = project;
        this.fileListener = new FileChangeAdapter() {
            @Override
            public void fileChanged(FileEvent fe) {
                reload();
            }

            @Override
            public void fileDeleted(FileEvent fe) {
                file = null;
                repository = null;
                config = new CommunicationsConfig();
                cs.fireChange();
            }
        };
        reload();
    }

    public final void reload() {
        FileObject projectDir = project.getProjectDirectory();
        boolean commProject = "Comunicacion".equalsIgnoreCase(projectDir.getName())
                || projectDir.getFileObject(CONFIG_FILE) != null
                || projectDir.getFileObject("comm.merlot") != null;
        if (!commProject) {
            file = null;
            repository = null;
            config = new CommunicationsConfig();
            cs.fireChange();
            return;
        }

        boolean created = false;
        file = projectDir.getFileObject(CONFIG_FILE);
        if (file == null) {
            try {
                file = projectDir.createData(CONFIG_FILE);
                created = true;
            } catch (IOException ex) {
                file = null;
            }
        }
        if (file == null) {
            config = new CommunicationsConfig();
            cs.fireChange();
            return;
        }

        file.removeFileChangeListener(fileListener);
        file.addFileChangeListener(fileListener);
        repository = new CommConfigServices().createRepository(new ProjectFileStorage(file));
        try {
            config = repository.load();
        } catch (Throwable t) {
            config = new CommunicationsConfig();
        }
        if (created) {
            try {
                repository.save(config);
            } catch (Exception ignored) {
                // el archivo vacío se materializará en el primer save()
            }
        }
        cs.fireChange();
    }

    public CommunicationsConfig getConfig() {
        return config;
    }

    public List<DeviceConfigData> getDevices() {
        return config.getDevices();
    }

    public DeviceConfigData findByUuid(String uuid) {
        return config.findByUuid(uuid);
    }

    public CommConfigData getComms(String deviceUuid) {
        return config.getComms(deviceUuid);
    }

    public void upsertDevice(DeviceConfigData device) {
        config.upsertDevice(device);
    }

    public void upsertComms(String deviceUuid, CommConfigData commConfig) {
        config.upsertComms(deviceUuid, commConfig);
    }

    public boolean removeDevice(String uuid) {
        return config.removeDevice(uuid);
    }

    /**
     * Guarda la configuración en {@code comunicacion.xml} y la vuelca a la base
     * de datos {@code boot.db} del proyecto.
     *
     * @return true si el XML se guardó y la base de datos quedó actualizada
     */
    public boolean save() {
        try {
            FileObject projectDir = project.getProjectDirectory();
            if (file == null || !file.isValid()) {
                file = projectDir.getFileObject(CONFIG_FILE);
            }
            if (file == null) {
                file = projectDir.createData(CONFIG_FILE);
            }
            CommConfigRepository repo = new CommConfigServices()
                    .createRepository(new ProjectFileStorage(file));
            file.removeFileChangeListener(fileListener);
            try {
                repo.save(config);
            } finally {
                file.addFileChangeListener(fileListener);
            }
            repository = repo;
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo guardar la configuración de comunicaciones", ex);
        }
        boolean databaseUpdated;
        try {
            databaseUpdated = HMIPanelDataBaseFactory.rebuild(project.getProjectDirectory(), config);
        } catch (Exception ex) {
            System.err.println("Error volcando configuracion a boot.db: " + ex.getMessage());
            databaseUpdated = false;
        }
        if (!databaseUpdated) {
            System.err.println("boot.db no pudo actualizarse; revise la salida de la aplicacion.");
        }
        cs.fireChange();
        return databaseUpdated;
    }

    public void addChangeListener(ChangeListener cl) {
        cs.addChangeListener(cl);
    }

    public void removeChangeListener(ChangeListener cl) {
        cs.removeChangeListener(cl);
    }

    public Project getProject() {
        return project;
    }
}