# Plan de publicacion para RankGA

## Objetivo

Llevar este repositorio a una publicacion seria en una revista de alto nivel,
no solo a una demostracion de codigo que "funciona". La meta debe ser un
articulo con una tesis unica, evidencia experimental fuerte y reproducibilidad
completa.

## Diagnostico actual

- El repositorio contiene un motor RankGA con varias codificaciones y varios
  problemas de prueba.
- Hay material util para experimentos y redaccion, pero aun no hay un paquete
  listo para someter.
- `revisionAlgGenE.tex` es una nota de revision de capitulo, no el manuscrito
  final para revista.
- Las corridas y los logs actuales son valiosos como trazas internas, pero no
  bastan como evidencia cientifica final.

## Tesis publicable recomendada

La version publicable debe centrarse en una sola idea fuerte:

**RankGA es un esquema de control por rango para seleccion, cruzamiento y
mutacion que ajusta la presion evolutiva de forma sistematica.**

Para una revista exigente, no conviene venderlo como "un GA para todo".
Conviene venderlo como:

- un marco general con una regla clara de asignacion de presion por rango,
- una comparacion experimental rigurosa contra baselines serios,
- y, si se conserva la parte adaptativa, una extension claramente separada del
  nucleo principal.

## Decision estrategica

Hay dos caminos posibles:

1. **Paper metodologico general**: RankGA como framework, evaluado en una
   familia reducida de problemas representativos.
2. **Paper por problema**: una variante RankGA especializada en un solo dominio
   con evidencia muy fuerte.

Para una revista de alto nivel, la opcion mas defendible es la 1. La opcion 2
solo conviene si la mejora en un problema concreto es espectacular y muy
solida.

## Riesgos cientificos que hay que corregir antes de someter

- Reproducibilidad debil: hoy no hay semilla global configurable.
- Semantica inconsistente de `mutate(double)`: en algunos genes funciona como
  probabilidad y en otros como intensidad de paso o desviacion estandar.
- Criterio de paro basado en tiempo: para publicar, el presupuesto por
  evaluaciones debe ser el eje principal.
- Comparacion incompleta: faltan baselines fuertes y una bateria estatistica
  formal.
- Mezcla de representaciones: no todo puede compararse como si tuviera el mismo
  significado de mutacion.
- Adaptacion en tiempo de ejecucion: si se mantiene, debe aislarse como
  contribucion separada y evaluarse con cuidado.

## Plan de trabajo por fases

### Fase 1. Congelar la definicion del metodo

- Definir exactamente el algoritmo que va al paper.
- Separar con claridad:
  - seleccion por rango,
  - recombinacion,
  - mutacion,
  - criterio de paro,
  - y adaptacion, si se conserva.
- Introducir una semilla configurable para reproducibilidad.
- Hacer explicita la semantica de la mutacion por tipo de codificacion.
- Eliminar ambiguedades entre "intensidad", "probabilidad" y "desviacion".

### Fase 2. Construir una version experimental limpia

- Aislar una version "paper-ready" del codigo.
- Registrar por corrida:
  - instancia,
  - semilla,
  - algoritmo,
  - numero de evaluaciones,
  - mejor fitness,
  - tiempo,
  - y criterio de termino.
- Exportar resultados en formato estructurado, no solo en txt libres.
- Mantener los logs txt como trazabilidad, pero no como formato principal.

### Fase 3. Definir la bateria de prueba

- Elegir una familia principal de problemas y, si acaso, 1 o 2 familias
  secundarias.
- No mezclar demasiadas representaciones en el mismo claim principal.
- Usar instancias publicas o reconstruibles.
- Fijar tamano de poblacion, presupuesto de evaluaciones y criterio de paro.
- Ejecutar cada algoritmo al menos 100 veces por instancia, como minimo.

### Fase 4. Baselines serios

Comparar contra:

- GA simple.
- GA elitista.
- RankGA sin la parte adaptativa.
- RankGA con mutacion plana.
- Un baseline aleatorio o de busqueda local, si es pertinente.
- Si existe literatura fuerte para el problema elegido, incluir el mejor
  baseline clasico razonable.

### Fase 5. Ablation studies

La revista no debe recibir solo resultados finales. Debe quedar claro que cada
pieza aporta algo.

Probar, al menos:

- sin seleccion por rango,
- sin programacion de intensidad por rango,
- sin adaptacion,
- sin recombinacion adyacente,
- con otro criterio de paro,
- con otra estrategia de emparejamiento.

### Fase 6. Analisis estadistico

- Reportar mediana, media, desviacion, cuartiles y tasa de exito.
- No depender solo de "mejor valor observado".
- Usar pruebas no parametrizadas cuando corresponda.
- Incluir tamano de efecto, no solo p-values.
- Separar claramente resultados por instancia y resumen agregado.

### Fase 7. Redaccion del articulo

Estructura minima recomendada:

1. Introduccion.
2. Tesis y contribucion.
3. Metodo RankGA.
4. Implementacion y protocolos.
5. Experimentos.
6. Ablation y discusion.
7. Limitaciones.
8. Conclusiones.

La discusion debe ser critica. Si RankGA pierde contra un baseline en algun
caso, eso se reporta. Ocultarlo debilita la credibilidad del trabajo.

### Fase 8. Paquete de reproducibilidad

Antes de someter, dejar listo:

- codigo congelado por version,
- configuraciones de corrida,
- seeds,
- datos de entrada,
- scripts de graficacion,
- tablas finales,
- y un archivo de reproducibilidad con instrucciones exactas.

## Criterios de aceptacion interna

No enviar a revista hasta que se cumpla todo esto:

- El claim principal cabe en una frase y se puede defender con datos.
- El algoritmo se puede correr con una semilla fija y repetir el resultado.
- Los baselines estan implementados y documentados.
- Las figuras y tablas se regeneran desde scripts.
- La semantica de mutacion no depende de interpretaciones ambiguas.
- Hay al menos una conclusion que sea falsable y medible.

## Riesgo editorial principal

El mayor riesgo no es tecnico, sino narrativo: que el trabajo parezca una
coleccion de variantes aplicadas a muchos problemas sin una contribucion
central clara.

Para una revista de alto nivel, el manuscrito debe responder sin rodeos:

**Que aporta RankGA que no exista ya, en que condiciones funciona mejor, y en
que condiciones no conviene usarlo.**

## Recomendacion final

Si el objetivo es realmente "publicarlo en serio", la prioridad no es agregar
mas problemas al repositorio. La prioridad es:

1. fijar una tesis unica,
2. limpiar la metodologia,
3. blindar la reproducibilidad,
4. y construir evidencia comparativa fuerte.

Solo despues de eso conviene escribir y someter el articulo.
