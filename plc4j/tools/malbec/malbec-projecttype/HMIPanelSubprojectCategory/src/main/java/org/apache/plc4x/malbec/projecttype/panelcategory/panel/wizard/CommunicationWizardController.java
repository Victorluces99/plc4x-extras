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
package org.apache.plc4x.malbec.projecttype.panelcategory.panel.wizard;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.plc4x.malbec.projecttype.panelcategory.comms.xml.HMICommunicationModel;
import org.apache.plc4x.malbec.projecttype.panelcategory.panel.CommConfigData;
import org.apache.plc4x.malbec.projecttype.panelcategory.panel.DeviceConfigData;
import org.netbeans.api.project.Project;

/**
 * Reglas de negocio y persistencia del asistente de comunicación.
 *
 * <p>Los paneles no tocan ni el modelo de configuración ni la base de datos:
 * le piden una operación al controlador y refreshizan la vista. Las reglas que
 * importan están todas acá:</p>
 *
 * <ul>
 *   <li>Modificar conserva uuid, nombre, grupo de escaneo y md5: el registro se
 *       sobreescribe en el XML y en la base, nunca se duplica.</li>
 *   <li>El grupo de escaneo de un área no se cambia desde la UI.</li>
 *   <li>Un área sin grupo bloquea el guardado completo, no se completa sola.</li>
 *   <li>Un grupo con áreas asociadas no se borra; un área con variables las
 *       arrastra en cascada, avisando antes.</li>
 *   <li>El tag del área tiene que entenderse y no puede pisar a otra: dos áreas
 *       sobre los mismos bytes del mismo bloque es un error de configuración, no
 *       una coincidencia.</li>
 *   <li>Un área es homogénea: todas sus variables comparten tipo y, por tanto,
 *       tamaño. Lo fija el tag si declara uno y, si no, la primera variable.</li>
 * </ul>
 */
public class CommunicationWizardController {

    private static final String MD5_GROUP = "md5_group_hash";
    private static final String MD5_ITEM = "md5_item_hash";
    private static final String MD5_PV = "md5_pv_hash";

    private final Project project;
    private final HMICommunicationModel model;
    private final CommunicationWizardState state;

    public CommunicationWizardController(Project project, CommunicationWizardState state) {
        this.project = project;
        this.state = state;
        this.model = project != null ? project.getLookup().lookup(HMICommunicationModel.class) : null;
    }

    public CommunicationWizardState getState() {
        return state;
    }

    public Project getProject() {
        return project;
    }

    // --- dispositivo ---------------------------------------------------------

    public List<DeviceConfigData> getAvailableDevices() {
        return model != null ? model.getDevices() : new ArrayList<>();
    }

    public void selectDevice(DeviceConfigData device) {
        state.setSelectedDevice(device);
        if (device != null && model != null) {
            state.reset(model.getComms(device.getUuid()));
        } else {
            state.reset(null);
        }
    }

    // --- grupos --------------------------------------------------------------

    public void addGroup(String uuid, String name, String desc, String scan, boolean enable) {
        state.getGroups().add(
                new CommConfigData.GroupConfig(uuid, name, desc, scan, enable, MD5_GROUP));
        state.setCambiosSinGuardar(true);
    }

    /**
     * Aplica cambios sobre el grupo existente conservando uuid, nombre y md5:
     * el nombre es la identidad del grupo y el scantime bloqueado en la UI se
     * conserva si el combo no tiene nada seleccionado.
     */
    public void updateGroup(String uuid, String desc, String scan, boolean enable) {
        CommConfigData.GroupConfig actual = state.groupByUuid(uuid);
        if (actual == null) {
            return;
        }
        String scantime = scan != null && !scan.isEmpty() ? scan : actual.getScantime();
        int idx = state.getGroups().indexOf(actual);
        state.getGroups().set(idx, new CommConfigData.GroupConfig(
                actual.getUuid(), actual.getName(), desc, scantime, enable, actual.getMd5()));
        state.setCambiosSinGuardar(true);
    }

    /**
     * Elimina el grupo, salvo que tenga áreas asociadas.
     *
     * @return los nombres de las áreas que impiden el borrado; lista vacía si el
     *         grupo se eliminó. Nunca lanza: el aviso es cosa de la vista.
     */
    public List<String> deleteGroup(String uuid) {
        CommConfigData.GroupConfig group = state.groupByUuid(uuid);
        if (group == null) {
            return List.of();
        }
        List<String> areasDelGrupo = new ArrayList<>();
        for (CommConfigData.ItemConfig i : state.getItems()) {
            if (group.getUuid().equals(i.getGroupUuid())) {
                areasDelGrupo.add(i.getName());
            }
        }
        if (!areasDelGrupo.isEmpty()) {
            return areasDelGrupo;
        }
        state.getGroups().remove(group);
        state.setCambiosSinGuardar(true);
        return List.of();
    }

    // --- áreas (items) -------------------------------------------------------

    public void addItem(String uuid, String name, String desc, String tag, boolean enable, String groupUuid) {
        state.getItems().add(new CommConfigData.ItemConfig(
                uuid, name, desc, tag, enable, MD5_ITEM, groupUuid));
        state.getItemsGroup().put(uuid, groupUuid);
        state.setCambiosSinGuardar(true);
    }

    /**
     * Aplica cambios sobre el área existente conservando uuid, nombre, grupo de
     * escaneo y md5. El grupo no llega como parámetro a propósito: la relación se
     * fija al crear el área y no se modifica desde la UI.
     */
    public void updateItem(String uuid, String desc, String tag, boolean enable) {
        CommConfigData.ItemConfig actual = state.itemByUuid(uuid);
        if (actual == null) {
            return;
        }
        int idx = state.getItems().indexOf(actual);
        state.getItems().set(idx, new CommConfigData.ItemConfig(
                actual.getUuid(), actual.getName(), desc, tag, enable,
                actual.getMd5(), actual.getGroupUuid()));
        state.getItemsGroup().put(actual.getUuid(), actual.getGroupUuid());
        state.setCambiosSinGuardar(true);
    }

    /**
     * Elimina el área y arrastra sus variables en cascada, para no dejar
     * variables apuntando a un área que ya no existe.
     *
     * @return los nombres de las variables que se eliminarían; lista vacía si se
     *         borró. La vista debe pedir confirmación cuando no esté vacía.
     */
    public List<String> deleteItem(String uuid) {
        CommConfigData.ItemConfig item = state.itemByUuid(uuid);
        if (item == null) {
            return List.of();
        }
        List<String> nombres = new ArrayList<>();
        for (CommConfigData.PvConfig pv : state.getPvs()) {
            if (item.getUuid().equals(pv.getId())) {
                nombres.add(pv.getName());
            }
        }
        state.getPvs().removeIf(pv -> uuid.equals(pv.getId()));
        state.getItems().remove(item);
        state.getItemsGroup().remove(uuid);
        state.setCambiosSinGuardar(true);
        return nombres;
    }

    // --- variables (pvs) -----------------------------------------------------

    public void addPv(CommConfigData.PvConfig pv) {
        state.getPvs().add(pv);
        state.setCambiosSinGuardar(true);
    }

    /**
     * Aplica cambios sobre la variable existente conservando lo que define su
     * ubicación en la memoria: uuid, nombre, tipo, PvId, offset, md5 y ruta S88.
     *
     * <p>El offset no llega como parámetro a propósito. Es
     * {@code posición × tamaño del tipo}, y la posición depende del orden de las
     * variables del área: recalcularlo al modificar desplazaría el resto de las
     * variables que vienen después. Por eso mover una variable a otro PvId no se
     * permite; hay que borrarla y volver a crearla.</p>
     */
    public void updatePv(String uuid, String descriptor, String scanTime,
            boolean scanEnable, boolean writeEnable, String displayLimitLow,
            String displayLimitHigh, String displayDescription, String displayFormat,
            String displayUnits, String controlLimitLow, String controlLimitHigh,
            String controlMinStep) {
        CommConfigData.PvConfig actual = state.pvByUuid(uuid);
        if (actual == null) {
            return;
        }
        int idx = state.getPvs().indexOf(actual);
        state.getPvs().set(idx, new CommConfigData.PvConfig(
                actual.getUuid(), actual.getName(), actual.getType(), actual.getId(),
                actual.getOffset(), descriptor, scanTime, scanEnable, writeEnable,
                displayLimitLow, displayLimitHigh, displayDescription, displayFormat,
                displayUnits, controlLimitLow, controlLimitHigh, controlMinStep,
                actual.getMd5(), actual.getS88Path()));
        state.setCambiosSinGuardar(true);
    }

    /**
     * Elimina la variable. No arrastra nada en cascada: es el último eslabón de
     * la relación área → variable, así que no hay registros que la referencien.
     *
     * <p>Si era la primera variable del área, el área queda sin tipo fijado y
     * vuelve a admitir variables de cualquier tipo.</p>
     *
     * @return true si la variable existía y fue eliminada
     */
    public boolean deletePv(String uuid) {
        CommConfigData.PvConfig pv = state.pvByUuid(uuid);
        if (pv == null) {
            return false;
        }
        state.getPvs().remove(pv);
        state.setCambiosSinGuardar(true);
        return true;
    }

    /** Md5 de las variables: todas las que se agregan usan el mismo marcador. */
    public static String pvMd5() {
        return MD5_PV;
    }

    /** Md5 de los grupos. */
    public static String groupMd5() {
        return MD5_GROUP;
    }

    // --- layout de memoria de un área ---------------------------------------

    /**
     * Tag del área ya interpretado.
     *
     * @return el tag, o null si el área no existe o su tag no se entiende
     */
    private MemoryTag tagOf(String areaUuid) {
        CommConfigData.ItemConfig item = state.itemByUuid(areaUuid);
        return item == null ? null : MemoryTag.parse(item.getTag());
    }

    /**
     * Tipo al que queda fijado el área.
     *
     * <p>Si el tag declara familia y el código de área deja un solo tipo posible,
     * manda el tag: es el usuario quien escribió el área y es lo que espera. Si el
     * tag admite más de un tipo, la elección la hace el desplegable, así que aquí
     * todavía no hay nada fijado. Y si el tag no declara nada, lo fija la primera
     * variable guardada.</p>
     *
     * @return el tipo fijado, o null si el área todavía admite varios
     */
    public DataType lockedType(String areaUuid) {
        MemoryTag tag = tagOf(areaUuid);
        if (tag != null && tag.family() != null) {
            List<DataType> candidatos = DataType.candidatos(tag.family(), tag.codeBits());
            if (candidatos.size() == 1) {
                return candidatos.get(0);
            }
        }
        return tipoDePrimeraVariable(areaUuid);
    }

    /**
     * Tipos que declara el tag del área, sin mirar ninguna variable. Es lo que se
     * ofrece mientras el área está vacía.
     *
     * @return los candidatos, o lista vacía si el tag no dice nada
     */
    private List<DataType> candidatosDelTag(String areaUuid) {
        MemoryTag tag = tagOf(areaUuid);
        if (tag == null || tag.family() == null) {
            return List.of();
        }
        return DataType.candidatos(tag.family(), tag.codeBits());
    }

    /**
     * Tipo de la primera variable del área.
     *
     * <p>Es la que fija el tipo de todas las demás, porque el offset de cada una
     * sale de la posición que le toca y esa posición se mide en bytes del mismo
     * tipo. Si las variables no tuvieran todas el mismo tamaño, las siguientes
     * apuntarían a un sitio equivocado.</p>
     *
     * @return el tipo, o null si el área todavía no tiene variables
     */
    private DataType tipoDePrimeraVariable(String areaUuid) {
        List<CommConfigData.PvConfig> areaPvs = state.pvsOfArea(areaUuid);
        return areaPvs.isEmpty() ? null : DataType.find(areaPvs.get(0).getType());
    }

    /**
     * Tipos que se pueden elegir para el área.
     *
     * <p>Mientras el área está vacía se ofrecen todos los tipos que caben en su
     * tag, que es cuando el usuario está eligiendo. En cuanto guarda una variable
     * manda el tipo de esa variable y sólo se ofrece ése, porque un área es un
     * bloque homogéneo: si una variable es un float de cuatro bytes y la siguiente
     * un double de ocho, el offset de la segunda apuntaría a mitad del float
     * anterior y el PLC leería bytes que no son.</p>
     *
     * <p>El orden importa. Si se mirase la familia del tag primero, un área REAL
     * con un float ya guardado seguiría ofreciendo float y double, y el área se
     * llenaría de los dos sin avisar. La primera variable es la que fija el tipo,
     * y el tag sólo decide qué se ofrece antes de que exista ninguna.</p>
     *
     * <p>Si el tipo de la primera variable no encaja con el tag, se prefiere el
     * tag. Puede ocurrir con datos importados a mano, y en ese caso el tag es lo
     * que describe la memoria de verdad.</p>
     */
    public List<DataType> typesAllowed(String areaUuid) {
        if (state.itemByUuid(areaUuid) == null) {
            return List.of();
        }
        List<DataType> candidatos = candidatosDelTag(areaUuid);
        DataType elegido = tipoDePrimeraVariable(areaUuid);
        if (elegido != null && (candidatos.isEmpty() || candidatos.contains(elegido))) {
            return List.of(elegido);
        }
        if (!candidatos.isEmpty()) {
            return candidatos;
        }
        DataType fijado = lockedType(areaUuid);
        if (fijado != null) {
            return List.of(fijado);
        }
        List<DataType> todos = new ArrayList<>();
        for (DataType t : DataType.values()) {
            if (t.habilitado()) {
                todos.add(t);
            }
        }
        return todos;
    }

    /**
     * Offset siguiente del área, relativo a su byte base. La dirección real es
     * {@code baseByte + offset}.
     *
     * <p>Devuelve el primer hueco libre y no el que seguiría la cuenta. La cuenta
     * no vale: al borrar una variable del medio, las que quedan después conservan
     * su offset y la siguiente caería justo encima de una de ellas. Recorriendo lo
     * ocupado el hueco se rellena sin mover nada, que es lo que hace falta para que
     * borrar no descuadre la memoria.</p>
     *
     * <p>Los booleanos se direccionan {@code byte.bit} y el resto con el byte
     * pelado.</p>
     *
     * @return el offset, o null si el área no tiene tag válido, el tipo no tiene
     *         tamaño fijo o ya no queda hueco
     */
    public String nextOffset(String areaUuid, DataType type) {
        MemoryTag tag = tagOf(areaUuid);
        if (tag == null || type == null || !type.habilitado() || tag.byteCapacity() <= 0) {
            return null;
        }
        Set<Integer> ocupados = bitsOcupados(areaUuid);
        int desde = tag.firstByte() * 8;
        int hasta = desde + tag.byteCapacity() * 8;
        int ancho = type.slotBits();
        for (int bits = desde; bits + ancho <= hasta; bits += ancho) {
            if (libre(bits, ancho, ocupados)) {
                return type.bitAddressed()
                        ? (bits / 8) + "." + (bits % 8)
                        : String.valueOf(bits / 8);
            }
        }
        return null;
    }

    /**
     * true si los {@code ancho} bits que empiezan en {@code desde} están libres.
     *
     * <p>No basta con mirar el primero. En un área de diez bytes con int, los
     * offsets 0, 4 y 8 están detrás de tres variables; el hueco que queda son los
     * bytes 9 a 11, que ni existen. Devolver el byte 9 daría por buena una variable
     * que se saldría del área, y el error sólo aparecería al leer del PLC. Por eso
     * se comprueba la ranura entera y que quepa dentro del área.</p>
     */
    private static boolean libre(int desde, int ancho, Set<Integer> ocupados) {
        for (int bit = desde; bit < desde + ancho; bit++) {
            if (ocupados.contains(bit)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Bits que ocupa cada variable del área, puesta su extensión completa.
     *
     * <p>Se marcan todos los bits de cada variable, no sólo el del offset. Un int
     * en el byte 0 ocupa los bytes 0 a 3, y si sólo se marcara el 0 una variable
     * que empezara en el byte 2 se solaparía con ella sin que nada lo notara.</p>
     *
     * <p>Si el tipo no se reconoce, se marca el offset y un byte entero, que es lo
     * mínimo que se puede asumir y lo bastante para no solapar dos variables.</p>
     */
    private Set<Integer> bitsOcupados(String areaUuid) {
        Set<Integer> ocupados = new HashSet<>();
        for (CommConfigData.PvConfig pv : state.pvsOfArea(areaUuid)) {
            int bits = bitsDeOffset(pv.getOffset());
            if (bits < 0) {
                continue;
            }
            DataType type = DataType.find(pv.getType());
            int ancho = type == null ? 8 : type.slotBits();
            for (int bit = bits; bit < bits + ancho; bit++) {
                ocupados.add(bit);
            }
        }
        return ocupados;
    }

    /**
     * Offset de una variable contado en bits desde el inicio del rango.
     *
     * @return los bits, o -1 si el offset guardado no se entiende
     */
    private static int bitsDeOffset(String offset) {
        if (offset == null || offset.isEmpty()) {
            return -1;
        }
        try {
            int punto = offset.indexOf('.');
            int byteOffset = Integer.parseInt(punto < 0 ? offset : offset.substring(0, punto));
            int bitOffset = punto < 0 ? 0 : Integer.parseInt(offset.substring(punto + 1));
            return byteOffset * 8 + bitOffset;
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    /**
     * Bytes que abarca el área.
     *
     * @return la capacidad, o -1 si de su tag no se puede saber nada. Con tags
     *         simbólicos o sin código ni rango el área sigue siendo válida, pero
     *         no hay tamaño que acotar
     */
    public int areaCapacityBytes(String areaUuid) {
        MemoryTag tag = tagOf(areaUuid);
        if (tag == null) {
            return -1;
        }
        int bytes = tag.byteCapacity();
        return bytes > 0 ? bytes : -1;
    }

    /**
     * Casillas que le quedan al área para el tipo dado.
     *
     * <p>Una ranura es un byte, salvo en los booleanos, que van ocho por byte.</p>
     *
     * @return las libres, o -1 si no hay tag válido o el tipo no tiene tamaño
     *         fijo. En ese caso no se puede acotar y no se bloquea nada, porque
     *         inventar un límite impediría configurar áreas legítimas. Un área a la
     *         que ya no le caben más devuelve 0, nunca negativo, y jamás -1: el -1
     *         está reservado para "no se sabe", y confundirlos haría que un área
     *         llena pareciera un área sin límite.
     */
    public int areaFreeSlots(String areaUuid, DataType type) {
        MemoryTag tag = tagOf(areaUuid);
        if (tag == null || type == null || !type.habilitado()) {
            return -1;
        }
        int bytes = tag.byteCapacity();
        if (bytes <= 0) {
            return -1;
        }
        return Math.max(0, (bytes * 8) / type.slotBits() - state.pvsOfArea(areaUuid).size());
    }

    /**
     * Bytes que le quedan al área.
     *
     * <p>Es el dato que el usuario entiende: el área abarca unos bytes y se han
     * usado otros tantos. Decir "quedan N casillas en M bytes" con M siendo el
     * total del área mezcla las dos cosas y no dice nada de lo que queda de
     * verdad, que son los bytes.</p>
     *
     * @return los bytes sin ocupar, o -1 si no se puede saber
     */
    public int areaFreeBytes(String areaUuid) {
        MemoryTag tag = tagOf(areaUuid);
        if (tag == null || tag.byteCapacity() <= 0) {
            return -1;
        }
        // No puede salir negativo. Si lo está, es que las variables guardadas se
        // salen del área, y el área está llena de todas formas: lo que no puede
        // aparecer es un "-2 bytes libres" en pantalla.
        return Math.max(0, tag.byteCapacity() - bytesOcupados(areaUuid));
    }

    /** Bytes que llevan las variables del área. */
    private int bytesOcupados(String areaUuid) {
        int total = 0;
        for (CommConfigData.PvConfig pv : state.pvsOfArea(areaUuid)) {
            DataType type = DataType.find(pv.getType());
            if (type == null) {
                continue;
            }
            total += (type.slotBits() + 7) / 8;
        }
        return total;
    }

    /**
     * Área ya guardada cuyos bytes se pisan con este tag.
     *
     * @param uuidEnEdicion área que se está modificando, que no choca consigo misma
     * @return el área que estorba, o null si el tag no pisa a nadie
     */
    public CommConfigData.ItemConfig areaQueChoca(String tag, String uuidEnEdicion) {
        MemoryTag nuevo = MemoryTag.parse(tag);
        if (nuevo == null) {
            return null;
        }
        for (CommConfigData.ItemConfig i : state.getItems()) {
            if (uuidEnEdicion != null && uuidEnEdicion.equals(i.getUuid())) {
                continue;
            }
            if (nuevo.solapaCon(MemoryTag.parse(i.getTag()))) {
                return i;
            }
        }
        return null;
    }

/**
     * Área ya guardada que repite este tag sin que se pueda saber si se pisan.
 *
     * <p>Sólo se usa para los tags sin dirección. Un tag simbólico como
 * * {@code Control_Panel.Start_Button} no se descompone en nada, así que el
     * solapamiento no tiene nada que comparar y dos áreas con el mismo nombre se
 * colarían: apuntarían al mismo símbolo sin que nada se entere. Ahí el tag entero
     * es la identidad y compararlo es lo único que queda.</p>
 *
 * * <p>Con dirección no se usa. Dos áreas del mismo bloque y código con rangos
 * * distintos sí pueden convivir, siempre que no se pisen, y eso lo decide
 * * {@link #areaQueChoca}. Por ejemplo {@code %DB22.DBB4[10..16]} y
 * * {@code %DB22.DBB4[17..22]} comparten byte de arranque y son válidas: la segunda
 * * empieza donde termina la primera.</p>
 *
 * @param uuidEnEdicion área que se está modificando, que no se pisa a sí misma
 * @return el área que ya usa ese tag, o null si está libre o el tag tiene dirección
 */
    public CommConfigData.ItemConfig areaConMismoTag(String tag, String uuidEnEdicion) {
        MemoryTag t = MemoryTag.parse(tag);
        if (t == null || t.tieneDireccion()) {
            return null;
        }
        String buscado = normalizaTexto(tag);
        if (buscado.isEmpty()) {
            return null;
        }
        for (CommConfigData.ItemConfig i : state.getItems()) {
            if (uuidEnEdicion != null && uuidEnEdicion.equals(i.getUuid())) {
                continue;
            }
            MemoryTag suyo = MemoryTag.parse(i.getTag());
            if (suyo != null && suyo.tieneDireccion()) {
                continue;
            }
            if (buscado.equals(normalizaTexto(i.getTag()))) {
                return i;
            }
        }
        return null;
    }

    /**
     * Reduce un tag a la clave con la que se comparan dos nombres, quitando el
     * {@code %} inicial, el {@code :TIPO} final y los espacios sobrantes, que son
     * maneras de escribir lo mismo.
     */
    private static String normalizaTexto(String tag) {
        if (tag == null) {
            return "";
        }
        String t = tag.trim();
        if (t.startsWith("%")) {
            t = t.substring(1).trim();
        }
        int dosPuntos = t.lastIndexOf(':');
        if (dosPuntos > 0) {
            t = t.substring(0, dosPuntos).trim();
        }
        return t.replaceAll("\\s+", "").toUpperCase();
    }

    /** Tag del área ya interpretado, para las decisiones que dependen de él. */
    public MemoryTag areaTag(String areaUuid) {
        return tagOf(areaUuid);
    }

    /**
     * Catálogo completo para el desplegable: primero los tipos que el área admite y
     * detrás los demás.
     *
     * <p>Los que no tienen peso definido van siempre al final, porque no se pueden
     * elegir en ninguna circunstancia. Listarlos evita que el usuario busque un
     * tipo que el programa contempla y no lo encuentre por ninguna parte.</p>
     */
    public List<DataType> typesForCombo(String areaUuid) {
        List<DataType> combo = new ArrayList<>(typesAllowed(areaUuid));
        for (DataType t : DataType.values()) {
            if (!combo.contains(t) && !t.habilitado()) {
                combo.add(t);
            }
        }
        return combo;
    }

    /**
     * true si el tipo se puede elegir para el área. Un tipo sin peso definido
     * nunca lo es, y uno con peso solo lo es si el área lo admite.
     */
    public boolean typeAllowed(String areaUuid, DataType type) {
        return type != null && type.habilitado()
                && typesAllowed(areaUuid).contains(type);
    }

    /**
     * true si una variable de planta del tipo dado debe listarse para un área
     * fijada a este tipo.
     *
     * <p>La comparación va por la familia que declara la variable y no por el
     * nombre exacto. En la planta INTEGER puede ser short, int o long según lo que
     * imponga el código de área, y comparar "int" con "INTEGER" descartaría todas
     * las variables de un área de int.</p>
     *
     * <p>Un tipo de planta que no se reconoce no descarta nada. Filtrar a ciegas
     * dejaría áreas sin ninguna variable que añadir, que es peor que mostrar de
     * más: es el usuario quien decide al elegir el tipo en el desplegable.</p>
     *
     * <p>El tipo que se pasa es el que ya tiene fijado el área, que mientras esté
     * vacía es null y no descarta nada. En cuanto el área guarde una variable, las
     * de planta que no encajen con ese tipo desaparecen de la lista, que es como se
     * ve que la elección del principio ya no admite vuelta atrás.</p>
     *
     * @param locked tipo ya fijado por el área, o null si todavía no tiene ninguno
     */
    public boolean areaAdmiteTipo(String areaUuid, String plantType, DataType locked) {
        if (locked == null || plantType == null || plantType.isEmpty()) {
            return true;
        }
        // Lo primero es el bloqueo. Si el área ya no admite ese tipo, la variable no
        // entra por mucho que su familia case con el tag: es el área la que manda
        // sobre qué se puede meter, porque es ella la que reparte los offsets.
        if (!typesAllowed(areaUuid).contains(locked)) {
            return false;
        }
        MemoryTag tag = tagOf(areaUuid);
        List<DataType> posibles = tag == null
                ? List.of()
                : DataType.candidatos(plantType, tag.codeBits());
        if (!posibles.isEmpty()) {
            return posibles.contains(locked);
        }
        DataType exacto = DataType.findIgnoreCase(plantType);
        return exacto == null || exacto == locked;
    }

    // --- variables de planta ya asignadas -----------------------------------

    /** Clave de identidad de una variable de planta: ruta dentro del área S88 + nombre. */
    public static String pvKey(String path, String name) {
        return (path == null || path.isEmpty() ? "" : path + "/") + name;
    }

    /** Claves de todas las variables ya guardadas, para no ofrecerlas de nuevo. */
    public Set<String> getUsedPvKeys() {
        Set<String> usedKeys = new HashSet<>();
        for (CommConfigData.PvConfig pv : state.getPvs()) {
            usedKeys.add(pvKey(pv.getS88Path(), pv.getName()));
        }
        return usedKeys;
    }

    // --- validación y persistencia ------------------------------------------

    /** Nombres de las áreas sin grupo de escaneo; vacío si la configuración es válida. */
    public List<String> areasSinGrupoNames() {
        List<String> nombres = new ArrayList<>();
        for (CommConfigData.ItemConfig i : state.areasSinGrupo()) {
            nombres.add(i.getName());
        }
        return nombres;
    }

    public boolean hasAreasWithoutGroup() {
        return !state.areasSinGrupo().isEmpty();
    }

    /**
     * Vuelca la configuración a {@code comunicacion.xml} y a {@code boot.db}.
     *
     * @return true si el XML se guardó y la base quedó actualizada; false si la
     *         base falló, en cuyo caso el XML sí quedó escrito
     */
    public boolean saveAll() {
        if (model == null || state.getSelectedDevice() == null) {
            return false;
        }
        // Copia defensiva: las listas del wizard siguen mutando mientras el
        // usuario edita, y lo que se guarda debe ser el estado de este instante.
        CommConfigData config = new CommConfigData(
                state.getSelectedDevice().getDeviceName(),
                new ArrayList<>(state.getGroups()),
                new ArrayList<>(state.getItems()),
                new ArrayList<>(state.getPvs()));
        model.upsertComms(state.getSelectedDevice().getUuid(), config);
        boolean saved = model.save();
        // Con un guardado parcial se sigue avisando al cerrar, porque el modelo
        // en memoria difiere de la base de datos.
        state.setCambiosSinGuardar(!saved);
        return saved;
    }
}
