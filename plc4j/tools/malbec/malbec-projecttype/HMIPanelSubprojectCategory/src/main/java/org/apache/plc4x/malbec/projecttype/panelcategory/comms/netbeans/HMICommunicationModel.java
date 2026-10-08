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
package org.apache.plc4x.malbec.projecttype.panelcategory.comms.netbeans;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.ChangeListener;
import org.apache.plc4x.malbec.projecttype.panelcategory.comms.persistence.BootDbRepository;
import org.apache.plc4x.malbec.projecttype.panelcategory.comms.api.CommConfigRepository;
import org.apache.plc4x.malbec.projecttype.panelcategory.comms.api.CommunicationStore;
import org.apache.plc4x.malbec.projecttype.panelcategory.comms.api.CommunicationsConfig;
import org.apache.plc4x.malbec.projecttype.panelcategory.model.CommConfigData;
import org.apache.plc4x.malbec.projecttype.panelcategory.model.DeviceConfigData;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileChangeAdapter;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.ChangeSupport;

public final class HMICommunicationModel implements CommunicationStore {

    public static final String CONFIG_FILE = "comunicacion.xml";

    private static final Logger LOGGER = Logger.getLogger(HMICommunicationModel.class.getName());

    private final Project project;
    private final ChangeSupport cs = new ChangeSupport(this);
    private final FileChangeAdapter fileListener;
    private CommConfigRepository repository;
    private CommunicationsConfig config;
    private FileObject file;
    private Exception loadError;

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
                loadError = null;
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
            loadError = null;
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
            // No se pudo ni leer ni crear el archivo. Sin archivo no hay nada que
            // cargar, y guardar aqui escribiria un modelo vacio sobre boot.db.
            loadError = new IOException("no se pudo crear " + CONFIG_FILE
                    + " en el proyecto " + projectDir.getName());
            config = new CommunicationsConfig();
            LOGGER.log(Level.SEVERE, loadError.getMessage());
            cs.fireChange();
            return;
        }

        file.removeFileChangeListener(fileListener);
        file.addFileChangeListener(fileListener);
        repository = new CommConfigServices().createRepository(new ProjectFileStorage(file));
        CommConfigLoader.Result carga;
        try {
            carga = new CommConfigLoader(repository).load();
        } catch (Exception ex) {
            // H-01: el fallo ya no se traga. Antes se sustitodia la configuracion
            // por una vacia sin decir nada, de modo que un XML danado se veia como
            // «no hay dispositivos» y un «Guardar» posterior vaciaba boot.db.
            // Ahora se conserva lo que habia, se registra el error y queda disponible
            // para que la interfaz avise. No se guarda nada en este estado.
            loadError = ex;
            if (config == null) {
                config = new CommunicationsConfig();
            }
            LOGGER.log(Level.SEVERE, "No se pudo cargar " + CONFIG_FILE
                    + "; se conserva la configuración anterior.", ex);
            cs.fireChange();
            return;
        }
        loadError = null;
        config = carga.config();
        boolean reparado = carga.uuidReparados();
        if (created) {
            try {
                repository.save(config);
            } catch (Exception ignored) {
                // el archivo vacío se materializará en el primer save()
            }
        } else if (reparado) {
            try {
                repository.save(config);
            } catch (Exception ex) {
                System.err.println("No se pudo guardar el UUID reparado de "
                        + CONFIG_FILE + ": " + ex.getMessage());
            }
        }
        cs.fireChange();
    }

    static boolean repararUuidsAusentes(CommunicationsConfig config) {
        boolean reparado = false;
        for (int i = 0; i < config.getDevices().size(); i++) {
            DeviceConfigData device = config.getDevices().get(i);
            if (device.getUuid() != null && !device.getUuid().isBlank()) {
                continue;
            }
            reparado = true;
            String uuid = UUID.randomUUID().toString();
            System.err.println(CONFIG_FILE + ": el dispositivo '" + device.getDeviceName()
                    + "' no tenía UUID y recibió " + uuid + " al cargar.");
            Properties p = new Properties();
            p.put("brand", device.getBrand());
            p.put("model", device.getModel());
            p.put("protocol", device.getProtocol());
            p.put("deviceName", device.getDeviceName());
            p.put("deviceKey", device.getDeviceKey());
            p.put("description", device.getDescription());
            p.put("uuid", uuid);
            p.put("enable", device.isEnabled());
            p.put("s88Node", device.getS88Node() == null ? "" : device.getS88Node());
            p.put("s88Uuid", device.getS88Uuid() == null ? "" : device.getS88Uuid());
            p.put("specificParameters",
                    device.getSpecificParameters() == null ? "" : device.getSpecificParameters());
            config.replaceDevice(i, new DeviceConfigData(p));
        }
        return reparado;
    }

    public CommunicationsConfig getConfig() {
        return config;
    }

    /**
     * Error de la ultima carga, si lo hubo (H-01).
     *
     * <p>La interfaz debe consultarlo para avisar al usuario. Mientras siga
     * presente, {@link #save()} se niega a escribir: el modelo puede contener solo
     * parte de la informacion o estar vacio, y volcar eso a {@code boot.db}
     * destruiria datos que siguen intactos en el fichero.
     *
     * @return el fallo de carga, o vacio si la ultima carga fue correcta
     */
    public Optional<Exception> getLoadError() {
        return Optional.ofNullable(loadError);
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

    public boolean save() {
        if (loadError != null) {
            // H-01: con la carga fallida, el modelo puede estar vacio o incompleto.
            // Escribirlo reconstruiria boot.db sin los dispositivos que si siguen
            // en comunicacion.xml.
            LOGGER.log(Level.SEVERE, "No se guarda nada: la última carga de " + CONFIG_FILE
                    + " falló, así que el modelo no es fiable.", loadError);
            return false;
        }
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
            File dbFolder = project != null ? FileUtil.toFile(project.getProjectDirectory()) : null;
            databaseUpdated = BootDbRepository.rebuild(dbFolder, config);
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