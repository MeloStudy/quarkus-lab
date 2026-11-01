# Reglas para `labs/` (camino guiado)

Aplican a todo el trabajo dentro de esta carpeta. No tocar `00-*`, `01-*`, `02-*` ni `03-microservices` salvo petición expresa.

## Contexto
- El usuario es desarrollador que usará Quarkus en un trabajo nuevo. Objetivo: aprender de forma práctica, con versiones actuales.
- Repite temas ya vistos a propósito: es una revisión actualizada, no se salta nada.
- Idioma: español; términos técnicos en inglés.
- Stack de referencia: Quarkus 3.39.5, Java 21, Maven wrapper (`./mvnw`).

## Estructura de cada lab
```
labs/lab-NN-<tema>/
├── README.md     (basado en labs/_template/README.md)
├── starter/      proyecto Maven que compila, con TODOs y tests que fallan
└── solution/     versión completa, tests en verde
```
- Labs cortos: 1-2 horas.
- Los tests son el criterio de aceptación. `starter` debe compilar y fallar en los tests objetivo; `solution` debe pasar `./mvnw verify`.
- Verificar siempre que `solution` pasa antes de dar el lab por terminado.
- La carpeta `solution/` es visible a propósito: el usuario decide cuándo mirarla.

## Al construir un lab
1. Construir **un lab a la vez**, no varios por adelantado.
2. Consultar la documentación de la versión actual (`https://quarkus.io/version/3.39/guides/`) y enlazar las guías exactas; no escribir de memoria APIs que hayan cambiado.
3. Preferir las extensiones y APIs vigentes (Quarkus REST, no RESTEasy Classic).
4. Actualizar el estado en `SYLLABUS.md`.
5. Un commit por lab: `feat(lab_N): <descripción>`.

## Al ayudar con un lab
- Ante una duda, dar primero una pista o una pregunta; el código completo solo si lo pide.
- Explicar el porqué antes del cómo.
- Al terminar, ofrecer revisión de código y una retrospectiva breve (qué entendió, qué reforzar), y usarla para ajustar el siguiente lab.
