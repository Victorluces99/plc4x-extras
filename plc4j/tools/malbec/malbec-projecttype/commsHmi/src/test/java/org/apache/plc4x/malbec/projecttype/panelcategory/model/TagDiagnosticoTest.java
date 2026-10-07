/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.plc4x.malbec.projecttype.panelcategory.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class TagDiagnosticoTest {

    private static boolean algunoErrores(String tag) {
        return !TagDiagnostico.errores(tag).isEmpty();
    }

    private static boolean algunoAvisos(String tag) {
        return !TagDiagnostico.avisos(tag).isEmpty();
    }

    @Test
    void losTagsBienEscritosNoSeAvisan() {
        assertFalse(algunoErrores("%DB1.DBB0[0..9]:BYTE"), "no debe avisar de un tag válido");
        assertFalse(algunoErrores("%DB231.DBB0[0..1848]:BOOL"));
        assertFalse(algunoErrores("%DB1.DBX0.0:BOOL"));
        assertFalse(algunoErrores("%DB5:10:INT"));
        assertFalse(algunoErrores("%DB1.DBB0:BYTE[1849]"));
        assertFalse(algunoErrores("%M0.0:BOOL"));
        assertFalse(algunoErrores("%MD100:DINT"));
        assertFalse(algunoErrores("40001"));
        assertFalse(algunoAvisos("%DB1.DBB0[0..9]:BYTE"),
                "un tag con rango y tipo no deja avisos");
    }

    @Test
    void unRangoQueNoLoEsSeAvisa() {
        // [%.0.9] con los puntos de mil es el caso que encontró el usuario: el
        // parser no lo entiende y el tag se queda como si fuera un nombre.
        assertTrue(algunoErrores("%DB1.DBB0[.0.9]:BYTE"));
        assertTrue(algunoErrores("%DB1.DBB0[0,9]:BYTE"), "la coma no separa nada");
        assertFalse(algunoErrores("%DB1.DBB0[0..9]:BYTE"), "pero el rango bueno no avisa");
        assertFalse(algunoErrores("%DB1.DBB0[10]:BYTE"), "ni un índice suelto");
    }

    @Test
    void losCorchetesDescompensadosSeAvisan() {
        assertTrue(algunoErrores("%DB1.DBB0[0..9:BYTE"), "falta cerrar el corchete");
        assertTrue(algunoErrores("%DB1.DBB00..9]:BYTE"), "falta abrirlo");
    }

    @Test
    void loQueNoEsUnaDireccionSeAvisa() {
        List<String> errores = TagDiagnostico.errores("-lkñl{l0'o");
        assertFalse(errores.isEmpty(), "un tag sin forma de dirección debe avisar");
        assertTrue(errores.stream().anyMatch(e -> e.contains("no es una dirección válida")),
                "el aviso tiene que decir por qué: " + errores);
    }

    @Test
    void losCaracteresRarosSeAvisan() {
        List<String> errores = TagDiagnostico.errores("%DB1.DBB0[0..9]:ÑBYTE");
        assertFalse(errores.isEmpty(), "la ñ no puede ser parte de una dirección");
    }

    @Test
    void unTagSimbolicoNoSeMarcaComoError() {
        // Los símbolos del S88 son nombres, no direcciones, y son válidos: el panel
        // los deja guardar, pero no admiten variables. Por eso van en errores, que
        // sólo pintan el borde, y no bloquean.
        assertTrue(algunoErrores("Control_Panel.Start_Button"));
        assertTrue(TagDiagnostico.avisos("Control_Panel.Start_Button").isEmpty(),
                "un símbolo no necesita además un aviso de tamaño");
    }

    @Test
    void unTipoDesconocidoSeAvisa() {
        assertTrue(algunoErrores("%DB1.DBB0[0..9]:RAW_BYTE_ARRAY_ALIAS"),
                "un tipo que no existe no debe dejar el área sin tipos");
        assertFalse(algunoErrores("%DB1.DBB0[0..9]:RAW_BYTE_ARRAY"),
                "pero el alias de BYTE sí se reconoce");
    }

    @Test
    void unaDireccionSinTamanoSeAvisaSinMarcarComoError() {
        // %DB1:0 es una dirección válida pero sin tipo ni rango, así que no se sabe
        // cuánto ocupa. Es un aviso: el borde no se pone rojo, pero el tooltip avisa.
        List<String> errores = TagDiagnostico.errores("%DB1:0");
        assertTrue(errores.isEmpty(), "una dirección válida no se marca: " + errores);
        assertTrue(TagDiagnostico.avisos("%DB1:0").size() > 0,
                "debería avisar de que no se sabe el tamaño");
    }

    @Test
    void unBloqueSinDireccionSeAvisa() {
        // %DB1[] y %DB1[0..9] no se pueden leer del PLC: les falta la dirección
        // dentro del bloque. Antes el parser se las tragaba y el área quedaba
        // marcada en verde, con un tamaño de un byte que no existe.
        List<String> vacio = TagDiagnostico.errores("%DB1[]");
        assertTrue(vacio.stream().anyMatch(e -> e.contains("no es una dirección válida")),
                "un bloque sin dirección debe avisar: " + vacio);

        List<String> rango = TagDiagnostico.errores("%DB1[0..9]");
        assertTrue(rango.stream().anyMatch(e -> e.contains("no es una dirección válida")),
                "el rango no sustituye a la dirección: " + rango);
    }

    @Test
    void unTagVacioNoDiceNada() {
        assertEquals(0, TagDiagnostico.errores("").size());
        assertEquals(0, TagDiagnostico.errores(null).size());
        assertEquals(0, TagDiagnostico.errores("   ").size());
        assertEquals(0, TagDiagnostico.avisos("").size());
    }
}