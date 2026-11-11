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
package org.apache.plc4x.malbec.projecttype.panel.plant;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.plc4x.malbec.s88.api.S88RepositoryProvider;
import org.openide.util.Lookup;

public final class ImportFormats {

    private static final Logger LOGGER = Logger.getLogger(ImportFormats.class.getName());
    public static final Map<String, String> FORMATS =
            Map.of("xml", "Plant Model (*.xml)");

    private ImportFormats() {
    }

    public static String extensionOf(File file) {
        String name = file.getName();
        int punto = name.lastIndexOf('.');
        return punto < 0 || punto == name.length() - 1
                ? "" : name.substring(punto + 1).toLowerCase(Locale.ROOT);
    }

    public static boolean extensionApproved(File file) {
        return FORMATS.containsKey(extensionOf(file));
    }

    public static String allowedFormats() {
        return String.join(" o ", FORMATS.values());
    }

    public static FilenameFilter fileFilter() {
        return (dir, name) -> new File(dir, name).isDirectory()
                || FORMATS.containsKey(extensionOf(new File(name)));
    }

    public static Set<String> formatsWithoutProvider(
            Collection<? extends S88RepositoryProvider> providers, Collection<String> declared) {
        Set<String> withoutProvider = new TreeSet<>(declared);
        for (S88RepositoryProvider provider : providers) {
            withoutProvider.removeIf(provider::accepts);
        }
        return withoutProvider;
    }

    private static final AtomicBoolean NOTICE_ISSUED = new AtomicBoolean();

    public static boolean warnMisalignedFormats() {
        if (!NOTICE_ISSUED.compareAndSet(false, true)) {
            return false;
        }
        Set<String> withoutProvider = formatsWithoutProvider(Lookup.getDefault().lookupAll(S88RepositoryProvider.class),
                FORMATS.keySet());
        if (!withoutProvider.isEmpty()) {
            
LOGGER.log(Level.WARNING,
                    "Plant model formats are declared that no S88RepositoryProvider"
                            + " accept: {0}",
                    withoutProvider);
        }
        return true;
    }
}