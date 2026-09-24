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
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.plc4x.malbec.projecttype.panelcategory.panel.DeviceConfigData;
import org.apache.xmlbeans.XmlObject;
import org.junit.jupiter.api.Test;
import org.plcopen.xml.tc60201.ProjectDocument;

class DeviceXmlMapperTest {

    @Test
    void templateIsSchemaValid() throws Exception {
        assertTrue(ProjectDocument.Factory.parse(PlcOpenCommConfigRepositoryImpl.TEMPLATE)
                .validate());
    }

    @Test
    void roundTripDevice() throws Exception {
        ProjectDocument doc = ProjectDocument.Factory.parse(PlcOpenCommConfigRepositoryImpl.TEMPLATE);
        XmlObject configurations = doc.selectPath(".//*[local-name()='configurations']")[0];

        DeviceConfigData device = new DeviceConfigData(
                "Siemens", "S7-1500", "S7", "PLC_Uno",
                "Control principal", "uuid-1", true,
                "S88.1", "s88-uuid-1",
                "ipa=10.0.0.1\nrack=0\nslot=1");

        DeviceXmlMapper.writeDevice(device, configurations);

        DeviceConfigData read = DeviceXmlMapper.readDevice(
                doc.selectPath(".//*[local-name()='configuration']")[0]);

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
        XmlObject configurations = doc.selectPath(".//*[local-name()='configurations']")[0];

        DeviceConfigData device = new DeviceConfigData(
                "Siemens & Co<\"XL\">", "A 'B'", "S7", "PLC & \"A\" <1>",
                "line1\nline2 & more <now>", "uuid-1", false,
                "", "", "x=1&y=2");

        DeviceXmlMapper.writeDevice(device, configurations);

        DeviceConfigData read = DeviceXmlMapper.readDevice(
                doc.selectPath(".//*[local-name()='configuration']")[0]);

        assertEquals(device.getDeviceName(), read.getDeviceName());
        assertEquals(device.getBrand(), read.getBrand());
        assertEquals(device.getModel(), read.getModel());
        assertEquals(device.getDescription(), read.getDescription());
        assertEquals(device.getSpecificParameters(), read.getSpecificParameters());
    }

    @Test
    void readDeviceReturnsNullWhenNoPayload() throws Exception {
        ProjectDocument doc = ProjectDocument.Factory.parse(PlcOpenCommConfigRepositoryImpl.TEMPLATE);
        assertTrue(doc.selectPath(".//*[local-name()='configuration']").length == 0);
    }

    @Test
    void nullFieldsRoundTripAsEmptyString() throws Exception {
        ProjectDocument doc = ProjectDocument.Factory.parse(PlcOpenCommConfigRepositoryImpl.TEMPLATE);
        XmlObject configurations = doc.selectPath(".//*[local-name()='configurations']")[0];

        DeviceConfigData device = new DeviceConfigData(
                null, null, null, "PLC", null, null, false, null, null, null);

        DeviceXmlMapper.writeDevice(device, configurations);

        DeviceConfigData read = DeviceXmlMapper.readDevice(
                doc.selectPath(".//*[local-name()='configuration']")[0]);

        assertEquals("PLC", read.getDeviceName());
        assertEquals("", read.getBrand());
        assertEquals("", read.getModel());
        assertEquals("", read.getDescription());
        assertEquals("", read.getUuid());
        assertEquals("", read.getS88Node());
        assertEquals("", read.getS88Uuid());
        assertEquals("", read.getSpecificParameters());
    }
}