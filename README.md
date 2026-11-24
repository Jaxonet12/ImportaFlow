# ImportaFlow 3.2

Sistema académico de gestión de envíos y paquetería para el curso de Algoritmos y Estructuras de Datos de la UTP. Desarrollado en Java Swing, con Maven y MySQL.

## Funcionalidades

- Gestión de clientes, recepción de paquetes, seguimiento, cobros y reportes.
- Acceso con roles de administrador, operador y consulta.
- Laboratorio de arreglos, ordenamientos, matrices, listas, pilas, colas, árboles binarios de búsqueda (ABB) y árboles AVL.

La información logística se almacena en MySQL. Las demostraciones académicas trabajan sobre estructuras en memoria; no modifican los registros reales. La cola de prioridad participa en la atención de paquetes.

## Requisitos

- JDK 17 o superior.
- Apache NetBeans con soporte Maven (o Maven desde terminal).
- MySQL Server 8.0.16 o superior; MySQL Workbench es opcional.

## Instalación y ejecución

1. Iniciar MySQL y ejecutar completo `database/IMPORTAFLOW_31_INSTALACION.sql` con una cuenta autorizada para crear bases, tablas y rutinas (CREATE ROUTINE, ALTER ROUTINE y EXECUTE). El script crea `importaflow_final`, sin borrar la base anterior. En una instalación vacía carga 15 clientes ficticios, 20 paquetes pendientes, 20 movimientos y tres usuarios. No reinicia registros existentes.
2. Copiar `db.local.properties.example` como `db.local.properties` en la carpeta que contiene `pom.xml`. Configurar el usuario, contraseña y puerto de MySQL. Este archivo es local y está excluido de Git.
3. Abrir la carpeta del proyecto en NetBeans. Ejecutar **Clean and Build** y luego **Run**. La clase principal es `com.importaflow.Main`. Si NetBeans no la reconoce, seleccionarla en Properties > Run y comprobar que la configuración activa sea Default.

Si ya existe una instalación funcional de ImportaFlow 3.2, solo hay que conservar `db.local.properties` y abrir el proyecto. No es necesario volver a ejecutar el SQL.

También se puede ejecutar desde la terminal con Maven:

```sh
mvn clean package
mvn exec:java
```

### Cuentas académicas

| Usuario | Contraseña inicial de prueba | Rol |
|---|---|---|
| admin | Importa2026! | ADMIN |
| operador | Operador2026! | OPERADOR |
| consulta | Consulta2026! | CONSULTA |

Son cuentas y datos ficticios de práctica. Los correos `.invalid` no son entregables. No utilizar los contactos para comunicarse con personas reales. Las claves de MySQL y las de la aplicación son independientes. Cambiar las claves académicas antes de cualquier uso con datos reales.

## Organización del código

| Paquete | Responsabilidad |
|---|---|
| `modelo` | Entidades del sistema. |
| `dao` | Persistencia JDBC y transacciones. |
| `config` | Conexión a MySQL. |
| `seguridad` | Hash y verificación de contraseñas. |
| `servicio` | TAD, gestión y validaciones. |
| `estructuras` | Nodos, listas, pilas y colas. |
| `algoritmos` | Arreglos, matrices, ordenamientos y árboles. |
| `vista` | Interfaz gráfica y laboratorio AED. |

La correspondencia entre las estructuras y el sílabo se encuentra en `docs/MAPA_SILABO.md`.

## Pruebas y diagnóstico

- `database/VERIFICAR_CARGA.sql`: consultas de solo lectura para revisar la base académica.
- `DiagnosticoBD.java`: diagnóstico de conexión y tablas.
- Las clases `PruebaTemario`, `PruebaEstructuras`, `PruebaFase1` a `PruebaFase4`, `PruebaFinal31` y `PruebaAVL32` contienen comprobaciones ejecutables mediante **Run File** en NetBeans. No son pruebas JUnit; `mvn package` no las ejecuta automáticamente. `PruebaFase4` requiere una pantalla y ejecutarse desde la raíz del proyecto.

El formateo del código se verificó con compilación Java 17 y las ocho clases de pruebas. La ejecución de las transacciones contra MySQL requiere el servidor local.

