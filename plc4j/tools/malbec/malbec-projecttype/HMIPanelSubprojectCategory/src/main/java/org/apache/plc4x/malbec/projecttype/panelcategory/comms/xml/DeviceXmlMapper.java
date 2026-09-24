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
import org.plcopen.xml.tc60201.AddData;
import org.plcopen.xml.tc60201.ProjectDocument.Project.Instances.Configurations;
import org.plcopen.xml.tc60201.ProjectDocument.Project.Instances.Configurations.Configuration;

public final class DeviceXmlMapper {

    private DeviceXmlMapper() {
    }

    public static void writeDevice(DeviceConfigData device, Configurations configurations) {
        Configuration configuration = configurations.addNewConfiguration();
        configuration.setName(device.getDeviceName());
        AddData addData = configuration.addNewAddData();
        AddData.Data data = addData.addNewData();
        data.setName(MalbecNamespaces.DEVICE_DATA_NAME);
        data.setHandleUnknown(AddData.Data.HandleUnknown.PRESERVE);
        String deviceXml = "<device xmlns=\"" + MalbecNamespaces.DEVICE_NS + "\""
                + " uuid=\"" + escape(device.getUuid()) + "\""
                + " enabled=\"" + device.isEnabled() + "\""
                + " s88Uuid=\"" + escape(device.getS88Uuid()) + "\">"
                + "<brand>" + escape(device.getBrand()) + "</brand>"
                + "<model>" + escape(device.getModel()) + "</model>"
                + "<protocol>" + escape(device.getProtocol()) + "</protocol>"
                + "<description>" + escape(device.getDescription()) + "</description>"
                + "<s88Node>" + escape(device.getS88Node()) + "</s88Node>"
                + "<specificParameters>" + escape(device.getSpecificParameters())
                + "</specificParameters></device>";
        XmlCursor cursor = data.newCursor();
        try {
            cursor.toEndToken();
            XmlCursor source = XmlObject.Factory.parse(deviceXml).newCursor();
            try {
                source.toFirstChild();
                source.copyXml(cursor);
            } finally {
                source.dispose();
            }
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo serializar el dispositivo", e);
        } finally {
            cursor.dispose();
        }
    }

    public static DeviceConfigData readDevice(Configuration configuration) {
        AddData addData = configuration.getAddData();
        if (addData == null) {
            return null;
        }
        for (AddData.Data data : addData.getDataArray()) {
            if (MalbecNamespaces.DEVICE_DATA_NAME.equals(data.getName())) {
                XmlObject device = firstChild(data, "device");
                if (device == null) {
                    return null;
                }
                return new DeviceConfigData(
                        text(device, "brand"),
                        text(device, "model"),
                        text(device, "protocol"),
                        configuration.getName(),
                        text(device, "description"),
                        attr(device, "uuid"),
                        Boolean.parseBoolean(attr(device, "enabled")),
                        text(device, "s88Node"),
                        attr(device, "s88Uuid"),
                        text(device, "specificParameters"));
            }
        }
        return null;
    }

    private static XmlObject firstChild(XmlObject parent, String localName) {
        XmlCursor cursor = parent.newCursor();
        try {
            if (cursor.toFirstChild()) {
                do {
                    QName name = cursor.getName();
                    if (name != null && localName.equals(name.getLocalPart())) {
                        return cursor.getObject();
                    }
                } while (cursor.toNextSibling());
            }
        } finally {
            cursor.dispose();
        }
        return null;
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