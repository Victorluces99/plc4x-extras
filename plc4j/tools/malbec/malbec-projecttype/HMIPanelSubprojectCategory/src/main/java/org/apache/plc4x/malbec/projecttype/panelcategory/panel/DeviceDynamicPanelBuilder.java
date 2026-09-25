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

public interface DeviceDynamicPanelBuilder {

    void initForModel(DeviceModel model);

    String getProtocol();

    void setHost(String host);

    void setTransport(String transport);

    void setPort(String port);

    String getDefaultTransport();

    String[] getTransportOptions();

    LinkedHashMap<String, String> getParameterDefaults();

    void addParameter(String key, String value);

    void removeLastParameter();

    void clearParameters();

    String getSpecificParametersAsString();

    static String assembleUrl(String protocol, String transport, String host, String port,
                              LinkedHashMap<String, String> params) {
        StringBuilder sb = new StringBuilder();
        sb.append(protocol == null ? "" : protocol)
                .append(":").append(transport == null ? "" : transport)
                .append("://").append(host == null ? "" : host);
        if (port != null && !port.trim().isEmpty()) {
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