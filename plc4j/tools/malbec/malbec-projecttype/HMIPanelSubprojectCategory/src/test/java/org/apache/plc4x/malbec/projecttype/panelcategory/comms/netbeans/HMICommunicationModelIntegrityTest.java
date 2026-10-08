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
package org.apache.plc4x.malbec.projecttype.panelcategory.comms.netbeans;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Properties;
import org.apache.plc4x.malbec.projecttype.panelcategory.comms.api.CommConfigRepository;
import org.apache.plc4x.malbec.projecttype.panelcategory.comms.api.CommConfigStorage;
import org.apache.plc4x.malbec.projecttype.panelcategory.comms.api.CommunicationsConfig;
import org.apache.plc4x.malbec.projecttype.panelcategory.model.DeviceConfigData;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Integridad de la carga de {@code comunicacion.xml} (H-01).
 *
 * <p>El fallo estaba en tres capas, y solo una se comportaba mal:
 *
 * <ol>
 *   <li>{@code PlcOpenCommConfigRepositoryImpl.load()} ya propagaba el error de un
 *       XML mal formado.</li>
 *   <li>{@code CommConfigServices.createRepository()} entregaba ese repositorio sin
 *       alterar nada.</li>
 *   <li>{@code reload()} lo envolvia en {@code catch (Throwable)} y sustituania la
 *       configuracion por una vacia, sin registrar nada. Era la unica capa que
 *       perdera informacion, y por eso se extrajo a {@link CommConfigLoader} para
 *       poderla verificar sin el runtime de NetBeans.</li>
 * </ol>
 *
 * <p>La correccion: la carga falla de forma visible, se conserva la configuracion
 * anterior en lugar de vaciarla, y {@code save()} se niega a escribir mientras haya
 * un error de carga pendiente, para que un {@code boot.db} no se reconstruya desde
 * un modelo que no se ha podido leer.
 */
class HMICommunicationModelIntegrityTest {

    private static final String XML_ROTO = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://www.plcopen.org/xml/tc6_0201">
              <instances>
                <configurations>
                  <configuration name="roto">
                    <addData>
                      <data name="http://apache.org/plc4x/malbec/device" handleUnknown="preserve">
            """;

    private static final String XML_VACIO = "";

    @Test
    void elRepositorioPropagaElErrorDeUnXmlMalFormado() {
        CommConfigRepository repo = new CommConfigServices()
                .createRepository(new TextoFijo(XML_ROTO));

        assertThrows(Exception.class, repo::load,
                "H-01, capa 1: la lectura de un XML roto tiene que fallar");
    }

    @Test
    void elCableadoDeNetbeansNoSeTragaElError() {
        CommConfigRepository repo = new CommConfigServices()
                .createRepository(new TextoFijo(XML_ROTO));

        assertThrows(Exception.class, repo::load,
                "H-01, capa 2: crear el repositorio no puede alterar como falla la lectura");
    }

    @Test
    void unArchivoVacioNoSeConsideraUnError() throws Exception {
        CommConfigLoader.Result carga = new CommConfigLoader(
                new CommConfigServices().createRepository(new TextoFijo(XML_VACIO))).load();

        assertEquals(0, carga.config().getDevices().size(),
                "un archivo vacio es un proyecto nuevo, no una carga fallida");
        assertFalse(carga.uuidReparados());
    }

    @Test
    void unXmlDanadoNoPuedeCargarseEnSilencio() {
        CommConfigLoader loader = new CommConfigLoader(
                new CommConfigServices().createRepository(new TextoFijo(XML_ROTO)));

        assertThrows(Exception.class, loader::load,
                "H-01: la carga de un XML danado tiene que fallar, no devolver un modelo vacio");
    }

    @Test
    void laCargaPropagaElFalloTalCualDelRepositorio() {
        Exception original = new IllegalStateException("xml corrupto en el byte 412");
        CommConfigRepository queFalla = new RepositorioQueFalla(original);

        Exception recibido = assertThrows(Exception.class,
                () -> new CommConfigLoader(queFalla).load());

        assertEquals(original, recibido,
                "H-01: el error debe llegar intacto para que el mensaje sea util");
    }

    @Test
    void laCargaRechazaUnaConfiguracionNula() {
        assertThrows(IllegalStateException.class,
                () -> new CommConfigLoader(new RepositorioVacio()).load(),
                "un repositorio que devuelve null es un fallo, no una configuracion vacia");
    }

    @Test
    void laCargaReparaLosUuidsAusentesYLoInforma() throws Exception {
        CommunicationsConfig config = new CommunicationsConfig();
        config.upsertDevice(dispositivo("", "Compresor viejo"));
        config.upsertDevice(dispositivo("uuid-ok", "Compresor"));

        CommConfigLoader.Result carga = new CommConfigLoader(new RepositorioFijo(config)).load();

        assertTrue(carga.uuidReparados(),
                "el modelo debe saber que el fichero necesita reescribirse");
        assertEquals("uuid-ok", carga.config().getDevices().get(1).getUuid(),
                "un dispositivo con UUID no se toca");
        assertFalse(carga.config().getDevices().get(0).getUuid().isBlank(),
                "el dispositivo sin UUID debe recibir uno");
    }

    @Test
    void unaCargaCorrectaNoDejaUuidsQueReparar() throws Exception {
        CommunicationsConfig config = new CommunicationsConfig();
        config.upsertDevice(dispositivo("uuid-ok", "Compresor"));

        CommConfigLoader.Result carga = new CommConfigLoader(new RepositorioFijo(config)).load();

        assertFalse(carga.uuidReparados(),
                "sin UUID ausentes no hay que reescribir el fichero");
    }

    @Disabled("H-01, parte 2: verificar que save() se niega a escribir cuando la carga "
            + "falló. Requiere un HMICommunicationModel constructed con un Project real, y "
            + "en este build no hay FileSystem registrado, asi que el modelo no se puede "
            + "construir desde un test. La guarda es una linea en save(); queda pendiente "
            + "el arnes de NetBeans o una Costura equivalente. Ver docs/OPEN_QUESTIONS.md.")
    @Test
    void unXmlDanadoNoPuedeTerminarEnUnBootDbVacio() {
        fail("Pendiente: construir el modelo sobre un XML roto, invocar save() y comprobar "
                + "que devuelve false y no toca boot.db ni comunicacion.xml.");
    }

    private static DeviceConfigData dispositivo(String uuid, String nombre) {
        Properties p = new Properties();
        p.put("brand", "Siemens");
        p.put("model", "S7-1500");
        p.put("protocol", "S7");
        p.put("deviceName", nombre);
        p.put("deviceKey", "K-" + nombre);
        p.put("description", "descripcion de " + nombre);
        p.put("uuid", uuid);
        p.put("enable", Boolean.TRUE);
        p.put("s88Node", "MD70");
        p.put("s88Uuid", "uuid-md70");
        p.put("specificParameters", "s7:cotp://192.168.0.10:102");
        return new DeviceConfigData(p);
    }

    /** Repositorio que devuelve siempre la misma configuracion. */
    private static final class RepositorioFijo implements CommConfigRepository {

        private final CommunicationsConfig config;

        RepositorioFijo(CommunicationsConfig config) {
            this.config = config;
        }

        @Override
        public CommunicationsConfig load() {
            return config;
        }

        @Override
        public void save(CommunicationsConfig config) {
            // este test solo carga
        }
    }

    /** Repositorio que simula un fallo de lectura. */
    private static final class RepositorioQueFalla implements CommConfigRepository {

        private final Exception error;

        RepositorioQueFalla(Exception error) {
            this.error = error;
        }

        @Override
        public CommunicationsConfig load() throws Exception {
            throw error;
        }

        @Override
        public void save(CommunicationsConfig config) {
            // no llega aqui
        }
    }

    /** Repositorio que devuelve null, que es un fallo distinto de una carga vacia. */
    private static final class RepositorioVacio implements CommConfigRepository {

        @Override
        public CommunicationsConfig load() {
            return null;
        }

        @Override
        public void save(CommunicationsConfig config) {
            // este test solo carga
        }
    }

    /** Almacenamiento que siempre devuelve el mismo texto, sin tocar disco. */
    private static final class TextoFijo implements CommConfigStorage {

        private final String texto;

        TextoFijo(String texto) {
            this.texto = texto;
        }

        @Override
        public String open() {
            return texto;
        }

        @Override
        public OutputStream create() {
            return new ByteArrayOutputStream() {
                @Override
                public void close() {
                    // se descarga y se ignora: este test solo lee
                }
            };
        }
    }
}
