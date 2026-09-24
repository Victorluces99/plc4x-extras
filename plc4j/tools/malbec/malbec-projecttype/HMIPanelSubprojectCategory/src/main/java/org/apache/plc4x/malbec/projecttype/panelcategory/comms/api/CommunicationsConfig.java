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
import java.util.List;
import org.apache.plc4x.malbec.projecttype.panelcategory.panel.DeviceConfigData;

public final class CommunicationsConfig {

    private final List<DeviceConfigData> devices;

    public CommunicationsConfig() {
        this.devices = new ArrayList<>();
    }

    public CommunicationsConfig(List<DeviceConfigData> devices) {
        this.devices = devices == null ? new ArrayList<>() : new ArrayList<>(devices);
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

    public void clear() {
        devices.clear();
    }
}