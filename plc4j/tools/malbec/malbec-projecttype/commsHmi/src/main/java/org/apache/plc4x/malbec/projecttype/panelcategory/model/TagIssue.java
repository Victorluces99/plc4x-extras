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
package org.apache.plc4x.malbec.projecttype.panelcategory.model;

import java.util.List;

/**
 * Aviso sobre un tag, sin texto y sin presentacion (T3.8).
 *
 * <p>Antes el modelo devolvia {@code List<String>} con el HTML dentro, de modo que la
 * capa de dominio arrastraba etiquetas de salto de linea que solo tienen sentido
 * pintadas. Ahora devuelve un codigo, la gravedad y los datos para interpolar, y quien
 * lo muestra decide comoPresentarlo.
 *
 * @param codigo    identificador estable del problema, para que la UI lo traduce
 * @param gravedad  si impide usar el tag o solo conviene avisar
 * @param args      valores que la plantilla necesita, en orden
 */
public record TagIssue(String codigo, Gravedad gravedad, List<String> args) {

    public enum Gravedad {
        ERROR,
        AVISO
    }

    /** Códigos de los avisos que este diagnostico sabe emitir. */
    public static final String CORCHETE_SIN_CERRAR = "tag.corchete.sinCerrar";
    public static final String CORCHETE_SOBRANTE = "tag.corchete.sobrante";
    public static final String RANGO_INVALIDO = "tag.rango.invalido";
    public static final String CARACTERES_RAROS = "tag.caracteres.raros";
    public static final String NO_ES_DIRECCION = "tag.no.esDireccion";
    public static final String SIN_TIPOS = "tag.sin.tipos";
    public static final String SIN_TAMANO = "tag.sin.tamano";

    public TagIssue(String codigo, Gravedad gravedad, String... args) {
        this(codigo, gravedad, List.of(args));
    }
}
