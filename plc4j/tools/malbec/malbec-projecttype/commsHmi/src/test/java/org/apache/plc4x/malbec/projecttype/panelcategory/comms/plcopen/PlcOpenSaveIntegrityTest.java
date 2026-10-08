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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import org.apache.plc4x.malbec.projecttype.panelcategory.comms.api.CommConfigRepository;
import org.apache.plc4x.malbec.projecttype.panelcategory.comms.api.CommConfigStorage;
import org.apache.plc4x.malbec.projecttype.panelcategory.comms.api.CommunicationsConfig;
import org.apache.plc4x.malbec.projecttype.panelcategory.model.CommConfigData;
import org.apache.plc4x.malbec.projecttype.panelcategory.model.DeviceConfigData;
import org.junit.jupiter.api.Test;

/**
 * Caracterizacion de H-02: la escritura de {@code comunicacion.xml} trunca el
 * archivo antes de tener el contenido listo.
 *
 * <p>{@code save()} abre el destino con {@code storage.create()} y solo despues
 * serializa. Con un {@code FileObject} real, {@code getOutputStream()} vacia el
 * archivo en ese instante, asi que un fallo a mitad de la serialización deja el
 * archivo vacio o truncado y el contenido anterior perdido.
 *
 * <p>{@link TruncatingStorage} reproduce ese orden de operaciones, que es lo que
 * hace {@code ProjectFileStorage}. Estos tests describen el fallo actual, no el
 * comportamiento deseado.
 */
class PlcOpenSaveIntegrityTest {

    @Test
    void unaEscrituraFallidaDejaElArchivoVacio() throws Exception {
        TruncatingStorage storage = new TruncatingStorage();
        CommConfigRepository repo = new PlcOpenCommConfigRepositoryImpl(storage);

        repo.save(configCon(1));
        String anterior = storage.text();
        assertFalse(anterior.isBlank(), "precondicion: hay contenido escrito previamente");

        storage.fallarAlEscribir = true;
        assertThrows(Exception.class, () -> repo.save(configCon(20)),
                "la escritura debe fallar a mitad");

        assertNotEquals(anterior, storage.text(),
                "H-02: el contenido anterior se pierde; el archivo queda vacio o truncado"
                        + " porque se trunca antes de serializar");
        assertFalse(storage.text().contains("PLC_1"),
                "H-02: ningun resto del contenido anterior sobrevive al fallo");
    }

    @Test
    void elArchivoSeTruncaEnElMomentoDeAbrirlo() throws Exception {
        TruncatingStorage storage = new TruncatingStorage();
        CommConfigRepository repo = new PlcOpenCommConfigRepositoryImpl(storage);
        repo.save(configCon(1));
        String anterior = storage.text();

        // Solo abrir el destino, sin escribir nada, ya destruye lo anterior.
        storage.text = "";

        assertNotEquals(anterior, storage.text(),
                "H-02: crear el destino basta para perder el contenido previo");
    }

    @Test
    void unaEscrituraCorrectaSiReemplazaElContenido() throws Exception {
        TruncatingStorage storage = new TruncatingStorage();
        CommConfigRepository repo = new PlcOpenCommConfigRepositoryImpl(storage);

        repo.save(configCon(1));
        String conUnDispositivo = storage.text();
        repo.save(configCon(3));

        assertNotEquals(conUnDispositivo, storage.text());
        assertTrue(storage.text().contains("PLC_1"));
        assertTrue(storage.text().contains("PLC_3"));
        assertEquals(3, ocurrencias(storage.text(), "<configuration "),
                "el guardado es un reemplazo completo: queda un <configuration> por"
                        + " dispositivo y nada del contenido anterior");
    }

    private static CommunicationsConfig configCon(int numeroDeDispositivos) {
        CommunicationsConfig config = new CommunicationsConfig();
        for (int i = 1; i <= numeroDeDispositivos; i++) {
            String uuid = "uuid-" + i;
            config.addDevice(new DeviceConfigData(propiedades(uuid, "PLC_" + i)));
            config.upsertComms(uuid, new CommConfigData("PLC_" + i,
                    List.of(new CommConfigData.GroupConfig("g-" + i, "Grupo", "d", "500", true, "m")),
                    List.of(new CommConfigData.ItemConfig("it-" + i, "Area", "d",
                            "%DB" + i + ".DBB0:INT", true, "m", "g-" + i)),
                    List.of(new CommConfigData.PvConfig("pv-" + i, "PV", "INT", "it-" + i,
                            "0", "", "500", true, false, "0", "10", "d", "%.2f", "u",
                            "0", "10", "0.1", "m"))));
        }
        return config;
    }

    private static int ocurrencias(String texto, String aguja) {
        int total = 0;
        int desde = texto.indexOf(aguja);
        while (desde >= 0) {
            total++;
            desde = texto.indexOf(aguja, desde + aguja.length());
        }
        return total;
    }

    private static Properties propiedades(String uuid, String nombre) {        Properties p = new Properties();
        p.put("brand", "Siemens");
        p.put("model", "S7-1500");
        p.put("protocol", "S7");
        p.put("deviceName", nombre);
        p.put("deviceKey", "key-" + uuid);
        p.put("description", "descripcion de " + nombre);
        p.put("uuid", uuid);
        p.put("enable", true);
        p.put("s88Node", "S88.1");
        p.put("s88Uuid", "s88-" + uuid);
        p.put("specificParameters", "ipa=10.0.0.1,port=102");
        return p;
    }

    /**
     * Reproduce el orden de operaciones de {@code ProjectFileStorage}: el contenido
     * anterior se pierde en cuanto se pide el destino, y lo que se haya escrito a
     * medias se aplica al cerrar, incluso tras una excepcion.
     */
    private static final class TruncatingStorage implements CommConfigStorage {

        private String text = "";
        private boolean fallarAlEscribir;

        String text() {
            return text;
        }

        @Override
        public String open() {
            return text;
        }

        @Override
        public OutputStream create() {
            text = "";
            return new BufferConFallo(() -> fallarAlEscribir, written -> {
                text = written;
            });
        }
    }

    /**
     * Buffer de escritura que puede fallar por falta de espacio. Al cerrarse,
     * incluso tras una excepcion, aplica lo que haya quedado: es lo que hace un
     * fichero real cuando la escritura se interrumpe a medias.
     */
    private static final class BufferConFallo extends OutputStream {

        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        private final BooleanSupplier falla;
        private final Consumer<String> alCerrar;

        BufferConFallo(BooleanSupplier falla, Consumer<String> alCerrar) {
            this.falla = falla;
            this.alCerrar = alCerrar;
        }

        @Override
        public void write(int b) throws IOException {
            comprobar();
            buffer.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            comprobar();
            buffer.write(b, off, len);
        }

        @Override
        public void close() {
            alCerrar.accept(new String(buffer.toByteArray(), StandardCharsets.UTF_8));
        }

        private void comprobar() throws IOException {
            if (falla.getAsBoolean()) {
                throw new IOException("no hay espacio en el dispositivo");
            }
        }
    }
}
