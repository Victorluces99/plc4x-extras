# Validacion manual pendiente en el IDE

Estos puntos no los cubre el build: dependen del runtime de NetBeans. Estan
pendientes de comprobacion manual antes de dar por cerrada la Fase 4.

## T4.2 - consolidation de las factories de categoria

Refactor aplicado: ocho NodeFactory de categoria, cada una con su copia de
unos 195 lineas, ahora extienden CategoryFilesNodeFactory y ocupan 34-40
lineas cada una. Lo que decide que se muestra ya no vive en ellas, sino en
HMICategoryDefinition (T4.1), que esta probado.

El gate comprueba que cada clase apunta a su categoria, conserva su posicion en
el arbol y que la que anade accion propia sigue anadiendola. Lo que **no**
comprueba es lo siguiente:

- [ ] El arbol del proyecto se ve igual que antes: mismos nodos, mismo orden.
- [ ] Los nodos se refrescan al crear, renombrar y borrar un fichero dentro de
      la carpeta de una categoria.
- [ ] El fichero base (ase.rcp, ase.wmg, ...) sigue sin aparecer.
- [ ] Las carpetas que no son de una categoria siguen mostrando una lista vacia.
- [ ] Editar un dispositivo y cambiar de modelo sigue limpiando la conexion.
- [ ] Las acciones propias (Graficos, Usuarios runtime, Configuracion de panel)
      siguen apareciendo en el menu contextual de sus ficheros.

## Por que no se automatiza

@NodeFactory.Registration la procesa NetBeans en tiempo de compilacion y no
queda en el class con retencion RUNTIME, asi que no se puede leer por
reflexion desde un test. Y el comportamiento del arbol solo se observa con el
IDE abierto. Las posiciones se pueden comprobar porque viven en
CategoryNodePositions, que si es codigo normal: ahi esta la unica copia y la
anotacion la referencia.
