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
package org.apache.plc4x.malbec.projecttype.panelcategory.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.plc4x.malbec.projecttype.panelcategory.model.CommConfigData;
import org.apache.plc4x.malbec.projecttype.panelcategory.model.DeviceConfigData;

public class CommunicationWizardState {

    private DeviceConfigData selectedDevice;
    private final List<CommConfigData.GroupConfig> groups = new ArrayList<>();
    private final List<CommConfigData.ItemConfig> items = new ArrayList<>();
    private final List<CommConfigData.PvConfig> pvs = new ArrayList<>();
    private final Map<String, String> itemsGroup = new HashMap<>();
    private boolean cambiosSinGuardar = false;
    private boolean missingGroupsWarned = false;
    
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
