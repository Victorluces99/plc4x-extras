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
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import org.junit.jupiter.api.Test;

/**
 * Las tablas de grupos y de áreas son sólo el espejo del estado: el usuario no las
 * edita, elige la fila y rellena el formulario de al lado. Y sus columnas están
 * fijas en dos sentidos que conviene distinguir.
 *
 * <p>Lo primero es que las columnas no se puedan reordenar arrastrando la cabecera.
 * El sitio de las columnas no lo decide el usuario, lo decide el orden en que se
 * guardan, porque hay código que las lee por posición.
 *
 * <p>Lo segundo es lo contrario: sí se pueden ensanchar, y ensanchar una no debe
 * mover el resto. Con el autoajuste por defecto
 * ({@code AUTO_RESIZE_SUBSEQUENT_COLUMNS}) el ancho se reparte entre las columnas
 * siguientes, así que al ensanchar "Tag" se encogen "Tipo", "Libre", "Grupo",
 * "Enable" y "UUID" y el efecto que se ve es que las columnas se mueven. Con
 * {@code AUTO_RESIZE_OFF} el tirador sólo cambia la columna que se está arrastrando.
 *
 * <p>El bloqueo del reordenado no choca con el ensanchado: en la cabecera son dos
 * banderas distintas ({@code reorderingAllowed} y {@code resizingAllowed}), y
 * desactivar la primera deja la segunda como estaba.
 */
class StepGroupAreaPanelTest {

    /**
     * Los datos entran por el controlador y no a las listas, por un motivo concreto:
     * el índice inverso área → grupo lo mantiene {@code addItem} (poniendo el par en
     * {@code itemsGroup}) y no se deduce de las listas. Meterlos a mano dejaría el
     * índice vacío, la columna "Grupo" mostraría SIN GRUPO y el test comprobaría una
     * Mentira en lugar del comportamiento real.
     */
    private StepGroupAreaPanel panelConUnGrupoYUnArea() {
        CommunicationWizardController controller =
                new CommunicationWizardController(null, new CommunicationWizardState());
        controller.addGroup("uuid-grupo", "Rápido", "variables rápidas", "100", true);
        controller.addItem("uuid-area", "Area", "descripción del área",
                "%DB22.DBB4[10..16]:REAL", true, "uuid-grupo");
        StepGroupAreaPanel panel = new StepGroupAreaPanel(controller);
        panel.refresh();
        return panel;
    }

    @Test
    void lasColumnasNoSePuedenReordenar() {
        // El valor por defecto de la cabecera es reorderingAllowed = true, así que
        // sin tocar nada el usuario mueve las columnas a donde le parezca. Se
        // comprueba una columna por lo menos de cada tabla: el cambio se hizo con
        // un helper sobre la tabla entera.
        StepGroupAreaPanel panel = panelConUnGrupoYUnArea();
        assertFalse(panel.getGroupTable().getTableHeader().getReorderingAllowed(),
                "las columnas de grupos se pueden mover");
        assertFalse(panel.getItemTable().getTableHeader().getReorderingAllowed(),
                "las columnas de áreas se pueden mover");
    }

    @Test
    void unaColumnaNoMueveLasDemasAlEnsancharla() {
        StepGroupAreaPanel panel = panelConUnGrupoYUnArea();
        assertEquals(JTable.AUTO_RESIZE_OFF, panel.getGroupTable().getAutoResizeMode(),
                "con el autoajuste por defecto el ancho se reparte entre las columnas siguientes");
        assertEquals(JTable.AUTO_RESIZE_OFF, panel.getItemTable().getAutoResizeMode(),
                "con el autoajuste por defecto el ancho se reparte entre las columnas siguientes");
    }

    @Test
    void todasLasColumnasSePuedenRedimensionar() {
        // El bloqueo del reordenado no puede arrastrar al redimensionado, que es lo
        // que el usuario sí necesita para leer un tag largo o un UUID entero.
        StepGroupAreaPanel panel = panelConUnGrupoYUnArea();
        assertRedimensionables(panel.getGroupTable(), "grupos");
        assertRedimensionables(panel.getItemTable(), "áreas");
    }

    private void assertRedimensionables(JTable tabla, String cual) {
        TableColumnModel columnas = tabla.getColumnModel();
        for (int i = 0; i < columnas.getColumnCount(); i++) {
            assertTrue(columnas.getColumn(i).getResizable(),
                    "la columna " + tabla.getColumnName(i) + " de " + cual
                            + " no se puede redimensionar");
        }
        assertTrue(tabla.getTableHeader().getResizingAllowed(),
                "la cabecera de " + cual + " no deja redimensionar");
    }

    @Test
    void unaColumnaSePuedeEnsancharAToSinTecho() {
        // El ajuste automático de anchos limita el ancho a 260 px, pero eso es sólo
        // el preferred de partida: la mano del usuario no encuentra un tope porque
        // sizeColumnsToContent no fija maxWidth.
        StepGroupAreaPanel panel = panelConUnGrupoYUnArea();
        TableColumnModel columnas = panel.getItemTable().getColumnModel();
        TableColumn descripcion = columnas.getColumn(1);
        int preferredInicial = descripcion.getPreferredWidth();
        descripcion.setWidth(preferredInicial + 300);
        assertEquals(preferredInicial + 300, descripcion.getWidth(),
                "no se deja ensanchar la columna más allá del ajuste automático");
    }

    @Test
    void ensancharUnaColumnaNoCambiaElAnchoDeLasOtras() {
        StepGroupAreaPanel panel = panelConUnGrupoYUnArea();
        TableColumnModel columnas = panel.getItemTable().getColumnModel();
        int[] antes = new int[columnas.getColumnCount()];
        for (int i = 0; i < columnas.getColumnCount(); i++) {
            antes[i] = columnas.getColumn(i).getWidth();
        }

        columnas.getColumn(2).setWidth(antes[2] + 250);

        for (int i = 0; i < columnas.getColumnCount(); i++) {
            if (i == 2) {
                continue;
            }
            assertEquals(antes[i], columnas.getColumn(i).getWidth(),
                    "la columna " + panel.getItemTable().getColumnName(i)
                            + " se movió al ensanchar otra");
        }
    }

    @Test
    void lasCeldasNoSeEditanDirectamente() {
        // Editar aquí no guardaría nada: el modelo de la tabla se reconstruye desde
        // el estado en cada refresh. Es edición falsa y hay que impedirla.
        StepGroupAreaPanel panel = panelConUnGrupoYUnArea();
        JTable grupos = panel.getGroupTable();
        assertFalse(grupos.getModel().isCellEditable(0, 0));
        assertFalse(grupos.getModel().isCellEditable(0, 3), "ni la columna Enable");
        assertFalse(grupos.getModel().isCellEditable(0, 4), "ni el UUID");

        JTable areas = panel.getItemTable();
        assertFalse(areas.getModel().isCellEditable(0, 2), "ni el Tag");
        assertFalse(areas.getModel().isCellEditable(0, 7), "ni el UUID");
    }

    @Test
    void elUsuarioNoPuedePonerseAEscribirEnLasCeldas() {
        // Lo que cierra el teclado y el doble clic es que no llegue a arrancar un
        // editor, y editCellAt es exactamente eso: devuelve false cuando la celda no
        // es editable, antes de crear ningún componente.
        //
        // Ojo con lo que no sirve aquí: JTable.setValueAt no consulta nada y pasa
        // directo al modelo, así que escribir a mano en la tabla sí la cambia. Eso no
        // es un agujero para el usuario, que nunca ejecuta ese camino, sino una
        // llamada de código. La UI está cubierta por isCellEditable.
        StepGroupAreaPanel panel = panelConUnGrupoYUnArea();

        JTable grupos = panel.getGroupTable();
        assertEquals("Rápido", grupos.getValueAt(0, 0));
        assertFalse(grupos.editCellAt(0, 0),
                "arranca un editor en la tabla de grupos: el usuario podría escribir");
        assertEquals("Rápido", grupos.getValueAt(0, 0),
                "el nombre del grupo cambió al intentar editarlo");

        JTable areas = panel.getItemTable();
        assertEquals("%DB22.DBB4[10..16]:REAL", areas.getValueAt(0, 2));
        assertFalse(areas.editCellAt(0, 2),
                "arranca un editor en la tabla de áreas: el usuario podría escribir");
        assertEquals("%DB22.DBB4[10..16]:REAL", areas.getValueAt(0, 2),
                "el tag del área cambió al intentar editarlo");
    }

    @Test
    void lasTablasMuestranLoDelEstado() {
        // Sin esto los tests de columnas pasarían también con las tablas vacías.
        StepGroupAreaPanel panel = panelConUnGrupoYUnArea();
        JTable grupos = panel.getGroupTable();
        assertEquals(1, grupos.getRowCount());
        assertEquals(5, grupos.getColumnCount());
        assertEquals("Rápido", grupos.getValueAt(0, 0));
        assertEquals("variables rápidas", grupos.getValueAt(0, 1));
        assertEquals("TRUE", grupos.getValueAt(0, 3));

        JTable areas = panel.getItemTable();
        assertEquals(1, areas.getRowCount());
        assertEquals(8, areas.getColumnCount());
        assertEquals("Area", areas.getValueAt(0, 0));
        assertEquals("%DB22.DBB4[10..16]:REAL", areas.getValueAt(0, 2));
        assertEquals("Rápido", areas.getValueAt(0, 5),
                "el área muestra el nombre del grupo al que pertenece");
    }

    // --- topes de caracteres ---------------------------------------------------

    @Test
    void losNombresSeCortanEnVeinte() {
        StepGroupAreaPanel panel = panelConUnGrupoYUnArea();
        panel.getGroupName().setText("g".repeat(40));
        assertEquals(20, panel.getGroupName().getText().length(),
                "el nombre del grupo admite más de 20 caracteres");
        panel.getItemName().setText("a".repeat(40));
        assertEquals(20, panel.getItemName().getText().length(),
                "el nombre del área admite más de 20 caracteres");
    }

    @Test
    void lasDescripcionesSeCortanEnSesenta() {
        StepGroupAreaPanel panel = panelConUnGrupoYUnArea();
        panel.getGroupDescription().setText("g".repeat(100));
        assertEquals(60, panel.getGroupDescription().getText().length(),
                "la descripción del grupo admite más de 60 caracteres");
        panel.getItemDescription().setText("a".repeat(100));
        assertEquals(60, panel.getItemDescription().getText().length(),
                "la descripción del área admite más de 60 caracteres");
    }

    @Test
    void justoAlTopeSeEscribeEntero() {
        // El recorte sólo tiene que notar cuando se pasa: un texto que ya cabe se
        // tiene que quedar como estaba, sin comerse ni un carácter.
        StepGroupAreaPanel panel = panelConUnGrupoYUnArea();
        panel.getGroupName().setText("g".repeat(20));
        assertEquals(20, panel.getGroupName().getText().length());
        panel.getItemDescription().setText("d".repeat(60));
        assertEquals(60, panel.getItemDescription().getText().length());
    }

    @Test
    void elTagNoSeCortaPorqueEsUnaDireccion() {
        // Recortar una dirección daría otra dirección distinta, que es peor que un
        // campo largo. El tag es la referencia de memoria y tiene que entrar entero.
        StepGroupAreaPanel panel = panelConUnGrupoYUnArea();
        String tag = "%DB22.DBB4[10..16]:REAL";
        panel.getItemTag().setText(tag);
        assertEquals(tag, panel.getItemTag().getText());
    }
}