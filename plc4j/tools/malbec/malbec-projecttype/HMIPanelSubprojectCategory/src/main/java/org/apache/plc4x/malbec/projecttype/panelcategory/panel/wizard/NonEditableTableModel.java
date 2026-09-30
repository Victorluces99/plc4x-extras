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

import javax.swing.table.DefaultTableModel;

/**
 * Modelo de tabla que no admite edición de celda.
 *
 * <p>Las tablas del asistente son un espejo del modelo: muestran lo que hay
 * guardado y no son un canal de entrada. Editar una celda en el lugar no
 * actualizaría la configuración en memoria, de modo que el cambio se perdería
 * sin aviso al refrescar, o peor, quedaría a medias entre la tabla y el modelo.
 * Toda modificación pasa por el formulario de la izquierda, que valida las
 * reglas y escribe a través del controlador.</p>
 *
 * <p>El nombre dice "no editable" y no "sólo lectura" a propósito: los datos
 * cambian, lo que no cambia nunca es la celda.</p>
 */
public class NonEditableTableModel extends DefaultTableModel {

    private static final long serialVersionUID = 1L;

    public NonEditableTableModel(String[] columns) {
        super(columns, 0);
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }
}
