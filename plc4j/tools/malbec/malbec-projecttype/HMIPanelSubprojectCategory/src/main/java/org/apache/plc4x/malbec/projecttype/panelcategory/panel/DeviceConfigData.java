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
package org.apache.plc4x.malbec.projecttype.panelcategory.panel;

public class DeviceConfigData {
    private final String brand;
    private final String model;
    private final String protocol;
    private final String deviceName;
    private final String description;
    private final String uuid;
    private final boolean enabled;
    private final String s88Node;
    private final String s88Uuid;
    // Parámetros dinámicos específicos del PLC (IP, Rack, Slot, Path, etc.)
    private final String specificParameters; 

    public DeviceConfigData(String brand, String model, String protocol, String deviceName, 
                            String description, String uuid, boolean enabled, 
                            String s88Node, String s88Uuid, String specificParameters) {
        this.brand = brand;
        this.model = model;
        this.protocol = protocol;
        this.deviceName = deviceName;
        this.description = description;
        this.uuid = uuid;
        this.enabled = enabled;
        this.s88Node = s88Node;
        this.s88Uuid = s88Uuid;
        this.specificParameters = specificParameters;
    }

    // Getters
    public String getBrand() { return brand; }
    public String getModel() { return model; }
    public String getProtocol() { return protocol; }
    public String getDeviceName() { return deviceName; }
    public String getDescription() { return description; }
    public String getUuid() { return uuid; }
    public boolean isEnabled() { return enabled; }
    public String getS88Node() { return s88Node; }
    public String getS88Uuid() { return s88Uuid; }
    public String getSpecificParameters() { return specificParameters; }

    /**
     * Convierte toda la configuración a un formato de texto (p. ej. JSON o Properties)
     * para escribirlo en el archivo del proyecto.
     */
    public String toFileContent() {
        StringBuilder sb = new StringBuilder();
        sb.append("DeviceName=").append(deviceName).append("\n");
        sb.append("Brand=").append(brand).append("\n");
        sb.append("Model=").append(model).append("\n");
        sb.append("Protocol=").append(protocol).append("\n");
        sb.append("Description=").append(description).append("\n");
        sb.append("UUID=").append(uuid).append("\n");
        sb.append("Enabled=").append(enabled).append("\n");
        sb.append("S88Node=").append(s88Node).append("\n");
        sb.append("S88UUID=").append(s88Uuid).append("\n");
        sb.append("--- Specific Parameters ---\n");
        sb.append(specificParameters);
        return sb.toString();
    }
}
