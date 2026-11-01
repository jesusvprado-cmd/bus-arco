# Bus Arteixo

App Android muy sencilla para saber a qué hora pasa el bus de la Xunta entre Arteixo y A Coruña.
Tiene dos botones grandes (**Ir a Coruña** y **Volver a Arteixo**). Al pulsar uno sale el próximo autobús en grande, cuántos minutos faltan, a qué hora hay que salir de casa y, si el bus lleva GPS, si viene con retraso.

Los datos salen de los mismos servidores que usa la app oficial *Transporte Público de Galicia* (bus.gal). No es una API oficial para el público: si la Xunta la cambia, puede dejar de funcionar.

---

## 1. Compilar el APK (con GitHub, como Teclado TV)

1. Crea un repositorio nuevo en GitHub, por ejemplo `BusArteixo`.
2. Sube **todo el contenido** de esta carpeta (incluida la carpeta oculta `.github`).
3. Pestaña **Actions** → el trabajo *Compilar APK* se lanza solo (o pulsa *Run workflow*).
4. Cuando termine (3-5 min), entra en el trabajo y descarga **BusArteixo-apk** (un .zip con `BusArteixo.apk`).

## 2. Instalar

Pasa `BusArteixo.apk` al móvil (Drive, Telegram, WhatsApp a ti mismo…) y ábrelo. Android pedirá permitir "instalar apps desconocidas".
Las siguientes versiones se instalan **encima** sin desinstalar (va firmada siempre con `app/busarteixo.keystore`; no la borres).

## 3. Configurar (una sola vez)

1. Abre la app y **mantén pulsada la rueda ⚙** de arriba a la derecha unos 2 segundos. Así tu madre no entra en Ajustes sin querer.
2. **Botón 1**: *Parada donde coge el bus* → Elegir → escribe parte del nombre (por ejemplo «Arteixo») → Buscar → toca la parada.
3. *A dónde va*: puede ser una parada concreta (estación, Entrexardíns…) o **el concello entero** (A Coruña). Con el concello salen todos los buses que van a Coruña, y la app indica en qué parada llega cada uno.
4. *Minutos andando de casa a la parada*: con esto la app dice «Sal de casa a las…».
5. Pulsa **⇄ Poner el botón 2 como el 1 al revés** para la vuelta, y ajusta lo que haga falta.
6. **Probar este botón** para ver si salen horarios.
7. Tamaño de letra: Normal, Grande o Muy grande.

## 4. Si algo no funciona

En Ajustes → **Registro de pruebas** → **Prueba completa** → **Copiar todo**, y pega el texto en el chat con Claude. Ahí sale lo que contesta exactamente la Xunta.

## Cómo funciona por dentro

| Qué | Servidor |
|---|---|
| Buscar paradas | `tpgal-ws.xunta.gal/tpgal_ws/rest/busstops/autocomplete` |
| Horario del día entre dos paradas | `…/service/search` |
| Código de la parada para el directo | `…/public/expedition/from` |
| Tiempo real (SIRI) | `sae-its.xunta.gal:8019/SiriHubWs.asmx` |

- Si no hay internet, enseña el último horario que se guardó para ese día.
- El tiempo real se mira cada 45 s mientras la pantalla está abierta. Si no hay directo, pone «Horario previsto».
- Si quedan pocos buses hoy, añade los de mañana.

## Estructura

```
app/src/main/assets/index.html                     toda la pantalla y la lógica (HTML + JavaScript)
app/src/main/java/com/suso/busarteixo/MainActivity.java   puente: internet, guardar ajustes, copiar
app/busarteixo.keystore                             clave de firma (no borrar)
.github/workflows/build.yml                         compila el APK en GitHub
```

Se puede probar `index.html` en el navegador del ordenador, pero allí la Xunta bloquea las peticiones (CORS). Dentro de la app no pasa, porque las hace Java.
