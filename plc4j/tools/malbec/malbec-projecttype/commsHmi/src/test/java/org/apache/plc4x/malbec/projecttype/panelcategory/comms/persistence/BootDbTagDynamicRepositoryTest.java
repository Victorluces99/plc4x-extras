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
package org.apache.plc4x.malbec.projecttype.panelcategory.comms.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Stream;
import org.apache.plc4x.malbec.projecttype.panelcategory.model.DeviceConfigData;
import org.apache.plc4x.malbec.projecttype.panelcategory.model.PvConfigData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BootDbTagDynamicRepositoryTest {

    @Test
    void createDbCreatesFolderAndSchema(@TempDir Path temp) throws Exception {
        Path folder = temp.resolve("nested").resolve("panel");

        assertTrue(BootDbTagDynamicRepository.createDB(folder.toString()));
        assertTrue(Files.isDirectory(folder));
        assertTrue(Files.exists(folder.resolve("boot.db")));
        assertEquals(0, rows(folder, "Devices").size());
        assertEquals(0, rows(folder, "PvRecords").size());

        assertTrue(BootDbTagDynamicRepository.createDB(folder.toString()),
                "createDB must be idempotent");
    }

    @Test
    void createDbFailsWhenThePathIsNotAFolder(@TempDir Path temp) throws Exception {
        Path file = temp.resolve("not-a-folder");
        Files.writeString(file, "content");

        assertFalse(BootDbTagDynamicRepository.createDB(file.toString()));
        assertTrue(Files.isRegularFile(file));
    }

    @Test
    void rebuildWritesAllDeviceAndPvFields(@TempDir Path temp) throws Exception {
        RebuildResults result = BootDbTagDynamicRepository.rebuildWithReason(
                temp.toFile(),
                List.of(device("dev-1", "PLC_One")),
                List.of(pv("pv-1", "Var_1", "PLC_One", "1000ms")));

        assertTrue(result.success());
        assertNull(result.reason());

        Map<String, String> device = rows(temp, "Devices").get(0);
        assertEquals("dev-1", device.get("DeviceUuId"));
        assertEquals("S7", device.get("DriverName"));
        assertEquals("key-PLC_One", device.get("DeviceKey"));
        assertEquals("ipa=10.0.0.1", device.get("DeviceUrl"));
        assertEquals("PLC_One", device.get("DeviceName"));
        assertEquals("desc-PLC_One", device.get("DeviceDescription"));
        assertEquals("true", device.get("DeviceEnable"));
        assertEquals("device-md5", device.get("Md5"));

        Map<String, String> pv = rows(temp, "PvRecords").get(0);
        assertEquals("pv-1", pv.get("PvUuId"));
        assertEquals("Var_1", pv.get("PvName"));
        assertEquals("INT", pv.get("PvType"));
        assertEquals("PLC_One", pv.get("PvDeviceName"));
        assertEquals("%DB1.DBD0", pv.get("PvTag"));
        assertEquals("descriptor", pv.get("PvDescriptor"));
        assertEquals("1000", pv.get("PvScanTime"));
        assertEquals("true", pv.get("PvScanEnable"));
        assertEquals("false", pv.get("PvWriteEnable"));
        assertEquals("0", pv.get("PvDisplayLimitLow"));
        assertEquals("10", pv.get("PvDisplayLimitHigh"));
        assertEquals("display", pv.get("PvDisplayDescription"));
        assertEquals("%.0f", pv.get("PvDisplayFormat"));
        assertEquals("u", pv.get("PvDisplayUnits"));
        assertEquals("0", pv.get("PvControlLimitLow"));
        assertEquals("10", pv.get("PvControlLimitHigh"));
        assertEquals("1", pv.get("PvControlMinStep"));
        assertEquals("pv-md5", pv.get("Md5"));
    }

    @Test
    void rebuildWithNullListsCreatesEmptyDatabase(@TempDir Path temp) throws Exception {
        RebuildResults result = BootDbTagDynamicRepository.rebuildWithReason(
                temp.toFile(), null, null);

        assertTrue(result.success());
        assertEquals(0, rows(temp, "Devices").size());
        assertEquals(0, rows(temp, "PvRecords").size());
    }

    @Test
    void rebuildRejectsNullDevice() {
        RebuildResults result = BootDbTagDynamicRepository.rebuildWithReason(
                Path.of(".").toFile(), Arrays.asList((DeviceConfigData) null), List.of());

        assertFalse(result.success());
        assertEquals("There is no device record to save.", result.reason());
    }

    @Test
    void rebuildRejectsDeviceWithoutName(@TempDir Path temp) {
        DeviceConfigData device = deviceWith("dev-1", "  ", "key-dev-1");

        RebuildResults result = BootDbTagDynamicRepository.rebuildWithReason(
                temp.toFile(), List.of(device), List.of());

        assertFalse(result.success());
        assertEquals("The device has no name. Assign one before saving.", result.reason());
        assertFalse(Files.exists(temp.resolve("boot.db")));
    }

    @Test
    void rebuildRejectsDeviceWithoutKey(@TempDir Path temp) {
        DeviceConfigData device = deviceWith("dev-1", "PLC_One", " ");

        RebuildResults result = BootDbTagDynamicRepository.rebuildWithReason(
                temp.toFile(), List.of(device), List.of());

        assertFalse(result.success());
        assertNotNull(result.reason());
        assertTrue(result.reason().contains("no name"),
                "the reason must explain that the device key is missing");
        assertFalse(Files.exists(temp.resolve("boot.db")));
    }

    @Test
    void rebuildKeepsPreviousDatabaseWhenValidationFails(@TempDir Path temp) throws Exception {
        assertTrue(BootDbTagDynamicRepository.rebuild(
                temp.toFile(), List.of(device("dev-1", "PLC_One")), List.of()));

        RebuildResults result = BootDbTagDynamicRepository.rebuildWithReason(
                temp.toFile(), List.of(deviceWith("dev-2", "PLC_Two", "")), List.of());

        assertFalse(result.success());
        List<Map<String, String>> devices = rows(temp, "Devices");
        assertEquals(1, devices.size());
        assertEquals("dev-1", devices.get(0).get("DeviceUuId"));
    }

    @Test
    void rebuildRejectsNullFolderAndNonDirectoryFolder(@TempDir Path temp) throws Exception {
        RebuildResults nullFolder = BootDbTagDynamicRepository.rebuildWithReason(
                null, List.of(), List.of());
        assertFalse(nullFolder.success());
        assertEquals("There is no project or configuration to save", nullFolder.reason());

        Path file = temp.resolve("file");
        Files.writeString(file, "content");
        RebuildResults fileFolder = BootDbTagDynamicRepository.rebuildWithReason(
                file.toFile(), List.of(), List.of());
        assertFalse(fileFolder.success());
        assertEquals("The folder where boot.db is created does not exist", fileFolder.reason());
    }

    @Test
    void rebuildLinksPvsOnlyToTheirDevice(@TempDir Path temp) throws Exception {
        PvConfigData orphan = new PvConfigData("pv-orphan", "Orphan", "INT", null,
                "%DB1.DBD8", "", "1000ms", true, false,
                "", "", "", "", "", "", "", "", "");
        PvConfigData unknownDevice = pv("pv-unknown", "Unknown", "UNKNOWN", "1000ms");

        List<PvConfigData> pvs = new ArrayList<>(List.of(
                pv("pv-1", "Var_1", "PLC_One", "1000ms"),
                pv("pv-2", "Var_2", "PLC_Two", "1000ms"),
                orphan,
                unknownDevice));
        pvs.add(null);

        RebuildResults result = BootDbTagDynamicRepository.rebuildWithReason(
                temp.toFile(),
                List.of(device("dev-1", "PLC_One"), device("dev-2", "PLC_Two")),
                pvs);

        assertTrue(result.success());
        assertEquals(2, rows(temp, "Devices").size());
        List<Map<String, String>> savedPvs = rows(temp, "PvRecords");
        assertEquals(2, savedPvs.size());
        assertEquals("pv-1", savedPvs.get(0).get("PvUuId"));
        assertEquals("pv-2", savedPvs.get(1).get("PvUuId"));
    }

    @Test
    void rebuildGeneratesUuidsWhenMissing(@TempDir Path temp) throws Exception {
        RebuildResults result = BootDbTagDynamicRepository.rebuildWithReason(
                temp.toFile(),
                List.of(device(null, "PLC_One")),
                List.of(pv(null, "Var_1", "PLC_One", "1000ms")));

        assertTrue(result.success());

        String deviceUuid = rows(temp, "Devices").get(0).get("DeviceUuId");
        String pvUuid = rows(temp, "PvRecords").get(0).get("PvUuId");
        assertNotNull(deviceUuid);
        assertFalse(deviceUuid.isBlank());
        assertNotNull(pvUuid);
        assertFalse(pvUuid.isBlank());
        UUID.fromString(deviceUuid);
        UUID.fromString(pvUuid);
    }

    @Test
    void rebuildRemovesMillisecondSuffixFromScanTime(@TempDir Path temp) throws Exception {
        RebuildResults result = BootDbTagDynamicRepository.rebuildWithReason(
                temp.toFile(),
                List.of(device("dev-1", "PLC_One")),
                List.of(
                        pv("pv-1", "Var_1", "PLC_One", "250ms"),
                        pv("pv-2", "Var_2", "PLC_One", "250MS"),
                        pv("pv-3", "Var_3", "PLC_One", "1000"),
                        pv("pv-4", "Var_4", "PLC_One", null)));

        assertTrue(result.success());
        List<Map<String, String>> pvs = rows(temp, "PvRecords");
        assertEquals("250", rowWith(pvs, "PvUuId", "pv-1").get("PvScanTime"));
        assertEquals("250", rowWith(pvs, "PvUuId", "pv-2").get("PvScanTime"));
        assertEquals("1000", rowWith(pvs, "PvUuId", "pv-3").get("PvScanTime"));
        assertEquals("", rowWith(pvs, "PvUuId", "pv-4").get("PvScanTime"));
    }

    @Test
    void rebuildLeavesNoTemporaryFiles(@TempDir Path temp) throws Exception {
        assertTrue(BootDbTagDynamicRepository.rebuildWithReason(
                temp.toFile(), List.of(device("dev-1", "PLC_One")), List.of()).success());

        try (Stream<Path> files = Files.list(temp)) {
            assertTrue(files.noneMatch(file
                    -> file.getFileName().toString().startsWith("boot.db.")
                    && file.getFileName().toString().endsWith(".tmp")));
        }
    }

    @Test
    void insertDeviceConfigWritesDeviceAndPvs(@TempDir Path temp) throws Exception {
        assertTrue(BootDbTagDynamicRepository.createDB(temp.toString()));

        assertTrue(BootDbTagDynamicRepository.insertDeviceConfig(
                temp.toString(),
                device("dev-1", "PLC_One"),
                List.of(pv("pv-1", "Var_1", "PLC_One", "1000ms"))));

        assertEquals(1, rows(temp, "Devices").size());
        Map<String, String> pv = rows(temp, "PvRecords").get(0);
        assertEquals("pv-1", pv.get("PvUuId"));
        assertEquals("PLC_One", pv.get("PvDeviceName"));
    }

    @Test
    void insertDeviceConfigReplacesExistingPv(@TempDir Path temp) throws Exception {
        assertTrue(BootDbTagDynamicRepository.createDB(temp.toString()));

        assertTrue(BootDbTagDynamicRepository.insertDeviceConfig(
                temp.toString(),
                device("dev-1", "PLC_One"),
                List.of(pv("pv-1", "Old_Name", "PLC_One", "1000ms"))));
        assertTrue(BootDbTagDynamicRepository.insertDeviceConfig(
                temp.toString(),
                device("dev-1", "PLC_One"),
                List.of(pv("pv-1", "New_Name", "PLC_One", "1000ms"))));

        assertEquals(1, rows(temp, "Devices").size());
        List<Map<String, String>> pvs = rows(temp, "PvRecords");
        assertEquals(1, pvs.size());
        assertEquals("New_Name", pvs.get(0).get("PvName"));
    }

    @Test
    void insertDeviceConfigRejectsInvalidDevice(@TempDir Path temp) throws Exception {
        assertTrue(BootDbTagDynamicRepository.createDB(temp.toString()));

        assertFalse(BootDbTagDynamicRepository.insertDeviceConfig(
                temp.toString(),
                deviceWith("dev-1", "PLC_One", ""),
                List.of()));

        assertEquals(0, rows(temp, "Devices").size());
    }

    @Test
    void insertDeviceConfigAcceptsNullPv(@TempDir Path temp) throws Exception {
        assertTrue(BootDbTagDynamicRepository.createDB(temp.toString()));

        assertTrue(BootDbTagDynamicRepository.insertDeviceConfig(
                temp.toString(), device("dev-1", "PLC_One"), (PvConfigData) null));

        assertEquals(1, rows(temp, "Devices").size());
        assertEquals(0, rows(temp, "PvRecords").size());
    }

    @Test
    void insertDeviceConfigFailsWithoutSchema(@TempDir Path temp) {
        assertFalse(BootDbTagDynamicRepository.insertDeviceConfig(
                temp.toString(), device("dev-1", "PLC_One"), List.of()));
    }

    @Test
    void deletePvsDeletesOnlyRequestedUuids(@TempDir Path temp) throws Exception {
        assertTrue(BootDbTagDynamicRepository.createDB(temp.toString()));
        assertTrue(BootDbTagDynamicRepository.insertDeviceConfig(
                temp.toString(),
                device("dev-1", "PLC_One"),
                List.of(
                        pv("pv-1", "Var_1", "PLC_One", "1000ms"),
                        pv("pv-2", "Var_2", "PLC_One", "1000ms"),
                        pv("pv-3", "Var_3", "PLC_One", "1000ms"))));

        assertTrue(BootDbTagDynamicRepository.deletePvs(
                temp.toString(), Arrays.asList("pv-2", null, "  ")));

        List<Map<String, String>> pvs = rows(temp, "PvRecords");
        assertEquals(2, pvs.size());
        assertNotNull(rowWith(pvs, "PvUuId", "pv-1"));
        assertNotNull(rowWith(pvs, "PvUuId", "pv-3"));
    }

    @Test
    void deletePvsWithNullOrEmptyDoesNotCreateDatabase(@TempDir Path temp) {
        assertTrue(BootDbTagDynamicRepository.deletePvs(temp.toString(), null));
        assertTrue(BootDbTagDynamicRepository.deletePvs(temp.toString(), List.of()));
        assertFalse(Files.exists(temp.resolve("boot.db")));
    }

    @Test
    void deletePvsFailsWhenDatabaseHasNoSchema(@TempDir Path temp) {
        assertFalse(BootDbTagDynamicRepository.deletePvs(
                temp.toString(), List.of("pv-1")));
    }

    @Test
    void rebuildReturnsBooleanResult(@TempDir Path temp) {
        assertTrue(BootDbTagDynamicRepository.rebuild(
                temp.toFile(), List.of(device("dev-1", "PLC_One")), List.of()));
        assertFalse(BootDbTagDynamicRepository.rebuild(
                temp.toFile(), List.of(deviceWith("dev-2", "PLC_Two", "")), List.of()));
    }

    private static DeviceConfigData device(String uuid, String name) {
        return deviceWith(uuid, name, "key-" + name);
    }

    private static DeviceConfigData deviceWith(String uuid, String name, String key) {
        Properties properties = new Properties();
        if (uuid != null) {
            properties.setProperty("uuid", uuid);
        }
        properties.setProperty("deviceName", name);
        properties.setProperty("deviceKey", key);
        properties.setProperty("protocol", "S7");
        properties.setProperty("specificParameters", "ipa=10.0.0.1");
        properties.setProperty("description", "desc-" + name);
        properties.setProperty("enable", "true");
        properties.setProperty("md5", "device-md5");
        return new DeviceConfigData(properties);
    }

    private static PvConfigData pv(String uuid, String name, String deviceName, String scanTime) {
        return new PvConfigData(uuid, name, "INT", deviceName, "%DB1.DBD0",
                "descriptor", scanTime, true, false,
                "0", "10", "display", "%.0f", "u", "0", "10", "1", "pv-md5");
    }

    private static List<Map<String, String>> rows(Path folder, String table) throws SQLException {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:sqlite:" + folder.resolve("boot.db"));
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT * FROM " + table)) {
            ResultSetMetaData metaData = resultSet.getMetaData();
            List<Map<String, String>> rows = new ArrayList<>();
            while (resultSet.next()) {
                Map<String, String> row = new LinkedHashMap<>();
                for (int i = 1; i <= metaData.getColumnCount(); i++) {
                    row.put(metaData.getColumnLabel(i), resultSet.getString(i));
                }
                rows.add(row);
            }
            return rows;
        }
    }

    private static Map<String, String> rowWith(List<Map<String, String>> rows,
            String column, String value) {
        return rows.stream()
                .filter(row -> Objects.equals(row.get(column), value))
                .findFirst()
                .orElseThrow();
    }
}
