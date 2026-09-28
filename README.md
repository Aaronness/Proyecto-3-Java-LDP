# Proyecto #3 - Almacén Robótico Concurrente

**Autores:** Aaron Contreras (C.I. 31.871.275), Isaac Perez (C.I. 31.065.844)

---

Este proyecto es una simulación concurrente de un almacén robótico en una cuadrícula de 6x6. Utiliza hilos (_threads_) en Java para representar robots (consumidores) y cintas transportadoras (productores), sincronizados a través de un monitor central para evitar condiciones de carrera y colisiones.

---

## Arquitectura de Concurrencia

Para garantizar la integridad de los datos y cumplir con el paradigma de programación concurrente, el sistema se divide en las siguientes entidades:

### 1. El Monitor (Recurso Crítico)

La clase `Tablero` actúa como el Monitor del sistema. Protege la matriz de 6x6 (`char[][] matriz`) y las variables de estado (como el conteo de cajas y casillas ocupadas).

- Se garantiza la exclusión mutua utilizando métodos declarados como `synchronized` (ej. `insertarCaja`, `intentarMovimiento`).
- La sincronización condicional se logra mediante los métodos `wait()` y `notifyAll()`.

### 2. Los Procesos (Hilos)

- **Robots (Consumidores):** Implementan `Runnable`. Calculan su ruta, intentan moverse a través del monitor y consumen recursos (batería y cajas objetivo).
- **Cintas Transportadoras (Productores):** Implementan `Runnable`. Generan cajas de forma aleatoria y las insertan en el tablero de manera segura utilizando el monitor.
- **Hilo Principal (Main):** Inicializa el sistema, arranca los hilos y utiliza `Thread.join()` para esperar a que todos los robots finalicen antes de imprimir el reporte estadístico.

---

## Lógica de Búsqueda (Algoritmo BFS)

1. El "cerebro" de cada robot es un algoritmo de Búsqueda en Anchura (BFS) adaptado para empujar cajas.
2. **Recálculo constante:** Se decidió ejecutar el BFS en cada movimiento del robot. De esta forma, el robot es dinámico y siempre toma la mejor decisión basada en el estado más actualizado del tablero, reaccionando a cajas nuevas u obstáculos imprevistos.
3. **Exclusividad de Objetivos:** Una vez que el BFS de un robot encuentra una ruta exitosa hacia (5,5) empujando una caja objetivo, el robot "reclama" esa caja (`setTieneRobot(true)`). Ningún otro robot puede interactuar con ella o robarla. Si el robot original encuentra una mejor opción después, libera la caja anterior.

---

## Decisiones de Diseño y Reglas de Negocio

Para asegurar una simulación fluida, equilibrada y sin errores infinitos, se tomaron las siguientes decisiones fundamentales:

1. **Equilibrio de Producción (50/50):** Las cintas transportadoras tienen exactamente un 50% de probabilidad de generar una Caja Objetivo (`'O'`) y un 50% de generar una Caja de Bloqueo (`'X'`).
2. **Ventaja de Velocidad para los Robots:** Los hilos de los robots duermen menos tiempo (`Thread.sleep(300)`) que las cintas transportadoras (`Thread.sleep(600)`). Esto evita que el tablero se sature instantáneamente y da a los robots tiempo para procesar y extraer cajas eficientemente.
3. **Mecanismo de Rendición (Anti-Starvation):** Si un robot ejecuta su BFS durante 25 turnos consecutivos sin encontrar una ruta válida hacia una caja objetivo (ya sea por estar encerrado por cajas de bloqueo o sin cajas disponibles), el robot se rinde, libera su caja y se apaga. Esto libera espacio y evita el uso inútil de la CPU.

---

## Validaciones de Configuración Inicial

1. Si el archivo de entrada indica `0` robots, el programa lanza un error y aborta, ya que la simulación no tiene sentido.
2. Si el archivo tiene `0` transportadoras, el juego sí se ejecuta, siempre y cuando exista al menos una caja objetivo generada en el estado inicial.

---

## Condiciones de Finalización del Juego

1. **Por agotamiento:** El juego termina de forma natural cuando todos los robots se quedan sin batería o se han rendido.
2. **Por Saturación (Deadlock Total):** Si el tablero se llena por completo y las casillas circundantes a la salida (5,5) están bloqueadas impidiendo cualquier extracción futura, el monitor detecta el interbloqueo irrecuperable y finaliza la simulación para evitar que el programa se cuelgue infinitamente.

---

## Prevención de Interbloqueos (Deadlocks) y Colisiones

1. **Colisiones de Robots:** Evitadas gracias a que la validación de la celda de destino y el movimiento per se ocurren dentro de un método `synchronized` en el `Tablero`. Dos robots no pueden ocupar el mismo espacio al mismo tiempo.
2. **Deadlock de Hilos:** Se previene asegurando que todo hilo en estado `wait()` (productores esperando espacio o robots esperando cajas) sea despertado con `notifyAll()` cada vez que el tablero cambia de estado (se mueve un robot, se extrae una caja, o se apaga la fábrica).

---

## Casos de Prueba (Archivos TXT) y Justificación

A continuación, se detalla la configuración y el propósito de cada escenario de prueba incluido en la entrega. Cada archivo `.txt` ha sido diseñado cuidadosamente para estresar y evaluar una arista distinta de la arquitectura del monitor y el ciclo de vida de los hilos:

### 1. `prueba_ideal.txt` (Rendimiento Base y Cooperación)

- **Configuración:** Tablero inicial vacío (0 cajas objetivo, 0 de bloqueo), 3 robots con 50 movimientos de batería y 1 productor.
- **Justificación Técnica:** Evalúa el comportamiento cooperativo clásico. Al arrancar con el almacén vacío, los robots deben entrar obligatoriamente en estado de espera (`wait()`) protegiendo el procesador. Una vez que el productor inyecta entidades, se observa una intercalación natural y fluida de los hilos, consumiendo la batería de los robots progresivamente y permitiendo que el hilo `Main` espere mediante `join()` sin llegar a una saturación temprana.

### 2. `prueba_deadlock.txt` (Prueba de Estrés y Atasco Total)

- **Configuración:** 2 cajas objetivo, 25 cajas de bloqueo, 2 robots (50 de batería) y 5 productores activos.
- **Justificación Técnica:** Diseñado estrictamente para forzar el detector de interbloqueos del Monitor. El tablero inicia con 29 casillas ocupadas (25+2+2). Con 5 productores inyectando cajas rápidamente frente a solo 2 consumidores, la matriz se satura en escasos segundos. Este caso demuestra empíricamente que el sistema no se congela en un bucle infinito; el monitor evalúa las físicas, detecta el "Deadlock" sin salida y ejecuta un apagado de emergencia cooperativo, liberando todos los hilos y retornando el control a la terminal.

### 3. `prueba_failfast.txt` (Validación de Configuración Inicial)

- **Configuración:** 20 cajas objetivo, 15 cajas de bloqueo, 5 robots y 2 productores.
- **Justificación Técnica:** Evalúa el mecanismo de seguridad "Fail-Fast" previo a la concurrencia. La suma total de entidades iniciales (20 + 15 + 5 = 40) excede el tamaño físico de la matriz de 6x6 (36 casillas). Demuestra que el programa lee el archivo en _batch_, calcula la viabilidad y aborta la ejecución con un mensaje de error claro antes de instanciar cualquier hilo, previniendo así un desbordamiento de memoria o un cuelgue inmediato.

### 4. `prueba_bateria.txt` (Muerte Súbita y Retiro de Entidades)

- **Configuración:** 5 cajas objetivo, 2 de bloqueo, 4 robots con batería crítica (3 movimientos) y 1 productor.
- **Justificación Técnica:** Pone a prueba la correcta finalización individual de los consumidores. Al tener solo 3 unidades de energía, los robots agotan su ciclo de vida rápidamente. Se valida que la condición `bateria > 0` rompa el bucle de ejecución de forma segura, obligando a los robots a invocar el método `registrarRobotAgotado()` para borrar su representación (`'R'`) de la matriz, restar las casillas ocupadas y permitir que el programa emita el reporte estadístico final ignorando al productor restante.

### 5. `prueba_sin_productores.txt` (Explosión de Estados y Rendición)

- **Configuración:** 3 cajas objetivo, 6 de bloqueo, 2 robots con 25 de batería y 0 productores.
- **Justificación Técnica:** Demuestra la resiliencia algorítmica y el uso eficiente de estructuras dinámicas (`HashSet`). Al no existir productores que saturen el tablero, quedan numerosos espacios vacíos. El BFS de los robots debe simular miles de universos paralelos evaluando posibles rutas a través del laberinto de 6 cajas de bloqueo. El sistema demuestra que puede procesar esta explosión de estados matemáticos, concluir que el tablero no tiene solución física, y activar el protocolo de rendición (`ATRAPADO`), finalizando los hilos voluntariamente en lugar de iterar infinitamente.

### 6. `prueba_1vs1.txt` (Baseline Concurrente)

- **Configuración:** 0 cajas iniciales, 1 único robot (50 de batería) y 1 productor.
- **Justificación Técnica:** Representa el entorno de prueba de concurrencia más aislado. Al enfrentar a un solo consumidor contra un solo productor, se maximiza la legibilidad de la "Traza de Ejecución" en la consola. Permite auditar visualmente y con total claridad que las operaciones críticas (inserción y movimiento) gozan de estricta exclusión mutua.

---

## Compilación

Para compilar el proyecto (en sistemas basados en Unix o Windows):

```bash
make all
```

De forma manual:

```bash
javac *.java
```

---

## Ejecución

Para ejecutar la simulación, debe proporcionar el nombre del archivo `.txt` como argumento:

```bash
java Main configuracion.txt
```
