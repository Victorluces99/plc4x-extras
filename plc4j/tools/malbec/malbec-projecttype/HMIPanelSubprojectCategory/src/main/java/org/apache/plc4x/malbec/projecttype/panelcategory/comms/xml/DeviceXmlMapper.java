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
package org.apache.plc4x.malbec.projecttype.panelcategory.comms.xml;

import javax.xml.namespace.QName;
import org.apache.plc4x.malbec.projecttype.panelcategory.panel.DeviceConfigData;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;

public final class DeviceXmlMapper {

    private DeviceXmlMapper() {
    }

    public static void writeDevice(DeviceConfigData device, XmlObject configurations) {
        String deviceXml = "<configuration xmlns=\"" + MalbecNamespaces.PLCOPEN_NS + "\""
                + " name=\"" + escape(device.getDeviceName()) + "\">"
                + "<addData><data name=\"" + MalbecNamespaces.DEVICE_DATA_NAME
                + "\" handleUnknown=\"preserve\">"
                + "<device xmlns=\"" + MalbecNamespaces.DEVICE_NS + "\""
                + " uuid=\"" + escape(device.getUuid()) + "\""
                + " enabled=\"" + device.isEnabled() + "\""
                + " s88Uuid=\"" + escape(device.getS88Uuid()) + "\">"
                + "<brand>" + escape(device.getBrand()) + "</brand>"
                + "<model>" + escape(device.getModel()) + "</model>"
                + "<protocol>" + escape(device.getProtocol()) + "</protocol>"
                + "<description>" + escape(device.getDescription()) + "</description>"
                + "<s88Node>" + escape(device.getS88Node()) + "</s88Node>"
                + "<specificParameters>" + escape(device.getSpecificParameters())
                + "</specificParameters></device></data></addData></configuration>";
        XmlCursor cursor = configurations.newCursor();
        try {
            cursor.toEndToken();
            cursor.insertXml(XmlObject.Factory.parse(deviceXml));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo serializar el dispositivo", e);
        } finally {
            cursor.dispose();
        }
    }

    public static DeviceConfigData readDevice(XmlObject configuration) {
        XmlObject[] devices = configuration.selectPath(".//*[local-name()='device']");
        if (devices.length == 0) {
            return null;
        }
        XmlObject device = devices[0];
        return new DeviceConfigData(
                text(device, "brand"),
                text(device, "model"),
                text(device, "protocol"),
                attr(configuration, "name"),
                text(device, "description"),
                attr(device, "uuid"),
                Boolean.parseBoolean(attr(device, "enabled")),
                text(device, "s88Node"),
                attr(device, "s88Uuid"),
                text(device, "specificParameters"));
    }

    private static String attr(XmlObject element, String name) {
        XmlCursor cursor = element.newCursor();
        try {
            String value = cursor.getAttributeText(new QName(name));
            return value == null ? "" : value;
        } finally {
            cursor.dispose();
        }
    }

    private static String text(XmlObject parent, String name) {
        XmlCursor cursor = parent.newCursor();
        try {
            if (cursor.toFirstChild()) {
                do {
                    if (name.equals(cursor.getName().getLocalPart())) {
                        String value = cursor.getTextValue();
                        return value == null ? "" : value;
                    }
                } while (cursor.toNextSibling());
            }
            return "";
        } finally {
            cursor.dispose();
        }
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}