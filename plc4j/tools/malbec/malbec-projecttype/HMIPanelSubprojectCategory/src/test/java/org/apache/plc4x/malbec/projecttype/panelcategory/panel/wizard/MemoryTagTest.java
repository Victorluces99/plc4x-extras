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
package org.apache.plc4x.malbec.projecttype.panelcategory.panel.wizard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * El tag se interpreta con la gramática del driver S7 de PLC4X, que es la
 * notación de TIA Portal: {@code %{Área}{dirección}[{selección}]:{TIPO}}.
 *
 * <p>Hay tres cosas que se prueban aparte y que se suelen confundir al leer el
 * código:
 *
 * <ul>
 *   <li><strong>La selección va antes de los dos puntos</strong> y la cantidad
 *       después. No es lo mismo {@code %DB1.DBB0[10]:BYTE}, que es un elemento, que
 *       {@code %DB1.DBB0:BYTE[10]}, que son diez.</li>
 *   <li><strong>El tipo va tras los dos puntos</strong>, y es la familia del área.</li>
 *   <li><strong>Ningún tag se rechaza</strong> por su formato. Cuando el driver es
 *       el que va a validar la dirección contra el PLC real, rechazarla aquí sólo
 *       impediría crear áreas que sí funcionan.</li>
 * </ul>
 */
class MemoryTagTest {

    // --- la gramática de PLC4X ------------------------------------------------

    @Test
    void laFormaGeneralConRangoYTipo() {
        // %DB231.DBB0[0..1848]:BYTE, tal como se copia de TIA Portal.
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
    void laCantidadVaDespuesDelTipo() {
        // La otra forma de acotar el área: :BYTE[1849] son mil ochocientas
        // cuarenta y nueve bytes, y el corchete va detrás del tipo.
        MemoryTag t = MemoryTag.parse("%DB231.DBB0:BYTE[1849]");
        assertEquals("DB231", t.block());
        assertEquals("DBB", t.code());
        assertEquals("BYTE", t.family());
        assertFalse(t.hasRange(), "aquí no hay rango, hay cantidad");
        assertEquals(1849, t.byteCapacity());
    }

    @Test
    void unIndiceSueltoAntesDelTipoEsUnSoloElemento() {
        // %DB1.DBB0[10]:BYTE es el byte 10 y nada más. No son diez bytes: por eso
        // el orden importa, porque :BYTE[10] serían diez.
        MemoryTag t = MemoryTag.parse("%DB1.DBB0[10]:BYTE");
        assertEquals(10, t.firstByte());
        assertEquals(10, t.lastByte());
        assertEquals(1, t.byteCapacity(), "un índice es un elemento, no diez");
    }

    @Test
    void laFormaCortaDeBloqueSeAcepta() {
        // DB1:0:INT omite el .DB y el código corto, y el driver la trata igual.
        MemoryTag t = MemoryTag.parse("%DB1:0:INT");
        assertEquals("DB1", t.block());
        assertEquals(0, t.baseByte());
        assertEquals("INT", t.family());
        assertEquals(-1, t.bit(), "sin punto final no hay offset de bit");
    }

    @Test
    void lasDireccionesDeBitLlevanElOffsetAlFinal() {
        // Sólo se usa con BOOL, y el offset va de 0 a 7.
        MemoryTag m = MemoryTag.parse("%M0.0:BOOL");
        assertEquals("M", m.code());
        assertEquals(0, m.baseByte());
        assertEquals(0, m.bit());
        assertEquals("BOOL", m.family());
        assertEquals(1, m.codeBits());

        MemoryTag db = MemoryTag.parse("%DB1.DBX0.0:BOOL");
        assertEquals("DB1", db.block());
        assertEquals("DBX", db.code());
        assertEquals(0, db.bit());
        assertEquals(1, db.codeBits());
    }

    @Test
    void laFormaCortaTambienAdmiteBit() {
        MemoryTag t = MemoryTag.parse("%DB1:0.0:BOOL");
        assertEquals("DB1", t.block());
        assertEquals(0, t.baseByte());
        assertEquals(0, t.bit());
        assertEquals(1, t.codeBits());
    }

    @Test
    void elPorcentajeYElTipoSonOpcionales() {
        // El driver los acepta y sin ellos, si es un MW, el tamaño sale del código.
        MemoryTag con = MemoryTag.parse("%MW20:INT");
        MemoryTag sin = MemoryTag.parse("MW20");
        assertEquals("MW", con.code());
        assertEquals("MW", sin.code());
        assertEquals("INT", con.family());
        assertNull(sin.family());
        assertEquals(con.codeBits(), sin.codeBits(), "el % no cambia el código");
        assertEquals(2, sin.byteCapacity(), "un word son dos bytes aunque no declare nada");
    }

    @Test
    void unCodigoDeAreaPeladoDaSuTamano() {
        // Sin tipo ni rango, el tamaño sale del código de área.
        assertEquals(1, MemoryTag.parse("%MB20").byteCapacity());
        assertEquals(2, MemoryTag.parse("%MW20").byteCapacity());
        assertEquals(4, MemoryTag.parse("%MD20").byteCapacity());
        assertEquals(0, MemoryTag.parse("%M0.0").byteCapacity(),
                "un bit no ocupa un byte entero, así que no acota");
    }

    @Test
    void lasMarcasEntradasYSalidasSeAceptan() {
        assertEquals(1, MemoryTag.parse("%I0.0").codeBits());
        assertEquals(1, MemoryTag.parse("%Q0.0").codeBits());
        assertEquals("QB", MemoryTag.parse("%QB20").code());
    }

    @Test
    void laLongitudDeclaradaDeUnStringSeAcepta() {
        // :STRING(20) acota a veinte caracteres, que es la extensión del driver.
        MemoryTag t = MemoryTag.parse("%DB1.DBB0:STRING(20)");
        assertEquals("STRING", t.family());
        assertEquals(20, t.byteCapacity());
    }

    // --- códigos de área ------------------------------------------------------

    @Test
    void elCodigoDeAreaDaElTamano() {
        // El tamaño sale de la última letra del código corto de PLC4X. El orden
        // importa: DBLD termina en D y es una doble palabra larga, 64 bits, no 32.
        assertEquals(1, MemoryTag.parse("%DB21.DBX7[0..3]").codeBits());
        assertEquals(8, MemoryTag.parse("%DB21.DBB7[0..3]").codeBits());
        assertEquals(16, MemoryTag.parse("%DB21.DBW7[0..3]").codeBits());
        assertEquals(32, MemoryTag.parse("%DB21.DBD7[0..3]").codeBits());
        assertEquals(64, MemoryTag.parse("%DB21.DBLD7[0..3]").codeBits(),
                "DBLD es de ocho bytes, no de cuatro");
        assertEquals(8, MemoryTag.parse("%MB7[0..3]").codeBits());
        assertEquals(32, MemoryTag.parse("%MD7[0..3]").codeBits());
    }

    @Test
    void unCodigoQueNoSeReconoceNoDaUnTamanoInventado() {
        assertEquals(0, MemoryTag.parse("%DB21.DBZ7[0..3]").codeBits(),
                "sin código conocido sólo se puede usar el rango");
        assertEquals(4, MemoryTag.parse("%DB21.DBZ7[0..3]").byteCapacity(),
                "el rango sigue mandando aunque el código no se conozca");
    }

    // --- rango y bytes -------------------------------------------------------

    @Test
    void elRangoEsRelativoAlByteBaseDelTag() {
        // %DB21.DBB4[0..9] son los bytes 4 a 13 del DB21: diez bytes, no del 0 al 9.
        MemoryTag t = MemoryTag.parse("%DB21.DBB4[0..9]:BYTE");
        assertEquals(4, t.absoluteFirst());
        assertEquals(13, t.absoluteLast());
        assertEquals(10, t.byteCapacity());
    }

    @Test
    void unRangoQueEmpiezaEnOtroSitioSeDesplazaIgual() {
        MemoryTag t = MemoryTag.parse("%DB21.DBB4[10..19]:BYTE");
        assertEquals(4, t.baseByte());
        assertEquals(10, t.firstByte());
        assertEquals(19, t.lastByte());
        assertEquals(14, t.absoluteFirst());
        assertEquals(23, t.absoluteLast());
    }

    @Test
    void unRangoDeUnSoloByteCabe() {
        MemoryTag t = MemoryTag.parse("%DB21.DBB0[7..7]:BYTE");
        assertEquals(1, t.byteCapacity());
        assertEquals(7, t.absoluteFirst());
        assertEquals(7, t.absoluteLast());
    }

    @Test
    void elRangoAlRevesSeMarcaEnLieuDeRechazarse() {
        MemoryTag t = MemoryTag.parse("%DB21.DBB0[1848..0]:BYTE");
        assertFalse(t.rangoValido(), "es un error de tecleo y hay que avisar");
        assertEquals(0, t.byteCapacity(), "pero no se calcula nada con él");
    }

    @Test
    void sinRangoNiCantidadNoSeAcotaLaCapacidad() {
        assertEquals(0, MemoryTag.parse("%DB1:0:INT").byteCapacity(),
                "la forma corta sin selección no dice cuántos bytes son");
    }

    @Test
    void elEspacioAlrededorYEnMedioSeIgnora() {
        assertEquals(1849, MemoryTag.parse("  %DB231.DBB0[0..1848]:BYTE  ").byteCapacity());
        // "%DB20. DBB4" con un espacio de más se escribía a menudo.
        MemoryTag conEspacio = MemoryTag.parse("%DB20. DBB4[0..16]:REAL");
        assertEquals("DB20", conEspacio.block());
        assertEquals("DBB", conEspacio.code());
        assertEquals(4, conEspacio.baseByte());
        assertEquals("REAL", conEspacio.family());
        assertEquals(17, conEspacio.byteCapacity());
    }

    // --- los ejemplos del documento del driver --------------------------------

    @Test
    void losEjemplosDelDriverSeInterpretanComoSeDice() {
        // %DB1.DBX0.0:BOOL, el bit suelto del bloque 1, byte 0, bit 0.
        MemoryTag bit = MemoryTag.parse("%DB1.DBX0.0:BOOL");
        assertEquals("DB1", bit.block());
        assertEquals("DBX", bit.code());
        assertEquals(0, bit.baseByte());
        assertEquals(0, bit.bit());
        assertEquals("BOOL", bit.family());

        // %DB10.DBW20:INT, un entero de 16 bits en el bloque 10, byte 20.
        MemoryTag entero = MemoryTag.parse("%DB10.DBW20:INT");
        assertEquals("DB10", entero.block());
        assertEquals("DBW", entero.code());
        assertEquals(20, entero.baseByte());
        assertEquals(2, entero.byteCapacity());

        // %I0.0:BOOL, el bit suelto del byte 0 de entradas.
        assertEquals("I", MemoryTag.parse("%I0.0:BOOL").code());
        assertEquals(0, MemoryTag.parse("%I0.0:BOOL").bit());

        // %MD100:DINT, un entero de 32 bits en el byte 100 de marcas.
        MemoryTag marca = MemoryTag.parse("%MD100:DINT");
        assertEquals("MD", marca.code());
        assertEquals(100, marca.baseByte());
        assertEquals("DINT", marca.family());

        // %DB1.DBB0[0..9]:BYTE, diez bytes desde el bloque 1, byte 0.
        assertEquals(10, MemoryTag.parse("%DB1.DBB0[0..9]:BYTE").byteCapacity());

        // %DB5:10:INT, con la forma corta del bloque.
        MemoryTag corto = MemoryTag.parse("%DB5:10:INT");
        assertEquals("DB5", corto.block());
        assertEquals(10, corto.baseByte());
        assertEquals("INT", corto.family());

        // %DB1.DBB0[0..15]:RAW_BYTE_ARRAY, dieciséis bytes en crudo, alias de BYTE.
        MemoryTag crudo = MemoryTag.parse("%DB1.DBB0[0..15]:RAW_BYTE_ARRAY");
        assertEquals(16, crudo.byteCapacity());
        assertEquals("RAW_BYTE_ARRAY", crudo.family());

        // %DB1.DB0:STRING(80) y %DB1.DB0:STRING, sin código corto en medio.
        assertEquals("DB1", MemoryTag.parse("%DB1.DB0:STRING(80)").block());
        assertEquals("", MemoryTag.parse("%DB1.DB0:STRING(80)").code());
        assertEquals(80, MemoryTag.parse("%DB1.DB0:STRING(80)").byteCapacity());
        assertEquals("STRING", MemoryTag.parse("%DB1.DB0:STRING").family());
    }

    @Test
    void lasDireccionesEspecialesNoSonDireccionesDeMemoria() {
        // ALM y QUERY:ALARM_S no son direcciones: son servicios del driver. No tienen
        // que rechazarse, pero tampoco pueden compararse como si ocuparan bytes.
        MemoryTag alarma = MemoryTag.parse("ALM");
        assertFalse(alarma.tieneDireccion());
        assertFalse(DataType.candidatos(alarma.family(), alarma.codeBits()).size() > 0,
                "no debe ofrecer tipos");

        MemoryTag consulta = MemoryTag.parse("QUERY:ALARM_S");
        assertFalse(consulta.tieneDireccion());
        assertTrue(DataType.candidatos(consulta.family(), consulta.codeBits()).isEmpty());
    }

    // --- lo que no es una dirección -------------------------------------------

    @Test
    void unaDireccionDeModbusSeAceptaTalCual() {
        MemoryTag t = MemoryTag.parse("40001");
        assertEquals(40001, t.baseByte());
        assertNull(t.family());
        assertEquals(0, t.byteCapacity(), "sin código ni rango no se puede acotar");
        assertEquals("", t.code());
        assertEquals("", t.block());
        assertTrue(t.tieneDireccion(), "el número es una dirección y sí se puede comparar");
        assertFalse(t.solapaCon(MemoryTag.parse("40002")));
        assertTrue(t.solapaCon(MemoryTag.parse("40001")));
    }

    @Test
    void unTagSimbolicoNoSeInterpreta() {
        MemoryTag t = MemoryTag.parse("Control_Panel.Start_Button");
        assertNotNull(t, "un tag simbólico nunca debe rechazarse");
        assertEquals("Control_Panel.Start_Button", t.raw());
        assertEquals("", t.code());
        assertEquals(-1, t.baseByte(), "no se inventa un número de byte");
        assertEquals(0, t.byteCapacity());
        assertFalse(t.tieneDireccion(), "y por tanto no se compara con nadie");
    }

    @Test
    void unTagSimbolicoConNumeroAlFinalNoSeConfundeConUnBit() {
        // El 1 de .Motor1 es parte del nombre: si se tomara como bit, la dirección se
        // quedaría sin número y el tag perdería el sentido.
        MemoryTag t = MemoryTag.parse("Data_Block_1.Motor1");
        assertEquals(-1, t.bit(), "el 1 final es parte del nombre, no un bit");
        assertEquals("", t.code(), "no es un bloque con código, es un nombre");
        assertFalse(t.tieneDireccion());
    }

    @Test
    void unTagSimbolicoNoSeParteEnDosPorUnosPuntos() {
        // Puede ocurrir que lo que hay tras los dos puntos parezca un tipo. Peor es
        // que el área se quede sin tipos que ofrecer que inventar uno.
        MemoryTag t = MemoryTag.parse("Struct.Field:Subfield");
        assertEquals(0, DataType.candidatos(t.family(), t.codeBits()).size(),
                "una familia inventada no debe ofrecer tipos");
    }

    @Test
    void soloUnTagVacioDevuelveNull() {
        assertNull(MemoryTag.parse(null));
        assertNull(MemoryTag.parse(""));
        assertNull(MemoryTag.parse("   "));
    }

    @Test
    void cualquierTagRaroSeAceptaYNoRevienta() {
        String[] raros = {
            "DB21.DBB0[0..9",              // falta el corchete de cierre
            "%DB21.DBB0[0..9]:BYTE:B",      // sobra una familia
            "%DB21.DBB0[0,9]",              // el rango va con dos puntos
            "%DB21.DBB0[",                  // corchete sin cerrar
            "%DB21.DBB0:STRING(",           // longitud sin cerrar
            "basura",
            "%%",
            "%",
            "Data.1.Struct.Field",
            "%DB231.DBB0[0..1848]:BOOL:extra",
        };
        for (String tag : raros) {
            assertNotNull(MemoryTag.parse(tag), "el tag '" + tag + "' no debería rechazarse");
        }
    }

    // --- mensajes e identidad ------------------------------------------------

    @Test
    void elTagOriginalSeConservaParaLosMensajes() {
        // Se quitan los espacios para entender el tag, pero el texto tal como lo
        // escribió el usuario es lo que se le enseña en los avisos.
        assertEquals("%DB20. DBB4[0..16]:REAL",
                MemoryTag.parse("%DB20. DBB4[0..16]:REAL").raw());
        assertEquals("  %DB21.DBB4[0..9]:BYTE  ",
                MemoryTag.parse("  %DB21.DBB4[0..9]:BYTE  ").raw());
    }

    @Test
    void laDireccionEsLaParteSinSeleccionNiTipo() {
        // La dirección dice en qué sitio del PLC se parte. Dos áreas con la misma
        // dirección y rangos distintos conviven: lo único que marca un error es que
        // sus rangos se toquen.
        assertEquals("DB21.DBB4", MemoryTag.parse("%DB21.DBB4[0..9]:INTEGER").direccion());
        assertEquals("DB21.DBB4", MemoryTag.parse("%DB21.DBB4[0..10]").direccion());
        assertEquals("DB21.DBB4", MemoryTag.parse("%DB21.DBB4:INT").direccion());
        assertEquals("MW66", MemoryTag.parse("%MW66:WORD").direccion());
        assertEquals("M0.0", MemoryTag.parse("%M0.0:BOOL").direccion(),
                "el bit forma parte de la dirección");
        assertEquals("DB1.0.0", MemoryTag.parse("%DB1:0.0:BOOL").direccion(),
                "en la forma corta el código no está, pero el sitio sí");
        assertEquals("40001", MemoryTag.parse("40001").direccion());
    }

    @Test
    void unTagSinDireccionDevuelveElTagEnteroComoIdentidad() {
        assertEquals("Control_Panel.Start_Button",
                MemoryTag.parse("Control_Panel.Start_Button").direccion());
        assertEquals("basura", MemoryTag.parse("basura").direccion());
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

    // --- solapamiento --------------------------------------------------------

    @Test
    void dosAreasDelMismoBloqueSeDetectan() {
        MemoryTag grande = MemoryTag.parse("%DB231.DBB0[0..1848]:BYTE");
        assertTrue(grande.solapaCon(MemoryTag.parse("%DB231.DBB10[0..5]:BYTE")));
        assertTrue(grande.solapaCon(MemoryTag.parse("DB231.DBB1848[0..0]:BYTE")),
                "da igual si el usuario pone el % o no");
        assertFalse(grande.solapaCon(MemoryTag.parse("%DB231.DBB3000[0..5]:BYTE")));
    }

    @Test
    void areasDeBloquesDistintosNoSePisan() {
        MemoryTag a = MemoryTag.parse("%DB231.DBB0[0..1848]:BYTE");
        assertFalse(a.solapaCon(MemoryTag.parse("%DB232.DBB0[0..1848]:BYTE")),
                "DB231 y DB232 son sitios distintos");
    }

    @Test
    void conRangoSeComparanLosBytesAunqueElCodigoSeaDistinto() {
        // Con los dos rangos declarados lo que manda son los bytes, no el código: en
        // un DB de verdad DBB0 y DBW0 ocupan la misma memoria, y fingir lo contrario
        // dejaría pasar dos áreas que el PLC leería superpuestas.
        MemoryTag a = MemoryTag.parse("%DB231.DBB0[0..1848]:BYTE");
        assertTrue(a.solapaCon(MemoryTag.parse("%DB231.DBW0[0..100]:WORD")),
                "los dos ocupan los mismos bytes del mismo bloque");
    }

    @Test
    void cadaBitEsUnSitioDistinto() {
        // %M0.0 y %M0.1 comparten byte, pero no son el mismo sitio: si se comparara
        // sólo el byte base se avisaría de un conflicto que no existe.
        MemoryTag bit0 = MemoryTag.parse("%M0.0:BOOL");
        assertTrue(bit0.solapaCon(MemoryTag.parse("%M0.0:BOOL")), "el mismo bit sí se repite");
        assertFalse(bit0.solapaCon(MemoryTag.parse("%M0.1:BOOL")), "otro bit es otro sitio");
        assertFalse(bit0.solapaCon(MemoryTag.parse("%M1.0:BOOL")), "otro byte es otro sitio");
        assertFalse(bit0.solapaCon(MemoryTag.parse("%DB1.DBX0.0:BOOL")),
                "otra área de memoria es otro sitio");
    }

    @Test
    void unAreaSinDireccionNoPisaANadie() {
        MemoryTag grande = MemoryTag.parse("%DB231.DBB0[0..1848]:BYTE");
        assertFalse(grande.solapaCon(MemoryTag.parse("Control_Panel.Start")));
        assertFalse(grande.solapaCon(MemoryTag.parse("40001")),
                "una dirección Modbus no se mide en bytes de bloque");
        assertFalse(grande.solapaCon(null));
    }
}