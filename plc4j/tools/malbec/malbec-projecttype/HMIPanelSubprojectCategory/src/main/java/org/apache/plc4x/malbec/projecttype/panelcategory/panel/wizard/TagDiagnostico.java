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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class TagDiagnostico {

    private static final Pattern CONTENIDO = Pattern.compile("\\[([^\\]]*)\\]");
    private static final Pattern CONTENIDO_VALIDO =
            Pattern.compile("\\s*\\d+\\s*(\\.\\.\\s*\\d+\\s*)?");
    private static final Pattern PERMITIDO = Pattern.compile("[A-Za-z0-9_.:%\\[\\]()\\-\\s]");

    private TagDiagnostico() {
    }

    static List<String> errores(String tag) {
        List<String> errores = new ArrayList<>();
        if (tag == null) {
            return errores;
        }
        String t = tag.trim();
        if (t.isEmpty()) {
            return errores;
        }
        revisarCorchetes(t, errores);
        revisarCaracteres(t, errores);

        MemoryTag m = MemoryTag.parse(t);
        if (m != null && !m.tieneDireccion()) {
            errores.add("«" + t + "» no es una dirección válida.<br><br>"
                    + "El área se puede guardar, pero se tratará como un nombre: no se le"
                    + " calculará el tamaño y no se podrán comprobar solapes con las"
                    + " demás áreas.<br><br>"
                    + "Si es una dirección, revise la notación: Ej: %DB22.DBB4[0..9]:BYTE.");
        }
        if (m != null && m.family() != null
                && DataType.candidatos(m.family(), m.codeBits()).isEmpty()) {
            errores.add("Con el tipo '" + m.family() + "' el área no va a ofrecer ningún"
                    + " tipo de dato, así que no se le podrán añadir variables.<br><br>"
                    + "Los tipos del programa son: " + DataType.nombres() + ".");
        }
        return errores;
    }

    static List<String> avisos(String tag) {
        List<String> avisos = new ArrayList<>();
        if (tag == null) {
            return avisos;
        }
        String t = tag.trim();
        if (t.isEmpty()) {
            return avisos;
        }
        MemoryTag m = MemoryTag.parse(t);
        if (m != null && m.tieneDireccion() && m.byteCapacity() == 0) {
            avisos.add("No se sabe el tamaño del área, así que no se le podrá añadir"
                    + " ninguna variable.<br><br>"
                    + "Añada un rango al tag, del tipo [0..9], o un tipo tras los dos"
                    + " puntos, del tipo :INT.");
        }
        return avisos;
    }

    private static void revisarCorchetes(String t, List<String> errores) {
        int abiertos = 0;
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (c == '[') {
                abiertos++;
            } else if (c == ']') {
                abiertos--;
            }
        }
        if (abiertos > 0) {
            errores.add("Falta cerrar el corchete: hay un '[' sin su ']'.");
        } else if (abiertos < 0) {
            errores.add("Hay un ']' sin el '[' que lo abre.");
        }
        Matcher m = CONTENIDO.matcher(t);
        while (m.find()) {
            String dentro = m.group(1);
            if (dentro.isBlank() || CONTENIDO_VALIDO.matcher(dentro).matches()) {
                continue;
            }
            errores.add("El rango «[" + dentro.trim() + "]» no es válido.<br><br>"
                    + "Un rango se escribe con dos puntos, del tipo [0..9], y un solo"
                    + " número es un índice, del tipo [10].");
        }
    }

    private static void revisarCaracteres(String t, List<String> errores) {
        StringBuilder raros = new StringBuilder();
        for (int i = 0; i < t.length(); i++) {
            if (!PERMITIDO.matcher(String.valueOf(t.charAt(i))).matches()) {
                raros.append(t.charAt(i));
            }
        }
        if (raros.length() > 0) {
            errores.add("El tag tiene caracteres que no pueden ser parte de una"
                    + " dirección: «" + raros + "».<br><br>"
                    + "Se guardará tal cual, pero el PLC puede que no lo entienda.");
        }
    }
}