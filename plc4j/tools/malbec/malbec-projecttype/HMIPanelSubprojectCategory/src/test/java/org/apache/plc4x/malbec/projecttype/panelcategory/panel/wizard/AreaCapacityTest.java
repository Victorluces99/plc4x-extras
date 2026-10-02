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

import java.util.List;
import org.apache.plc4x.malbec.projecttype.panelcategory.panel.CommConfigData;
import org.junit.jupiter.api.Test;

/**
 * Capacidad, huecos y solapamiento de las Ã¡reas de memoria.
 *
 * <p>El caso que mÃ¡s importa es {@code unHuecoSeRellenaSinMoverNada}: con la cuenta
 * de variables, borrar una del medio hace que la siguiente caiga encima de otra que
 * sigue viva, y eso no se ve hasta que el PLC lee el valor de la variable
 * equivocada.</p>
 */
class AreaCapacityTest {

    private static final String AREA = "area-1";
    private static final String GRUPO = "grupo-1";

    private static CommConfigData.ItemConfig area(String uuid, String nombre, String tag) {
        return new CommConfigData.ItemConfig(uuid, nombre, "", tag, true, "md5", GRUPO);
    }

    private static CommunicationWizardController controllerCon(CommConfigData.ItemConfig... areas) {
        CommunicationWizardState state = new CommunicationWizardState();
        state.getItems().addAll(List.of(areas));
        return new CommunicationWizardController(null, state);
    }

    private static CommunicationWizardController controllerCon(String tag) {
        return controllerCon(area(AREA, "Temperaturas", tag));
    }

    private static void addPv(CommunicationWizardController c, String uuid, String offset,
            String type) {
        c.getState().getPvs().add(new CommConfigData.PvConfig(
                uuid, "variable-" + uuid, type, AREA, offset, "", "1000", true, false,
                "", "", "", "", "", "", "", "", "md5_pv_hash", "Area/" + uuid));
    }

    // --- capacidad -----------------------------------------------------------

    @Test
    void unAreaDeBytesAdmiteUnBytePorCasilla() {
        CommunicationWizardController c = controllerCon("%DB231.DBB0[0..1848]:BYTE");
        assertEquals(1849, c.areaCapacityBytes(AREA));
        assertEquals(1849, c.areaFreeSlots(AREA, DataType.BYTE));
        assertEquals("0", c.nextOffset(AREA, DataType.BYTE));
    }

    @Test
    void unByteEmpaquetaOchoBooleanos() {
        CommunicationWizardController c = controllerCon("%DB231.DBB0[0..1848]:BOOL");
        assertEquals(1849, c.areaCapacityBytes(AREA));
        assertEquals(1849 * 8, c.areaFreeSlots(AREA, DataType.BOOLEAN));
        assertEquals("0.0", c.nextOffset(AREA, DataType.BOOLEAN));
    }

    @Test
    void losOffsetsBooleanosAvanzanDeBitEnBitYDanLaVueltaEnElByte() {
        CommunicationWizardController c = controllerCon("%DB21.DBB0[0..2]:BOOL");
        for (int i = 0; i < 8; i++) {
            String esperado = (i / 8) + "." + (i % 8);
            assertEquals(esperado, c.nextOffset(AREA, DataType.BOOLEAN),
                    "el booleano " + i + " deberÃ­a caer en " + esperado);
            addPv(c, "p" + i, esperado, "boolean");
        }
        assertEquals("1.0", c.nextOffset(AREA, DataType.BOOLEAN),
                "al llegar a ocho hay que saltar al byte siguiente");
    }

    @Test
    void elOffsetGuardadoEsRelativoAlByteBaseDelTag() {
        CommunicationWizardController c = controllerCon("%DB21.DBB4[0..9]:BOOL");
        assertEquals(10, c.areaCapacityBytes(AREA));
        assertEquals("0.0", c.nextOffset(AREA, DataType.BOOLEAN),
                "el offset 0 del Ã¡rea es el byte 4 del DB21");
    }

    @Test
    void unAreaLlenaNoOfreceMasSitio() {
        CommunicationWizardController c = controllerCon("%DB21.DBB0[0..1]:BYTE");
        addPv(c, "a", "0", "byte");
        addPv(c, "b", "1", "byte");
        assertEquals(0, c.areaFreeSlots(AREA, DataType.BYTE));
        assertNull(c.nextOffset(AREA, DataType.BYTE));
    }

    @Test
    void sinTagNoHayCapacidadQueAcotarNiSeBloquea() {
        CommunicationWizardController c = controllerCon("esto-no-es-un-tag");
        assertEquals(-1, c.areaCapacityBytes(AREA), "sin tag interpretable no hay tamaÃ±o");
        assertEquals(-1, c.areaFreeSlots(AREA, DataType.BYTE));
        assertNull(c.nextOffset(AREA, DataType.BYTE),
                "sin tag no se puede saber dÃ³nde va la siguiente");
    }

    @Test
    void unTipoSinPesoNoSeAceptaNiParaCalcularNiParaOfrecer() {
        CommunicationWizardController c = controllerCon("%DB231.DBB0[0..1848]:BYTE");
        assertEquals(-1, c.areaFreeSlots(AREA, DataType.S7AI));
        assertNull(c.nextOffset(AREA, DataType.STRING));
        assertFalse(c.typeAllowed(AREA, DataType.STRING));
        assertFalse(c.typeAllowed(AREA, DataType.S7AI));
    }

    // --- huecos --------------------------------------------------------------

    @Test
    void unHuecoSeRellenaSinMoverNada() {
        CommunicationWizardController c = controllerCon("%DB21.DBB0[0..3]:BYTE");
        addPv(c, "a", "0", "byte");
        addPv(c, "b", "1", "byte");
        addPv(c, "c", "2", "byte");
        assertEquals("3", c.nextOffset(AREA, DataType.BYTE));

        assertTrue(c.deletePv("b"));
        assertEquals("1", c.nextOffset(AREA, DataType.BYTE),
                "el hueco del 1 se rellena; con la cuenta saldrÃ­a 2 y pisarÃ­a la 'c'");
        assertEquals(2, c.getState().pvsOfArea(AREA).size());
        assertEquals("2", c.getState().pvsOfArea(AREA).get(1).getOffset(),
                "la variable que queda en el 2 no se mueve");
    }

    @Test
    void borrarLaUltimaDejaElFinalLibre() {
        CommunicationWizardController c = controllerCon("%DB21.DBB0[0..3]:BYTE");
        addPv(c, "a", "0", "byte");
        addPv(c, "b", "1", "byte");
        c.deletePv("b");
        assertEquals("1", c.nextOffset(AREA, DataType.BYTE));
    }

    @Test
    void unOffsetIlegibleNoBloqueaElHueco() {
        CommunicationWizardController c = controllerCon("%DB21.DBB0[0..3]:BYTE");
        addPv(c, "a", "no-es-un-offset", "byte");
        assertEquals("0", c.nextOffset(AREA, DataType.BYTE),
                "un offset que no se entiende no puede ocuparse a sÃ­ mismo");
    }

    @Test
    void unIntSaltaDeCuatroEnCuatroBytes() {
        CommunicationWizardController c = controllerCon("%DB21.DBB0[0..19]:INT");
        assertEquals("0", c.nextOffset(AREA, DataType.INT));
        addPv(c, "a", "0", "int");
        assertEquals("4", c.nextOffset(AREA, DataType.INT));
        addPv(c, "b", "4", "int");
        assertEquals("8", c.nextOffset(AREA, DataType.INT));
        addPv(c, "d", "8", "int");
        assertEquals("12", c.nextOffset(AREA, DataType.INT));
        addPv(c, "e", "12", "int");
        assertEquals("16", c.nextOffset(AREA, DataType.INT));
        addPv(c, "f", "16", "int");
        assertNull(c.nextOffset(AREA, DataType.INT),
                "veinte bytes son cinco int y el sexto no cabe");
        assertEquals(0, c.areaFreeSlots(AREA, DataType.INT));
    }

    @Test
    void unIntNoSeColocaEnElByteSueltoQueQuedaAlFinal() {
        // Diez bytes con tres ints: 0, 4 y 8. Quedan los bytes 9 y 10, pero ninguno
        // de los dos puntos de inicio admite un int entero. Devolver el 9 darÃ­a por
        // buena una variable que se saldrÃ­a del Ã¡rea y sÃ³lo se verÃ­a al leer del PLC,
        // asÃ­ que se prefiere decir que estÃ¡ llena.
        CommunicationWizardController c = controllerCon("%DB21.DBB0[0..9]:INT");
        assertEquals(10, c.areaCapacityBytes(AREA));
        addPv(c, "a", "0", "int");
        addPv(c, "b", "4", "int");
        addPv(c, "d", "8", "int");
        assertNull(c.nextOffset(AREA, DataType.INT),
                "un byte suelto no vale para un int de cuatro");
        assertEquals(0, c.areaFreeBytes(AREA),
                "los tres ints cubren los diez bytes: el Ãºltimo se sale por el final");
        assertEquals(0, c.areaFreeSlots(AREA, DataType.INT));
        assertNull(c.nextOffset(AREA, DataType.BYTE),
                "no queda ni un byte entero libre");
    }

    @Test
    void unaVariableSeDetectaAunqueOtraLePiseElCuerpo() {
        // Un int en el byte 0 ocupa del 0 al 3. Si sÃ³lo se marcara el byte 0, una
        // variable de byte en el 2 se solaparÃ­a sin que nada se enterara.
        CommunicationWizardController c = controllerCon("%DB21.DBB0[0..7]:INT");
        addPv(c, "a", "0", "int");
        assertEquals("4", c.nextOffset(AREA, DataType.BYTE),
                "los bytes 0 a 3 estÃ¡n dentro del int, asÃ­ que el byte libre es el 4");
        assertEquals("4", c.nextOffset(AREA, DataType.INT));
        assertNull(c.nextOffset(AREA, DataType.DOUBLE),
                "ocho bytes y un int ocupa cuatro: un double ya no cabe");
    }

    @Test
    void unByteSeColocaEnLoQueDejaUnInt() {
        // Al revÃ©s que el anterior: el int del 0 al 3 deja libres del 4 al 7, y ahÃ­
        // caben bytes aunque el Ã¡rea sea de ints.
        CommunicationWizardController c = controllerCon("%DB21.DBB0[0..7]:INT");
        addPv(c, "a", "0", "int");
        assertEquals("4", c.nextOffset(AREA, DataType.BYTE));
        addPv(c, "b", "4", "byte");
        assertEquals("5", c.nextOffset(AREA, DataType.BYTE));
    }

    @Test
    void losBytesLibresSeCuentanDeLoQueOcupanLasVariables() {
        CommunicationWizardController c = controllerCon("%DB21.DBB0[0..9]:INT");
        assertEquals(10, c.areaFreeBytes(AREA));
        addPv(c, "a", "0", "int");
        assertEquals(6, c.areaFreeBytes(AREA));
        addPv(c, "b", "4", "int");
        assertEquals(2, c.areaFreeBytes(AREA));
    }

    @Test
    void unAreaSinTamanoNoDiceCuantosBytesQuedan() {
        CommunicationWizardController c = controllerCon("Control_Panel.Start_Button");
        assertEquals(-1, c.areaFreeBytes(AREA));
    }

    // --- homogenousidad -----------------------------------------------------

    @Test
    void unTagQueDeclaraTipoFijaElArea() {
        assertEquals(DataType.BOOLEAN,
                controllerCon("%DB231.DBB0[0..9]:BOOL").lockedType(AREA));
        assertEquals(DataType.WORD, controllerCon("%MW66:WORD").lockedType(AREA));
    }

    @Test
    void unAreaVaciaOfreceLosTiposPosiblesYAlElegirUnoSoloQuedaEse() {
        // El ciclo que pide el usuario: vacÃ­a, se ven las opciones; en cuanto se
        // guarda una variable, esa es la Ãºnica que se puede seguir usando.
        CommunicationWizardController c = controllerCon("%DB21.DBLD0[0..7]:REAL");
        assertEquals(List.of(DataType.FLOAT, DataType.DOUBLE), c.typesAllowed(AREA),
                "vacÃ­a, el usuario elige entre los reales que caben");
        assertNull(c.lockedType(AREA));

        addPv(c, "a", "0", "float");
        assertEquals(List.of(DataType.FLOAT), c.typesAllowed(AREA),
                "con un float guardado, el double deja de ofrecerse");
        assertEquals(DataType.FLOAT, c.lockedType(AREA));
        assertTrue(c.typeAllowed(AREA, DataType.FLOAT));
        assertFalse(c.typeAllowed(AREA, DataType.DOUBLE),
                "mezclar tamaÃ±os dejarÃ­a los offsets siguientes apuntando a mitad");
    }

    @Test
    void elTipoElegidoTambienFiltraLasVariablesDePlanta() {
        CommunicationWizardController c = controllerCon("%DB21.DBLD0[0..7]:REAL");
        addPv(c, "a", "0", "double");
        assertEquals(DataType.DOUBLE, c.lockedType(AREA));
        assertTrue(c.areaAdmiteTipo(AREA, "REAL", DataType.DOUBLE));
        assertFalse(c.areaAdmiteTipo(AREA, "REAL", DataType.FLOAT),
                "ya no se puede meter un float en un Ã¡rea de doubles");
        assertFalse(c.areaAdmiteTipo(AREA, "INTEGER", DataType.DOUBLE));
    }

    @Test
    void unAreaSinTipoFijoSegueAdmintiendoTodasLasFamilias() {
        // Sin variables y sin familia en el tag no hay nada que decidir todavÃ­a, asÃ­
        // que no se descarta ninguna variable de planta.
        CommunicationWizardController c = controllerCon("%DB21.DBD0[0..99]");
        assertNull(c.lockedType(AREA));
        assertTrue(c.areaAdmiteTipo(AREA, "REAL", null));
        assertTrue(c.areaAdmiteTipo(AREA, "INTEGER", null));
        assertTrue(c.areaAdmiteTipo(AREA, "BYTE", null));
        assertEquals(12, c.typesAllowed(AREA).size(), "el catÃ¡logo entero de tipos con peso");
    }

    @Test
    void elTagMandaSiLaPrimeraVariableNoEncajaConEl() {
        // Datos importados a mano pueden traer un tipo que el tag no admite. En ese
        // caso gana el tag, que es lo que de verdad describe la memoria del PLC.
        CommunicationWizardController c = controllerCon("%DB21.DBW0[0..99]:WORD");
        addPv(c, "a", "0", "double");
        assertEquals(List.of(DataType.WORD), c.typesAllowed(AREA));
        assertFalse(c.typeAllowed(AREA, DataType.DOUBLE));
    }

    @Test
    void losTagsSinFamiliaTambienSeFijanAlElegir() {
        CommunicationWizardController c = controllerCon("%DB21.DBD0[0..99]");
        addPv(c, "a", "0", "int");
        assertEquals(DataType.INT, c.lockedType(AREA));
        assertEquals(List.of(DataType.INT), c.typesAllowed(AREA),
                "el Ã¡rea se va llenando de un solo tipo aunque el tag no diga nada");
    }

    @Test
    void unAreaRealNoQuedaFijadaAUnSoloTipo() {
        // REAL admite float y double, asÃ­ que el Ã¡rea no se fija sola: de eso se
        // encarga el desplegable. Fijarla en float dejarÃ­a sin poder usar los
        // double en un Ã¡rea de ocho bytes, y fijarla en double dejarÃ­a sin float a
        // las de cuatro.
        CommunicationWizardController doble = controllerCon("%DB21.DBLD0[0..7]:REAL");
        assertNull(doble.lockedType(AREA));
        assertEquals(List.of(DataType.FLOAT, DataType.DOUBLE),
                doble.typesAllowed(AREA));
        assertTrue(doble.typeAllowed(AREA, DataType.FLOAT));
        assertTrue(doble.typeAllowed(AREA, DataType.DOUBLE));

        CommunicationWizardController simple = controllerCon("%DB21.DBD0[0..7]:REAL");
        assertNull(simple.lockedType(AREA));
        assertEquals(List.of(DataType.FLOAT, DataType.DOUBLE),
                simple.typesAllowed(AREA));
        assertEquals(2, simple.areaFreeSlots(AREA, DataType.FLOAT));
        assertEquals(1, simple.areaFreeSlots(AREA, DataType.DOUBLE),
                "ocho bytes y un double ocupa ocho: sÃ³lo uno");
        assertEquals("0", simple.nextOffset(AREA, DataType.FLOAT));
        assertEquals("0", simple.nextOffset(AREA, DataType.DOUBLE));
    }

    @Test
    void unaVariableRealSeListaEnUnAreaQueAdmiteLosDosTipos() {
        CommunicationWizardController c = controllerCon("%DB21.DBLD0[0..7]:REAL");
        addPv(c, "a", "0", "double");
        assertEquals(DataType.DOUBLE, c.lockedType(AREA),
                "la primera variable sÃ­ fija el tipo de un Ã¡rea que no lo traÃ­a");
        assertTrue(c.areaAdmiteTipo(AREA, "REAL", DataType.DOUBLE),
                "el double es un real y el Ã¡rea es de reales");
        assertFalse(c.areaAdmiteTipo(AREA, "REAL", DataType.INT),
                "pero un entero sigue sin caber en un Ã¡rea de reales");
    }

    @Test
    void unTagQueAdmiteVariosTiposNoFijaElAreaYLoEligeElDesplegable() {
        assertNull(controllerCon("%DB231.DBB0[0..9]:BYTE").lockedType(AREA),
                "byte admite byte y ubyte: la elecciÃ³n es del usuario");
        assertNull(controllerCon("%DB231.DBD0[0..99]:INTEGER").lockedType(AREA));
    }

    @Test
    void unTagSinFamiliaDejaQueLaFijaLaPrimeraVariable() {
        CommunicationWizardController c = controllerCon("%DB21.DBB0[0..9]");
        assertNull(c.lockedType(AREA));
        addPv(c, "a", "0", "int");
        assertEquals(DataType.INT, c.lockedType(AREA));
    }

    @Test
    void soloSeOfrecenLosTiposQueElAreaAdmite() {
        CommunicationWizardController c = controllerCon("%DB231.DBB0[0..9]:BYTE");
        assertEquals(List.of(DataType.BYTE, DataType.UBYTE), c.typesAllowed(AREA));
        assertTrue(c.typeAllowed(AREA, DataType.UBYTE));
        assertFalse(c.typeAllowed(AREA, DataType.INT));
    }

    @Test
    void unAreaSinTipoFijoOfreceTodosLosPrimitivos() {
        List<DataType> offered = controllerCon("%DB21.DBB0[0..9]").typesAllowed(AREA);
        assertEquals(12, offered.size());
        assertFalse(offered.contains(DataType.STRING));
        assertFalse(offered.contains(DataType.S7AI));
        assertTrue(offered.contains(DataType.LONG));
    }

    @Test
    void elDesplegableIncluyeLosTiposSinPesoAlFinal() {
        List<DataType> combo = controllerCon("%DB21.DBB0[0..9]").typesForCombo(AREA);
        assertTrue(combo.contains(DataType.S7AI), "que no se puedan elegir no es razÃ³n para ocultarlos");
        assertTrue(combo.indexOf(DataType.S7AI) > combo.indexOf(DataType.LONG),
                "los que sÃ­ se pueden elegir van delante");
    }

    // --- filtro de variables de planta ---------------------------------------

    @Test
    void unAreaDeIntSigueListandoLasVariablesIntegerDeLaPlanta() {
        // La comparaciÃ³n tiene que ir por familia: en la planta INTEGER puede ser
        // int, long o short segÃºn el cÃ³digo de Ã¡rea, y comparar los nombres dejarÃ­a
        // el Ã¡rea sin ninguna variable que ofrecer.
        CommunicationWizardController c = controllerCon("%DB231.DBD0[0..99]:INTEGER");
        addPv(c, "a", "0", "int");
        DataType fijado = c.lockedType(AREA);
        assertEquals(DataType.INT, fijado);
        assertTrue(c.areaAdmiteTipo(AREA, "INTEGER", fijado));
    }

    @Test
    void unAreaDeIntNoAdmiteVariablesDeOtraFamilia() {
        CommunicationWizardController c = controllerCon("%DB231.DBD0[0..99]:INTEGER");
        assertFalse(c.areaAdmiteTipo(AREA, "REAL", DataType.INT));
        assertFalse(c.areaAdmiteTipo(AREA, "BYTE", DataType.INT));
    }

    @Test
    void unAreaSinTipoFijoAdmiteTodo() {
        CommunicationWizardController c = controllerCon("%DB231.DBD0[0..99]:INTEGER");
        assertTrue(c.areaAdmiteTipo(AREA, "REAL", null));
        assertTrue(c.areaAdmiteTipo(AREA, "BYTE", null));
    }

    // --- tag repetido -------------------------------------------------------

    @Test
    void dosRangosDistintosEnLaMismaDireccionSonValidos() {
        // El caso que dice el usuario: %DB22.DBB4[10..16] y %DB22.DBB4[17..22]
        // arrancan en el mismo byte pero no se tocan, así que son dos áreas
        // legítimas. Lo que no puede repetirse es el espacio, no la dirección.
        CommunicationWizardController c = controllerCon(
                area("a1", "Primera", "%DB22.DBB4[10..16]:REAL"));
        assertNull(c.areaQueChoca("%DB22.DBB4[17..22]:REAL", null),
                "la segunda empieza justo donde acaba la primera");
        assertNull(c.areaConMismoTag("%DB22.DBB4[17..22]:REAL", null),
                "con dirección decide el solapamiento, no la dirección repetida");
    }

    @Test
    void elEspacioQueSeReparteEsElDelRango() {
        // La dirección dice de dónde se parte y el rango hasta dónde. Cambiar sólo
        // el rango amplía o reduce el espacio sin mover el sitio.
        CommunicationWizardController c = controllerCon(
                area("a1", "Corta", "%DB22.DBB4[0..3]:BYTE"));
        assertEquals(4, c.areaCapacityBytes("a1"));
        assertNull(c.areaQueChoca("%DB22.DBB4[4..7]:BYTE", null),
                "justo detrás, sin huecos ni pisadas");
        assertNotNull(c.areaQueChoca("%DB22.DBB4[3..7]:BYTE", null),
                "el byte 3 ya lo ocupa la otra área");
    }

    @Test
    void unEspacioDeMasEnElTagNoDejaColarUnSolapamiento() {
        // "%DB20. DBB4[0..16]" con un espacio de más. Mientras el espacio estuvo, el
        // tag no se reconocía como bloque y ni el solapamiento ni la comparación de
        // texto lo detectaban.
        CommunicationWizardController c = controllerCon(
                area("a1", "Reales", "%DB20. DBB4[0..16]:REAL"));
        assertNotNull(c.areaQueChoca("%DB20.DBB4[0..10]:REAL", null),
                "las dos abarcan los bytes 4 a 16");
        assertNotNull(c.areaQueChoca("%DB20. DBB4[0..10]:REAL", null));
        assertNull(c.areaQueChoca("%DB20.DBB4[17..22]:REAL", null),
                "fuera del rango, así que cabe");
    }

    @Test
    void laComprobacionDeTagRepetidoNoSeAplicaConDireccion() {
        // Si el tag tiene dirección, decide el solapamiento. Sin ella el
        // solapamiento es ciego y hay que quedarse con la comparación de texto.
        CommunicationWizardController c = controllerCon(
                area("a1", "Bytes", "%DB21.DBB4[0..9]:BYTE"));
        assertNull(c.areaConMismoTag("%DB21.DBB4[0..9]:BYTE", null),
                "las direcciones repetidas las juzga el solapamiento");
        assertNotNull(c.areaQueChoca("%DB21.DBB4[0..9]:BYTE", null));
    }

    @Test
    void dosAreasConElMismoTagSimbólicoSeDetectan() {
        // El solapamiento no puede verlas: no hay bloque ni código que comparar. Si
        // no se comparara el texto, dos áreas apuntando al mismo símbolo pasarían
        // el filtro sin quejarse.
        CommunicationWizardController c = controllerCon(
                area("a1", "Boton", "Control_Panel.Start_Button"));
        assertNull(c.areaQueChoca("Control_Panel.Start_Button", null),
                "el solapamiento no ve tags simbólicos");
        CommConfigData.ItemConfig repetido =
                c.areaConMismoTag("Control_Panel.Start_Button", null);
        assertNotNull(repetido, "pero el tag repetido sí se detecta");
        assertEquals("Boton", repetido.getName());
    }

    @Test
    void sinDireccionElTagEnteroEsLaIdentidad() {
        CommunicationWizardController c = controllerCon(
                area("a1", "Simbolico", "Control_Panel.Start_Button"));
        assertNotNull(c.areaConMismoTag("control_panel.start_button", null),
                "los espacios y las mayúsculas no son otro símbolo");
        assertNull(c.areaConMismoTag("Control_Panel.Stop_Button", null));
        assertNull(c.areaConMismoTag("", null));
        assertNull(c.areaConMismoTag(null, null));
    }

    @Test
    void unAreaSimbolicaNoSeChocaConsigoMisma() {
        CommunicationWizardController c = controllerCon(
                area("a1", "Boton", "Control_Panel.Start_Button"));
        assertNull(c.areaConMismoTag("Control_Panel.Start_Button", "a1"),
                "guardar un área sin tocarla no es un conflicto consigo misma");
    }

    // --- solapamiento --------------------------------------------------------

    @Test
    void seDetectaElAreaQueSePisa() {
        CommunicationWizardController c = controllerCon(
                area("a1", "Almacen", "%DB231.DBB0[0..1848]:BYTE"));
        CommConfigData.ItemConfig choca = c.areaQueChoca("%DB231.DBB10[0..5]:BYTE", null);
        assertEquals("Almacen", choca.getName());
    }

    @Test
    void unAreaLejanaNoPisaANadie() {
        CommunicationWizardController c = controllerCon(
                area("a1", "Almacen", "%DB231.DBB0[0..1848]:BYTE"));
        assertNull(c.areaQueChoca("%DB231.DBB3000[0..5]:BYTE", null));
    }

    @Test
    void unaAreaNoSePisaConsigoMismaAlModificarse() {
        CommunicationWizardController c = controllerCon(
                area("a1", "Almacen", "%DB231.DBB0[0..1848]:BYTE"));
        assertNull(c.areaQueChoca("%DB231.DBB0[0..1848]:BYTE", "a1"));
        assertNull(c.areaQueChoca("%DB231.DBB0[0..9]:BYTE", "a1"),
                "reducir el rango propio es legÃ­timo");
    }

    @Test
    void unTagInvalidoNoBuscaSolapamientos() {
        CommunicationWizardController c = controllerCon(
                area("a1", "Almacen", "%DB231.DBB0[0..1848]:BYTE"));
        assertNull(c.areaQueChoca("basura", null));
    }

    @Test
    void unAreaDesconocidaNoTieneNiTagNiTipos() {
        CommunicationWizardController c = controllerCon();
        assertNull(c.areaTag("no-existe"));
        assertEquals(-1, c.areaCapacityBytes("no-existe"));
        assertTrue(c.typesAllowed("no-existe").isEmpty());
        assertNull(c.lockedType("no-existe"));
        assertNull(c.nextOffset("no-existe", DataType.BYTE));
        assertNull(c.nextOffset(null, DataType.BYTE));
    }

    @Test
    void unAreaConTagSimbolicoSeAceptaYNoSeAcota() {
        // Con tags optimizados el tag es un nombre, no una direcciÃ³n. El Ã¡rea se
        // guarda igual y lo que no se puede calcular simplemente no se calcula.
        CommunicationWizardController c = controllerCon("Control_Panel.Start_Button");
        assertNotNull(c.areaTag(AREA));
        assertEquals(-1, c.areaCapacityBytes(AREA), "no se puede acotar un tag simbÃ³lico");
        assertEquals(-1, c.areaFreeSlots(AREA, DataType.BYTE),
                "sin capacidad conocida no se bloquea nada");
        assertNull(c.nextOffset(AREA, DataType.BYTE));
        assertNull(c.lockedType(AREA), "el tag no declara tipo, asÃ­ que lo elige el desplegable");
    }

    @Test
    void unAreaConDireccionDeModbusSeAcepta() {
        CommunicationWizardController c = controllerCon("40001");
        assertEquals(-1, c.areaCapacityBytes(AREA));
        assertEquals(-1, c.areaFreeSlots(AREA, DataType.BYTE));
        assertTrue(c.typeAllowed(AREA, DataType.INT),
                "sin tag que acote, el Ã¡rea admite cualquier tipo");
    }

    @Test
    void unTipoDePlantaDesconocidoNoSeDescarta() {
        // Un tipo que no aparece en el catÃ¡logo no debe dejar el Ã¡rea sin variables:
        // es preferible mostrar de mÃ¡s que dejar al usuario sin nada que aÃ±adir.
        CommunicationWizardController c = controllerCon("%DB231.DBD0[0..99]:INTEGER");
        addPv(c, "a", "0", "int");
        assertEquals(DataType.INT, c.lockedType(AREA));
        assertTrue(c.areaAdmiteTipo(AREA, "UDINT", DataType.INT));
        assertTrue(c.areaAdmiteTipo(AREA, "LREAL", DataType.INT));
        assertTrue(c.areaAdmiteTipo(AREA, "", DataType.INT));
    }
}
