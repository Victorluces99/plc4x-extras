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
package org.apache.plc4x.malbec.projecttype.panelcategory.comms.plcopen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;
import org.apache.plc4x.malbec.projecttype.panelcategory.comms.api.CommConfigRepository;
import org.apache.plc4x.malbec.projecttype.panelcategory.comms.api.CommunicationsConfig;
import org.apache.plc4x.malbec.projecttype.panelcategory.model.DeviceConfigData;
import org.apache.plc4x.malbec.projecttype.panelcategory.model.PvConfigData;
import org.junit.jupiter.api.Test;
import org.plcopen.xml.tc60201.ProjectDocument;
import org.plcopen.xml.tc60201.ProjectDocument.Project.Instances.Configurations;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

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
        config.addDevice(device("uuid-1", "PLC_One"));
        config.addDevice(device("uuid-2", "VFD_Pump"));
        repo.save(config);

        CommunicationsConfig loaded = repo.load();

        assertEquals(2, loaded.getDevices().size());
        assertDevice(loaded.getDevices().get(0), deviceProps("uuid-1", "PLC_One"));
        assertDevice(loaded.getDevices().get(1), deviceProps("uuid-2", "VFD_Pump"));
    }

    @Test
    void savedXmlIsSchemaValid() throws Exception {
        InMemoryStorage storage = new InMemoryStorage();
        CommConfigRepository repo = new PlcOpenCommConfigRepositoryImpl(storage);

        CommunicationsConfig config = new CommunicationsConfig();
        config.addDevice(device("uuid-1", "PLC_One"));
        repo.save(config);

        String saved = storage.text();
        assertTrue(saved.contains(CommsXmlIdentifiers.DEVICE_DATA_NAME));
        assertTrue(ProjectDocument.Factory.parse(saved).validate());
    }

    @Test
    void saveReplacesPreviousDevices() throws Exception {
        InMemoryStorage storage = new InMemoryStorage();
        CommConfigRepository repo = new PlcOpenCommConfigRepositoryImpl(storage);

        CommunicationsConfig first = new CommunicationsConfig();
        first.addDevice(device("uuid-1", "PLC_One"));
        repo.save(first);

        CommunicationsConfig second = new CommunicationsConfig();
        second.addDevice(device("uuid-2", "VFD_Pump"));
        repo.save(second);

        CommunicationsConfig loaded = repo.load();
        assertEquals(1, loaded.getDevices().size());
        assertEquals("VFD_Pump", loaded.getDevices().get(0).getDeviceName());
    }

    @Test
    void loadPopulatesVariables() throws Exception {
        InMemoryStorage storage = new InMemoryStorage();
        ByteArrayOutputStream xml = new ByteArrayOutputStream();
        DynamicVariableXmlMapper.write(xml,
                List.of(device("uuid-1", "PLC_One")),
                List.of(pv("pv-1", "PLC_One")));
        storage.write(xml.toByteArray());

        CommunicationsConfig loaded = new PlcOpenCommConfigRepositoryImpl(storage).load();

        assertEquals(1, loaded.getVariables("uuid-1").size());
        assertEquals("PV1", loaded.getVariables("uuid-1").get(0).getName());
    }

    @Test
    void saveKeepsVariablesWhenTheDeviceIsRenamed() throws Exception {
        InMemoryStorage storage = new InMemoryStorage();
        ByteArrayOutputStream xml = new ByteArrayOutputStream();
        DynamicVariableXmlMapper.write(xml,
                List.of(device("uuid-1", "PLC_One")),
                List.of(pv("pv-1", "PLC_One")));
        storage.write(xml.toByteArray());

        CommConfigRepository repo = new PlcOpenCommConfigRepositoryImpl(storage);
        CommunicationsConfig config = repo.load();
        config.upsertDevice(device("uuid-1", "PLC_Renamed"));
        repo.save(config);

        CommunicationsConfig loaded = repo.load();
        assertEquals(1, loaded.getVariables("uuid-1").size(),
                "the variables must be preserved when the device is renamed");
        assertEquals("PLC_Renamed", loaded.getVariables("uuid-1").get(0).getDeviceName(),
                "the variable must point to the new device name");
    }

    @Test
    void loadAllowsDuplicatePvNames() throws Exception {
        InMemoryStorage storage = new InMemoryStorage();
        storage.write(xmlWithDuplicatePvNames().getBytes(StandardCharsets.UTF_8));

        CommunicationsConfig loaded = new PlcOpenCommConfigRepositoryImpl(storage).load();

        assertEquals(2, loaded.getVariables("uuid-1").size());
    }

    @Test
    void saveDocumentRejectsDuplicatePvNames() throws Exception {
        ProjectDocument document = PlcOpenCommConfigRepositoryImpl.parseDocument(
                xmlWithDuplicatePvNames());

        assertThrows(IllegalStateException.class,
                () -> PlcOpenCommConfigRepositoryImpl.saveDocument(
                        document, new ByteArrayOutputStream()));
    }

    @Test
    void saveRejectsDuplicatePvNamesAlreadyPresentInTheXml() throws Exception {
        InMemoryStorage storage = new InMemoryStorage();
        storage.write(xmlWithDuplicatePvNames().getBytes(StandardCharsets.UTF_8));
        CommConfigRepository repo = new PlcOpenCommConfigRepositoryImpl(storage);
        CommunicationsConfig config = repo.load();

        assertThrows(IllegalStateException.class, () -> repo.save(config));
    }

    @Test
    void writeRejectsDuplicatePvNames() {
        assertThrows(IllegalStateException.class,
                () -> DynamicVariableXmlMapper.write(
                        new ByteArrayOutputStream(),
                        List.of(device("uuid-1", "PLC_One")),
                        List.of(pv("pv-1", "PLC_One"), pv("pv-2", "PLC_One"))));
    }

    @Test
    void configurationRoundTrips() throws Exception {
        InMemoryStorage storage = new InMemoryStorage();
        CommConfigRepository repo = new PlcOpenCommConfigRepositoryImpl(storage);

        CommunicationsConfig config = new CommunicationsConfig();
        config.addDevice(device("uuid-1", "PLC_One"));
        repo.save(config);

        String saved = storage.text();
        assertFalse(saved.contains("http://apache.org/plc4x/malbec/comms"));
        assertTrue(ProjectDocument.Factory.parse(saved).validate());

        CommunicationsConfig loaded = repo.load();
        assertEquals(1, loaded.getDevices().size());
    }

    @Test
    void loadRejectsDeviceWithoutRequiredDeviceUrl() throws Exception {
        InMemoryStorage storage = new InMemoryStorage();
        storage.write(xmlWithPv()
                .replace(" deviceUrl=\"ipa=10.0.0.1\"", "")
                .getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalStateException.class,
                () -> new PlcOpenCommConfigRepositoryImpl(storage).load(),
                "The Malbec XSD must require deviceUrl");
    }

    @Test
    void loadRejectsDeviceWithoutRequiredDeviceKey() throws Exception {
        InMemoryStorage storage = new InMemoryStorage();
        storage.write(xmlWithPv()
                .replace(" deviceKey=\"key-uuid-1\"", "")
                .getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalStateException.class,
                () -> new PlcOpenCommConfigRepositoryImpl(storage).load(),
                "The Malbec XSD must require deviceKey");
    }

    @Test
    void loadRejectsInvalidPvrecordBoolean() throws Exception {
        InMemoryStorage storage = new InMemoryStorage();
        storage.write(xmlWithPv()
                .replace("scanEnable=\"true\"", "scanEnable=\"yes\"")
                .getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalStateException.class,
                () -> new PlcOpenCommConfigRepositoryImpl(storage).load(),
                "The Malbec XSD must validate pvrecord booleans");
    }

    @Test
    void loadRejectsEmptyRequiredDeviceField() throws Exception {
        InMemoryStorage storage = new InMemoryStorage();
        storage.write(xmlWithPv()
                .replace("deviceKey=\"key-uuid-1\"", "deviceKey=\"\"")
                .getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalStateException.class,
                () -> new PlcOpenCommConfigRepositoryImpl(storage).load(),
                "The Malbec XSD must reject an empty deviceKey");
    }

    @Test
    void loadRejectsEmptyRequiredPvField() throws Exception {
        InMemoryStorage storage = new InMemoryStorage();
        storage.write(xmlWithPv()
                .replace("name=\"PV1\"", "name=\"\"")
                .getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalStateException.class,
                () -> new PlcOpenCommConfigRepositoryImpl(storage).load(),
                "The Malbec XSD must reject an empty pvrecord name");
    }

    @Test
    void loadRejectsUnknownDeviceAttribute() throws Exception {
        InMemoryStorage storage = new InMemoryStorage();
        storage.write(xmlWithPv()
                .replace("deviceKey=\"key-uuid-1\"",
                        "deviceKey=\"key-uuid-1\" unexpected=\"value\"")
                .getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalStateException.class,
                () -> new PlcOpenCommConfigRepositoryImpl(storage).load(),
                "The Malbec XSD must reject undeclared attributes");
    }

    @Test
    void loadRejectsExtraDataElement() throws Exception {
        InMemoryStorage storage = new InMemoryStorage();
        storage.write(xmlWithPv()
                .replace("</addData>", """
                        <data name="http://apache.org/plc4x/malbec/device"
                              handleUnknown="preserve"/>
                        </addData>""")
                .getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalStateException.class,
                () -> new PlcOpenCommConfigRepositoryImpl(storage).load(),
                "PLCopen allows extra data; structural validation must reject it");
    }

    @Test
    void loadRejectsLegacyCommsData() throws Exception {
        InMemoryStorage storage = new InMemoryStorage();
        storage.write(xmlWithPv()
                .replace("</addData>", """
                        <data name="http://apache.org/plc4x/malbec/comms"
                              handleUnknown="preserve">
                          <comm xmlns="http://apache.org/plc4x/malbec/comms">
                            <pvs/>
                          </comm>
                        </data>
                        </addData>""")
                .getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalStateException.class,
                () -> new PlcOpenCommConfigRepositoryImpl(storage).load(),
                "The legacy comms-data-name format must no longer be accepted");
    }

    @Test
    void loadRejectsConfigurationWithoutDevice() throws Exception {
        InMemoryStorage storage = new InMemoryStorage();
        storage.write(xmlWithPv()
                .replace("</configurations>", """
                        <configuration name="without-device">
                          <addData/>
                        </configuration>
                        </configurations>""")
                .getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalStateException.class,
                () -> new PlcOpenCommConfigRepositoryImpl(storage).load(),
                "Each configuration must contain a Malbec device");
    }

    @Test
    void saveDocumentRejectsInvalidMalbecDevice() throws Exception {
        ProjectDocument document = ProjectDocument.Factory.parse(
                PlcOpenCommConfigRepositoryImpl.TEMPLATE);
        Configurations configurations = document.getProject()
                .getInstances().getConfigurations();
        DynamicVariableXmlMapper.writeDevice(device("uuid-1", "PLC_One"), configurations);
        NodeList devices = ((Document) document.getDomNode())
                .getElementsByTagNameNS(CommsXmlIdentifiers.DEVICE_NS, "device");
        assertEquals(1, devices.getLength());
        ((Element) devices.item(0)).removeAttribute("deviceUrl");

        assertThrows(IllegalStateException.class,
                () -> PlcOpenCommConfigRepositoryImpl.saveDocument(
                        document, new ByteArrayOutputStream()),
                "Saving must reject an invalid device before writing");
    }

    private static String xmlWithPv() throws Exception {
        ByteArrayOutputStream xml = new ByteArrayOutputStream();
        DynamicVariableXmlMapper.write(xml,
                List.of(device("uuid-1", "PLC_One")),
                List.of(pv("pv-1", "PLC_One")));
        return new String(xml.toByteArray(), StandardCharsets.UTF_8);
    }

    private static String xmlWithDuplicatePvNames() throws Exception {
        String xml = xmlWithPv();
        int start = xml.indexOf("<pvrecord ");
        int end = xml.indexOf("/>", start) + 2;
        String duplicate = xml.substring(start, end)
                .replace("uuid=\"pv-1\"", "uuid=\"pv-2\"");
        return xml.replace("</pvrecords>", duplicate + "</pvrecords>");
    }

    private static Properties deviceProps(String uuid, String deviceName) {
        Properties pDevice = new Properties();

        pDevice.put("brand", "Siemens");
        pDevice.put("model", "S7-1500");
        pDevice.put("protocol", "S7");
        pDevice.put("deviceName", deviceName);
        pDevice.put("deviceKey", "key-" + uuid);
        pDevice.put("description", "Main control");
        pDevice.put("uuid", uuid);
        pDevice.put("enable", true);
        pDevice.put("s88Node", "S88.1");
        pDevice.put("s88Uuid", "s88-uuid-1");
        pDevice.put("specificParameters", "ipa=10.0.0.1");

        return pDevice;
    }

    private static DeviceConfigData device(String uuid, String deviceName) {
        return new DeviceConfigData(deviceProps(uuid, deviceName));
    }

    private static PvConfigData pv(String uuid, String deviceName) {
        return new PvConfigData(uuid, "PV1", "INT", deviceName, "%DB1.DBB0:INT", "d",
                "500", true, false, "0", "10", "d", "%.0f", "u", "0", "10", "1", "m");
    }

    private static void assertDevice(DeviceConfigData actual, Properties pdevice) {
        assertEquals(pdevice.getProperty("brand"), actual.getBrand());
        assertEquals(pdevice.getProperty("model"), actual.getModel());
        assertEquals(pdevice.getProperty("protocol"), actual.getProtocol());
        assertEquals(pdevice.getProperty("deviceName"), actual.getDeviceName());
        assertEquals(pdevice.getProperty("deviceKey"), actual.getDeviceKey());
        assertEquals(pdevice.getProperty("description"), actual.getDescription());
        assertEquals(pdevice.getProperty("uuid"), actual.getUuid());
        Object enableValue = pdevice.get("enable");
        assertEquals(enableValue != null && Boolean.parseBoolean(String.valueOf(enableValue)), actual.isEnabled());
        assertEquals(pdevice.getProperty("s88Node"), actual.getS88Node());
        assertEquals(pdevice.getProperty("s88Uuid"), actual.getS88Uuid());
        assertEquals(pdevice.getProperty("specificParameters"), actual.getSpecificParameters());
    }
}
