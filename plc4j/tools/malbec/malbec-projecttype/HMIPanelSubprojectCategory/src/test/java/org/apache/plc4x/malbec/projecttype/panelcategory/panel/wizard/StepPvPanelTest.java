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
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.swing.JTable;
import javax.swing.table.TableColumnModel;
import org.apache.plc4x.malbec.projecttype.panelcategory.panel.CommConfigData;
import org.junit.jupiter.api.Test;

/**
 * La tabla de variables es el espejo de los veinte atributos de un PvRecord y
 * tiene que cumplir dos cosas: mostrar los veinte campos y dejar que el usuario
 * agrande y achique columnas a voluntad.
 *
 * <p>El segundo punto es el que se rompe de forma silenciosa. Si se fija el
 * preferred size de la tabla, el {@code JScrollPane} toma ese valor como ancho
 * del viewport, cree que todo cabe, no muestra barra horizontal y recorta las
 * columnas que quedan afuera. Como el borde derecho de esas columnas también
 * queda fuera de la ventana, el usuario no puede ni verlas ni arrastrarlas.
 */
class StepPvPanelTest {

    private static final CommConfigData.PvConfig PV = new CommConfigData.PvConfig(
            "uuid-pv", "Level", "float", "uuid-area", "0", "descripcion",
            "1000", true, false,
            "1", "5", "fwefwe", "123", "3123", "2", "6", "1",
            "md5-pv", "Area/Level");

    private StepPvPanel panelConUnaVariable() {
        CommunicationWizardState state = new CommunicationWizardState();
        state.getPvs().add(PV);
        StepPvPanel panel = new StepPvPanel(
                new CommunicationWizardController(null, state),
                new PlantModelReader(null));
        panel.refresh();
        return panel;
    }

    @Test
    void laTablaExponeLosVeinteCamposDelPvRecord() {
        JTable tabla = panelConUnaVariable().getPvTable();
        assertEquals(20, tabla.getColumnCount(), "faltan columnas en la tabla");
        assertEquals(1, tabla.getRowCount(), "la variable no llegó a la tabla");
    }

    @Test
    void laTablaMuestraLosValoresDeLimitesDescripcionFormatoYUnidades() {
        JTable tabla = panelConUnaVariable().getPvTable();
        assertEquals("FALSE", tabla.getValueAt(0, 8), "writeEnable");
        assertEquals("1", tabla.getValueAt(0, 9), "displayLimitLow");
        assertEquals("5", tabla.getValueAt(0, 10), "displayLimitHigh");
        assertEquals("fwefwe", tabla.getValueAt(0, 11), "displayDescription");
        assertEquals("123", tabla.getValueAt(0, 12), "displayFormat");
        assertEquals("3123", tabla.getValueAt(0, 13), "displayUnits");
        assertEquals("2", tabla.getValueAt(0, 14), "controlLimitLow");
        assertEquals("6", tabla.getValueAt(0, 15), "controlLimitHigh");
        assertEquals("1", tabla.getValueAt(0, 16), "controlMinStep");
    }

    @Test
    void laTablaNoDeclaraUnAnchoInferiorAlDeSusColumnas() {
        JTable tabla = panelConUnaVariable().getPvTable();
        TableColumnModel columnas = tabla.getColumnModel();
        int anchoColumnas = 0;
        for (int i = 0; i < columnas.getColumnCount(); i++) {
            anchoColumnas += columnas.getColumn(i).getWidth();
        }
        int anchoDeclarado = tabla.getPreferredSize().width;
        assertTrue(anchoDeclarado >= anchoColumnas,
                "la tabla se declara de " + anchoDeclarado + " px pero sus columnas suman "
                        + anchoColumnas
                        + " px: las últimas quedan recortadas y, al no haber barra de scroll,"
                        + " tampoco se pueden redimensionar");
    }

    @Test
    void todasLasColumnasSePuedenRedimensionar() {
        JTable tabla = panelConUnaVariable().getPvTable();
        TableColumnModel columnas = tabla.getColumnModel();
        for (int i = 0; i < columnas.getColumnCount(); i++) {
            assertTrue(columnas.getColumn(i).getResizable(),
                    "la columna " + tabla.getColumnName(i) + " no se puede redimensionar");
        }
    }

    @Test
    void lasCeldasNoSeEditanDirectamente() {
        JTable tabla = panelConUnaVariable().getPvTable();
        assertFalse(tabla.getModel().isCellEditable(0, 0));
        assertFalse(tabla.getModel().isCellEditable(0, 11));
    }
}
