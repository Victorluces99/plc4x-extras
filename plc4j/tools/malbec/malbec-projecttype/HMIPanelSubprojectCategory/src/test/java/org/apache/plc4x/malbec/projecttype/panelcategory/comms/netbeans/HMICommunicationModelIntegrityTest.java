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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import org.apache.plc4x.malbec.projecttype.panelcategory.comms.api.CommConfigRepository;
import org.apache.plc4x.malbec.projecttype.panelcategory.comms.api.CommConfigStorage;
import org.apache.plc4x.malbec.projecttype.panelcategory.comms.api.CommunicationsConfig;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Integridad de la carga de {@code comunicacion.xml} (H-01).
 *
 * <p>El diagnostico de H-01 son tres capas, y este test fija donde esta el
 * problema y donde no:
 *
 * <ol>
 *   <li>{@code PlcOpenCommConfigRepositoryImpl.load()} propaga el error de un XML
 *       mal formado (cubierto tambien en {@code GoldenFileContractTest}).</li>
 *   <li>{@code CommConfigServices.createRepository()} entrega ese mismo repositorio
 *       sin tragarse nada. Este test lo comprueba.</li>
 *   <li>{@code HMICommunicationModel.reload()} envuelve la llamada en
 *       {@code catch (Throwable)} y sustituye la configuracion por una vacia, sin
 *       registrar nada. Ahi es donde se pierde el error.</li>
 * </ol>
 *
 * <p>Los tests de la capa 3 estan deshabilitados porque {@code reload()} exige un
 * {@code Project} de NetBeans, y en este build no hay ningun {@code FileSystem}
 * registrado: {@code FileUtil.toFileObject(File)} devuelve {@code null} para
 * cualquier directorio real, ya que el layer de modulos no corre bajo surefire y
 * {@code org-openide-filesystems-nb} no esta en el classpath de test. Verificar
 * esa capa de verdad exige el arnes de pruebas de NetBeans o extraer la carga a un
 * punto inyectable; la decision queda en {@code docs/OPEN_QUESTIONS.md}.
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
        CommConfigRepository repo = new CommConfigServices()
                .createRepository(new TextoFijo(XML_VACIO));

        CommunicationsConfig config = repo.load();

        assertEquals(0, config.getDevices().size(),
                "un archivo vacio es un proyecto nuevo, no una carga fallida");
    }

    @Disabled("H-01: reload() se traga el error y devuelve una configuracion vacia. "
            + "Ademas reload() necesita un Project de NetBeans y en este build no hay "
            + "FileSystem registrado, asi que ni siquiera es invocable desde un test.")
    @Test
    void unXmlDanadoNoPuedeCargarseEnSilencio() {
        fail("Pendiente de H-01: invocar reload() con un XML roto y comprobar que la carga"
                + " fallida llega hasta arriba en lugar de devolver cero dispositivos."
                + " Requiere arnes de NetBeans o un punto de carga inyectable."
                + " Ver docs/OPEN_QUESTIONS.md.");
    }

    @Disabled("H-01: con el error tragado, guardar desde el modelo ya vacio reconstruye "
            + "boot.db sin ningun dispositivo. Misma limitacion de arnes que el test anterior.")
    @Test
    void unXmlDanadoNoPuedeTerminarEnUnBootDbVacio() {
        fail("Pendiente de H-01: comprobar que save() no puede volcar a boot.db una carga"
                + " que ha fallado. Misma limitacion de arnes que el test anterior."
                + " Ver docs/OPEN_QUESTIONS.md.");
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
