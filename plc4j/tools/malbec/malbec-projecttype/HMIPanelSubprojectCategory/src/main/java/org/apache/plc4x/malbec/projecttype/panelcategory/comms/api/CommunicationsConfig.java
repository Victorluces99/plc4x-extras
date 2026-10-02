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

    /**
     * Agrega o reemplaza un dispositivo por su UUID.
     *
     * <p>El UUID es la identidad: sin él no hay forma de saber cuál de los
     * dispositivos guardados se está modificando. El alta lo valida y la carga
     * del modelo lo completa, así que un UUID vacío acá significa que algo se
     * rompió aguas arriba. No se compensa inventando una clave de reserva porque
     * dos altas con la misma clave dejarían de ser dos dispositivos: se avisa
     * en consola y se agrega igual, que es lo que pasaba antes y lo que dejó
     * duplicados sin que nadie lo notara.
     */
    public void upsertDevice(DeviceConfigData device) {
        if (device.getUuid() == null || device.getUuid().isBlank()) {
            System.err.println("upsertDevice: el dispositivo '" + device.getDeviceName()
                    + "' llegó sin UUID. Se agrega como dispositivo nuevo y no se va a poder"
                    + " modificar más que una vez: conviene revisarlo.");
            devices.add(device);
            return;
        }
        for (int i = 0; i < devices.size(); i++) {
            if (devices.get(i).getUuid() != null && devices.get(i).getUuid().equals(device.getUuid())) {
                devices.set(i, device);
                return;
            }
        }
        devices.add(device);
    }

    /**
     * Reemplaza el dispositivo de la posición dada.
     *
     * <p>Existe para la reparación de UUID de la carga, que no puede usar
     * {@link #addDevice} ni {@link #upsertDevice} porque el dispositivo a
     * corregir no tiene UUID con el que matchear.
     *
     * @throws IndexOutOfBoundsException si la posición no existe
     */
    public void replaceDevice(int index, DeviceConfigData device) {
        devices.set(index, device);
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