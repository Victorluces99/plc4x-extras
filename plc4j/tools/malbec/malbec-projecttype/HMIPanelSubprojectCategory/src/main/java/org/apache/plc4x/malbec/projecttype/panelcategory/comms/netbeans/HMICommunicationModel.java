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

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import javax.swing.event.ChangeListener;
import org.apache.plc4x.malbec.projecttype.panelcategory.comms.api.CommConfigRepository;
import org.apache.plc4x.malbec.projecttype.panelcategory.comms.api.CommConfigStorage;
import org.apache.plc4x.malbec.projecttype.panelcategory.comms.api.CommunicationsConfig;
import org.apache.plc4x.malbec.projecttype.panelcategory.comms.api.CommunicationStore;
import org.apache.plc4x.malbec.projecttype.panelcategory.comms.persistence.BootDbRepository;
import org.apache.plc4x.malbec.projecttype.panelcategory.model.CommConfigData;
import org.apache.plc4x.malbec.projecttype.panelcategory.model.DeviceConfigData;
import org.apache.plc4x.malbec.projecttype.panelcategory.ProjectIds;
import org.netbeans.api.project.Project;
import org.openide.util.ChangeSupport;
import org.apache.plc4x.malbec.projecttype.panelcategory.comms.api.SaveResult;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class HMICommunicationModel implements CommunicationStore {

    /**
     * Nombre del fichero de configuracion de comunicaciones.
     *
     * <p>Se mantiene por compatibilidad con {@code NetBeansProjectStorage}, que lo usa.
     * El valor vive en {@link ProjectIds}; aqui solo se renombra, para no tener dos nombres
     * distintos del mismo fichero.
     */
    public static final String CONFIG_FILE = ProjectIds.COMMUNICATION_CONFIG_FILE;

    private static final Logger LOGGER = Logger.getLogger(HMICommunicationModel.class.getName());

    private final ProjectStorage storage;
    private final ChangeSupport cs = new ChangeSupport(this);

    /**
     * Exclusion mutua sobre {@link #config}, {@link #loadError} y {@link #repository}.
     *
     * <p>Antes no habia nada, y hacia falta: el watcher de ficheros recarga desde un
     * hilo y el usuario guarda desde otro, de modo que las dos cosas ocurren a la vez
     * de verdad. Los tres campos se asignan por separado, asi que una recarga podia
     * colarse entre la comprobacion de la guarda H-01 y la escritura, y el guardado
     * podia tomar la configuracion antes de una recarga y reconstruir {@code boot.db}
     * despues de ella. Los dos ficheros acababan describiendo cosas distintas sin que
     * nada avise, y el resultado dependia del planificador.
     *
     * <p>El bloqueo de escritura cubre el guardado y la recarga enteros, no solo las
     * asignaciones: la parte interesante es la escritura a disco, que es donde importa
     * que nadie cambie lo que se esta guardando.
     */
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private CommConfigRepository repository;
    private CommunicationsConfig config;
    private Exception loadError;

    public HMICommunicationModel(Project project) {
        this(new NetBeansProjectStorage(project));
    }

    HMICommunicationModel(ProjectStorage storage) {
        this.storage = storage;
        storage.setOnChanged(this::reload);
        storage.setOnDeleted(this::olvidarConfiguracion);
        reload();
    }

    /**
     * Recarga la configuracion desde el almacenamiento.
     *
     * <p>Toma el bloqueo de escritura porque reasignar {@code config} mientras otro
     * hilo guarda es exactamente la carrera que este bloqueo elimina. Es reentrante,
     * asi queolver a llamar desde dentro no se queda esperando a si mismo.
     */
    public final void reload() {
        lock.writeLock().lock();
        try {
            recargar();
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void recargar() {
        boolean commProject = "Comunicacion".equalsIgnoreCase(storage.directoryName())
                || storage.has(CONFIG_FILE)
                || storage.has("comm.merlot");
        if (!commProject) {
            olvidarConfiguracion();
            loadError = null;
            cs.fireChange();
            return;
        }

        boolean created = false;
        CommConfigStorage configStorage = storage.config();
        if (configStorage == null) {
            try {
                configStorage = storage.createConfig();
                created = true;
            } catch (IOException ex) {
                configStorage = null;
            }
        }
        if (configStorage == null) {
            // No se pudo ni leer ni crear el archivo. Sin archivo no hay nada que
            // cargar, y guardar aqui escribiria un modelo vacio sobre boot.db.
            loadError = new IOException("no se pudo crear " + CONFIG_FILE
                    + " en el proyecto " + storage.directoryName());
            config = new CommunicationsConfig();
            LOGGER.log(Level.SEVERE, loadError.getMessage());
            cs.fireChange();
            return;
        }

        storage.watchForChanges();
        repository = CommConfigRepositoryLookup.createRepository(configStorage);
        CommConfigLoader.Result carga;
        try {
            carga = new CommConfigLoader(repository).load();
        } catch (Exception ex) {
            // H-01: el fallo ya no se traga. Antes se sustituid la configuracion
            // por una vacia sin decir nada, de modo que un archivo ilegible se veia
            // como Â«no hay dispositivosÂ» y un guardado posterior podia escribir
            // encima un modelo que nunca se habia leido.
            // Ahora se conserva lo que habia, se registra el error y queda disponible
            // para que la interfaz avise. No se guarda nada en este estado.
            loadError = ex;
            if (config == null) {
                config = new CommunicationsConfig();
            }
            LOGGER.log(Level.SEVERE, "No se pudo cargar " + CONFIG_FILE
                    + "; se conserva la configuraciÃ³n anterior.", ex);
            cs.fireChange();
            return;
        }
        loadError = null;
        config = carga.config();
        if (created || carga.uuidReparados()) {
            escribir(repository, created);
        }
        cs.fireChange();
    }

    /** Reestablece el modelo tras borrarse el archivo de configuracion. */
    private void olvidarConfiguracion() {
        lock.writeLock().lock();
        try {
            repository = null;
            config = new CommunicationsConfig();
            loadError = null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Reescribe {@code comunicacion.xml} sin disparar una recarga sobre una
     * escritura a medias.
     */
    private void escribir(CommConfigRepository destino, boolean archivoNuevo) {
        storage.pauseNotifications();
        try {
            destino.save(config);
        } catch (Exception ex) {
            if (archivoNuevo) {
                // El archivo vacio se materializara en el primer save() real.
                LOGGER.log(Level.FINE, "Se dejara para el primer guardado: " + ex.getMessage());
            } else {
                LOGGER.log(Level.WARNING,
                        "No se pudo guardar el UUID reparado de " + CONFIG_FILE, ex);
            }
        } finally {
            storage.resumeNotifications();
        }
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
            LOGGER.log(Level.WARNING, CONFIG_FILE + ": el dispositivo '{0}' no tenía UUID "
                    + "y recibió {1} al cargar.",
                    new Object[]{device.getDeviceName(), uuid});
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
    /**
     * Los lectores toman el bloqueo de lectura, para que no vean el modelo a medio
     * cambiar mientras una recarga lo reasigna.
     */
    public Optional<Exception> getLoadError() {
        lock.readLock().lock();
        try {
            return Optional.ofNullable(loadError);
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<DeviceConfigData> getDevices() {
        lock.readLock().lock();
        try {
            return config.getDevices();
        } finally {
            lock.readLock().unlock();
        }
    }

    public DeviceConfigData findByUuid(String uuid) {
        lock.readLock().lock();
        try {
            return config.findByUuid(uuid);
        } finally {
            lock.readLock().unlock();
        }
    }

    public CommConfigData getComms(String deviceUuid) {
        lock.readLock().lock();
        try {
            return config.getComms(deviceUuid);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void upsertDevice(DeviceConfigData device) {
        lock.writeLock().lock();
        try {
            config.upsertDevice(device);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void upsertComms(String deviceUuid, CommConfigData commConfig) {
        lock.writeLock().lock();
        try {
            config.upsertComms(deviceUuid, commConfig);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean removeDevice(String uuid) {
        lock.writeLock().lock();
        try {
            return config.removeDevice(uuid);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Guarda la configuracion y reconstruye {@code boot.db}.
     *
     * <p>Toma el bloqueo de escritura durante todo el guardado, incluida la
     * reconstruccion de la base: leer {@code config} una vez al principio y usarla
     * despues, sin el bloqueo, permitiria que una recarga la cambiase entre medias y
     * que el XML y la base acabaran con contenidos distintos.
     *
     * <p>El evento de cambio se dispara aqui mismo, en el hilo que guarda, igual que
     * antes de este refactor. Moverlo al EDT es la parte de T4.6 que no se hace aqui.
     */
    public SaveResult save() {
        lock.writeLock().lock();
        try {
            return guardar();
        } finally {
            lock.writeLock().unlock();
        }
    }

    private SaveResult guardar() {
        if (loadError != null) {
            // H-01: con la carga fallida el modelo puede estar vacio o incompleto.
            // Guardar aqui escribiria eso sobre comunicacion.xml y reconstruiria
            // boot.db desde un modelo que nunca se ha podido leer. Se niega, y el
            // motivo queda en el registro.
            String motivo = "la carga de " + CONFIG_FILE + " fallo, asi que el modelo en "
                    + "memoria no es de fiar";
            LOGGER.log(Level.SEVERE, "No se guarda nada: " + motivo, loadError);
            return SaveResult.noGuardado(motivo);
        }
        CommConfigStorage configStorage = storage.config();
        if (configStorage == null) {
            try {
                configStorage = storage.createConfig();
            } catch (IOException ex) {
                // Aqui todavia no se ha escrito nada, asi que se puede devolver un
                // resultado en vez de propagar la excepcion como antes.
                LOGGER.log(Level.SEVERE, "No se pudo crear el almacen de la configuracion", ex);
                return SaveResult.noGuardado(
                        "no se pudo preparar el fichero de configuracion: " + ex.getMessage());
            }
        }
        final CommConfigRepository repo = CommConfigRepositoryLookup.createRepository(configStorage);
        try {
            storage.pauseNotifications();
            try {
                repo.save(config);
            } finally {
                storage.resumeNotifications();
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "No se pudo escribir " + CONFIG_FILE, ex);
            return SaveResult.noGuardado(
                    "no se pudo escribir " + CONFIG_FILE + ": " + ex.getMessage());
        }
        repository = repo;
        boolean databaseUpdated;
        try {
            databaseUpdated = BootDbRepository.rebuild(storage.bootDbFolder(), config);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Error volcando la configuracion a boot.db", ex);
            databaseUpdated = false;
        }
        cs.fireChange();
        if (databaseUpdated) {
            return SaveResult.exito();
        }
        // El XML ya esta escrito: no es lo mismo que no haber guardado nada, y quien
        // llama tiene que poder decirlo.
        LOGGER.warning("boot.db no pudo actualizarse; revise el registro de la aplicacion.");
        return SaveResult.parcial(
                "se escribio " + CONFIG_FILE + " pero no se pudo actualizar boot.db");
    }

    public void addChangeListener(ChangeListener cl) {
        cs.addChangeListener(cl);
    }

    public void removeChangeListener(ChangeListener cl) {
        cs.removeChangeListener(cl);
    }
}
