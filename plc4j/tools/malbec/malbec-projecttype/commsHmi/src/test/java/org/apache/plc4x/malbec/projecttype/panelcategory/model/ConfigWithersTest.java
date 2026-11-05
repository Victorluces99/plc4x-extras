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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class ConfigWithersTest {

    private static final String[] ORIGINAL = {
        "uuid-1", "name", "description", "500", "label", "md5-1", "group-1",
        "second", "third", "fourth", "fifth", "sixth", "seventh", "eighth", "ninth",
        "tenth", "eleventh", "twelfth", "thirteenth", "fourteenth"};

    @Test
    void eachWitherChangesOnlyItsField() {
        for (Class<?> type : List.of(PvConfigData.class)) {
            for (RecordComponent component : type.getRecordComponents()) {
                verifyWitherChangesOnlyOneField(type, component);
            }
        }
    }

    private void verifyWitherChangesOnlyOneField(Class<?> type, RecordComponent component) {
        String name = "with" + Character.toUpperCase(component.getName().charAt(0))
                + component.getName().substring(1);
        Object original = exampleInstance(type);
        Object copy = invoke(original, name, component.getType(),
                differentValue(component.getType()));

        assertNotEquals(original, copy,
                type.getSimpleName() + "." + name + " must produce another instance");

        for (RecordComponent other : type.getRecordComponents()) {
            boolean isEditedField = other.getName().equals(component.getName());
            Object expected = isEditedField ? differentValue(other.getType())
                    : read(original, other.getName());
            Object actual = read(copy, other.getName());
            assertEquals(expected, actual,
                    type.getSimpleName() + "." + name + " did not leave "
                            + other.getName() + " unchanged");
        }
    }

    @Test
    void withersReturnCopiesNotTheSameObject() {
        PvConfigData pv = originalPv();

        PvConfigData copy = pv.withDescriptorText("another descriptor");

        assertEquals("another descriptor", copy.getDescriptorText());
        assertEquals(pv.getUuid(), copy.getUuid());
        assertEquals(pv.getMd5(), copy.getMd5());
        assertTrue(pv != copy, "a copy must be a different object");
    }

    @Test
    void chainedWithersAccumulateChanges() {
        PvConfigData pv = originalPv();

        PvConfigData copy = pv
                .withDescriptorText("new descriptor")
                .withScanEnable(false)
                .withDisplayUnits("l/h")
                .withControlMinStep("2.5");

        assertEquals("new descriptor", copy.getDescriptorText());
        assertFalse(copy.isScanEnable(), "scanEnable must remain disabled");
        assertEquals("l/h", copy.getDisplayUnits());
        assertEquals("2.5", copy.getControlMinStep());
        assertEquals(pv.getUuid(), copy.getUuid(), "untouched fields are preserved");
        assertEquals(pv.getDeviceName(), copy.getDeviceName());
        assertTrue(pv.isScanEnable(), "the original is not modified");
    }

    @Test
    void everyComponentHasItsWither() {
        for (Class<?> type : List.of(PvConfigData.class)) {
            for (RecordComponent component : type.getRecordComponents()) {
                String name = "with" + Character.toUpperCase(component.getName().charAt(0))
                        + component.getName().substring(1);
                boolean exists = Arrays.stream(type.getDeclaredMethods())
                        .anyMatch(m -> m.getName().equals(name)
                                && m.getParameterCount() == 1);
                assertTrue(exists, type.getSimpleName() + " must have " + name + "()");
            }
        }
    }

    private static Object exampleInstance(Class<?> type) {
        RecordComponent[] components = type.getRecordComponents();
        Class<?>[] types = new Class<?>[components.length];
        Object[] values = new Object[components.length];
        for (int i = 0; i < components.length; i++) {
            types[i] = components[i].getType();
            values[i] = components[i].getType() == boolean.class
                    ? Boolean.FALSE : differentValues()[i];
        }
        try {
            return type.getDeclaredConstructor(types).newInstance(values);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not construct " + type.getSimpleName(), e);
        }
    }

    private static String[] differentValues() {
        String[] values = new String[ORIGINAL.length];
        Arrays.fill(values, "x");
        values[0] = "original-uuid";
        return values;
    }

    private static Object differentValue(Class<?> type) {
        return type == boolean.class ? Boolean.TRUE : "different-value";
    }

    private static Object read(Object record, String component) {
        try {
            return record.getClass().getMethod(component).invoke(record);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not read " + component, e);
        }
    }

    private static Object invoke(Object record, String method, Class<?> type, Object value) {
        try {
            Method m = record.getClass().getDeclaredMethod(method, type);
            m.setAccessible(true);
            return m.invoke(record, value);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("There is no " + method + "(" + type.getSimpleName()
                    + ") in " + record.getClass().getName()
                    + "; methods: " + namesOf(record.getClass()), e);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not invoke " + method, e);
        }
    }

    private static String namesOf(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods()).map(Method::getName).sorted()
                .reduce((a, b) -> a + ", " + b).orElse("(none)");
    }

    private static PvConfigData originalPv() {
        return new PvConfigData("uuid-1", "PV1", "INT", "Area 1", "0", "descriptor",
                "500", true, true, "0", "10", "Pressure (bar)", "%.2f", "bar", "0", "10",
                "0.1", "md5-pv");
    }
}
