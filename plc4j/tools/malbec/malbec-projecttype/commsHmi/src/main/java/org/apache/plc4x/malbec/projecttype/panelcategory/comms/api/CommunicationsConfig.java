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
package org.apache.plc4x.malbec.projecttype.panelcategory.comms.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.plc4x.malbec.projecttype.panelcategory.model.DeviceConfigData;
import org.apache.plc4x.malbec.projecttype.panelcategory.model.PvConfigData;

public final class CommunicationsConfig {

    private static final Logger LOGGER
            = Logger.getLogger(CommunicationsConfig.class.getName());
    private final List<DeviceConfigData> device = new ArrayList<>();
    private final Map<String, Integer> indexByUUID = new HashMap<>();
    private final Map<String, List<PvConfigData>> variables = new HashMap<>();

    public CommunicationsConfig() {
    }

    public CommunicationsConfig(List<DeviceConfigData> devices) {
        for (DeviceConfigData device : devices == null ? List.<DeviceConfigData>of() : devices) {
            addDevice(device);
        }
    }

    public List<DeviceConfigData> getDevices() {
        return Collections.unmodifiableList(device);
    }

    public void addDevice(DeviceConfigData device) {
        this.device.add(device);
        indexar(device);
    }

    public void upsertDevice(DeviceConfigData device) {
        String uuid = device.getUuid();
        if (uuid == null || uuid.isBlank()) {
            String name = device.getDeviceName();
            LOGGER.log(Level.FINE, () -> "The Device '" + name + "' arrives without a UUID;"
                    + " It is added as a new item and will be assigned when it is loaded.");
            this.device.add(device);
            return;
        }
        Integer position = indexByUUID.get(uuid);
        if (position == null) {
            this.device.add(device);
            indexar(device);
            return;
        }
        int i = position;
        DeviceConfigData previous = this.device.set(i, device);
        renameVariables(uuid, previous, device);
    }

    private void renameVariables(String deviceUuid, DeviceConfigData previous,
            DeviceConfigData current) {
        if (previous == null || current == null
                || Objects.equals(previous.getDeviceName(), current.getDeviceName())) {
            return;
        }
        List<PvConfigData> pvs = variables.get(deviceUuid);
        if (pvs == null || pvs.isEmpty()) {
            return;
        }
        variables.put(deviceUuid, pvs.stream()
                .map(pv -> pv.withDeviceName(current.getDeviceName()))
                .toList());
    }

    public DeviceConfigData findByUuid(String uuid) {
        if (uuid == null || uuid.isBlank()) {
            return null;
        }
        Integer position = indexByUUID.get(uuid);
        return position == null ? null : device.get(position);
    }

    /**
     * Variables stored in the XML for a device.
     */
    public List<PvConfigData> getVariables(String deviceUuid) {
        return variables.getOrDefault(deviceUuid, List.of());
    }

    public void upsertVariables(String deviceUuid, List<PvConfigData> pvs) {
        if (deviceUuid == null || deviceUuid.isBlank() || pvs == null || pvs.isEmpty()) {
            variables.remove(deviceUuid);
        } else {
            variables.put(deviceUuid, List.copyOf(pvs));
        }
    }

    public void removeVariables(String deviceUuid) {
        variables.remove(deviceUuid);
    }

    public boolean removeDevice(String uuid) {
        if (uuid == null || uuid.isBlank()) {
            return false;
        }
        Integer position = indexByUUID.remove(uuid);
        if (position == null) {
            return false;
        }
        device.remove(position.intValue());
        reindexar();
        variables.remove(uuid);
        return true;
    }

    public void replaceDevice(int index, DeviceConfigData device) {
        DeviceConfigData previous = this.device.set(index, device);
        if (previous != null) {
            desindexar(previous);
        }
        indexar(device, index);
    }

    public void clear() {
        device.clear();
        indexByUUID.clear();
        variables.clear();
    }

    private void indexar(DeviceConfigData device) {
        indexar(device, this.device.size() - 1);
    }

    private void indexar(DeviceConfigData device, int position) {
        String uuid = device == null ? null : device.getUuid();
        if (uuid != null && !uuid.isBlank()) {
            indexByUUID.put(uuid, position);
        }
    }

    private void desindexar(DeviceConfigData device) {
        String uuid = device.getUuid();
        if (uuid != null && !uuid.isBlank()) {
            indexByUUID.remove(uuid);
        }
    }

    private void reindexar() {
        indexByUUID.clear();
        for (int i = 0; i < device.size(); i++) {
            indexar(device.get(i), i);
        }
    }
}
