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
import java.util.Properties;

public class CommConfigData {

    private final String deviceName;
    private final List<GroupConfig> groups;
    private final List<ItemConfig> items;
    private final List<PvConfig> pvs;

    public CommConfigData(String deviceName, List<GroupConfig> groups, List<ItemConfig> items, List<PvConfig> pvs) {
        this.deviceName = deviceName;
        this.groups = groups;
        this.items = items;
        this.pvs = pvs;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public List<GroupConfig> getGroups() {
        return groups;
    }

    public List<ItemConfig> getItems() {
        return items;
    }

    public List<PvConfig> getPvs() {
        return pvs;
    }

    // --- MODEL GROUP ---
    public static class GroupConfig {

        private String uuid;
        private String name;
        private String description;
        private String scantime;
        private boolean enable;
        private String md5;

        public GroupConfig(String uuid, String name, String description, String scantime, boolean enable, String md5) {
            this.uuid = uuid;
            this.name = name;
            this.description = description;
            this.scantime = scantime;
            this.enable = enable;
            this.md5 = md5;
        }

        public String getUuid() {
            return uuid;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public String getScantime() {
            return scantime;
        }

        public boolean isEnable() {
            return enable;
        }

        public String getMd5() {
            return md5;
        }
    }

    // --- MODEL ITEM ---
    public static class ItemConfig {

        private String uuid;
        private String name;
        private String description;
        private String tag;
        private boolean enable;
        private String md5;
        private String groupUuid;

        public ItemConfig(String uuid, String name, String description, String tag, boolean enable, String md5) {
            this(uuid, name, description, tag, enable, md5, null);
        }

        public ItemConfig(String uuid, String name, String description, String tag, boolean enable, String md5,
                String groupUuid) {
            this.uuid = uuid;
            this.name = name;
            this.description = description;
            this.tag = tag;
            this.enable = enable;
            this.md5 = md5;
            this.groupUuid = groupUuid;
        }

        public String getUuid() {
            return uuid;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public String getTag() {
            return tag;
        }

        public boolean isEnable() {
            return enable;
        }

        public String getMd5() {
            return md5;
        }

        public String getGroupUuid() {
            return groupUuid;
        }
    }

    // --- MODEL PV ---
    public static class PvConfig {

        private String uuid;
        private String name;
        private String type;
        private String id;
        private String offset;
        private String descriptor;
        private String scanTime;
        private boolean scanEnable;
        private boolean writeEnable;
        private String displayLimitLow;
        private String displayLimitHigh;
        private String displayDescription;
        private String displayFormat;
        private String displayUnits;
        private String controlLimitLow;
        private String controlLimitHigh;
        private String controlMinStep;
        private String md5;
        private String s88Path;

        //TODO: Property java para pasarla al constructor
        public PvConfig(String uuid, String name, String type, String id, String offset, String descriptor,
                String scanTime, boolean scanEnable, boolean writeEnable, String displayLimitLow,
                String displayLimitHigh, String displayDescription, String displayFormat,
                String displayUnits, String controlLimitLow, String controlLimitHigh,
                String controlMinStep, String md5) {
            this(uuid, name, type, id, offset, descriptor, scanTime, scanEnable, writeEnable,
                    displayLimitLow, displayLimitHigh, displayDescription, displayFormat,
                    displayUnits, controlLimitLow, controlLimitHigh, controlMinStep, md5, "");
        }

        public PvConfig(String uuid, String name, String type, String id, String offset, String descriptor,
                String scanTime, boolean scanEnable, boolean writeEnable, String displayLimitLow,
                String displayLimitHigh, String displayDescription, String displayFormat,
                String displayUnits, String controlLimitLow, String controlLimitHigh,
                String controlMinStep, String md5, String s88Path) {
            this.uuid = uuid;
            this.name = name;
            this.type = type;
            this.id = id;
            this.offset = offset;
            this.descriptor = descriptor;
            this.scanTime = scanTime;
            this.scanEnable = scanEnable;
            this.writeEnable = writeEnable;
            this.displayLimitLow = displayLimitLow;
            this.displayLimitHigh = displayLimitHigh;
            this.displayDescription = displayDescription;
            this.displayFormat = displayFormat;
            this.displayUnits = displayUnits;
            this.controlLimitLow = controlLimitLow;
            this.controlLimitHigh = controlLimitHigh;
            this.controlMinStep = controlMinStep;
            this.md5 = md5;
            this.s88Path = s88Path;
        }

        public PvConfig(Properties p) {
            p.get("uuid");
        }

        public String getUuid() {
            return uuid;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        public String getId() {
            return id;
        }

        public String getOffset() {
            return offset;
        }

        public String getDescriptor() {
            return descriptor;
        }

        public String getScanTime() {
            return scanTime;
        }

        public boolean isScanEnable() {
            return scanEnable;
        }

        public boolean isWriteEnable() {
            return writeEnable;
        }

        public String getDisplayLimitLow() {
            return displayLimitLow;
        }

        public String getDisplayLimitHigh() {
            return displayLimitHigh;
        }

        public String getDisplayDescription() {
            return displayDescription;
        }

        public String getDisplayFormat() {
            return displayFormat;
        }

        public String getDisplayUnits() {
            return displayUnits;
        }

        public String getControlLimitLow() {
            return controlLimitLow;
        }

        public String getControlLimitHigh() {
            return controlLimitHigh;
        }

        public String getControlMinStep() {
            return controlMinStep;
        }

        public String getMd5() {
            return md5;
        }

        public String getS88Path() {
            return s88Path;
        }
    }
}
