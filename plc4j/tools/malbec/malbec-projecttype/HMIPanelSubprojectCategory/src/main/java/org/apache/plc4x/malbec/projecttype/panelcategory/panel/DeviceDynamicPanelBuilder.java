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
package org.apache.plc4x.malbec.projecttype.panelcategory.panel;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public interface DeviceDynamicPanelBuilder {

    void initForModel(DeviceModel model);

    String getProtocol();

    void setHost(String host);

    void setTransport(String transport);

    void setPort(String port);

    String getDefaultTransport();

    String[] getTransportOptions();

    LinkedHashMap<String, String> getParameterDefaults();

    /**
     * Catálogo de parámetros válidos para un transporte concreto. El método
     * por defecto delega en {@link #getParameterDefaults()}.
     */
    default LinkedHashMap<String, String> getParameterDefaults(String transport) {
        return getParameterDefaults();
    }

    /**
     * Etiqueta del campo "host". Los transportes serie utilizan la ruta del
     * dispositivo (p. ej. {@code /dev/ttyUSB0} o {@code COM3}) en lugar de una IP.
     */
    default String getHostLabel() {
        return "Host/IP:";
    }

    /**
     * Indica si el campo de puerto aplica al transporte activo. No aplica, por
     * ejemplo, en transportes serie.
     */
    default boolean isPortApplicable() {
        return true;
    }

    /**
     * Elimina de los parámetros ya añadidos los que no estén en el catálogo
     * {@code validKeys}. Sin efecto por defecto.
     */
    default void retainParameters(Set<String> validKeys) {
    }

    void addParameter(String key, String value);

    void removeLastParameter();

    void clearParameters();

    /**
     * Parámetros realmente cargados, en orden, a diferencia de
     * {@link #getParameterDefaults()} que devuelve el catálogo con los valores
     * de fábrica.
     *
     * <p>Sin esto el formulario no puede distinguir "este parámetro vale 0" de
     * "este parámetro no está en la conexión", y al precargar un dispositivo
     * existente mostraría los defaults en lugar de lo que está en el XML.
     *
     * @return copia defensiva: el llamador no debe mutar el builder por acá
     */
    LinkedHashMap<String, String> getParameters();

    String getSpecificParametersAsString();

    static String assembleUrl(String protocol, String transport, String host, String port,
                              LinkedHashMap<String, String> params) {
        return assembleUrl(protocol, transport, host, port, params, true);
    }

    static String assembleUrl(String protocol, String transport, String host, String port,
                              LinkedHashMap<String, String> params, boolean includePort) {
        StringBuilder sb = new StringBuilder();
        sb.append(protocol == null ? "" : protocol);
        if (transport != null && !transport.trim().isEmpty()) {
            sb.append(":").append(transport);
        }
        sb.append("://").append(host == null ? "" : host);
        if (includePort && port != null && !port.trim().isEmpty()) {
            sb.append(":").append(port.trim());
        }
        LinkedHashMap<String, String> ordered = new LinkedHashMap<>();
        if (params != null) {
            ordered.putAll(params);
        }
        if (!ordered.isEmpty()) {
            sb.append("?");
            int i = 0;
            for (Map.Entry<String, String> e : ordered.entrySet()) {
                if (i++ > 0) {
                    sb.append("&");
                }
                sb.append(e.getKey()).append("=").append(e.getValue());
            }
        }
        return sb.toString();
    }
}