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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.plc4x.malbec.projecttype.panelcategory.comms.xml.HMICommunicationModel;
import org.apache.plc4x.malbec.projecttype.panelcategory.panel.CommConfigData;
import org.apache.plc4x.malbec.projecttype.panelcategory.panel.DeviceConfigData;
import org.netbeans.api.project.Project;

/**
 * Reglas de negocio y persistencia del asistente de comunicación.
 *
 * <p>Los paneles no tocan ni el modelo de configuración ni la base de datos:
 * le piden una operación al controlador y refreshizan la vista. Las reglas que
 * importan están todas acá:</p>
 *
 * <ul>
 *   <li>Modificar conserva uuid, nombre, grupo de escaneo y md5: el registro se
 *       sobreescribe en el XML y en la base, nunca se duplica.</li>
 *   <li>El grupo de escaneo de un área no se cambia desde la UI.</li>
 *   <li>Un área sin grupo bloquea el guardado completo, no se completa sola.</li>
 *   <li>Un grupo con áreas asociadas no se borra; un área con variables las
 *       arrastra en cascada, avisando antes.</li>
 * </ul>
 */
public class CommunicationWizardController {

    private static final String MD5_GROUP = "md5_group_hash";
    private static final String MD5_ITEM = "md5_item_hash";
    private static final String MD5_PV = "md5_pv_hash";

    private final Project project;
    private final HMICommunicationModel model;
    private final CommunicationWizardState state;

    public CommunicationWizardController(Project project, CommunicationWizardState state) {
        this.project = project;
        this.state = state;
        this.model = project != null ? project.getLookup().lookup(HMICommunicationModel.class) : null;
    }

    public CommunicationWizardState getState() {
        return state;
    }

    public Project getProject() {
        return project;
    }

    // --- dispositivo ---------------------------------------------------------

    public List<DeviceConfigData> getAvailableDevices() {
        return model != null ? model.getDevices() : new ArrayList<>();
    }

    public void selectDevice(DeviceConfigData device) {
        state.setSelectedDevice(device);
        if (device != null && model != null) {
            state.reset(model.getComms(device.getUuid()));
        } else {
            state.reset(null);
        }
    }

    // --- grupos --------------------------------------------------------------

    public void addGroup(String uuid, String name, String desc, String scan, boolean enable) {
        state.getGroups().add(
                new CommConfigData.GroupConfig(uuid, name, desc, scan, enable, MD5_GROUP));
        state.setCambiosSinGuardar(true);
    }

    /**
     * Aplica cambios sobre el grupo existente conservando uuid, nombre y md5:
     * el nombre es la identidad del grupo y el scantime bloqueado en la UI se
     * conserva si el combo no tiene nada seleccionado.
     */
    public void updateGroup(String uuid, String desc, String scan, boolean enable) {
        CommConfigData.GroupConfig actual = state.groupByUuid(uuid);
        if (actual == null) {
            return;
        }
        String scantime = scan != null && !scan.isEmpty() ? scan : actual.getScantime();
        int idx = state.getGroups().indexOf(actual);
        state.getGroups().set(idx, new CommConfigData.GroupConfig(
                actual.getUuid(), actual.getName(), desc, scantime, enable, actual.getMd5()));
        state.setCambiosSinGuardar(true);
    }

    /**
     * Elimina el grupo, salvo que tenga áreas asociadas.
     *
     * @return los nombres de las áreas que impiden el borrado; lista vacía si el
     *         grupo se eliminó. Nunca lanza: el aviso es cosa de la vista.
     */
    public List<String> deleteGroup(String uuid) {
        CommConfigData.GroupConfig group = state.groupByUuid(uuid);
        if (group == null) {
            return List.of();
        }
        List<String> areasDelGrupo = new ArrayList<>();
        for (CommConfigData.ItemConfig i : state.getItems()) {
            if (group.getUuid().equals(i.getGroupUuid())) {
                areasDelGrupo.add(i.getName());
            }
        }
        if (!areasDelGrupo.isEmpty()) {
            return areasDelGrupo;
        }
        state.getGroups().remove(group);
        state.setCambiosSinGuardar(true);
        return List.of();
    }

    // --- áreas (items) -------------------------------------------------------

    public void addItem(String uuid, String name, String desc, String tag, boolean enable, String groupUuid) {
        state.getItems().add(new CommConfigData.ItemConfig(
                uuid, name, desc, tag, enable, MD5_ITEM, groupUuid));
        state.getItemsGroup().put(uuid, groupUuid);
        state.setCambiosSinGuardar(true);
    }

    /**
     * Aplica cambios sobre el área existente conservando uuid, nombre, grupo de
     * escaneo y md5. El grupo no llega como parámetro a propósito: la relación se
     * fija al crear el área y no se modifica desde la UI.
     */
    public void updateItem(String uuid, String desc, String tag, boolean enable) {
        CommConfigData.ItemConfig actual = state.itemByUuid(uuid);
        if (actual == null) {
            return;
        }
        int idx = state.getItems().indexOf(actual);
        state.getItems().set(idx, new CommConfigData.ItemConfig(
                actual.getUuid(), actual.getName(), desc, tag, enable,
                actual.getMd5(), actual.getGroupUuid()));
        state.getItemsGroup().put(actual.getUuid(), actual.getGroupUuid());
        state.setCambiosSinGuardar(true);
    }

    /**
     * Elimina el área y arrastra sus variables en cascada, para no dejar
     * variables apuntando a un área que ya no existe.
     *
     * @return los nombres de las variables que se eliminarían; lista vacía si se
     *         borró. La vista debe pedir confirmación cuando no esté vacía.
     */
    public List<String> deleteItem(String uuid) {
        CommConfigData.ItemConfig item = state.itemByUuid(uuid);
        if (item == null) {
            return List.of();
        }
        List<String> nombres = new ArrayList<>();
        for (CommConfigData.PvConfig pv : state.getPvs()) {
            if (item.getUuid().equals(pv.getId())) {
                nombres.add(pv.getName());
            }
        }
        state.getPvs().removeIf(pv -> uuid.equals(pv.getId()));
        state.getItems().remove(item);
        state.getItemsGroup().remove(uuid);
        state.setCambiosSinGuardar(true);
        return nombres;
    }

    // --- variables (pvs) -----------------------------------------------------

    public void addPv(CommConfigData.PvConfig pv) {
        state.getPvs().add(pv);
        state.setCambiosSinGuardar(true);
    }

    /**
     * Aplica cambios sobre la variable existente conservando lo que define su
     * ubicación en la memoria: uuid, nombre, tipo, PvId, offset, md5 y ruta S88.
     *
     * <p>El offset no llega como parámetro a propósito. Es
     * {@code posición × tamaño del tipo}, y la posición depende del orden de las
     * variables del área: recalcularlo al modificar desplazaría el resto de las
     * variables que vienen después. Por eso mover una variable a otro PvId no se
     * permite; hay que borrarla y volver a crearla.</p>
     */
    public void updatePv(String uuid, String descriptor, String scanTime,
            boolean scanEnable, boolean writeEnable, String displayLimitLow,
            String displayLimitHigh, String displayDescription, String displayFormat,
            String displayUnits, String controlLimitLow, String controlLimitHigh,
            String controlMinStep) {
        CommConfigData.PvConfig actual = state.pvByUuid(uuid);
        if (actual == null) {
            return;
        }
        int idx = state.getPvs().indexOf(actual);
        state.getPvs().set(idx, new CommConfigData.PvConfig(
                actual.getUuid(), actual.getName(), actual.getType(), actual.getId(),
                actual.getOffset(), descriptor, scanTime, scanEnable, writeEnable,
                displayLimitLow, displayLimitHigh, displayDescription, displayFormat,
                displayUnits, controlLimitLow, controlLimitHigh, controlMinStep,
                actual.getMd5(), actual.getS88Path()));
        state.setCambiosSinGuardar(true);
    }

    /**
     * Elimina la variable. No arrastra nada en cascada: es el último eslabón de
     * la relación área → variable, así que no hay registros que la referencien.
     *
     * <p>Si era la primera variable del área, el área queda sin tipo fijado y
     * vuelve a admitir variables de cualquier tipo.</p>
     *
     * @return true si la variable existía y fue eliminada
     */
    public boolean deletePv(String uuid) {
        CommConfigData.PvConfig pv = state.pvByUuid(uuid);
        if (pv == null) {
            return false;
        }
        state.getPvs().remove(pv);
        state.setCambiosSinGuardar(true);
        return true;
    }

    /** Md5 de las variables: todas las que se agregan usan el mismo marcador. */
    public static String pvMd5() {
        return MD5_PV;
    }

    /** Md5 de los grupos. */
    public static String groupMd5() {
        return MD5_GROUP;
    }

    // --- layout de memoria de un área ---------------------------------------

    /**
     * Tipo al que queda fijado el área: el de su primera variable guardada. Si
     * el área está vacía devuelve {@code null} y todavía admite cualquier tipo,
     * que lo fija la primera variable elegida.
     */
    public String itemLockedType(String areaUuid) {
        List<CommConfigData.PvConfig> areaPvs = state.pvsOfArea(areaUuid);
        if (areaPvs.isEmpty()) {
            return null;
        }
        return areaPvs.get(0).getType();
    }

    /**
     * Tamaño en bytes de un tipo de dato. Devuelve 0 si el tamaño no es
     * determinable (por ejemplo {@code string}), en cuyo caso no se puede
     * aplicar la fórmula estándar de offset.
     */
    public static int typeSizeBytes(String type) {
        if (type == null || type.isEmpty()) {
            return 0;
        }
        return switch (type.toLowerCase()) {
            case "boolean", "byte", "ubyte" -> 1;
            case "short", "ushort" -> 2;
            case "int", "uint", "long", "ulong", "float" -> 4;
            case "double" -> 8;
            case "string" -> 0;
            default -> 4;
        };
    }

    /**
     * Tamaño en bytes (Tam) que aplica al área: el del tipo al que está fijada o,
     * si todavía está vacía, el de la variable elegida.
     */
    public int selectedAreaTam(String areaUuid, String variableType) {
        String type = itemLockedType(areaUuid);
        if (type == null || type.isEmpty()) {
            type = variableType;
        }
        return typeSizeBytes(type);
    }

    /**
     * Offset siguiente para el área según la fórmula estándar
     * {@code (Mediación - 1) × Tam}: la medición es la posición de la variable
     * dentro del área (empieza en 1) y {@code Tam} el tamaño en bytes del tipo.
     *
     * @return -1 cuando todavía no hay tipo definido o su tamaño es variable
     */
    public int nextOffset(String areaUuid, String variableType) {
        int tam = selectedAreaTam(areaUuid, variableType);
        if (tam <= 0) {
            return -1;
        }
        return state.pvsOfArea(areaUuid).size() * tam;
    }

    // --- variables de planta ya asignadas -----------------------------------

    /** Clave de identidad de una variable de planta: ruta dentro del área S88 + nombre. */
    public static String pvKey(String path, String name) {
        return (path == null || path.isEmpty() ? "" : path + "/") + name;
    }

    /** Claves de todas las variables ya guardadas, para no ofrecerlas de nuevo. */
    public Set<String> getUsedPvKeys() {
        Set<String> usedKeys = new HashSet<>();
        for (CommConfigData.PvConfig pv : state.getPvs()) {
            usedKeys.add(pvKey(pv.getS88Path(), pv.getName()));
        }
        return usedKeys;
    }

    // --- validación y persistencia ------------------------------------------

    /** Nombres de las áreas sin grupo de escaneo; vacío si la configuración es válida. */
    public List<String> areasSinGrupoNames() {
        List<String> nombres = new ArrayList<>();
        for (CommConfigData.ItemConfig i : state.areasSinGrupo()) {
            nombres.add(i.getName());
        }
        return nombres;
    }

    public boolean hasAreasWithoutGroup() {
        return !state.areasSinGrupo().isEmpty();
    }

    /**
     * Vuelca la configuración a {@code comunicacion.xml} y a {@code boot.db}.
     *
     * @return true si el XML se guardó y la base quedó actualizada; false si la
     *         base falló, en cuyo caso el XML sí quedó escrito
     */
    public boolean saveAll() {
        if (model == null || state.getSelectedDevice() == null) {
            return false;
        }
        // Copia defensiva: las listas del wizard siguen mutando mientras el
        // usuario edita, y lo que se guarda debe ser el estado de este instante.
        CommConfigData config = new CommConfigData(
                state.getSelectedDevice().getDeviceName(),
                new ArrayList<>(state.getGroups()),
                new ArrayList<>(state.getItems()),
                new ArrayList<>(state.getPvs()));
        model.upsertComms(state.getSelectedDevice().getUuid(), config);
        boolean saved = model.save();
        // Con un guardado parcial se sigue avisando al cerrar, porque el modelo
        // en memoria difiere de la base de datos.
        state.setCambiosSinGuardar(!saved);
        return saved;
    }
}
