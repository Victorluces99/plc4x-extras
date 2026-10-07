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

    public boolean deletePv(String uuid) {
        CommConfigData.PvConfig pv = state.pvByUuid(uuid);
        if (pv == null) {
            return false;
        }
        state.getPvs().remove(pv);
        state.setCambiosSinGuardar(true);
        return true;
    }

    public static String pvMd5() {
        return MD5_PV;
    }

    public static String groupMd5() {
        return MD5_GROUP;
    }

    // --- layout de memoria de un área ---------------------------------------
    private MemoryTag tagOf(String areaUuid) {
        CommConfigData.ItemConfig item = state.itemByUuid(areaUuid);
        return item == null ? null : MemoryTag.parse(item.getTag());
    }

    public DataType lockedType(String areaUuid) {
        MemoryTag tag = tagOf(areaUuid);
        if (tag != null && tag.family() != null) {
            List<DataType> candidatos = DataType.candidatos(tag.family(), tag.codeBits());
            if (candidatos.size() == 1) {
                return candidatos.get(0);
            }
        }
        return tipoDePrimeraVariable(areaUuid);
    }

    private List<DataType> candidatosDelTag(String areaUuid) {
        MemoryTag tag = tagOf(areaUuid);
        if (tag == null || tag.family() == null) {
            return List.of();
        }
        return DataType.candidatos(tag.family(), tag.codeBits());
    }

    private DataType tipoDePrimeraVariable(String areaUuid) {
        List<CommConfigData.PvConfig> areaPvs = state.pvsOfArea(areaUuid);
        return areaPvs.isEmpty() ? null : DataType.find(areaPvs.get(0).getType());
    }

    public List<DataType> typesAllowed(String areaUuid) {
        if (state.itemByUuid(areaUuid) == null) {
            return List.of();
        }
        List<DataType> candidatos = candidatosDelTag(areaUuid);
        DataType elegido = tipoDePrimeraVariable(areaUuid);
        if (elegido != null && (candidatos.isEmpty() || candidatos.contains(elegido))) {
            return List.of(elegido);
        }
        if (!candidatos.isEmpty()) {
            return candidatos;
        }
        DataType fijado = lockedType(areaUuid);
        if (fijado != null) {
            return List.of(fijado);
        }
        List<DataType> todos = new ArrayList<>();
        for (DataType t : DataType.values()) {
            if (t.habilitado()) {
                todos.add(t);
            }
        }
        return todos;
    }

    public String nextOffset(String areaUuid, DataType type) {
        MemoryTag tag = tagOf(areaUuid);
        if (tag == null || type == null || !type.habilitado() || tag.byteCapacity() <= 0) {
            return null;
        }
        Set<Integer> ocupados = bitsOcupados(areaUuid);
        int byteInicio = Math.max(0, tag.firstByte());
        int desde = byteInicio * 8;
        int hasta = desde + tag.byteCapacity() * 8;
        int ancho = type.slotBits();
        
        for (int bits = desde; bits + ancho <= hasta; bits += ancho) {
            if (libre(bits, ancho, ocupados)) {
                return type.bitAddressed()
                        ? (bits / 8) + "." + (bits % 8)
                        : String.valueOf(bits / 8);
            }
        }
        return null;
    }

    private static boolean libre(int desde, int ancho, Set<Integer> ocupados) {
        for (int bit = desde; bit < desde + ancho; bit++) {
            if (ocupados.contains(bit)) {
                return false;
            }
        }
        return true;
    }

    private Set<Integer> bitsOcupados(String areaUuid) {
        Set<Integer> ocupados = new HashSet<>();
        for (CommConfigData.PvConfig pv : state.pvsOfArea(areaUuid)) {
            int bits = bitsDeOffset(pv.getOffset());
            if (bits < 0) {
                continue;
            }
            DataType type = DataType.find(pv.getType());
            int ancho = type == null ? 8 : type.slotBits();
            for (int bit = bits; bit < bits + ancho; bit++) {
                ocupados.add(bit);
            }
        }
        return ocupados;
    }

    private static int bitsDeOffset(String offset) {
        if (offset == null || offset.isEmpty()) {
            return -1;
        }
        try {
            int punto = offset.indexOf('.');
            int byteOffset = Integer.parseInt(punto < 0 ? offset : offset.substring(0, punto));
            int bitOffset = punto < 0 ? 0 : Integer.parseInt(offset.substring(punto + 1));
            return byteOffset * 8 + bitOffset;
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    public int areaCapacityBytes(String areaUuid) {
        MemoryTag tag = tagOf(areaUuid);
        if (tag == null) {
            return -1;
        }
        int bytes = tag.byteCapacity();
        return bytes > 0 ? bytes : -1;
    }

    public int areaFreeSlots(String areaUuid, DataType type) {
        MemoryTag tag = tagOf(areaUuid);
        if (tag == null || type == null || !type.habilitado()) {
            return -1;
        }
        int bytes = tag.byteCapacity();
        if (bytes <= 0) {
            return -1;
        }
        return Math.max(0, (bytes * 8) / type.slotBits() - state.pvsOfArea(areaUuid).size());
    }

    public int areaFreeBytes(String areaUuid) {
        MemoryTag tag = tagOf(areaUuid);
        if (tag == null || tag.byteCapacity() <= 0) {
            return -1;
        }

        return Math.max(0, tag.byteCapacity() - bytesOcupados(areaUuid));
    }

    private int bytesOcupados(String areaUuid) {
        int total = 0;
        for (CommConfigData.PvConfig pv : state.pvsOfArea(areaUuid)) {
            DataType type = DataType.find(pv.getType());
            if (type == null) {
                continue;
            }
            total += (type.slotBits() + 7) / 8;
        }
        return total;
    }

    public CommConfigData.ItemConfig areaQueChoca(String tag, String uuidEnEdicion) {
        MemoryTag nuevo = MemoryTag.parse(tag);
        if (nuevo == null) {
            return null;
        }
        for (CommConfigData.ItemConfig i : state.getItems()) {
            if (uuidEnEdicion != null && uuidEnEdicion.equals(i.getUuid())) {
                continue;
            }
            if (nuevo.solapaCon(MemoryTag.parse(i.getTag()))) {
                return i;
            }
        }
        return null;
    }

    public CommConfigData.ItemConfig areaConMismoTag(String tag, String uuidEnEdicion) {
        MemoryTag t = MemoryTag.parse(tag);
        if (t == null || t.tieneDireccion()) {
            return null;
        }
        String buscado = normalizaTexto(tag);
        if (buscado.isEmpty()) {
            return null;
        }
        for (CommConfigData.ItemConfig i : state.getItems()) {
            if (uuidEnEdicion != null && uuidEnEdicion.equals(i.getUuid())) {
                continue;
            }
            MemoryTag suyo = MemoryTag.parse(i.getTag());
            if (suyo != null && suyo.tieneDireccion()) {
                continue;
            }
            if (buscado.equals(normalizaTexto(i.getTag()))) {
                return i;
            }
        }
        return null;
    }

    private static String normalizaTexto(String tag) {
        if (tag == null) {
            return "";
        }
        String t = tag.trim();
        if (t.startsWith("%")) {
            t = t.substring(1).trim();
        }
        int dosPuntos = t.lastIndexOf(':');
        if (dosPuntos > 0) {
            t = t.substring(0, dosPuntos).trim();
        }
        return t.replaceAll("\\s+", "").toUpperCase();
    }

    public MemoryTag areaTag(String areaUuid) {
        return tagOf(areaUuid);
    }

    public List<DataType> typesForCombo(String areaUuid) {
        List<DataType> combo = new ArrayList<>(typesAllowed(areaUuid));
        for (DataType t : DataType.values()) {
            if (!combo.contains(t) && !t.habilitado()) {
                combo.add(t);
            }
        }
        return combo;
    }

    public boolean typeAllowed(String areaUuid, DataType type) {
        return type != null && type.habilitado()
                && typesAllowed(areaUuid).contains(type);
    }

    public boolean areaAdmiteTipo(String areaUuid, String plantType, DataType locked) {
        if (locked == null || plantType == null || plantType.isEmpty()) {
            return true;
        }

        if (!typesAllowed(areaUuid).contains(locked)) {
            return false;
        }
        MemoryTag tag = tagOf(areaUuid);
        List<DataType> posibles = tag == null
                ? List.of()
                : DataType.candidatos(plantType, tag.codeBits());
        if (!posibles.isEmpty()) {
            return posibles.contains(locked);
        }
        DataType exacto = DataType.findIgnoreCase(plantType);
        return exacto == null || exacto == locked;
    }

    // --- variables de planta ya asignadas -----------------------------------

    public static String pvKey(String path, String name) {
        return (path == null || path.isEmpty() ? "" : path + "/") + name;
    }

    public Set<String> getUsedPvKeys() {
        Set<String> usedKeys = new HashSet<>();
        for (CommConfigData.PvConfig pv : state.getPvs()) {
            usedKeys.add(pvKey(pv.getS88Path(), pv.getName()));
        }
        return usedKeys;
    }

    // --- validación y persistencia ------------------------------------------
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

    public boolean saveAll() {
        if (model == null || state.getSelectedDevice() == null) {
            return false;
        }
        CommConfigData config = new CommConfigData(
                state.getSelectedDevice().getDeviceName(),
                new ArrayList<>(state.getGroups()),
                new ArrayList<>(state.getItems()),
                new ArrayList<>(state.getPvs()));
        model.upsertComms(state.getSelectedDevice().getUuid(), config);
        boolean saved = model.save();
        state.setCambiosSinGuardar(!saved);
        return saved;
    }
}
