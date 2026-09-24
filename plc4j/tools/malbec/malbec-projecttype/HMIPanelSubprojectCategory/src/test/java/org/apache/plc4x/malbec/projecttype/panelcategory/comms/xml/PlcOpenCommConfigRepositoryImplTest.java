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
package org.apache.plc4x.malbec.projecttype.panelcategory.comms.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.plc4x.malbec.projecttype.panelcategory.comms.api.CommConfigRepository;
import org.apache.plc4x.malbec.projecttype.panelcategory.comms.api.CommunicationsConfig;
import org.apache.plc4x.malbec.projecttype.panelcategory.panel.DeviceConfigData;
import org.junit.jupiter.api.Test;
import org.plcopen.xml.tc60201.ProjectDocument;

class PlcOpenCommConfigRepositoryImplTest {

    @Test
    void loadEmptyStorageReturnsEmptyConfig() throws Exception {
        InMemoryStorage storage = new InMemoryStorage();
        CommConfigRepository repo = new PlcOpenCommConfigRepositoryImpl(storage);

        CommunicationsConfig config = repo.load();

        assertEquals(0, config.getDevices().size());
    }

    @Test
    void saveAndLoadRoundTrip() throws Exception {
        InMemoryStorage storage = new InMemoryStorage();
        CommConfigRepository repo = new PlcOpenCommConfigRepositoryImpl(storage);

        CommunicationsConfig config = new CommunicationsConfig();
        config.addDevice(device("uuid-1", "PLC_Uno"));
        config.addDevice(device("uuid-2", "VFD_Bomba"));
        repo.save(config);

        CommunicationsConfig loaded = repo.load();

        assertEquals(2, loaded.getDevices().size());
        assertDevice(loaded.getDevices().get(0), "Siemens", "S7-1500", "S7", "PLC_Uno",
                "Control principal", "uuid-1", true, "S88.1", "s88-uuid-1", "ipa=10.0.0.1");
        assertDevice(loaded.getDevices().get(1), "Siemens", "S7-1500", "S7", "VFD_Bomba",
                "Control principal", "uuid-2", true, "S88.1", "s88-uuid-1", "ipa=10.0.0.1");
    }

    @Test
    void savedXmlIsSchemaValid() throws Exception {
        InMemoryStorage storage = new InMemoryStorage();
        CommConfigRepository repo = new PlcOpenCommConfigRepositoryImpl(storage);

        CommunicationsConfig config = new CommunicationsConfig();
        config.addDevice(device("uuid-1", "PLC_Uno"));
        repo.save(config);

        String saved = storage.text();
        assertTrue(saved.contains(MalbecNamespaces.DEVICE_DATA_NAME));
        assertTrue(ProjectDocument.Factory.parse(saved).validate());
    }

    @Test
    void saveReplacesPreviousDevices() throws Exception {
        InMemoryStorage storage = new InMemoryStorage();
        CommConfigRepository repo = new PlcOpenCommConfigRepositoryImpl(storage);

        CommunicationsConfig first = new CommunicationsConfig();
        first.addDevice(device("uuid-1", "PLC_Uno"));
        repo.save(first);

        CommunicationsConfig second = new CommunicationsConfig();
        second.addDevice(device("uuid-2", "VFD_Bomba"));
        repo.save(second);

        CommunicationsConfig loaded = repo.load();
        assertEquals(1, loaded.getDevices().size());
        assertEquals("VFD_Bomba", loaded.getDevices().get(0).getDeviceName());
    }

    private static DeviceConfigData device(String uuid, String deviceName) {
        return new DeviceConfigData("Siemens", "S7-1500", "S7", deviceName,
                "Control principal", uuid, true, "S88.1", "s88-uuid-1", "ipa=10.0.0.1");
    }

    private static void assertDevice(DeviceConfigData actual, String brand, String model,
                                     String protocol, String deviceName, String description,
                                     String uuid, boolean enabled, String s88Node,
                                     String s88Uuid, String specificParameters) {
        assertEquals(brand, actual.getBrand());
        assertEquals(model, actual.getModel());
        assertEquals(protocol, actual.getProtocol());
        assertEquals(deviceName, actual.getDeviceName());
        assertEquals(description, actual.getDescription());
        assertEquals(uuid, actual.getUuid());
        assertEquals(enabled, actual.isEnabled());
        assertEquals(s88Node, actual.getS88Node());
        assertEquals(s88Uuid, actual.getS88Uuid());
        assertEquals(specificParameters, actual.getSpecificParameters());
    }
}