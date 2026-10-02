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
package org.apache.plc4x.malbec.projecttype.panelcategory.panel.wizard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * El tag depende del driver y del PLC, así que el parser tiene que ser
 * permisivo: se aparta lo que reconoce y deja el resto intacto.
 *
 * <p>La regla que se prueba aquí es la importante: <strong>ningún tag se
 * rechaza por su formato</strong>. Cuando el parser se puso estricto, el usuario
 * no podía crear ni un área con tags que él usaba a diario, y eso pesa más que
 * adivinar un tamaño que luego tampoco se puede aplicar.</p>
 */
class MemoryTagTest {

    // --- formas aceptadas ---------------------------------------------------

    @Test
    void unTagAbsolutoDeS7ConRangoYTipoSeInterpretaCompleto() {
        MemoryTag t = MemoryTag.parse("%DB231.DBB0[0..1848]:BYTE");
        assertEquals("DB231", t.block());
        assertEquals("DBB", t.code());
        assertEquals(0, t.baseByte());
        assertEquals(0, t.firstByte());
        assertEquals(1848, t.lastByte());
        assertEquals("BYTE", t.family());
        assertTrue(t.hasRange());
        assertEquals(1849, t.byteCapacity());
    }

    @Test
    void unTagDeS7ClasicoSinPorcentajeSeInterpretaIgual() {
        // DB1.DBX0.0 es la misma dirección que %DB1.DBX0.0: el % es opcional.
        MemoryTag t = MemoryTag.parse("DB1.DBX0.0");
        assertEquals("DB1", t.block());
        assertEquals("DBX", t.code());
        assertEquals(0, t.baseByte());
        assertEquals(0, t.bit());
        assertNull(t.family());
        assertEquals(1, t.codeBits());
    }

    @Test
    void losMarcadoresYLasEntradasYSalidasAceptanElPorcentaje() {
        assertEquals(1, MemoryTag.parse("%M0.0").codeBits());
        assertEquals(1, MemoryTag.parse("%I0.0").codeBits());
        assertEquals(1, MemoryTag.parse("%Q0.0").codeBits());
        assertEquals(2, MemoryTag.parse("%MW20").byteCapacity());
        assertEquals("MW", MemoryTag.parse("%MW20").code());
    }

    @Test
    void unTagDeMarcaOEntradaSinPorcentajeTambienSeAcepta() {
        assertEquals(1, MemoryTag.parse("M0.0").codeBits());
        assertEquals(2, MemoryTag.parse("MW2").byteCapacity());
    }

    @Test
    void unTagEscalarConTipoNoTieneRango() {
        MemoryTag t = MemoryTag.parse("%MW66:WORD");
        assertEquals("MW", t.code());
        assertEquals(66, t.baseByte());
        assertFalse(t.hasRange());
        assertEquals("WORD", t.family());
        assertEquals(2, t.byteCapacity(), "un WORD ocupa dos bytes aunque no declare rango");
    }

    @Test
    void unTagEscalarPeladoSeAcepta() {
        MemoryTag t = MemoryTag.parse("%MW66");
        assertEquals(66, t.baseByte());
        assertFalse(t.hasRange());
        assertNull(t.family());
        assertEquals(16, t.codeBits());
        assertEquals(2, t.byteCapacity());
    }

    @Test
    void unaDireccionDeModbusSeAceptaTalCual() {
        MemoryTag t = MemoryTag.parse("40001");
        assertEquals(40001, t.baseByte());
        assertNull(t.family());
        assertEquals(0, t.byteCapacity(), "sin código ni rango no se puede acotar");
        assertEquals("", t.code());
        assertEquals("", t.block());
        assertTrue(t.tieneDireccion(), "el número es una dirección y sí se puede comparar");
        // Dos direcciones distintas no se pisan, pero la misma repetida sí avisa.
        assertFalse(t.solapaCon(MemoryTag.parse("40002")));
        assertTrue(t.solapaCon(MemoryTag.parse("40001")));
    }

    @Test
    void unTagSimbolicoSeGuardaTalCualSinInterpretar() {
        MemoryTag t = MemoryTag.parse("Control_Panel.Start_Button");
        assertNotNull(t, "un tag simbólico nunca debe rechazarse");
        assertEquals("Control_Panel.Start_Button", t.raw());
        assertEquals("", t.code());
        assertEquals("", t.block());
        assertEquals(0, t.baseByte());
        assertEquals(0, t.byteCapacity());
        assertFalse(t.tieneDireccion());
    }

    @Test
    void unTagSimbolicoConNumeroAlFinalNoSeConfundeConUnBit() {
        // El 1 de .Motor1 es parte del nombre: si se tomara como bit, el resto
        // dejaría de ser una dirección y el tag se quedaría sin nada que interpretar.
        MemoryTag t = MemoryTag.parse("Data_Block_1.Motor1");
        assertEquals(-1, t.bit(), "el 1 final es parte del nombre, no un bit");
        assertEquals("Motor", t.code(), "se guarda como bloque y código, aunque no sean reales");
        assertEquals(0, t.codeBits(), "y no se le inventa un tamaño");
        assertEquals(0, t.byteCapacity());
    }

    // --- rango --------------------------------------------------------------

    @Test
    void elRangoEsRelativoAlByteBaseDelTag() {
        // %DB21.DBB4[0..9] son los bytes 4 a 13 del DB21: diez bytes, no del 0 al 9.
        MemoryTag t = MemoryTag.parse("%DB21.DBB4[0..9]");
        assertEquals(4, t.absoluteFirst());
        assertEquals(13, t.absoluteLast());
        assertEquals(10, t.byteCapacity());
    }

    @Test
    void unRangoQueEmpiezaEnOtroSitioSeDesplazaIgual() {
        MemoryTag t = MemoryTag.parse("%DB21.DBB4[10..19]");
        assertEquals(4, t.baseByte());
        assertEquals(10, t.firstByte());
        assertEquals(19, t.lastByte());
        assertEquals(14, t.absoluteFirst());
        assertEquals(23, t.absoluteLast());
        assertEquals(10, t.byteCapacity());
    }

    @Test
    void unRangoDeUnSoloByteCabe() {
        MemoryTag t = MemoryTag.parse("%DB21.DBB0[7..7]");
        assertEquals(1, t.byteCapacity());
        assertEquals(7, t.absoluteFirst());
        assertEquals(7, t.absoluteLast());
    }

    @Test
    void elRangoAlRevesSeMarcaEnLieuDeRechazarse() {
        MemoryTag t = MemoryTag.parse("%DB21.DBB0[1848..0]");
        assertFalse(t.rangoValido(), "es un error de tecleo y hay que avisar");
        assertEquals(0, t.byteCapacity(), "pero no se calcula nada con él");
    }

    @Test
    void elEspacioAlrededorDelTagSeIgnora() {
        assertEquals(1849, MemoryTag.parse("  %DB231.DBB0[0..1848]:BOOL  ").byteCapacity());
    }

    // --- códigos de área -----------------------------------------------------

    @Test
    void elCodigoDeAreaMandaEnLosBits() {
        assertEquals(1, MemoryTag.parse("%DB21.DBX7[0..3]").codeBits());
        assertEquals(8, MemoryTag.parse("%DB21.DBB7[0..3]").codeBits());
        assertEquals(16, MemoryTag.parse("%DB21.DBW7[0..3]").codeBits());
        assertEquals(32, MemoryTag.parse("%DB21.DBD7[0..3]").codeBits());
        assertEquals(64, MemoryTag.parse("%DB21.DBLD7[0..3]").codeBits());
        assertEquals(8, MemoryTag.parse("%DB21.MB7[0..3]").codeBits());
        assertEquals(32, MemoryTag.parse("%DB21.MD7[0..3]").codeBits());
    }

    @Test
    void unCodigoQueNoSeReconoceNoDaUnTamanoInventado() {
        assertEquals(0, MemoryTag.parse("%DB21.DBZ7[0..3]").codeBits(),
                "sin código conocido sólo se puede usar el rango");
        assertEquals(4, MemoryTag.parse("%DB21.DBZ7[0..3]").byteCapacity(),
                "el rango sigue mandando aunque el código no se conozca");
    }

    @Test
    void sinCodigoNiRangoNoSeAcotaLaCapacidad() {
        assertEquals(0, MemoryTag.parse("Control_Panel.Start").byteCapacity());
        assertEquals(-1, MemoryTag.parse("40001").bit());
    }

    @Test
    void losEspaciosDelMedioNoRompenElDesglose() {
        // "%DB20. DBB4" con un espacio de más se escribía a menudo. Mientras el
        // espacio estuvo, el tag no se reconocía como bloque y se comparaba como
        // texto, donde el rango hacía que [0..16] y [0..10] parecieran distintos y
        // las dos áreas se colaban.
        MemoryTag t = MemoryTag.parse("%DB20. DBB4[0..16]:REAL");
        assertEquals("DB20", t.block());
        assertEquals("DBB", t.code());
        assertEquals(4, t.baseByte());
        assertEquals(16, t.lastByte());
        assertEquals("REAL", t.family());
        assertEquals("DB20.DBB4", t.direccion());

        assertEquals(t.direccion(),
                MemoryTag.parse("%DB20.DBB4[0..10]:REAL").direccion(),
                "con y sin espacio, la misma dirección");
        assertEquals(t.direccion(),
                MemoryTag.parse("  %db20.dbb4 [ 0 .. 10 ] : REAL ").direccion(),
                "ni el espacio alrededor de los dos puntos ni el corchete influyen");
    }

    @Test
    void elTagOriginalSeConservaParaLosMensajes() {
        // Se quitan los espacios para entender el tag, pero el texto tal como lo
        // escribió el usuario es lo que se le enseña en los avisos.
        assertEquals("%DB20. DBB4[0..16]:REAL",
                MemoryTag.parse("%DB20. DBB4[0..16]:REAL").raw());
    }

    @Test
    void laDireccionEsLaParteSinRangoNiTipo() {
        // La dirección dice en qué sitio del PLC se parte, y el rango cuánto ocupa a
        // partir de ahí. Por eso %DB21.DBB4[0..9]:INTEGER y %DB21.DBB4[0..10] dan la
        // misma dirección: no son la misma zona, pero arrancan en el mismo byte, y
        // eso no es un error por sí solo. Sólo lo sería que sus rangos se tocaran.
        assertEquals("DB21.DBB4", MemoryTag.parse("%DB21.DBB4[0..9]:INTEGER").direccion());
        assertEquals("DB21.DBB4", MemoryTag.parse("%DB21.DBB4[0..10]").direccion());
        assertEquals("DB21.DBB4", MemoryTag.parse("db21.dbb4").direccion());
        assertEquals("MW66", MemoryTag.parse("%MW66:WORD").direccion());
        assertEquals("M0.0", MemoryTag.parse("%M0.0").direccion(),
                "el bit forma parte de la dirección");
        assertEquals("40001", MemoryTag.parse("40001").direccion());
    }

    @Test
    void dosRangosSeguidosEnLaMismaDireccionNoSePisan() {
        // El caso que dice el usuario: %DB22.DBB4[10..16] y %DB22.DBB4[17..22].
        // Los bytes absolutos son 14..20 y 21..26, así que la segunda empieza justo
        // donde acaba la primera.
        MemoryTag primera = MemoryTag.parse("%DB22.DBB4[10..16]:REAL");
        MemoryTag segunda = MemoryTag.parse("%DB22.DBB4[17..22]:REAL");
        assertEquals(14, primera.absoluteFirst());
        assertEquals(20, primera.absoluteLast());
        assertEquals(21, segunda.absoluteFirst());
        assertEquals(26, segunda.absoluteLast());
        assertFalse(primera.solapaCon(segunda), "son dos áreas legítimas");
        assertFalse(segunda.solapaCon(primera));
        assertEquals(primera.direccion(), segunda.direccion(),
                "misma dirección de arranque, y aun así no se pisan");
    }

    @Test
    void unTagSinDireccionDevuelveElTagEnteroComoIdentidad() {
        assertEquals("Control_Panel.Start_Button",
                MemoryTag.parse("Control_Panel.Start_Button").direccion());
        assertEquals("basura", MemoryTag.parse("basura").direccion());
    }

    // --- lo que ya no se rechaza --------------------------------------------

    @Test
    void soloUnTagVacioDevuelveNull() {
        assertNull(MemoryTag.parse(null));
        assertNull(MemoryTag.parse(""));
        assertNull(MemoryTag.parse("   "));
    }

    @Test
    void cualquierTagRaroSeAceptaYNoRevienta() {
        String[] raros = {
            "DB21.DBB0[0..9",          // falta el corchete de cierre
            "%DB21.DBB0[0..9]:BYTE:B",  // sobra una familia
            "%DB21.DBB0[0,9]",          // el rango va con dos puntos
            "basura",
            "%%",
            "Data.1.Struct.Field",
            "%DB231.DBB0[0..1848]:BOOL:extra",
        };
        for (String tag : raros) {
            assertNotNull(MemoryTag.parse(tag), "el tag '" + tag + "' no debería rechazarse");
        }
    }

    // --- solapamiento --------------------------------------------------------

    @Test
    void dosAreasDelMismoBloqueConElMismoCodigoSeDetectan() {
        MemoryTag grande = MemoryTag.parse("%DB231.DBB0[0..1848]:BYTE");
        assertTrue(grande.solapaCon(MemoryTag.parse("%DB231.DBB10[0..5]:BYTE")));
        assertTrue(grande.solapaCon(MemoryTag.parse("DB231.DBB1848[0..0]:BYTE")),
                "da igual si el usuario pone el % o no");
        assertFalse(grande.solapaCon(MemoryTag.parse("%DB231.DBB3000[0..5]:BYTE")));
    }

    @Test
    void areasDeBloquesDistintosNoSePisan() {
        MemoryTag a = MemoryTag.parse("%DB231.DBB0[0..1848]:BYTE");
        assertFalse(a.solapaCon(MemoryTag.parse("%DB232.DBB0[0..1848]:BYTE")));
    }

    @Test
    void areasConCodigosDistintosNoSeComparan() {
        // DBB0 y DBW0 se pisan en un DB de verdad, pero distinguirlo exige conocer la
        // disposición del bloque. Es preferible dejar pasar una coincidencia que
        // bloquear un área legítima.
        MemoryTag a = MemoryTag.parse("%DB231.DBB0[0..1848]:BYTE");
        assertFalse(a.solapaCon(MemoryTag.parse("%DB231.DBW0[0..100]:WORD")));
    }

    @Test
    void unaAreaSinDireccionReconocibleNoPisaANadie() {
        MemoryTag grande = MemoryTag.parse("%DB231.DBB0[0..1848]:BYTE");
        assertFalse(grande.solapaCon(MemoryTag.parse("Control_Panel.Start")));
        assertFalse(grande.solapaCon(MemoryTag.parse("40001")));
        assertFalse(MemoryTag.parse("40001").solapaCon(MemoryTag.parse("40002")));
    }

    @Test
    void unTagNuloNoPisaANadie() {
        assertFalse(MemoryTag.parse("%DB231.DBB0[0..9]:BYTE").solapaCon(null));
    }
}