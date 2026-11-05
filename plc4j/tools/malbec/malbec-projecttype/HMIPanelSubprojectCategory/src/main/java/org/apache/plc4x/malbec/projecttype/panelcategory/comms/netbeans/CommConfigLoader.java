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
package org.apache.plc4x.malbec.projecttype.panelcategory.comms.netbeans;

import org.apache.plc4x.malbec.projecttype.panelcategory.comms.api.CommConfigRepository;
import org.apache.plc4x.malbec.projecttype.panelcategory.comms.api.CommunicationsConfig;

final class CommConfigLoader {

    /**
     * @param config         the configuration file, now with the UUIDs fixed
     * @param uuidReparados  whether it was necessary to fill in any missing UUIDs,
     *                       to determine whether the file needs to be rewritten
     */
    record Result(CommunicationsConfig config, boolean uuidReparados) {
    }

    private final CommConfigRepository repository;

    CommConfigLoader(CommConfigRepository repository) {
        if (repository == null) {
            throw new IllegalArgumentException("A configuration repository is required");
        }
        this.repository = repository;
    }

    Result load() throws Exception {
        CommunicationsConfig config = repository.load();
        if (config == null) {
            throw new IllegalStateException(
                    "The repository returned a null configuration instead of an empty one");
        }
        boolean repaired = HMICommunicationModel.repararUuidsAusentes(config);
        return new Result(config, repaired);
    }
}
