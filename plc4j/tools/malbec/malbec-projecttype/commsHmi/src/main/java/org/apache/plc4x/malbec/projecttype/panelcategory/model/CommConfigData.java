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
 * Comunicacion de un dispositivo: sus grupos de escaneo, sus areas y sus registros.
 *
 * <p>Los tres tipos anidados son {@code record}, asi que sus campos no se pueden reasignar
 * y el compilador lo impide. Editar una fila del asistente es crear un objeto nuevo y
 * sustituirlo en la lista, nunca tocar el que ya estaba.
 *
 * <p>Se mantienen los nombres {@code getXxx()} porque son el API que consume el resto del
 * proyecto, incluida la tabla de campos de {@code DeviceXmlMapper}.
 */
public class CommConfigData {

    private final String deviceName;
    private final List<GroupConfig> groups;
    private final List<ItemConfig> items;
    private final List<PvConfig> pvs;

    public CommConfigData(String deviceName, List<GroupConfig> groups, List<ItemConfig> items,
            List<PvConfig> pvs) {
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

    /** Grupo de escaneo: cada area pertenece como mucho a uno. */
    public record GroupConfig(String uuid, String name, String description, String scantime,
            boolean enable, String md5) {

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

    /** Area: una direccion de memoria con su etiqueta y su grupo. */
    public record ItemConfig(String uuid, String name, String description, String tag,
            boolean enable, String md5, String groupUuid) {

        public ItemConfig(String uuid, String name, String description, String tag, boolean enable,
                String md5) {
            this(uuid, name, description, tag, enable, md5, "");
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

    /**
     * Registro: un valor del area a la que apunta.
     *
     * <p>{@code areaUuid} es el UUID del area, aunque el atributo del XML siga siendo
     * {@code id}. El nombre viejo {@code getId()} era ambiguo, porque no decia de que
     * identificador se trataba.
     */
    public record PvConfig(String uuid, String name, String type, String areaUuid, String offset,
            String descriptor, String scanTime, boolean scanEnable, boolean writeEnable,
            String displayLimitLow, String displayLimitHigh, String displayDescription,
            String displayFormat, String displayUnits, String controlLimitLow,
            String controlLimitHigh, String controlMinStep, String md5, String s88Path) {

        /** Registro sin ruta en S88, que es el caso habitual. */
        public PvConfig(String uuid, String name, String type, String areaUuid, String offset,
                String descriptor, String scanTime, boolean scanEnable, boolean writeEnable,
                String displayLimitLow, String displayLimitHigh, String displayDescription,
                String displayFormat, String displayUnits, String controlLimitLow,
                String controlLimitHigh, String controlMinStep, String md5) {
            this(uuid, name, type, areaUuid, offset, descriptor, scanTime, scanEnable,
                    writeEnable, displayLimitLow, displayLimitHigh, displayDescription,
                    displayFormat, displayUnits, controlLimitLow, controlLimitHigh,
                    controlMinStep, md5, "");
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

        /** El area a la que pertenece este registro. El XML lo llama {@code id}. */
        public String getAreaUuid() {
            return areaUuid;
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