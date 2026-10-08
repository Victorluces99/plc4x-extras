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

Estado: Fase 0 completada. B-1 y B-2 resueltas. Actualizado tras `[H-01]`.

---

## Resueltas

### B-1. T1.12 — alcance de `DisplayNodeFactory` — EXCLUIDA

**Decision.** `DisplayNodeFactory` queda fuera de la refactorizacion. No se toca en
ninguna fase.

**Alcance exacto.** La clase se mantiene intacta: sin cambios de comportamiento, sin
reformateo y sin moverla. Como tampoco se toca, sus dependencias con el resto de clases
no se modifican y no queda trabajo de desacoplamiento pendiente alrededor de ella.

**Sin pendientes.** No hace falta ninguna accion posterior para T1.12. La decision se
tomo con la Fase 0 ya cerrada, asi que la tarea nunca llego a entrar en la lista.

### B-2. H-01 — verificacion mediante costura inyectable — RESUELTA EN `[H-01]`

**Decision.** Levantar el arnes de NetBeans queda descartado, igual que la verificacion
manual. Se aisla la politica de carga del entorno.

**Como quedo.** `CommConfigLoader` (package-private) recibe el `CommConfigRepository`
por constructor y encapsulate la politica de carga. `reload()` delega en el, de modo que
la politica ya no necesita `Project`, `FileObject` ni `FileSystem`, y se puede simular
con un repositorio que falle.

**Comprobado que el test tiene dientes.** Se reintrodujo el `catch` que se acaba de
eliminar y 3 de los tests fallaron con mensajes explicitos; la mutacion se revirtio
despues. Un test que nunca ha fallado no demuestra nada.

**Lo que sigue sin poder verificarse automaticamente.** La guarda de `save()` —que se
niega a escribir si hay un error de carga pendiente— necesita construir un
`HMICommunicationModel` con un `Project` real. Este test queda `@Disabled` con `fail()`
para que no produzca un falso verde. Es una linea de codigo; queda pendiente el arnes
o una costura equivalente para el guardado.

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
| T1.12 | `DisplayNodeFactory` excluida de la refactorizacion por decision (ver B-1). |
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
