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

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;
import org.apache.plc4x.malbec.projecttype.panelcategory.comms.api.CommunicationsConfig;
import org.apache.plc4x.malbec.projecttype.panelcategory.panel.CommConfigData;
import org.apache.plc4x.malbec.projecttype.panelcategory.panel.DeviceConfigData;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

public class HMIPanelDataBaseFactory {

    private static final String createdb = null;

    //Variables::
    //Primero Variable de la estructura leida de planta
    /*
    1. Generar la base de datos en el proyecto actual.
    2. Generar las tablas asociadas
    3. Leer xml de planta de daniel.
    4. Determinar El Device (PLC), Item (Area de Memoria), Group (Tiempo de escaneo)
    y los PVRecords ( de proceso) del Control Module.
    5. Con el punto 4 listo, realizar las inserciones en cada tabla correspondiente.
     */
    private static final String SQL_CREATE_TABLE_DEVICES
            = "CREATE TABLE IF NOT EXISTS Devices("
            + "DeviceUuId TEXT NOT NULL PRIMARY KEY,"
            + "DriverName TEXT,"
            + "DeviceKey TEXT,"
            + "DeviceUrl TEXT,"
            + "DeviceName TEXT,"
            + "DeviceDescription TEXT,"
            + "DeviceEnable TEXT,"
            + "Md5 TEXT)";

    private static final String SQL_CREATE_TABLE_GROUPS
            = "CREATE TABLE IF NOT EXISTS Groups("
            + "GroupUuid TEXT NOT NULL PRIMARY KEY,"
            + "DeviceUuid TEXT,"
            + "GroupName TEXT,"
            + "GroupDescription TEXT,"
            + "GroupScantime TEXT,"
            + "GroupEnable TEXT,"
            + "Md5 TEXT)";

    private static final String SQL_CREATE_TABLE_ITEMS
            = "CREATE TABLE IF NOT EXISTS Items("
            + "ItemUuid TEXT NOT NULL PRIMARY KEY,"
            + "DeviceUuid TEXT,"
            + "GroupUuid TEXT,"
            + "ItemName TEXT,"
            + "ItemDescription TEXT,"
            + "ItemTag TEXT,"
            + "ItemEnable TEXT,"
            + "Md5 TEXT)";
    /* TODO: todos estos serian los PVType 
        s5time, s7date s7time s7tod s7dat s7counter s7di s7ai s7ao s7valve
        s7vlv s7avlv s7motor boolean byte double float int long short
        string ubyte uint ulong ushort
    */
    private static final String SQL_CREATE_TABLE_PVRECORDS
            = "CREATE TABLE IF NOT EXISTS PvRecords("
            + "PvUuId TEXT NOT NULL PRIMARY KEY,"
            + "PvName TEXT,"
            + "PvType TEXT,"
            + "PvId TEXT,"
            + "PvOffset TEXT,"
            + "PvDescriptor TEXT,"
            + "PvScanTime TEXT,"
            + "PvScanEnable TEXT,"
            + "PvWriteEnable TEXT,"
            + "PvDisplayLimitLow TEXT,"
            + "PvDisplayLimitHigh TEXT,"
            + "PvDisplayDescription TEXT,"
            + "PvDisplayFormat TEXT,"
            + "PvDisplayUnits TEXT,"
            + "PvControlLimitLow TEXT,"
            + "PvControlLimitHigh TEXT,"
            + "PvControlMinStep TEXT,"
            + "Md5 TEXT)";

    public static boolean createDB(String rutaCarpeta) {
        File carpeta = new File(rutaCarpeta);
        // Construir la URL de JDBC con la ruta completa
        File archivoDB = new File(carpeta, "boot.db");
        String url = "jdbc:sqlite:" + archivoDB;

        System.out.println("Creando/Conectando base de datos en: " + archivoDB);

        // Ejecutar la creación de tablas dentro de una sola transacción
        try (Connection conn = DriverManager.getConnection(url);
           Statement stmt = conn.createStatement()) {
            System.out.println("Conectado");
            if (conn != null) {
                // Ejecutar las sentencias
                stmt.execute(SQL_CREATE_TABLE_DEVICES);
                stmt.execute(SQL_CREATE_TABLE_GROUPS);
                stmt.execute(SQL_CREATE_TABLE_ITEMS);
                stmt.execute(SQL_CREATE_TABLE_PVRECORDS);

                System.out.println("Las 4 tablas fueron creadas exitosamente (vacías).");
            }
            return true;
        } catch (SQLException e) {
            System.err.println("Error al crear las tablas: " + e.getMessage());
            return false;
        }
    }
    
    public static boolean insertDeviceConfig(String rutaCarpeta, CommConfigData config) {
        return insertDeviceConfig(rutaCarpeta, null, config);
    }

    public static boolean insertDeviceConfig(String rutaCarpeta, DeviceConfigData device, CommConfigData comms) {
        File archivoDB = new File(rutaCarpeta, "boot.db");
        String url = "jdbc:sqlite:" + archivoDB;

        String deviceUuid = (device != null && device.getUuid() != null && !device.getUuid().isEmpty())
                ? device.getUuid() : UUID.randomUUID().toString();
        String deviceName = (device != null) ? device.getDeviceName() : (comms != null ? comms.getDeviceName() : "");
        String driverName = (device != null) ? device.getProtocol() : "DefaultDriver";
        String deviceDescription = (device != null) ? device.getDescription() : "Dispositivo guardado desde DeviceManager";

        // INSERT OR REPLACE: si el uuid ya existe se sobreescribe el registro con
        // los valores actuales en lugar de ignorarlo o duplicarlo. El esquema no
        // declara claves foráneas, así que no hay cascadas sobre otras tablas.
        String sqlDevice = "INSERT OR REPLACE INTO Devices(DeviceUuId, DriverName, DeviceKey, DeviceUrl, DeviceName, DeviceDescription, DeviceEnable, Md5) VALUES(?,?,?,?,?,?,?,?)";
        String sqlGroup = "INSERT OR REPLACE INTO Groups(GroupUuid, DeviceUuid, GroupName, GroupDescription, GroupScantime, GroupEnable, Md5) VALUES(?,?,?,?,?,?,?)";
        String sqlItem = "INSERT OR REPLACE INTO Items(ItemUuid, DeviceUuid, GroupUuid, ItemName, ItemDescription, ItemTag, ItemEnable, Md5) VALUES(?,?,?,?,?,?,?,?)";
        String sqlPv = "INSERT OR REPLACE INTO PvRecords(PvUuId, PvName, PvType, PvId, PvOffset, PvDescriptor, PvScanTime, PvScanEnable, PvWriteEnable, PvDisplayLimitLow, PvDisplayLimitHigh, PvDisplayDescription, PvDisplayFormat, PvDisplayUnits, PvControlLimitLow, PvControlLimitHigh, PvControlMinStep, Md5) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

        try (Connection conn = DriverManager.getConnection(url)) {
            // No se admite un área sin grupo: el grupo de escaneo es obligatorio,
            // así que antes de tocar la base se valida la configuración completa.
            String areaInvalida = primerAreaSinGrupo(comms);
            if (areaInvalida != null) {
                System.err.println("No se guardó el dispositivo '" + deviceName
                        + "': el área '" + areaInvalida + "' no tiene grupo de escaneo.");
                return false;
            }

            conn.setAutoCommit(false); // Transacción para insertar todo junto

            // 1. Insertar Dispositivo
            try (PreparedStatement pstmt = conn.prepareStatement(sqlDevice)) {
                pstmt.setString(1, deviceUuid);
                pstmt.setString(2, driverName);
                pstmt.setString(3, device != null && device.getDeviceKey() != null ? device.getDeviceKey() : "Key123");
                pstmt.setString(4, device != null ? device.getSpecificParameters() : "tcp://localhost:502");
                pstmt.setString(5, deviceName);
                pstmt.setString(6, deviceDescription);
                pstmt.setString(7, (device != null && !device.isEnabled()) ? "FALSE" : "TRUE");
                pstmt.setString(8, "md5_device_hash");
                pstmt.executeUpdate();
            }

            if (comms != null) {
                // 2. Insertar Grupos
                try (PreparedStatement pstmt = conn.prepareStatement(sqlGroup)) {
                    for (CommConfigData.GroupConfig g : orEmpty(comms.getGroups())) {
                        pstmt.setString(1, g.getUuid());
                        pstmt.setString(2, deviceUuid);
                        pstmt.setString(3, g.getName());
                        pstmt.setString(4, g.getDescription());
                        pstmt.setString(5, g.getScantime());
                        pstmt.setString(6, g.isEnable() ? "TRUE" : "FALSE");
                        pstmt.setString(7, g.getMd5());
                        pstmt.executeUpdate();
                    }
                }

                // 3. Insertar Items
                try (PreparedStatement pstmt = conn.prepareStatement(sqlItem)) {
                    for (CommConfigData.ItemConfig item : orEmpty(comms.getItems())) {
                        pstmt.setString(1, item.getUuid());
                        pstmt.setString(2, deviceUuid);
                        pstmt.setString(3, item.getGroupUuid());
                        pstmt.setString(4, item.getName());
                        pstmt.setString(5, item.getDescription());
                        pstmt.setString(6, item.getTag());
                        pstmt.setString(7, item.isEnable() ? "TRUE" : "FALSE");
                        pstmt.setString(8, item.getMd5());
                        pstmt.executeUpdate();
                    }
                }

                // 4. Insertar PV Records
                try (PreparedStatement pstmt = conn.prepareStatement(sqlPv)) {
                    for (CommConfigData.PvConfig pv : orEmpty(comms.getPvs())) {
                        pstmt.setString(1, pv.getUuid());
                        pstmt.setString(2, pv.getName());
                        pstmt.setString(3, pv.getType());
                        pstmt.setString(4, pv.getId());
                        pstmt.setString(5, pv.getOffset());
                        pstmt.setString(6, pv.getDescriptor());
                        pstmt.setString(7, pv.getScanTime());
                        pstmt.setString(8, pv.isScanEnable() ? "TRUE" : "FALSE");
                        pstmt.setString(9, pv.isWriteEnable() ? "TRUE" : "FALSE");
                        pstmt.setString(10, pv.getDisplayLimitLow());
                        pstmt.setString(11, pv.getDisplayLimitHigh());
                        pstmt.setString(12, pv.getDisplayDescription());
                        pstmt.setString(13, pv.getDisplayFormat());
                        pstmt.setString(14, pv.getDisplayUnits());
                        pstmt.setString(15, pv.getControlLimitLow());
                        pstmt.setString(16, pv.getControlLimitHigh());
                        pstmt.setString(17, pv.getControlMinStep());
                        pstmt.setString(18, pv.getMd5());
                        pstmt.executeUpdate();
                    }
                }
            }

            conn.commit();
            System.out.println("--- TODAS LAS TABLAS GUARDADAS CORRECTAMENTE EN BOOT.DB ---");
            return true;

        } catch (SQLException e) {
            System.err.println("Error insertando datos en SQLite: " + e.getMessage());
            return false;
        }
    }

    /**
     * @return el nombre del primer área sin grupo de escaneo, o {@code null} si
     *         todas las áreas tienen grupo
     */
    private static String primerAreaSinGrupo(CommConfigData comms) {
        if (comms == null) {
            return null;
        }
        for (CommConfigData.ItemConfig item : orEmpty(comms.getItems())) {
            String groupUuid = item.getGroupUuid();
            if (groupUuid == null || groupUuid.isEmpty()) {
                return item.getName();
            }
        }
        return null;
    }

    /**
     * Vuelca la configuración completa (devices + grupos/items/pvs) desde el
     * modelo en el archivo {@code boot.db} del directorio del proyecto.
     * Las tablas se regeneran desde cero; el XML es la fuente de verdad.
     *
     * @return true si todas las tablas quedaron escritas correctamente
     */
    public static boolean rebuild(FileObject projectDir, CommunicationsConfig config) {
        if (projectDir == null || config == null) {
            return false;
        }
        File dbFolder = FileUtil.toFile(projectDir);
        if (dbFolder == null || !dbFolder.isDirectory()) {
            return false;
        }
        File archivoDB = new File(dbFolder, "boot.db");
        String url = "jdbc:sqlite:" + archivoDB;

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS PvRecords");
            stmt.execute("DROP TABLE IF EXISTS Items");
            stmt.execute("DROP TABLE IF EXISTS Groups");
            stmt.execute("DROP TABLE IF EXISTS Devices");
        } catch (SQLException e) {
            System.err.println("Error limpiando boot.db: " + e.getMessage());
            return false;
        }

        if (!createDB(dbFolder.getAbsolutePath())) {
            return false;
        }
        boolean ok = true;
        for (DeviceConfigData device : config.getDevices()) {
            ok &= insertDeviceConfig(dbFolder.getAbsolutePath(), device, config.getComms(device.getUuid()));
        }
        return ok;
    }

    private static <T> List<T> orEmpty(List<T> list) {
        return list == null ? List.of() : list;
    }
}
