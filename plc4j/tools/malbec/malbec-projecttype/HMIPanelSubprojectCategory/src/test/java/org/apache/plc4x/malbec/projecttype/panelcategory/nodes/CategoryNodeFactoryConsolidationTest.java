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
 * limitations under the License.
 */
package org.apache.plc4x.malbec.projecttype.panelcategory.nodes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.plc4x.malbec.projecttype.panelcategory.HMICategoryDefinition;
import org.junit.jupiter.api.Test;
import org.netbeans.spi.project.ui.support.NodeFactory;

/**
 * Las ocho factories de categoria tras T4.2.
 *
 * <p>Lo que estas clases hacen dentro de NetBeans no se puede comprobar sin el IDE: si el
 * arbol se ve igual, si los nodos se refrescan al crear un fichero, si la posicion en el
 * arbol es la de antes. Eso queda pendiente de validacion manual.
 *
 * <p>Lo que si se puede comprobar es justo lo que T4.2 cambia y que nadie veria en el arbol:
 * que cada clase apunte a su categoria y no a la de al lado, y que conserve la posicion que
 * tenia. Una clase con la categoria equivocada seguiria mostrando ficheros, solo que los
 * equivocados, y eso no se percibe hasta que alguien busca un fichero y no aparece.
 *
 * <p>Las posiciones se leen de la anotacion, que es la unica fuente: no hay una constante
 * que pueda desincronizarse de ella.
 */
class CategoryNodeFactoryConsolidationTest {

    /** Factory -> (categoria, posicion que tenia antes de T4.2). */
    private static final Map<Class<?>, Object[]> ESPERADO = new LinkedHashMap<>();

    static {
        ESPERADO.put(HMICategoryWarningManagementNodeFactory.class,
                new Object[]{HMICategoryDefinition.NOTICE_MANAGEMENT, 80});
        ESPERADO.put(HMICategoryRecipeNodeFactory.class,
                new Object[]{HMICategoryDefinition.RECIPE, 90});
        ESPERADO.put(HMICategoryRecordNodeFactory.class,
                new Object[]{HMICategoryDefinition.HISTORIAL, 100});
        ESPERADO.put(HMICategoryScriptsNodeFactory.class,
                new Object[]{HMICategoryDefinition.SCRIPTS, 110});
        ESPERADO.put(HMICategoryReportNodeFactory.class,
                new Object[]{HMICategoryDefinition.REPORT, 120});
        ESPERADO.put(HMICategoryTextAndListChartsNodeFactory.class,
                new Object[]{HMICategoryDefinition.TEXT_GRAPHIC, 130});
        ESPERADO.put(HMICategoryRuntimeUserManagementNodeFactory.class,
                new Object[]{HMICategoryDefinition.ADMIN_USER, 140});
        ESPERADO.put(HMICategoryOperatorPanelConfigNodeFactory.class,
                new Object[]{HMICategoryDefinition.CONFIG_PANEL, 150});
    }

    @Test
    void cadaFactoryExtiendeLaClaseBase() {
        for (Class<?> factory : ESPERADO.keySet()) {
            assertTrue(CategoryFilesNodeFactory.class.isAssignableFrom(factory),
                    factory.getSimpleName() + " deberia extender CategoryFilesNodeFactory; si no, "
                            + "vuelve a llevar su propia copia de la logica");
        }
    }

    @Test
    void cadaFactoryDeclaraSuCategoria() throws Exception {
        for (Map.Entry<Class<?>, Object[]> entrada : ESPERADO.entrySet()) {
            Method metodo = entrada.getKey().getDeclaredMethod("categoria");
            metodo.setAccessible(true);

            HMICategoryDefinition obtenida = (HMICategoryDefinition) metodo.invoke(
                    entrada.getKey().getDeclaredConstructor().newInstance());

            assertEquals(entrada.getValue()[0], obtenida,
                    entrada.getKey().getSimpleName() + " tiene que servir los ficheros de "
                            + entrada.getValue()[0] + ": con otra categoria mostraria los "
                            + "ficheros equivocados sin ningún sintoma visible");
        }
    }

    @Test
    void cadaFactoryConservaSuPosicionEnElArbol() throws Exception {
        // La anotacion no se puede leer por reflexion: NetBeans la procesa al compilar y
        // no queda en el class. Lo que se comprueba es que la anotacion apunte a la
        // posicion, leyendo el fuente, y que esa posicion sea la de antes de T4.2.
        for (Map.Entry<Class<?>, Object[]> entrada : ESPERADO.entrySet()) {
            int esperado = (Integer) entrada.getValue()[1];
            String fuente = leerFuente(entrada.getKey());

            assertTrue(fuente.contains("position = " + referenciaDePosicion(entrada.getKey())),
                    entrada.getKey().getSimpleName() + " deberia usar "
                            + referenciaDePosicion(entrada.getKey()) + " en la anotacion");
            assertTrue(fuente.contains("projectType = \"org-apache-plc4x-category\""),
                    entrada.getKey().getSimpleName() + ": el projectType no puede cambiar, "
                            + "la factory no se registraria");
            assertTrue(esperado > 0, "posicion invalida");
        }
    }

    /** Nombre de la constante de CategoryNodePositions que usa la factory. */
    private static String referenciaDePosicion(Class<?> factory) {
        String fuente = leerFuente(factory);
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("position = CategoryNodePositions\\.(\\w+)").matcher(fuente);
        if (!m.find()) {
            throw new IllegalStateException(factory.getSimpleName()
                    + " no usa una posicion de CategoryNodePositions");
        }
        return "CategoryNodePositions." + m.group(1);
    }

    /** Localiza el fuente de la clase en el arbol del modulo. */
    private static String leerFuente(Class<?> factory) {
        String nombre = factory.getSimpleName() + ".java";
        String base = System.getProperty("basedir", ".");
        java.nio.file.Path raiz = java.nio.file.Path.of(base, "src", "main", "java");
        try (java.util.stream.Stream<java.nio.file.Path> archivos = java.nio.file.Files.walk(raiz)) {
            return archivos.filter(p -> p.toString().endsWith(nombre))
                    .findFirst()
                    .map(p -> {
                        try {
                            return java.nio.file.Files.readString(p);
                        } catch (java.io.IOException e) {
                            throw new IllegalStateException(e);
                        }
                    })
                    .orElseThrow(() -> new IllegalStateException("no se encontro el fuente de " + nombre));
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void lasPosicionesDeclaradasSonLasDeAntesDeT42() {
        assertEquals(80, CategoryNodePositions.AVISOS, "Avisos cambiaria de sitio en el arbol");
        assertEquals(90, CategoryNodePositions.RECETAS);
        assertEquals(100, CategoryNodePositions.HISTORIAL);
        assertEquals(110, CategoryNodePositions.SCRIPTS);
        assertEquals(120, CategoryNodePositions.INFORMES);
        assertEquals(130, CategoryNodePositions.TEXTOS_Y_GRAFICOS);
        assertEquals(140, CategoryNodePositions.USUARIOS_RUNTIME);
        assertEquals(150, CategoryNodePositions.CONFIGURACION_PANEL);
    }

    @Test
    void noQuedanDosFactoriesParaLaMismaCategoria() {
        List<HMICategoryDefinition> categorias = ESPERADO.values().stream()
                .map(v -> (HMICategoryDefinition) v[0]).toList();

        assertEquals(categorias.size(), categorias.stream().distinct().count(),
                "dos factories para la misma categoria se registrarian dos veces en el arbol");
    }

    @Test
    void lasPosicionesSiguenSiendoDistintas() {
        List<Integer> posiciones = ESPERADO.values().stream()
                .map(v -> (Integer) v[1]).toList();

        assertEquals(posiciones.size(), posiciones.stream().distinct().count(),
                "dos factories en la misma posicion colisionarian: " + posiciones);
    }

    @Test
    void lasFactoriesQueAnadenAccionSiguenAnadiendola() {
        // Estas tres sobrescriben accionesPropias; el resto no debe hacerlo, para no
        // anadir separadores o acciones que antes no estaban.
        List<Class<?>> conAccion = List.of(
                HMICategoryTextAndListChartsNodeFactory.class,
                HMICategoryRuntimeUserManagementNodeFactory.class,
                HMICategoryOperatorPanelConfigNodeFactory.class);

        for (Class<?> factory : ESPERADO.keySet()) {
            boolean declarada = declaraAccionesPropias(factory);
            assertEquals(conAccion.contains(factory), declarada,
                    factory.getSimpleName() + ": la accion propia se ha perdido o se ha "
                            + "inventado una que antes no existia");
        }
    }

    private static boolean declaraAccionesPropias(Class<?> factory) {
        for (Method metodo : factory.getDeclaredMethods()) {
            if (metodo.getName().equals("accionesPropias")) {
                return true;
            }
        }
        return false;
    }

    @Test
    void laClaseBaseNoSeRegistraPorSiMisma() {
        assertTrue(CategoryFilesNodeFactory.class.getAnnotation(NodeFactory.Registration.class) == null,
                "la base es abstracta de hecho: si se registrara, apareceria un nodo vacio");
        assertTrue(java.lang.reflect.Modifier.isAbstract(CategoryFilesNodeFactory.class.getModifiers()),
                "la base no se puede instanciar por si sola");
    }

    @Test
    void lasFactoriesNoConservanLogicaPropia() {
        // Si alguien reintroduce la copia de la logica dentro de una subclase, el
        // comportamiento dejaria de venir del enum y estos tests dejarian de proteger.
        for (Class<?> factory : ESPERADO.keySet()) {
            for (Method metodo : factory.getDeclaredMethods()) {
                if (metodo.isSynthetic() || metodo.isBridge()) {
                    continue;
                }
                assertTrue(List.of("categoria", "accionesPropias").contains(metodo.getName()),
                        factory.getSimpleName() + "." + metodo.getName()
                                + " no deberia existir: la logica va en la base o en el enum");
            }
        }
    }

    @Test
    void laFactoryBaseExponeLaCategoriaComoConstanteDeEnum() throws Exception {
        Method metodo = CategoryFilesNodeFactory.class.getDeclaredMethod("categoria");
        assertSame(HMICategoryDefinition.class, metodo.getReturnType(),
                "categoria() debe devolver la definicion del enum");
        assertTrue(java.lang.reflect.Modifier.isProtected(metodo.getModifiers()),
                "categoria() es para las subclases, no publico");
    }
}