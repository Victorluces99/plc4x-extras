<!--
  Licensed to the Apache Software Foundation (ASF) under one
  or more contributor license agreements.  See the NOTICE file
  distributed with this work for additional information
  regarding copyright ownership.  The ASF licenses this file
  to you under the Apache License, Version 2.0 (the
  "License"); you may not use this file except in compliance
  with the License.  You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing,
  software distributed under the License is distributed on an
  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  KIND, either express or implied.  See the License for the
  specific language governing permissions and limitations
  under the License.
-->

# Preguntas abiertas

Dudas que necesitan una decision externa antes de poder seguir. Todo lo que no
aparece aqui se puede resolver leyendo el codigo.

Estado: Fase 0 completada. Actualizado al terminar `[T0.4]`.

---

## Bloqueantes

### B-1. T1.12 — alcance de `DisplayNodeFactory`

**Pregunta.** Should `DisplayNodeFactory` apply to every category, or only to those
with a real display template?

**Por que importa.** El guard actual esta comentado, asi que la fabrica construye
nodos de display para categorias sin plantilla. Cambiarlo afecta al arbol de
nodos del explorador, no solo a la pintura de un panel.

**Opciones.**

1. Dejarlo como esta hasta que lo confirme el responsable (tarea en pausa).
2. Aplicarlo solo a categorias con `folderName` de display.
3. Aplicarlo a todas y tratar la plantilla ausente como caso valido.

**Quien decide.** Responsable del modulo de categorias. Prohibido tocar
`DisplayNodeFactory` mientras tanto.

### B-2. H-01 no se puede verificar de verdad en este build

**Pregunta.** How should the H-01 fix be verified: NetBeans test harness, or an
injectable load seam in `HMICommunicationModel`?

**Por que importa.** `reload()` traga el error con `catch (Throwable)` y devuelve
una configuracion vacia sin registrar nada (`HMICommunicationModel.java:102-106`).
Los tests de `reload()` estan `@Disabled` porque no hay ningun `FileSystem`
registrado bajo surefire: `FileUtil.toFileObject(File)` devuelve `null` para
cualquier directorio real, y `org-openide-filesystems-nb` no esta en el classpath
de test. Comprobado con un sonda, no supuesto.

**Opciones.**

1. Anadir el arnes de pruebas de NetBeans (mas fiel, mas coste).
2. Extraer la carga a un punto inyectable y testearlo sin `Project` (mas barato,
   toca codigo de produccion).
3. Corregir H-01 sin test automatico y verificarlo a mano.

**Nota.** Los dos tests deshabilitados de
`HMICommunicationModelIntegrityTest` llaman a `fail()` a proposito, para que
quitarlos no produzca un falso verde.

---

## No bloqueantes

### N-1. `boot.db` no guarda `brand`, `model`, `s88Node` ni `s88Uuid`

**Pregunta.** Es intencionado que la tabla `Devices` lose esos cuatro campos que si
existen en `comunicacion.xml`?

**Como se verifico.** `SQL_CREATE_TABLE_DEVICES` (`BootDbRepository.java:35`) solo
declara `DeviceUuId`, `DriverName`, `DeviceKey`, `DeviceUrl`, `DeviceName`,
`DeviceDescription`, `DeviceEnable` y `Md5`. El XML si persiste los cuatro campos.
El golden `boot_db_reference.sql` lo deja fijo.

**Opciones.** 1. Es correcto, `boot.db` solo necesita lo de ejecucion.
2. Anadir columnas (cambia el esquema, prohibido en este plan).

### N-2. `Devices.Md5` es un literal, los demas si llevan el md5 real

**Pregunta.** Se debe calcular el md5 del dispositivo como ya se hace con grupos,
areas y PVs?

**Como se verifico.** `BootDbRepository.java:149` hace
`pstmt.setString(8, "md5_device_hash")`, mientras las lineas 163, 178 y 203 usan
`g.getMd5()`, `item.getMd5()` y `pv.getMd5()`.

**Opciones.** 1. Dejarlo, es un placeholder conocido. 2. Calcularlo en T1.5.

### N-3. Los booleanos se guardan como texto en SQLite

**Pregunta.** Es intencionado que `DeviceEnable`, `GroupEnable`, `ItemEnable`,
`PvScanEnable` y `PvWriteEnable` se guarden como `'TRUE'` / `'FALSE'` en vez de
0/1?

**Como se verifico.** Todas las columnas se declaran `TEXT` y el golden muestra los
valores entrecomillados.

**Opciones.** 1. Aceptado. 2. Migrar a INTEGER, lo que cambia el formato y obliga a
rehacer el golden de `boot.db`.

---

## Desviaciones del plan ya resueltas

Se anotan para que no pasen por omision.

### D-1. T0.2(b): el golden de `boot.db` no es un round-trip

**Lo que pedia el plan.** Un round-trip `load -> save` de `boot.db`.

**Por que no es posible.** `BootDbRepository` solo expone `createDB`,
`insertDeviceConfig` y `rebuild`. No existe ningun metodo que lea `boot.db` de
vuelta a un `CommunicationsConfig`, porque su unica responsabilidad es construir la
base a partir del modelo en memoria.

**Lo que se hizo.** Generacion determinista: se construye el modelo con datos fijos,
se llama a `rebuild()` y se compara el volcado logico resultante contra
`boot_db_reference.sql`. El volcado ordena siempre tablas, columnas y filas.

**Descartado ademas.** Comparar bytes del fichero `.db`. SQLite no garantiza
reproducibilidad binaria, asi que daria falsos negativos ante refactors inocuos.

### D-2. El golden usa datos sinteticos, no un `comunicacion.xml` real

**Por que.** El proyecto `llenadora` contiene datos corruptos: un area sin rango
(`%DB1[]`) y dos areas solapadas sobre `%DB21.DDB4` (`INTEGER` en `[0..9]` frente a
`REAL` en `[0..12]`). Alimentar el golden con eso fijaria la corrupcion como
comportamiento esperado y bloquearia cualquier correccion futura.

**Pendiente de decision del usuario.** Si esos datos se deben reparar, y con que
criterio. Ver N-4 de la seccion siguiente.

### D-3. T1.12, T2.5, T6.2 y T6.5 no se ejecutan

| Tarea | Motivo |
|---|---|
| T1.12 | Prohibido tocar `DisplayNodeFactory`. Ver B-1. |
| T2.5 | `commsHmi` ya es `nbm` con el jar embebido via `X-Class-Path`, sin dependencias de NetBeans, y un spike previo verifico que funciona. |
| T6.2 | `HMIPanelImportAction` ya usa `sealed interface`, `record`, `ProgressUtils`, filtro de extension y `Logger`. |
| T6.5 | El aviso de formatos desalineados ya se emite al abrir el importador, no al pulsar «Importar». |

---

## Pendiente de informacion externa

### N-4. Datos corruptos en el proyecto `llenadora`

**Situacion.** `Comunicacion/comunicacion.xml` tiene un area sin rango y dos areas
solapadas. No se ha tocado el fichero.

**Pregunta.** Hay que repararlos, y quien decide el criterio de solapamiento?

### N-5. CLI de Phoebus (T1.7)

**Pregunta.** Cual es el ejecutable y la linea de comandos real para generar un
proyecto Phoebus desde linea de ordenes?

**Por que importa.** `ConfigPhoebus` no tiene ningun test (0 tests en el baseline)
y todo lo que se declare sobre su CLI quedaria sin verificar hasta saberlo.

### N-6. Cambio local ajeno, fuera de alcance

`plc4j/tools/malbec/application/pom.xml` tiene un cambio sin commitear que sustituye
`org-apache-commons-lang3` de `RELEASE130` a `${netbeans.version}`. Ese modulo esta
fuera del alcance de este plan, asi que se ha dejado intacto en el arbol de trabajo
y sin commitear en todas las fases.

---

## Notas de medicion

El plan estimaba ~260 tests en los cuatro modulos y ~180 en `commsHmi`. Los valores
reales del baseline fueron 247 y 166. Las estimaciones no sirven como medida de
exito; `docs/BASELINE.md` tiene el detalle por modulo.
