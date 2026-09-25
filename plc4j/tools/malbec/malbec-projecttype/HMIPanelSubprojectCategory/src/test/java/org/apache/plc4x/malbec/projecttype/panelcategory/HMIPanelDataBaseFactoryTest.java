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
package org.apache.plc4x.malbec.projecttype.panelcategory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;
import org.apache.plc4x.malbec.projecttype.panelcategory.panel.CommConfigData;
import org.apache.plc4x.malbec.projecttype.panelcategory.panel.DeviceConfigData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Verifica el contrato PvId: PvRecords.PvId guarda el uuid del Item (área) y
 * es la única relación PV -&gt; item -&gt; {group, device} a través de la tabla Items.
 */
class HMIPanelDataBaseFactoryTest {

    @TempDir
    Path tempDir;

    @Test
    void pvIdIsTheOnlyLinkBetweenPvAndItem() throws Exception {
        File dbFolder = tempDir.toFile();
        String deviceUuid = "uuid-1";
        String groupUuid = "g1";
        String itemUuid = "i1";
        String pvUuid = "p1";

        HMIPanelDataBaseFactory.createDB(dbFolder.getAbsolutePath());
        HMIPanelDataBaseFactory.insertDeviceConfig(
                dbFolder.getAbsolutePath(),
                device(deviceUuid, "PLC_Uno"),
                comms(groupUuid, itemUuid, pvUuid));

        File dbFile = new File(dbFolder, "boot.db");
        assertTrue(dbFile.isFile(), "boot.db debe haberse creado");

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
             Statement stmt = conn.createStatement()) {
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT PvUuId, PvId FROM PvRecords WHERE PvUuId = '" + pvUuid + "'")) {
                assertTrue(rs.next(), "La PV debe estar en PvRecords");
                assertEquals(itemUuid, rs.getString("PvId"),
                        "PvId debe contener el uuid del item (área)");
            }
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT ItemUuid, DeviceUuid, GroupUuid FROM Items WHERE ItemUuid = '" + itemUuid + "'")) {
                assertTrue(rs.next(), "El item debe estar en Items");
                assertEquals(itemUuid, rs.getString("ItemUuid"));
                assertEquals(deviceUuid, rs.getString("DeviceUuid"),
                        "El item debe quedar ligado al device");
                assertEquals(groupUuid, rs.getString("GroupUuid"),
                        "El item debe quedar ligado a su grupo de escaneo");
            }
        }
    }

    private static CommConfigData comms(String groupUuid, String itemUuid, String pvUuid) {
        return new CommConfigData("PLC_Uno",
                List.of(new CommConfigData.GroupConfig(groupUuid, "Grupo1", "Grupo 1", "500", true, "md5g")),
                List.of(new CommConfigData.ItemConfig(itemUuid, "Item1", "Item 1", "tag1", false, "md5i")),
                List.of(new CommConfigData.PvConfig(pvUuid, "PV1", "INT", itemUuid, "0", "descr",
                        "500", true, false, "-100", "100", "desc", "%.2f", "gpm",
                        "-50", "50", "0.1", "md5p")));
    }

    private static DeviceConfigData device(String uuid, String deviceName) {
        Properties pDevice = new Properties();
        pDevice.put("brand", "Siemens");
        pDevice.put("model", "S7-1500");
        pDevice.put("protocol", "S7");
        pDevice.put("deviceName", deviceName);
        pDevice.put("deviceKey", "key-" + uuid);
        pDevice.put("description", "Control principal");
        pDevice.put("uuid", uuid);
        pDevice.put("enable", true);
        pDevice.put("s88Node", "S88.1");
        pDevice.put("s88Uuid", "s88-uuid-1");
        pDevice.put("specificParameters", "ipa=10.0.0.1");
        return new DeviceConfigData(pDevice);
    }
}