# Matriz de trazabilidad del sílabo: semanas 1 a 18

**Criterio:** las estructuras se implementan en Java con arreglos y nodos propios. MySQL almacena el registro histórico; las estructuras en memoria sirven para los algoritmos y la lógica de atención.

| Semana | Contenido literal/esencial del sílabo | Evidencia concreta en el proyecto | Demostración |
|---|---|---|---|
| 1 | Estructuras estáticas, dinámicas lineales y no lineales | `algoritmos/`, `estructuras/`, `dao/` | Explicar las tres familias y la separación memoria/BD |
| 2 | Arreglo 1D: inserción, actualización, eliminación, recorrido y copia | `ArreglosAED` | Algoritmos > Arreglos, cargar IDs y operar posiciones |
| 3 | Comparación, clonación y fusión de arreglos | `ArreglosAED.comparar/copiar/fusionar` | Clonar + comparar y fusionar arreglos |
| 4 | Matriz triangular inferior, superior, tridiagonal, simétrica/asimétrica, transposición, determinante, inversión | `MatricesAED` | Matriz real 3×3 y su versión diagonalmente dominante para ilustrar inversión |
| 5 | Integración / repaso | Arreglos + matrices | Explicar relación entre arreglos 1D y matrices 2D |
| 6 | Tipo Abstracto de Datos (TAD) | `servicio/TADPedidos.java`, `GestorPedidos.java` | Mostrar interfaz y operaciones sin exponer nodos |
| 7 | Lista simple: nodos, inserción y eliminación | `ListaSimplePedidos.java`, `NodoPedido.java` | Cargar, insertar, eliminar y buscar nodos de la copia |
| 8 | Lista simple: recorrido, búsqueda y ordenamiento | `ListaSimplePedidos.ordenarPorCantidad` y `buscar` | Ordenar nodos por cantidad, buscar ID |
| 9 | Listas dobles y circulares: operaciones | `ListaDoblePedidos` y `ListaCircularTurnos` | Insertar/eliminar, recorrer ida/vuelta y girar turnos |
| 10 | Integración / repaso | Vista de listas | Comparar simple, doble y circular |
| 11 | Pila estática y dinámica (push, pop) | `PilaEstatica` y `PilaAcciones` | Ejecutar LIFO en arreglo y nodos |
| 12 | Cola estática, dinámica y con prioridad (enqueue, dequeue) | `ColaEstatica`, `ColaPedidos`, `ColaPrioridadPedidos` | Comparar FIFO y atención real de urgentes |
| 13 | Árbol binario: nodo y recorridos preorden, inorden, postorden | `ArbolBinarioEnvios` | Ver árbol, niveles, altura y tres recorridos paso a paso |
| 14 | Árbol binario: inserción, búsqueda, eliminación; árbol de expresiones | `ArbolBinarioEnvios`, `ArbolExpresion` | Insertar ID, buscar, borrar del índice y evaluar tarifa |
| 15 | Integración / repaso | Laboratorio AED | Comparar pilas, colas y árboles |
| 16 | ABB: inserción, eliminación y búsqueda | `ArbolBinarioEnvios` | Pestaña ABB S13-16: ruta de búsqueda y cambios en índice |
| 17 | AVL: inserción, rotaciones y borrado con balanceo | `ArbolAVL` | Pestaña AVL S17: FE, LL, RR, LR, RL, borrado y vistas antes/después |
| 18 | Evaluación final | Proyecto integrado | Entrega, no requiere otra estructura |

## Precisión importante para la exposición

El **sílabo adjunto no prescribe un algoritmo de ordenamiento diferente en cada semana**. Para enriquecer la demostración se añadieron Burbuja, Selección e Inserción en `ArreglosAED` y `OrdenamientoVisual` para trazas gráficas por ID, bultos, peso o tarifa; además ordenamiento por inserción de nodos en `ListaSimplePedidos`. Estos son complementos; no presentarlos como asignaciones obligatorias de las semanas 2, 3 o 4.

El **Árbol Binario de Búsqueda (ABB)** y los **árboles AVL** aparecen formalmente en las **semanas 16 y 17**, y ambos se incluyen en la versión 3.2. El ABB no se rebalancea automáticamente; el AVL sí mantiene el factor de equilibrio mediante rotaciones.

### En qué operaciones del negocio se usan realmente

- **Lista simple:** representa los pedidos cargados de MySQL con nodos enlazados.
- **Lista doble:** permite revisión de paquetes en ambos sentidos.
- **Lista circular:** asigna turnos rotativos de encargados, aunque estos turnos se mantienen en memoria.
- **Cola con prioridad:** **interviene efectivamente** en “Atender siguiente”, priorizando URGENTES y respetando FIFO dentro de cada grupo.
- **Pila dinámica:** recopila acciones reales ejecutadas durante la sesión. El laboratorio copia el historial; retirar una anotación de esa copia no modifica el historial original ni MySQL.
- **Arbol binario:** construye un índice temporal por ID para búsquedas y recorridos.
- **Matrices:** resumen estados y prioridad con datos reales. La inversión se muestra con una matriz derivada de esos mismos datos para evitar la singularidad inherente a la columna total.
- **Pilas y colas estáticas, arreglos y ejercicios de árboles:** operaciones académicas sobre estructuras de prueba en memoria.

### Notas de TAD

`TADPedidos` es un contrato de operaciones. Para `atenderSiguiente(usuario)`, el usuario debe tener permiso de edición y, para obtener un resultado, debe existir un pedido pendiente. Postcondición: el pedido prioritario pasa a EN_PROCESO y su movimiento queda registrado en la misma transacción; si no existe ningún pendiente retorna null. La interfaz actualiza después sus datos. El detalle de nodos y SQL queda oculto a quien llama al contrato.

- **AVL:** índice académico de IDs con alturas y rotaciones; no es el índice de MySQL.

### Complejidad orientativa

- `ListaSimplePedidos.buscar`: O(n), al recorrer nodos.
- `ListaDoblePedidos.insertarAlFinal`: O(1), porque conserva `ultimo`.
- `ColaPedidos.enqueue/dequeue`: O(1), conserva `primero`/`ultimo`.
- `PilaAcciones.push/pop`: O(1), opera en la cima.
- `ArbolBinarioEnvios.buscar`: O(h), con h altura; puede llegar a O(n) si degenera.
- `ArreglosAED.burbuja/seleccion/insercion`: O(n²) en el peor caso.
- `ArbolAVL`: búsqueda, inserción y borrado O(log n) en un árbol válido (sin contar capturas didácticas).
