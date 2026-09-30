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
package org.apache.plc4x.malbec.projecttype.panelcategory.panel.wizard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.plc4x.malbec.projecttype.panelcategory.panel.CommConfigData;
import org.apache.plc4x.malbec.projecttype.panelcategory.panel.DeviceConfigData;

/**
 * Estado editable del asistente: la configuración de comunicación del
 * dispositivo seleccionado, tal como se está modificando en memoria.
 *
 * <p>Es sólo un contenedor de datos y de los lookups que derivan de ellos. No
 * valida ni guarda: de eso se ocupa {@link CommunicationWizardController}. Las
 * listas se exponen vivas a propósito, porque el usuario las modifica de forma
 * incremental; la copia defensiva ocurre recién al guardar, en el
 * controlador.</p>
 */
public class CommunicationWizardState {

    private DeviceConfigData selectedDevice;
    private final List<CommConfigData.GroupConfig> groups = new ArrayList<>();
    private final List<CommConfigData.ItemConfig> items = new ArrayList<>();
    private final List<CommConfigData.PvConfig> pvs = new ArrayList<>();

    /** Índice inverso área → grupo, para no recorrer items en cada fila. */
    private final Map<String, String> itemsGroup = new HashMap<>();

    private boolean cambiosSinGuardar = false;

    /** Evita repetir el aviso de áreas sin grupo al cambiar de dispositivo. */
    private boolean missingGroupsWarned = false;

    /** Carga la configuración del dispositivo elegido, o la vacía si no hay. */
    public void reset(CommConfigData comms) {
        groups.clear();
        items.clear();
        pvs.clear();
        itemsGroup.clear();
        cambiosSinGuardar = false;
        missingGroupsWarned = false;

        if (comms != null) {
            if (comms.getGroups() != null) {
                groups.addAll(comms.getGroups());
            }
            if (comms.getItems() != null) {
                items.addAll(comms.getItems());
                for (CommConfigData.ItemConfig i : items) {
                    if (i.getGroupUuid() != null && !i.getGroupUuid().isEmpty()) {
                        itemsGroup.put(i.getUuid(), i.getGroupUuid());
                    }
                }
            }
            if (comms.getPvs() != null) {
                pvs.addAll(comms.getPvs());
            }
        }
    }

    public CommConfigData.GroupConfig groupByUuid(String uuid) {
        if (uuid == null) {
            return null;
        }
        for (CommConfigData.GroupConfig g : groups) {
            if (g.getUuid().equals(uuid)) {
                return g;
            }
        }
        return null;
    }

    public CommConfigData.ItemConfig itemByUuid(String uuid) {
        if (uuid == null) {
            return null;
        }
        for (CommConfigData.ItemConfig i : items) {
            if (uuid.equals(i.getUuid())) {
                return i;
            }
        }
        return null;
    }

    public CommConfigData.PvConfig pvByUuid(String uuid) {
        if (uuid == null) {
            return null;
        }
        for (CommConfigData.PvConfig pv : pvs) {
            if (uuid.equals(pv.getUuid())) {
                return pv;
            }
        }
        return null;
    }

    /**
     * Nombre del grupo de escaneo de un área, o cadena vacía.
     *
     * <p>La cadena vacía cubre dos situaciones distintas: un área guardada antes
     * de que existiera la persistencia del grupo (dato ausente) y un área cuyo
     * grupo fue eliminado (huérfana). Ambas se reportan como incompletas, pero
     * nunca se completan con un grupo inventado.</p>
     */
    public String groupNameOf(String itemUuid) {
        String groupUuid = itemsGroup.get(itemUuid);
        if (groupUuid == null) {
            return "";
        }
        for (CommConfigData.GroupConfig g : groups) {
            if (g.getUuid().equals(groupUuid)) {
                return g.getName();
            }
        }
        return "";
    }

    /** Áreas sin grupo de escaneo: dato incompleto, nunca asumido. */
    public List<CommConfigData.ItemConfig> areasSinGrupo() {
        List<CommConfigData.ItemConfig> sinGrupo = new ArrayList<>();
        for (CommConfigData.ItemConfig i : items) {
            if (groupNameOf(i.getUuid()).isEmpty()) {
                sinGrupo.add(i);
            }
        }
        return sinGrupo;
    }

    public List<CommConfigData.PvConfig> pvsOfArea(String areaUuid) {
        List<CommConfigData.PvConfig> result = new ArrayList<>();
        if (areaUuid == null) {
            return result;
        }
        for (CommConfigData.PvConfig pv : pvs) {
            if (areaUuid.equals(pv.getId())) {
                result.add(pv);
            }
        }
        return result;
    }

    public DeviceConfigData getSelectedDevice() {
        return selectedDevice;
    }

    public void setSelectedDevice(DeviceConfigData selectedDevice) {
        this.selectedDevice = selectedDevice;
    }

    public List<CommConfigData.GroupConfig> getGroups() {
        return groups;
    }

    public List<CommConfigData.ItemConfig> getItems() {
        return items;
    }

    public List<CommConfigData.PvConfig> getPvs() {
        return pvs;
    }

    public Map<String, String> getItemsGroup() {
        return itemsGroup;
    }

    public boolean isCambiosSinGuardar() {
        return cambiosSinGuardar;
    }

    public void setCambiosSinGuardar(boolean cambiosSinGuardar) {
        this.cambiosSinGuardar = cambiosSinGuardar;
    }

    public boolean isMissingGroupsWarned() {
        return missingGroupsWarned;
    }

    public void setMissingGroupsWarned(boolean missingGroupsWarned) {
        this.missingGroupsWarned = missingGroupsWarned;
    }
}
