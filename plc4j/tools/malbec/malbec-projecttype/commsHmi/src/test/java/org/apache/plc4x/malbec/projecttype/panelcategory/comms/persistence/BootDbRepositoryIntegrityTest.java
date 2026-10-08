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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;
import org.apache.plc4x.malbec.projecttype.panelcategory.comms.api.CommunicationsConfig;
import org.apache.plc4x.malbec.projecttype.panelcategory.model.CommConfigData;
import org.apache.plc4x.malbec.projecttype.panelcategory.model.DeviceConfigData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Caracterizacion de H-03 y H-04: como queda {@code boot.db} cuando algo falla a
 * mitad, y que valores se inventan al insertar.
 *
 * <p>Estos tests describen el comportamiento <em>actual</em>, que es incorrecto.
 * estan aqui para que quede fijada la forma exacta del fallo antes de arreglarlo,
 * no como especificacion. Cuando se corrija H-03 o H-04, el fallo debe observarse
 * aqui: o bien los tests pasan con el comportamiento nuevo, o bien se reescriben
 * como tests del comportamiento correcto. Ajustar la asercion para que vuelva a
 * cuadrar con el fallo seria perder justo lo que estos tests protegen.
 */
class BootDbRepositoryIntegrityTest {

    @TempDir
    Path tempDir;

    // ---------------------------------------------------------------- H-03

    @Test
    void unFalloEnElSegundoDispositivoDejaBootDbAPartial() throws Exception {
        CommunicationsConfig config = new CommunicationsConfig();
        config.addDevice(dispositivo("uuid-ok", "PLC_Bueno", true));
        config.upsertComms("uuid-ok", commsConGrupo("g-ok", "it-ok", "pv-ok"));

        // Este dispositivo es invalido: su area no tiene grupo de escaneo, asi que
        // insertDeviceConfig lo rechaza.
        config.addDevice(dispositivo("uuid-malo", "PLC_Malo", true));
        config.upsertComms("uuid-malo", commsSinGrupo("it-malo", "pv-malo"));

        boolean ok = BootDbRepository.rebuild(tempDir.toFile(), config);

        assertFalse(ok,
                "precondicion: rebuild debe informar del fallo");
        assertEquals(1, contarDispositivos(),
                "H-03: el fallo deja el primer dispositivo escrito y el segundo no");
        assertTrue(existeDispositivo("uuid-ok"),
                "el dispositivo valido queda en la base");
        assertFalse(existeDispositivo("uuid-malo"),
                "el invalido se rechaza");
        assertEquals(1, contar("Items"),
                "H-03: la base queda a medias, con parte del contenido y parte no");
    }

    @Test
    void unFalloDeInsertarNoRevierteElDropDeLasTablas() throws Exception {
        // Primero una base buena.
        CommunicationsConfig buena = new CommunicationsConfig();
        buena.addDevice(dispositivo("uuid-1", "PLC_Uno", true));
        buena.upsertComms("uuid-1", commsConGrupo("g-1", "it-1", "pv-1"));
        assertTrue(BootDbRepository.rebuild(tempDir.toFile(), buena));
        assertEquals(1, contarDispositivos(), "precondicion: hay una base buena");

        // Ahora una configuracion que falla.
        CommunicationsConfig mala = new CommunicationsConfig();
        mala.addDevice(dispositivo("uuid-2", "PLC_Dos", true));
        mala.upsertComms("uuid-2", commsSinGrupo("it-2", "pv-2"));
        assertFalse(BootDbRepository.rebuild(tempDir.toFile(), mala));

        assertEquals(0, contarDispositivos(),
                "H-03: el DROP TABLE no tiene transaccion global, asi que el fallo de"
                        + " insercion deja la base anterior destruida y vacia");
    }

    // ---------------------------------------------------------------- H-04

    @Test
    void unaClaveDeDispositivoNulaSeRellenaConKey123() throws Exception {
        Properties p = new Properties();
        p.put("brand", "Siemens");
        p.put("model", "S7-1500");
        p.put("protocol", "S7");
        p.put("deviceName", "Sin clave");
        p.put("uuid", "uuid-sin-clave");
        p.put("enable", true);
        p.put("s88Node", "S88.1");
        p.put("s88Uuid", "s88-1");
        p.put("specificParameters", "ipa=10.0.0.1");
        // deviceKey ausente a proposito

        CommunicationsConfig config = new CommunicationsConfig();
        config.addDevice(new DeviceConfigData(p));
        config.upsertComms("uuid-sin-clave", commsConGrupo("g-1", "it-1", "pv-1"));

        assertTrue(BootDbRepository.rebuild(tempDir.toFile(), config));

        assertEquals("Key123", valorDeDispositivo("uuid-sin-clave", "DeviceKey"),
                "H-04: una clave ausente se sustituye por un valor inventado");
    }

    @Test
    void sinDispositivoSeInventanTodosLosCampos() throws Exception {
        assertTrue(BootDbRepository.createDB(tempDir.toFile().getAbsolutePath()));

        // El overload de dos argumentos es el que no tiene dispositivo, y por tanto
        // el que rellena driver, url, clave y descripcion con valores fijos.
        boolean ok = BootDbRepository.insertDeviceConfig(tempDir.toFile().getAbsolutePath(),
                commsConGrupo("g-1", "it-1", "pv-1"));

        assertTrue(ok);
        assertEquals("DefaultDriver", valorUnicoDe("Devices", "DriverName"),
                "H-04: el driver no es el del proyecto, es un literal");
        assertEquals("Key123", valorUnicoDe("Devices", "DeviceKey"),
                "H-04: la clave no viene de ninguna parte");
        assertEquals("tcp://localhost:502", valorUnicoDe("Devices", "DeviceUrl"),
                "H-04: la URL no corresponde a la instalacion");
        assertEquals("Dispositivo guardado desde DeviceManager",
                valorUnicoDe("Devices", "DeviceDescription"),
                "H-04: la descripcion es un texto fijo, no la del usuario");
    }

    @Test
    void conDispositivoLaUrlVieneDeLosParametricos() throws Exception {
        Properties p = new Properties();
        p.put("protocol", "S7");
        p.put("deviceName", "Sin parametros");
        p.put("uuid", "uuid-sin-param");
        p.put("deviceKey", "key-1");
        p.put("enable", true);
        // specificParameters ausente a proposito

        CommunicationsConfig config = new CommunicationsConfig();
        config.addDevice(new DeviceConfigData(p));
        config.upsertComms("uuid-sin-param", commsConGrupo("g-1", "it-1", "pv-1"));

        assertTrue(BootDbRepository.rebuild(tempDir.toFile(), config));

        assertNull(valorDeDispositivo("uuid-sin-param", "DeviceUrl"),
                "con dispositivo no se inventa la URL: se escribe el valor, sea cual sea");
        assertEquals("S7", valorDeDispositivo("uuid-sin-param", "DriverName"),
                "con dispositivo el driver sale del protocolo, no de un literal");
    }

    @Test
    void unaClaveVaciaSeRespetaPeroUnaNulaSeSustituye() throws Exception {
        Properties p = new Properties();
        p.put("protocol", "S7");
        p.put("deviceName", "Con clave vacia");
        p.put("uuid", "uuid-vacia");
        p.put("deviceKey", "");
        p.put("enable", true);
        p.put("specificParameters", "ipa=10.0.0.1");

        CommunicationsConfig config = new CommunicationsConfig();
        config.addDevice(new DeviceConfigData(p));
        config.upsertComms("uuid-vacia", commsConGrupo("g-1", "it-1", "pv-1"));

        assertTrue(BootDbRepository.rebuild(tempDir.toFile(), config));
        assertEquals("", valorDeDispositivo("uuid-vacia", "DeviceKey"),
                "una clave vacia se respeta: el runtime recibe una clave vacia, no Key123");
    }

    @Test
    void elMd5DelDispositivoEsUnLiteralIgnorandoElDelModelo() throws Exception {
        CommunicationsConfig config = new CommunicationsConfig();
        config.addDevice(dispositivo("uuid-1", "PLC_Uno", true));
        config.upsertComms("uuid-1", commsConGrupo("g-1", "it-1", "pv-1"));

        assertTrue(BootDbRepository.rebuild(tempDir.toFile(), config));

        assertEquals("md5_device_hash", valorDeDispositivo("uuid-1", "Md5"),
                "H-04: el Md5 del dispositivo es un literal, no un hash del contenido");
    }

    @Test
    void losMd5DeGrupoAreaYPvSiVienenDelModelo() throws Exception {
        CommunicationsConfig config = new CommunicationsConfig();
        config.addDevice(dispositivo("uuid-1", "PLC_Uno", true));
        config.upsertComms("uuid-1", commsConGrupo("g-1", "it-1", "pv-1"));

        assertTrue(BootDbRepository.rebuild(tempDir.toFile(), config));

        assertEquals("md5-real-del-grupo", valorDe("Groups", "GroupUuid", "g-1", "Md5"),
                "el md5 del grupo viene del modelo");
        assertEquals("md5-real-del-area", valorDe("Items", "ItemUuid", "it-1", "Md5"),
                "el md5 del area viene del modelo");
        assertEquals("md5-real-de-la-pv", valorDe("PvRecords", "PvUuId", "pv-1", "Md5"),
                "el md5 de la PV viene del modelo");
    }

    // ---------------------------------------------------------------- ayudas

    private static Properties propiedades(String uuid, String nombre, boolean conClave) {
        Properties p = new Properties();
        p.put("brand", "Siemens");
        p.put("model", "S7-1500");
        p.put("protocol", "S7");
        p.put("deviceName", nombre);
        p.put("uuid", uuid);
        p.put("enable", true);
        p.put("s88Node", "S88.1");
        p.put("s88Uuid", "s88-" + uuid);
        p.put("specificParameters", "ipa=10.0.0.1,port=102");
        if (conClave) {
            p.put("deviceKey", "key-" + uuid);
        }
        return p;
    }

    private static DeviceConfigData dispositivo(String uuid, String nombre, boolean conClave) {
        return new DeviceConfigData(propiedades(uuid, nombre, conClave));
    }

    private static CommConfigData commsConGrupo(String groupUuid, String itemUuid, String pvUuid) {
        return new CommConfigData("PLC",
                List.of(new CommConfigData.GroupConfig(groupUuid, "Grupo", "descripcion",
                        "500", true, "md5-real-del-grupo")),
                List.of(new CommConfigData.ItemConfig(itemUuid, "Area", "descripcion",
                        "%DB1.DBB0:INT", true, "md5-real-del-area", groupUuid)),
                List.of(new CommConfigData.PvConfig(pvUuid, "PV", "INT", itemUuid, "0", "",
                        "500", true, false, "0", "10", "desc", "%.2f", "bar", "0", "10", "0.1",
                        "md5-real-de-la-pv")));
    }

    private static CommConfigData commsSinGrupo(String itemUuid, String pvUuid) {
        return new CommConfigData("PLC",
                List.of(new CommConfigData.GroupConfig("g-otro", "Grupo", "descripcion",
                        "500", true, "md5-real-del-grupo")),
                List.of(new CommConfigData.ItemConfig(itemUuid, "Area sin grupo", "descripcion",
                        "%DB1.DBB0:INT", true, "md5-real-del-area")),
                List.of(new CommConfigData.PvConfig(pvUuid, "PV", "INT", itemUuid, "0", "",
                        "500", true, false, "0", "10", "desc", "%.2f", "bar", "0", "10", "0.1",
                        "md5-real-de-la-pv")));
    }

    private Connection conexion() throws Exception {
        return DriverManager.getConnection("jdbc:sqlite:" + new File(tempDir.toFile(), "boot.db"));
    }

    private int contarDispositivos() throws Exception {
        return contar("Devices");
    }

    private int contar(String tabla) throws Exception {
        try (Connection conn = conexion(); Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + tabla)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private boolean existeDispositivo(String uuid) throws Exception {
        try (Connection conn = conexion(); Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT COUNT(*) FROM Devices WHERE DeviceUuId = '" + uuid + "'")) {
            rs.next();
            return rs.getInt(1) > 0;
        }
    }

    private String valorDeDispositivo(String uuid, String columna) throws Exception {
        try (Connection conn = conexion(); Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT " + columna + " FROM Devices WHERE DeviceUuId = '" + uuid + "'")) {
            return rs.next() ? rs.getString(1) : null;
        }
    }

    private String valorUnicoDe(String tabla, String columna) throws Exception {
        try (Connection conn = conexion(); Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT DISTINCT " + columna + " FROM " + tabla)) {
            return rs.next() ? rs.getString(1) : null;
        }
    }

    private String valorDe(String tabla, String columnaClave, String clave, String columna) throws Exception {
        try (Connection conn = conexion(); Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT " + columna + " FROM " + tabla
                     + " WHERE " + columnaClave + " = '" + clave + "'")) {
            return rs.next() ? rs.getString(1) : null;
        }
    }
}
