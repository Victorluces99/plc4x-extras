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
import org.apache.plc4x.malbec.projecttype.panelcategory.panel.CommConfigData;
import org.apache.plc4x.malbec.projecttype.panelcategory.panel.DeviceConfigData;

public final class CommunicationsConfig {

    private final List<DeviceConfigData> devices;
    private final Map<String, CommConfigData> comms;

    public CommunicationsConfig() {
        this.devices = new ArrayList<>();
        this.comms = new HashMap<>();
    }

    public CommunicationsConfig(List<DeviceConfigData> devices) {
        this.devices = devices == null ? new ArrayList<>() : new ArrayList<>(devices);
        this.comms = new HashMap<>();
    }

    public List<DeviceConfigData> getDevices() {
        return Collections.unmodifiableList(devices);
    }

    public void addDevice(DeviceConfigData device) {
        devices.add(device);
    }

    public void upsertDevice(DeviceConfigData device) {
        for (int i = 0; i < devices.size(); i++) {
            if (devices.get(i).getUuid() != null && devices.get(i).getUuid().equals(device.getUuid())) {
                devices.set(i, device);
                return;
            }
        }
        devices.add(device);
    }

    public DeviceConfigData findByUuid(String uuid) {
        for (DeviceConfigData device : devices) {
            if (device.getUuid() != null && device.getUuid().equals(uuid)) {
                return device;
            }
        }
        return null;
    }

    public boolean removeDevice(String uuid) {
        for (int i = 0; i < devices.size(); i++) {
            if (devices.get(i).getUuid() != null && devices.get(i).getUuid().equals(uuid)) {
                devices.remove(i);
                comms.remove(uuid);
                return true;
            }
        }
        return false;
    }

    public Map<String, CommConfigData> getComms() {
        return Collections.unmodifiableMap(comms);
    }

    public CommConfigData getComms(String deviceUuid) {
        return comms.get(deviceUuid);
    }

    public void upsertComms(String deviceUuid, CommConfigData commConfig) {
        if (commConfig == null) {
            comms.remove(deviceUuid);
        } else {
            comms.put(deviceUuid, commConfig);
        }
    }

    public void removeComms(String deviceUuid) {
        comms.remove(deviceUuid);
    }

    public void clear() {
        devices.clear();
        comms.clear();
    }
}