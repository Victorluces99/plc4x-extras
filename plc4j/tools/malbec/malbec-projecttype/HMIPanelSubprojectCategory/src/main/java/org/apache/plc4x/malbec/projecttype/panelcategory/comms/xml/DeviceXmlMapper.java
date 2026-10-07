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

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.xml.namespace.QName;
import org.apache.plc4x.malbec.projecttype.panelcategory.panel.CommConfigData;
import org.apache.plc4x.malbec.projecttype.panelcategory.panel.DeviceConfigData;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;
import org.plcopen.xml.tc60201.AddData;
import org.plcopen.xml.tc60201.ProjectDocument.Project.Instances.Configurations;
import org.plcopen.xml.tc60201.ProjectDocument.Project.Instances.Configurations.Configuration;

public final class DeviceXmlMapper {

    private DeviceXmlMapper() {
    }

    public static Configuration writeDevice(DeviceConfigData device, Configurations configurations) {
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
                + "<deviceKey>" + escape(device.getDeviceKey()) + "</deviceKey>"
                + "<description>" + escape(device.getDescription()) + "</description>"
                + "<s88Node>" + escape(device.getS88Node()) + "</s88Node>"
                + "<specificParameters>" + escape(device.getSpecificParameters())
                + "</specificParameters></device>";
        appendAnyContent(data, deviceXml);
        return configuration;
    }

    public static void writeComms(CommConfigData comms, Configuration configuration) {
        if (comms == null) {
            return;
        }
        AddData addData = configuration.getAddData();
        if (addData == null) {
            addData = configuration.addNewAddData();
        }
        AddData.Data data = addData.addNewData();
        data.setName(MalbecNamespaces.COMMS_DATA_NAME);
        data.setHandleUnknown(AddData.Data.HandleUnknown.PRESERVE);

        StringBuilder groups = new StringBuilder();
        if (comms.getGroups() != null) {
            for (CommConfigData.GroupConfig g : comms.getGroups()) {
                groups.append("<group")
                        .append(" uuid=\"").append(escape(g.getUuid())).append("\"")
                        .append(" name=\"").append(escape(g.getName())).append("\"")
                        .append(" description=\"").append(escape(g.getDescription())).append("\"")
                        .append(" scantime=\"").append(escape(g.getScantime())).append("\"")
                        .append(" enable=\"").append(g.isEnable()).append("\"")
                        .append(" md5=\"").append(escape(g.getMd5())).append("\"/>");
            }
        }
        StringBuilder items = new StringBuilder();
        if (comms.getItems() != null) {
            for (CommConfigData.ItemConfig i : comms.getItems()) {
                items.append("<item")
                        .append(" uuid=\"").append(escape(i.getUuid())).append("\"")
                        .append(" name=\"").append(escape(i.getName())).append("\"")
                        .append(" description=\"").append(escape(i.getDescription())).append("\"")
                        .append(" tag=\"").append(escape(i.getTag())).append("\"")
                        .append(" enable=\"").append(i.isEnable()).append("\"")
                        .append(" group=\"").append(escape(i.getGroupUuid())).append("\"")
                        .append(" md5=\"").append(escape(i.getMd5())).append("\"/>");
            }
        }
        StringBuilder pvs = new StringBuilder();
        if (comms.getPvs() != null) {
            for (CommConfigData.PvConfig p : comms.getPvs()) {
                pvs.append("<pv")
                        .append(" uuid=\"").append(escape(p.getUuid())).append("\"")
                        .append(" name=\"").append(escape(p.getName())).append("\"")
                        .append(" type=\"").append(escape(p.getType())).append("\"")
                        .append(" id=\"").append(escape(p.getId())).append("\"")
                        .append(" offset=\"").append(escape(p.getOffset())).append("\"")
                        .append(" descriptor=\"").append(escape(p.getDescriptor())).append("\"")
                        .append(" scanTime=\"").append(escape(p.getScanTime())).append("\"")
                        .append(" scanEnable=\"").append(p.isScanEnable()).append("\"")
                        .append(" writeEnable=\"").append(p.isWriteEnable()).append("\"")
                        .append(" displayLimitLow=\"").append(escape(p.getDisplayLimitLow())).append("\"")
                        .append(" displayLimitHigh=\"").append(escape(p.getDisplayLimitHigh())).append("\"")
                        .append(" displayDescription=\"").append(escape(p.getDisplayDescription())).append("\"")
                        .append(" displayFormat=\"").append(escape(p.getDisplayFormat())).append("\"")
                        .append(" displayUnits=\"").append(escape(p.getDisplayUnits())).append("\"")
                        .append(" controlLimitLow=\"").append(escape(p.getControlLimitLow())).append("\"")
                        .append(" controlLimitHigh=\"").append(escape(p.getControlLimitHigh())).append("\"")
                        .append(" controlMinStep=\"").append(escape(p.getControlMinStep())).append("\"")
                        .append(" s88Path=\"").append(escape(p.getS88Path())).append("\"")
                        .append(" md5=\"").append(escape(p.getMd5())).append("\"/>");
            }
        }
        String commsXml = "<comm xmlns=\"" + MalbecNamespaces.COMMS_NS + "\">"
                + "<groups>" + groups + "</groups>"
                + "<items>" + items + "</items>"
                + "<pvs>" + pvs + "</pvs>"
                + "</comm>";
        appendAnyContent(data, commsXml);
    }

    private static void appendAnyContent(AddData.Data data, String xml) {
        XmlCursor cursor = data.newCursor();
        try {
            cursor.toEndToken();
            XmlCursor source = XmlObject.Factory.parse(xml).newCursor();
            try {
                source.toFirstChild();
                source.copyXml(cursor);
            } finally {
                source.close();
            }
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo serializar el contenido dev/comms", e);
        } finally {
            cursor.close();
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
                Properties pDevice = new Properties();
                pDevice.put("brand", text(device, "brand"));
                pDevice.put("model", text(device, "model"));
                pDevice.put("protocol", text(device, "protocol"));
                pDevice.put("deviceName", configuration.getName());
                pDevice.put("description", text(device, "description"));
                pDevice.put("uuid", attr(device, "uuid"));
                pDevice.put("enable", Boolean.valueOf(attr(device, "enabled")));
                pDevice.put("s88Node", text(device, "s88Node"));
                pDevice.put("s88Uuid", attr(device, "s88Uuid"));
                pDevice.put("deviceKey", text(device, "deviceKey"));
                pDevice.put("specificParameters", text(device, "specificParameters"));
                
                return new DeviceConfigData(pDevice);
            }
        }
        return null;
    }

    public static CommConfigData readComms(Configuration configuration) {
        AddData addData = configuration.getAddData();
        if (addData == null) {
            return null;
        }
        for (AddData.Data data : addData.getDataArray()) {
            if (MalbecNamespaces.COMMS_DATA_NAME.equals(data.getName())) {
                XmlObject comm = firstChild(data, "comm");
                if (comm == null) {
                    return null;
                }
                List<CommConfigData.GroupConfig> groups = new ArrayList<>();
                XmlObject groupsEl = firstChild(comm, "groups");
                if (groupsEl != null) {
                    for (XmlObject g : children(groupsEl, "group")) {
                        groups.add(new CommConfigData.GroupConfig(
                                attr(g, "uuid"), attr(g, "name"), attr(g, "description"),
                                attr(g, "scantime"), Boolean.parseBoolean(attr(g, "enable")),
                                attr(g, "md5")));
                    }
                }
                List<CommConfigData.ItemConfig> items = new ArrayList<>();
                XmlObject itemsEl = firstChild(comm, "items");
                if (itemsEl != null) {
                    for (XmlObject i : children(itemsEl, "item")) {
                        items.add(new CommConfigData.ItemConfig(
                                attr(i, "uuid"), attr(i, "name"), attr(i, "description"),
                                attr(i, "tag"), Boolean.parseBoolean(attr(i, "enable")),
                                attr(i, "md5"), attr(i, "group")));
                    }
                }
                List<CommConfigData.PvConfig> pvs = new ArrayList<>();
                XmlObject pvsEl = firstChild(comm, "pvs");
                if (pvsEl != null) {
                    for (XmlObject p : children(pvsEl, "pv")) {
                        pvs.add(new CommConfigData.PvConfig(
                                attr(p, "uuid"), attr(p, "name"), attr(p, "type"),
                                attr(p, "id"), attr(p, "offset"), attr(p, "descriptor"),
                                attr(p, "scanTime"), Boolean.parseBoolean(attr(p, "scanEnable")),
                                Boolean.parseBoolean(attr(p, "writeEnable")),
                                attr(p, "displayLimitLow"), attr(p, "displayLimitHigh"),
                                attr(p, "displayDescription"), attr(p, "displayFormat"),
                                attr(p, "displayUnits"), attr(p, "controlLimitLow"),
                                attr(p, "controlLimitHigh"), attr(p, "controlMinStep"),
                                attr(p, "md5"), attr(p, "s88Path")));
                    }
                }
                return new CommConfigData(configuration.getName(), groups, items, pvs);
            }
        }
        return null;
    }

    private static List<XmlObject> children(XmlObject parent, String localName) {
        List<XmlObject> result = new ArrayList<>();
        XmlCursor cursor = parent.newCursor();
        try {
            if (cursor.toFirstChild()) {
                do {
                    QName name = cursor.getName();
                    if (name != null && localName.equals(name.getLocalPart())) {
                        result.add(cursor.getObject());
                    }
                } while (cursor.toNextSibling());
            }
        } finally {
            cursor.close();
        }
        return result;
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
            cursor.close();
        }
        return null;
    }

    private static String attr(XmlObject element, String name) {
        XmlCursor cursor = element.newCursor();
        try {
            String value = cursor.getAttributeText(new QName(name));
            return value == null ? "" : value;
        } finally {
            cursor.close();
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
            cursor.close();
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
