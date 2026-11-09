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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;


public final class UrlDisassembler {

    private UrlDisassembler() {
    }

    public record Parts(String protocol, String transport, String host, String port,
                 LinkedHashMap<String, String> params) {
    }

    public static Parts disassemble(String url) {
        if (url == null) {
            return null;
        }
        String clean = url.trim();
        int separator = clean.indexOf("://");
        if (separator < 0) {
            return null;
        }

        String left = clean.substring(0, separator);
        String remains = clean.substring(separator + 3);

        int transportSeparator = left.indexOf(':');
        String protocol = transportSeparator < 0 ? left : left.substring(0, transportSeparator);
        String transport = transportSeparator < 0 ? "" : left.substring(transportSeparator + 1);

        int question = remains.indexOf('?');
        String hostPort = question < 0 ? remains : remains.substring(0, question);
        String query = question < 0 ? "" : remains.substring(question + 1);

        String host;
        String port;
        if (hostPort.startsWith("[")) {
            int end = hostPort.indexOf(']');
            if (end > 0) {
                host = hostPort.substring(1, end);
                String behind = hostPort.substring(end + 1);
                port = behind.startsWith(":") ? behind.substring(1) : "";
            } else {
                host = hostPort;
                port = "";
            }
        } else {
            host = hostPort;
            port = "";
            int portSeparator = hostPort.lastIndexOf(':');
            if (portSeparator >= 0) {
                host = hostPort.substring(0, portSeparator);
                port = hostPort.substring(portSeparator + 1);
            }
        }

        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        for (String par : query.split("&")) {
            if (par.isEmpty()) {
                continue;
            }
            int same = par.indexOf('=');
            String clave = same < 0 ? par : par.substring(0, same);
            String valor = same < 0 ? "" : par.substring(same + 1);
            params.put(decodificar(clave), decodificar(valor));
        }

        return new Parts(protocol, transport, host, port, params);
    }

    static String decodificar(String text) {
        try {
            return URLDecoder.decode(text, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException | IllegalArgumentException ex) {
            return text;
        }
    }

    static String codificar(String text) {
        try {
            return URLEncoder.encode(text, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException ex) {
            return text;
        }
    }
}