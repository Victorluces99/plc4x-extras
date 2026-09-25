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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Properties;
import org.apache.plc4x.malbec.projecttype.panelcategory.panel.CommConfigData;
import org.apache.plc4x.malbec.projecttype.panelcategory.panel.DeviceConfigData;
import org.junit.jupiter.api.Test;
import org.plcopen.xml.tc60201.ProjectDocument;
import org.plcopen.xml.tc60201.ProjectDocument.Project.Instances.Configurations;
import org.plcopen.xml.tc60201.ProjectDocument.Project.Instances.Configurations.Configuration;

class DeviceXmlMapperTest {

    @Test
    void templateIsSchemaValid() throws Exception {
        assertTrue(ProjectDocument.Factory.parse(PlcOpenCommConfigRepositoryImpl.TEMPLATE)
                .validate());
    }

    @Test
    void roundTripDevice() throws Exception {
        ProjectDocument doc = ProjectDocument.Factory.parse(PlcOpenCommConfigRepositoryImpl.TEMPLATE);
        Configurations configurations = doc.getProject().getInstances().getConfigurations();

        Properties pDevice = new Properties();

        pDevice.put("brand", "Siemens");
        pDevice.put("model", "S7-1500");
        pDevice.put("protocol", "S7");
        pDevice.put("deviceName", "PLC_Uno");
        pDevice.put("description", "Control principal");
        pDevice.put("uuid", "uuid-1");
        pDevice.put("enable", true);
        pDevice.put("s88Node", "S88.1");
        pDevice.put("s88Uuid", "s88-uuid-1");
        pDevice.put("specificParameters", "ipa=10.0.0.1\nrack=0\nslot=1");

        DeviceConfigData device = new DeviceConfigData(pDevice);
//        DeviceConfigData device = new DeviceConfigData(
//                "Siemens", "S7-1500", "S7", "PLC_Uno",
//                "Control principal", "uuid-1", true,
//                "S88.1", "s88-uuid-1",
//                "ipa=10.0.0.1\nrack=0\nslot=1");

        DeviceXmlMapper.writeDevice(device, configurations);

        DeviceConfigData read = DeviceXmlMapper.readDevice(configurations.getConfigurationArray(0));

        assertNotNull(read);
        assertEquals(device.getBrand(), read.getBrand());
        assertEquals(device.getModel(), read.getModel());
        assertEquals(device.getProtocol(), read.getProtocol());
        assertEquals(device.getDeviceName(), read.getDeviceName());
        assertEquals(device.getDescription(), read.getDescription());
        assertEquals(device.getUuid(), read.getUuid());
        assertEquals(device.isEnabled(), read.isEnabled());
        assertEquals(device.getS88Node(), read.getS88Node());
        assertEquals(device.getS88Uuid(), read.getS88Uuid());
        assertEquals(device.getSpecificParameters(), read.getSpecificParameters());
    }

    @Test
    void escapeSpecialCharacters() throws Exception {
        ProjectDocument doc = ProjectDocument.Factory.parse(PlcOpenCommConfigRepositoryImpl.TEMPLATE);
        Configurations configurations = doc.getProject().getInstances().getConfigurations();
        Properties pDevice = new Properties();

        pDevice.put("brand", "Siemens & Co<\"XL\">");
        pDevice.put("model", "A 'B'");
        pDevice.put("protocol", "S7");
        pDevice.put("deviceName", "PLC & \"A\" <1>");
        pDevice.put("description", "line1\nline2 & more <now>");
        pDevice.put("uuid", "uuid-1");
        pDevice.put("enable", false);
        pDevice.put("s88Node", "");
        pDevice.put("s88Uuid", "");
        pDevice.put("specificParameters", "x=1&y=2");

        DeviceConfigData device = new DeviceConfigData(pDevice);

//        DeviceConfigData device = new DeviceConfigData(
//                "Siemens & Co<\"XL\">", "A 'B'", "S7", "PLC & \"A\" <1>",
//                "line1\nline2 & more <now>", "uuid-1", false,
//                "", "", "x=1&y=2");
        DeviceXmlMapper.writeDevice(device, configurations);

        DeviceConfigData read = DeviceXmlMapper.readDevice(configurations.getConfigurationArray(0));

        assertEquals(device.getDeviceName(), read.getDeviceName());
        assertEquals(device.getBrand(), read.getBrand());
        assertEquals(device.getModel(), read.getModel());
        assertEquals(device.getDescription(), read.getDescription());
        assertEquals(device.getSpecificParameters(), read.getSpecificParameters());
    }

    @Test
    void readDeviceReturnsNullWhenNoPayload() throws Exception {
        ProjectDocument doc = ProjectDocument.Factory.parse(PlcOpenCommConfigRepositoryImpl.TEMPLATE);
        assertTrue(doc.getProject().getInstances().getConfigurations().sizeOfConfigurationArray() == 0);
    }

    @Test
    void nullFieldsRoundTripAsEmptyString() throws Exception {
        ProjectDocument doc = ProjectDocument.Factory.parse(PlcOpenCommConfigRepositoryImpl.TEMPLATE);
        Configurations configurations = doc.getProject().getInstances().getConfigurations();

        Properties pDevice = new Properties();

        pDevice.put("deviceName", "PLC");
        pDevice.put("enable", false);

        DeviceConfigData device = new DeviceConfigData(pDevice);

//        DeviceConfigData device = new DeviceConfigData(
//                null, null, null, "PLC", null, null, false, null, null, null);
        DeviceXmlMapper.writeDevice(device, configurations);

        DeviceConfigData read = DeviceXmlMapper.readDevice(configurations.getConfigurationArray(0));

        assertEquals("PLC", read.getDeviceName());
        assertEquals("", read.getBrand());
        assertEquals("", read.getModel());
        assertEquals("", read.getDescription());
        assertEquals("", read.getUuid());
        assertEquals("", read.getS88Node());
        assertEquals("", read.getS88Uuid());
        assertEquals("", read.getSpecificParameters());
    }

    @Test
    void roundTripComms() throws Exception {
        ProjectDocument doc = ProjectDocument.Factory.parse(PlcOpenCommConfigRepositoryImpl.TEMPLATE);
        Configurations configurations = doc.getProject().getInstances().getConfigurations();
        Properties pDevice = new Properties();

        pDevice.put("brand", "Siemens");
        pDevice.put("model", "S7-1500");
        pDevice.put("protocol", "S7");
        pDevice.put("deviceName", "PLC_Uno");
        pDevice.put("description", "Control principal");
        pDevice.put("uuid", "uuid-1");
        pDevice.put("enable", true);
        pDevice.put("s88Node", "S88.1");
        pDevice.put("s88Uuid", "s88-uuid-1");
        pDevice.put("specificParameters", "");

        DeviceConfigData device = new DeviceConfigData(pDevice);

//        DeviceConfigData device = new DeviceConfigData(
//                "Siemens", "S7-1500", "S7", "PLC_Uno",
//                "Control principal", "uuid-1", true, "S88.1", "s88-uuid-1", "");
        Configuration configuration = DeviceXmlMapper.writeDevice(device, configurations);

        CommConfigData comms = new CommConfigData("PLC_Uno",
                List.of(new CommConfigData.GroupConfig("g1", "Grupo1", "Grupo 1", "500", true, "md5g")),
                List.of(new CommConfigData.ItemConfig("i1", "Item1", "Item 1", "tag1", true, "md5i")),
                List.of(new CommConfigData.PvConfig("p1", "PV1", "INT", "pv-id", "0", "descriptor",
                        "500", true, true, "-100", "100", "PV uno", "%.2f", "gpm",
                        "-50", "50", "0.1", "md5p")));

        DeviceXmlMapper.writeComms(comms, configuration);

        CommConfigData read = DeviceXmlMapper.readComms(configuration);

        assertNotNull(read);
        assertEquals("PLC_Uno", read.getDeviceName());
        assertEquals(1, read.getGroups().size());
        CommConfigData.GroupConfig g = read.getGroups().get(0);
        assertEquals("g1", g.getUuid());
        assertEquals("Grupo1", g.getName());
        assertEquals("Grupo 1", g.getDescription());
        assertEquals("500", g.getScantime());
        assertEquals(true, g.isEnable());
        assertEquals("md5g", g.getMd5());
        assertEquals(1, read.getItems().size());
        CommConfigData.ItemConfig i = read.getItems().get(0);
        assertEquals("i1", i.getUuid());
        assertEquals("Item1", i.getName());
        assertEquals("Item 1", i.getDescription());
        assertEquals("tag1", i.getTag());
        assertEquals(true, i.isEnable());
        assertEquals(1, read.getPvs().size());
        CommConfigData.PvConfig p = read.getPvs().get(0);
        assertEquals("p1", p.getUuid());
        assertEquals("PV1", p.getName());
        assertEquals("INT", p.getType());
        assertEquals("pv-id", p.getId());
        assertEquals("0", p.getOffset());
        assertEquals("descriptor", p.getDescriptor());
        assertEquals("500", p.getScanTime());
        assertEquals(true, p.isScanEnable());
        assertEquals(true, p.isWriteEnable());
        assertEquals("-100", p.getDisplayLimitLow());
        assertEquals("100", p.getDisplayLimitHigh());
        assertEquals("PV uno", p.getDisplayDescription());
        assertEquals("%.2f", p.getDisplayFormat());
        assertEquals("gpm", p.getDisplayUnits());
        assertEquals("-50", p.getControlLimitLow());
        assertEquals("50", p.getControlLimitHigh());
        assertEquals("0.1", p.getControlMinStep());
        assertEquals("md5p", p.getMd5());
    }

    @Test
    void readCommsReturnsNullWhenNoCommsBlock() throws Exception {
        ProjectDocument doc = ProjectDocument.Factory.parse(PlcOpenCommConfigRepositoryImpl.TEMPLATE);
        Configurations configurations = doc.getProject().getInstances().getConfigurations();
        Properties pDevice = new Properties();

        pDevice.put("brand", "Siemens");
        pDevice.put("model", "S7-1500");
        pDevice.put("protocol", "S7");
        pDevice.put("deviceName", "PLC_Uno");
        pDevice.put("description", "Control principal");
        pDevice.put("uuid", "uuid-1");
        pDevice.put("enable", true);
        pDevice.put("s88Node", "S88.1");
        pDevice.put("s88Uuid", "s88-uuid-1");
        pDevice.put("specificParameters", "");

        DeviceConfigData device = new DeviceConfigData(pDevice);
//        DeviceConfigData device = new DeviceConfigData(
//                "Siemens", "S7-1500", "S7", "PLC_Uno",
//                "Control principal", "uuid-1", true, "S88.1", "s88-uuid-1", "");

        DeviceXmlMapper.writeDevice(device, configurations);

        assertNull(DeviceXmlMapper.readComms(configurations.getConfigurationArray(0)));
    }
}
