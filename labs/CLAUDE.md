# Reglas para `labs/` (camino guiado)

Aplican a todo el trabajo dentro de esta carpeta. No tocar `00-*`, `01-*`, `02-*` ni `03-microservices` salvo petición expresa.

## Contexto
- El usuario es desarrollador que usará Quarkus en un trabajo nuevo. Objetivo: aprender de forma práctica, con versiones actuales.
- Repite temas ya vistos a propósito: es una revisión actualizada, no se salta nada.
- Idioma: español; términos técnicos en inglés.
- Stack de referencia: Quarkus 3.40.1.1 (LTS, soporte hasta 30-09-2027), Java 21, Maven wrapper (`./mvnw`).

## Estructura de cada lab
Cada lab es de uno de dos tipos (indicarlo en su README):

**Tipo A - código verificable por tests (starter + solution)**
```
labs/lab-NN-<tema>/
├── README.md     (basado en labs/_template/README.md)
├── starter/      proyecto Maven que compila, con TODOs y tests que fallan
└── solution/     versión completa, tests en verde
```
- Los tests son el criterio de aceptación. `starter` debe compilar y fallar en los tests objetivo; `solution` debe pasar `mvn verify`.
- Verificar siempre que `solution` pasa y que `starter` falla donde debe antes de dar el lab por terminado.
- Los tests no deben depender de textos arbitrarios atados a la versión; deben validar comportamiento.

**Tipo B - guía paso a paso (temas operativos/exploratorios)**
- Un solo proyecto (`demo/`) y un README con comandos, "qué deberías ver" y puntos de comprobación. Sin `solution/` separada.
- Verificar ejecutando de verdad todos los comandos de la guía.

**Comunes**
- Labs cortos: 1-2 horas.
- Quitar del proyecto generado lo que no aporta al tema (Dockerfiles, README autogenerado, etc.).
- En Git Bash `./mvnw` puede fallar por `JAVA_HOME`: verificar con `mvn` del sistema o `.\mvnw.cmd` en PowerShell, e indicar ambos en el README.
- La carpeta `solution/` es visible a propósito: el usuario decide cuándo mirarla.

## Estándar de calidad (criterio propio, no preguntar al usuario)
Yo, Claude, decido y valido el enfoque de cada lab; no se lo devuelvo al usuario como pregunta. Un lab no está terminado hasta cumplir:
1. **Nivel laboral:** cubre lo que se espera en un proyecto real y en una entrevista, no solo el mecanismo. Incluye cómo corre fuera del dev mode (jar/contenedor) cuando aplique, los errores típicos y qué haría un equipo en producción.
2. **Trampas explícitas:** al menos una situación donde lo intuitivo falla (precedencia, build-time vs runtime, prefijos, orden de arranque...), descubierta por el alumno y explicada después.
3. **Lo no obvio se verifica ejecutando**, no se afirma de memoria: toda respuesta, mensaje de error o comportamiento citado en el README debe haberse comprobado con la versión del proyecto.
4. **Predecir antes de ejecutar:** las exploraciones piden anotar una predicción antes de ver el resultado.
5. **Directo y corto:** 1-2 h; si crece más, separar el tema en dos labs en vez de inflarlo. Lo opcional va en "Reto extra".
6. **Sección "En proyectos reales"** (criterio profesional, seguridad, operación) y **"Autoevaluación"** (5 preguntas a nivel entrevista) en cada README.
7. **Comandos probados** en PowerShell (`.\mvnw.cmd`); en PowerShell los argumentos `-Dx.y=z` van entre comillas.
8. Tras cualquier cambio, volver a ejecutar `solution` (verde) y `starter` (rojo donde debe).

## Al construir un lab
1. Construir **un lab a la vez**, no varios por adelantado.
2. Consultar la documentación de la versión actual (`https://quarkus.io/guides/` (última versión; contrastar con la 3.40.1.1 del proyecto si hay dudas)) y enlazar las guías exactas; no escribir de memoria APIs que hayan cambiado.
3. Preferir las extensiones y APIs vigentes (Quarkus REST, no RESTEasy Classic).
4. Actualizar el estado en `SYLLABUS.md`.
5. Un commit por lab: `feat(lab_N): <descripción>`.

## Al ayudar con un lab
- Ante una duda, dar primero una pista o una pregunta; el código completo solo si lo pide.
- Explicar el porqué antes del cómo.
- Al terminar, ofrecer revisión de código y una retrospectiva breve (qué entendió, qué reforzar), y usarla para ajustar el siguiente lab.
