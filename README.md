# Pao Collection — sistema de pedidos

## Acceso privado

El panel y todas las API de gestión requieren iniciar sesión. Configura en el
entorno local y en Railway `ADMIN_USERNAME` (por defecto `admin`) y
`ADMIN_PASSWORD` (mínimo 14 caracteres y máximo 72 bytes UTF-8). Usa una
contraseña única generada por un gestor de contraseñas. Sin contraseña no se
crea ningún usuario y el acceso queda cerrado. No guardes estas variables en Git.
En Railway configura también `SESSION_COOKIE_SECURE=true` para HTTPS;
solo en desarrollo local con HTTP puede permanecer en `false`.

Las contraseñas se codifican con BCrypt en memoria al arrancar. Para cambiarlas,
actualiza las variables y redespliega; las sesiones en memoria no sobreviven al
reinicio. Hay cierre de sesión y caducidad tras 30 minutos sin actividad.
Las solicitudes que modifican datos, el login y el logout requieren CSRF.
El login acepta como máximo 10 intentos por minuto por instancia (límite global
para este panel de un administrador); un bloqueo también puede afectar al
administrador durante ese minuto. Para varias réplicas se requiere un límite
centralizado y almacenamiento compartido de sesiones. No hay registro público,
recuperación automática de contraseña ni autenticación de dos factores.

Solo son públicos el login y sus recursos, el endpoint CSRF, las páginas de
privacidad/eliminación y el webhook exacto de Instagram. El POST del webhook
conserva la verificación de firma de Meta; no necesita sesión ni CSRF de navegador.

Antes de desplegar esta versión, guarda las tres variables anteriores en Railway.
Verifica que un visitante sin sesión vea el login y reciba HTTP 401 en las API,
y que el acceso autorizado permita guardar y cerrar sesión.

Spring Boot 3.3.0, Java 18 y Maven; MySQL, OpenAI e Instagram.

## Desarrollo local

Configura `MYSQLPASSWORD` con tu contraseña local antes de iniciar la aplicación.
Los valores predeterminados son host `localhost`, puerto `3306`, base `pedidos_ai`
y usuario `root`. La contraseña no tiene valor predeterminado.
Configura también `OPENAI_API_KEY` y las variables de Instagram que vayas a utilizar.
Usa variables de entorno de tu terminal o configuración privada del IDE: Spring Boot
no carga automáticamente archivos `.env`. No guardes credenciales en archivos versionados.

```powershell
mvn clean install
mvn spring-boot:run
```

`SHOW_SQL=true` habilita SQL para diagnóstico local; por defecto está desactivado.
La URL conserva las opciones de conexión de desarrollo existentes. En Railway,
usa el host privado de MySQL dentro del mismo proyecto, no un endpoint público
sin TLS. Para un proveedor externo que requiera TLS, sustituye la URL completa
mediante `SPRING_DATASOURCE_URL` según sus requisitos.

## Despliegue en Railway Pro

1. En tu workspace Railway Pro, crea un proyecto y agrega un servicio **MySQL**.
2. Agrega un servicio desde el repositorio GitHub existente y selecciona la rama
   que contiene estos cambios. La raíz del servicio debe ser la carpeta con `pom.xml`.
3. Usa **Railpack**: detecta Maven por `pom.xml`, sin Dockerfile ni Procfile.
   Configura `RAILPACK_JDK_VERSION=18` para usar el mismo JDK validado localmente
   (Railpack usa Java 21 por defecto). Configura el comando de build
   `mvn -B clean install` y el comando de inicio
   `java -jar target/sistema-pedidos-ai-0.0.1-SNAPSHOT.jar`.
4. En las variables del servicio Java, agrega las referencias de MySQL siguientes.
   Sustituye `MySQL` por el nombre real del servicio de base de datos:

| Variable | Referencia Railway |
| --- | --- |
| `MYSQLHOST` | `${{MySQL.MYSQLHOST}}` |
| `MYSQLPORT` | `${{MySQL.MYSQLPORT}}` |
| `MYSQLUSER` | `${{MySQL.MYSQLUSER}}` |
| `MYSQLPASSWORD` | `${{MySQL.MYSQLPASSWORD}}` |
| `MYSQLDATABASE` | `${{MySQL.MYSQLDATABASE}}` |

5. Configura en Railway las variables privadas y opciones de las integraciones:

| Variable | Uso |
| --- | --- |
| `OPENAI_API_KEY` | Clave privada de OpenAI, leída por el SDK desde el entorno |
| `OPENAI_MODEL` | Modelo configurado; por defecto `gpt-5.6-luna` |
| `INSTAGRAM_ACCESS_TOKEN` | Token de acceso de la cuenta profesional |
| `INSTAGRAM_USER_ID` | ID de la cuenta profesional |
| `INSTAGRAM_APP_SECRET` | Secreto de la aplicación Meta para validar firmas |
| `INSTAGRAM_VERIFY_TOKEN` | Valor privado elegido para verificar el webhook en Meta |
| `INSTAGRAM_GRAPH_VERSION` | Versión Graph; por defecto `v26.0` |

   Mantén `SHOW_SQL=false`. Railway proporciona `PORT`; la aplicación lo usa
   automáticamente y en local utiliza `8080`.
6. Despliega y genera un dominio público en **Settings → Networking**.
   Puedes conectar después un dominio propio. Comprueba los logs y las páginas públicas.
7. En Meta configura estas URLs, reemplazando el dominio por el definitivo:
   - Webhook: `https://<dominio>/api/instagram/webhook`
   - Privacidad: `https://<dominio>/politica-privacidad.html`
   - Eliminación de datos: `https://<dominio>/eliminacion-datos.html`
   El verify token de Meta debe coincidir con `INSTAGRAM_VERIFY_TOKEN`.

La base Railway es independiente de la local; este procedimiento no migra ni borra
datos locales. Hibernate conserva `ddl-auto=update` y puede actualizar el esquema
de la base seleccionada al iniciar. Las pruebas actuales usan respuestas simuladas
de Instagram y no requieren iniciar la aplicación ni conectar MySQL.

Referencias: [Java en Railpack](https://railpack.com/languages/java/) y
[configuración de builds Railway](https://docs.railway.com/builds/build-configuration).


### Página pública de Pao Collection

La landing está disponible en `/pao-collection.html` (en producción: https://paocollection.up.railway.app/pao-collection.html). Solo esta página y su CSS son públicos; el panel `/` y sus APIs conservan autenticación. El contacto comercial se dirige a Instagram.
