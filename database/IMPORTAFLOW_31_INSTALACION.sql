-- ImportaFlow 3.1 | MySQL 8.0.16+ | Base académica independiente.
-- No elimina ni modifica la base importaflow anterior. No contiene datos reales.
-- Las cuentas son exclusivamente de práctica. Correos .invalid no entregables.
-- Ejecutar completo en Workbench, con una sola sesión de instalación. Requiere CREATE ROUTINE, ALTER ROUTINE y EXECUTE. La carga de datos es atómica.
CREATE DATABASE IF NOT EXISTS importaflow_final CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE importaflow_final;
SET NAMES utf8mb4;
CREATE TABLE IF NOT EXISTS pedidos (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    cliente VARCHAR(120) NOT NULL,
    producto VARCHAR(120) NOT NULL,
    pais_origen VARCHAR(80) NOT NULL,
    cantidad INT NOT NULL,
    prioridad ENUM('NORMAL', 'URGENTE') NOT NULL DEFAULT 'NORMAL',
    estado ENUM('PENDIENTE','EN_PROCESO','EN_TRANSITO','ENTREGADO','CANCELADO') NOT NULL DEFAULT 'PENDIENTE',
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_cantidad_positiva CHECK (cantidad > 0),
    INDEX idx_pedidos_estado_prioridad (estado, prioridad, id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS usuarios (
    id INT AUTO_INCREMENT PRIMARY KEY,
    usuario VARCHAR(45) NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL,
    clave_hash VARCHAR(200) NOT NULL,
    rol ENUM('ADMIN','OPERADOR','CONSULTA') NOT NULL DEFAULT 'OPERADOR',
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS clientes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    documento VARCHAR(20) NOT NULL UNIQUE,
    nombre VARCHAR(120) NOT NULL,
    telefono VARCHAR(30) NOT NULL DEFAULT '',
    correo VARCHAR(120) NOT NULL DEFAULT '',
    direccion VARCHAR(200) NOT NULL DEFAULT '',
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS detalles_envio (
    pedido_id INT NOT NULL PRIMARY KEY,
    codigo VARCHAR(24) NOT NULL UNIQUE,
    cliente_id INT NULL,
    destinatario VARCHAR(120) NOT NULL,
    telefono_destinatario VARCHAR(30) NOT NULL DEFAULT '',
    ciudad_destino VARCHAR(120) NOT NULL,
    direccion_destino VARCHAR(200) NOT NULL DEFAULT '',
    peso_kg DECIMAL(10,2) NOT NULL DEFAULT 1.00,
    tarifa DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    usuario_registro VARCHAR(45) NOT NULL DEFAULT 'MIGRACION',
    fecha_recepcion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_envio_pedido FOREIGN KEY (pedido_id) REFERENCES pedidos(id) ON DELETE CASCADE,
    CONSTRAINT fk_envio_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id) ON DELETE SET NULL,
    CONSTRAINT ck_envio_peso CHECK (peso_kg > 0),
    CONSTRAINT ck_envio_tarifa CHECK (tarifa >= 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS movimientos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    pedido_id INT NOT NULL,
    estado_anterior VARCHAR(25) NULL,
    estado_nuevo VARCHAR(25) NOT NULL,
    observacion VARCHAR(250) NOT NULL DEFAULT '',
    usuario VARCHAR(45) NOT NULL,
    fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_movimiento_pedido FOREIGN KEY (pedido_id) REFERENCES pedidos(id) ON DELETE CASCADE,
    INDEX idx_mov_pedido_fecha (pedido_id, fecha)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS ingresos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    pedido_id INT NOT NULL,
    monto DECIMAL(10,2) NOT NULL,
    metodo ENUM('EFECTIVO','TARJETA','TRANSFERENCIA','YAPE','PLIN') NOT NULL,
    usuario VARCHAR(45) NOT NULL,
    fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ingreso_pedido FOREIGN KEY (pedido_id) REFERENCES pedidos(id) ON DELETE CASCADE,
    CONSTRAINT ck_ingreso_monto CHECK (monto > 0),
    INDEX idx_ingresos_fecha (fecha)
) ENGINE=InnoDB;

DROP PROCEDURE IF EXISTS importaflow_carga_academica_31;
DELIMITER $$
CREATE PROCEDURE importaflow_carga_academica_31()
BEGIN
    DECLARE bloqueo INT DEFAULT 0;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET @carga=-1;
        IF bloqueo=1 THEN DO RELEASE_LOCK('importaflow_final_carga'); END IF;
        RESIGNAL;
    END;
    SELECT GET_LOCK('importaflow_final_carga',10) INTO bloqueo;
    IF bloqueo IS NULL OR bloqueo<>1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Otra instalación está en curso'; END IF;
    START TRANSACTION;
    SET @carga = IF((SELECT COUNT(*) FROM usuarios)+(SELECT COUNT(*) FROM clientes)+(SELECT COUNT(*) FROM pedidos)+(SELECT
        COUNT(*) FROM detalles_envio)+(SELECT COUNT(*) FROM movimientos)+(SELECT COUNT(*) FROM ingresos)=0,1,0);

    INSERT INTO usuarios (id, usuario, nombre, clave_hash, rol, activo)
    SELECT v.id, v.usuario, v.nombre, v.clave_hash, v.rol, v.activo FROM (
        SELECT 1 AS id, 'admin' AS usuario, 'Administrador general' AS nombre,
            'pbkdf2$160000$kKo0OxZgCyT/FGFIpiguag==$YWmF+FRVwXIgHc1J+ZIMNgs0zlv8ECRha9U8ophFEog=' AS clave_hash, 'ADMIN'
            AS rol, 1 AS activo
        UNION ALL
        SELECT 2, 'operador', 'Operador logístico',
            'pbkdf2$160000$Mt4Bip3Lo43AhyzvpY2qew==$9oEOodm2rMX8/KKP0wcgPZiGPwYWiwrsI4nJVZDg5zs=', 'OPERADOR', 1
        UNION ALL
        SELECT 3, 'consulta', 'Usuario de consulta',
            'pbkdf2$160000$HYpNeTJQDJ7Mwa7nMYamNA==$x4dvhQZxDhMjwWA3XXe3P5iQonCmccvH8+GR4ruzLg4=', 'CONSULTA', 1
    ) v WHERE @carga = 1;

    INSERT INTO clientes (id, documento, nombre, telefono, correo, direccion)
    SELECT v.id, v.documento, v.nombre, v.telefono, v.correo, v.direccion FROM (
        SELECT 1 AS id, '99000001' AS documento, 'Camila Ríos Medina' AS nombre, '900000001' AS telefono,
            'cliente01.importaflow@correo.invalid' AS correo, 'Av. Junín 202, Lima' AS direccion
        UNION ALL
        SELECT 2, '99000002', 'Martín Pacheco Luna', '900000002', 'cliente02.importaflow@correo.invalid',
            'Jr. Tacna 418, Lima'
        UNION ALL
        SELECT 3, '20990000003', 'Distribuidora Valle Norte S.A.C.', '900000003', 'cliente03.importaflow@correo.invalid',
            'Av. Los Álamos 805, Lima'
        UNION ALL
        SELECT 4, '99000004', 'Lucía Salazar Rivas', '900000004', 'cliente04.importaflow@correo.invalid',
            'Calle Las Dalias 136, Lima'
        UNION ALL
        SELECT 5, '99000005', 'Renato Flores Vega', '900000005', 'cliente05.importaflow@correo.invalid', 'Jr. Puno 564, Lima'
        UNION ALL
        SELECT 6, '20990000006', 'Comercial Las Palmeras S.A.C.', '900000006', 'cliente06.importaflow@correo.invalid',
            'Av. Brasil 742, Lima'
        UNION ALL
        SELECT 7, '99000007', 'Mariana Soto Peña', '900000007', 'cliente07.importaflow@correo.invalid',
            'Calle Los Cedros 290, Lima'
        UNION ALL
        SELECT 8, '99000008', 'Diego Navarro Cruz', '900000008', 'cliente08.importaflow@correo.invalid',
            'Jr. Ayacucho 315, Lima'
        UNION ALL
        SELECT 9, '20990000009', 'Transportes Costa Clara S.A.C.', '900000009', 'cliente09.importaflow@correo.invalid',
            'Av. Colonial 1280, Lima'
        UNION ALL
        SELECT 10, '99000010', 'Paola Herrera Gil', '900000010', 'cliente10.importaflow@correo.invalid',
            'Calle Las Flores 602, Lima'
        UNION ALL
        SELECT 11, '99000011', 'Bruno Chávez Reyes', '900000011', 'cliente11.importaflow@correo.invalid',
            'Jr. Huánuco 180, Lima'
        UNION ALL
        SELECT 12, '20990000012', 'Soluciones Sierra Azul S.A.C.', '900000012', 'cliente12.importaflow@correo.invalid',
            'Av. Arequipa 1735, Lima'
        UNION ALL
        SELECT 13, '99000013', 'Adriana León Díaz', '900000013', 'cliente13.importaflow@correo.invalid',
            'Calle Los Laureles 464, Lima'
        UNION ALL
        SELECT 14, '99000014', 'Javier Montes Ruiz', '900000014', 'cliente14.importaflow@correo.invalid',
            'Jr. Cuzco 330, Lima'
        UNION ALL
        SELECT 15, '20990000015', 'Inversiones Puerto Sur S.A.C.', '900000015', 'cliente15.importaflow@correo.invalid',
            'Av. Grau 920, Lima'
    ) v WHERE @carga = 1;

    INSERT INTO pedidos (id, cliente, producto, pais_origen, cantidad, prioridad, estado)
    SELECT v.id, v.cliente, v.producto, v.pais_origen, v.cantidad, v.prioridad, v.estado FROM (
        SELECT 1 AS id, 'Camila Ríos Medina' AS cliente, 'Accesorios de cómputo' AS producto, 'Lima, Perú' AS pais_origen,
            5 AS cantidad, 'NORMAL' AS prioridad, 'PENDIENTE' AS estado
        UNION ALL
        SELECT 2, 'Martín Pacheco Luna', 'Documentos comerciales', 'Lima, Perú', 1, 'URGENTE', 'PENDIENTE'
        UNION ALL
        SELECT 3, 'Distribuidora Valle Norte S.A.C.', 'Calzado deportivo', 'Lima, Perú', 3, 'NORMAL', 'PENDIENTE'
        UNION ALL
        SELECT 4, 'Lucía Salazar Rivas', 'Material de oficina', 'Lima, Perú', 2, 'NORMAL', 'PENDIENTE'
        UNION ALL
        SELECT 5, 'Renato Flores Vega', 'Repuestos de maquinaria', 'Lima, Perú', 7, 'URGENTE', 'PENDIENTE'
        UNION ALL
        SELECT 6, 'Comercial Las Palmeras S.A.C.', 'Libros y cuadernos', 'Lima, Perú', 4, 'NORMAL', 'PENDIENTE'
        UNION ALL
        SELECT 7, 'Mariana Soto Peña', 'Juguetes educativos', 'Lima, Perú', 6, 'NORMAL', 'PENDIENTE'
        UNION ALL
        SELECT 8, 'Diego Navarro Cruz', 'Muestras textiles', 'Lima, Perú', 2, 'URGENTE', 'PENDIENTE'
        UNION ALL
        SELECT 9, 'Transportes Costa Clara S.A.C.', 'Herramientas manuales', 'Lima, Perú', 8, 'NORMAL', 'PENDIENTE'
        UNION ALL
        SELECT 10, 'Paola Herrera Gil', 'Accesorios de telefonía', 'Lima, Perú', 1, 'URGENTE', 'PENDIENTE'
        UNION ALL
        SELECT 11, 'Bruno Chávez Reyes', 'Cerámica decorativa', 'Lima, Perú', 4, 'NORMAL', 'PENDIENTE'
        UNION ALL
        SELECT 12, 'Soluciones Sierra Azul S.A.C.', 'Material publicitario', 'Lima, Perú', 9, 'NORMAL', 'PENDIENTE'
        UNION ALL
        SELECT 13, 'Adriana León Díaz', 'Prendas de vestir', 'Lima, Perú', 3, 'URGENTE', 'PENDIENTE'
        UNION ALL
        SELECT 14, 'Javier Montes Ruiz', 'Artículos de ferretería', 'Lima, Perú', 6, 'NORMAL', 'PENDIENTE'
        UNION ALL
        SELECT 15, 'Inversiones Puerto Sur S.A.C.', 'Equipos de iluminación', 'Lima, Perú', 2, 'NORMAL', 'PENDIENTE'
        UNION ALL
        SELECT 16, 'Camila Ríos Medina', 'Archivos contables', 'Lima, Perú', 1, 'URGENTE', 'PENDIENTE'
        UNION ALL
        SELECT 17, 'Martín Pacheco Luna', 'Utensilios de cocina', 'Lima, Perú', 5, 'NORMAL', 'PENDIENTE'
        UNION ALL
        SELECT 18, 'Distribuidora Valle Norte S.A.C.', 'Componentes electrónicos', 'Lima, Perú', 7, 'NORMAL', 'PENDIENTE'
        UNION ALL
        SELECT 19, 'Lucía Salazar Rivas', 'Material escolar', 'Lima, Perú', 3, 'URGENTE', 'PENDIENTE'
        UNION ALL
        SELECT 20, 'Renato Flores Vega', 'Accesorios para mascotas', 'Lima, Perú', 4, 'NORMAL', 'PENDIENTE'
    ) v WHERE @carga = 1;

    INSERT INTO detalles_envio (pedido_id, codigo, cliente_id, destinatario, telefono_destinatario, ciudad_destino,
        direccion_destino, peso_kg, tarifa, usuario_registro)
    SELECT v.pedido_id, v.codigo, v.cliente_id, v.destinatario, v.telefono_destinatario, v.ciudad_destino,
        v.direccion_destino, v.peso_kg, v.tarifa, v.usuario_registro FROM (
        SELECT 1 AS pedido_id, 'IF-00000001' AS codigo, 1 AS cliente_id, 'Elena Vargas León' AS destinatario, '900001001'
            AS telefono_destinatario, 'Trujillo, Perú' AS ciudad_destino, 'Jr. Puno 564' AS direccion_destino, 8.2 AS
            peso_kg, 68 AS tarifa, 'operador' AS usuario_registro
        UNION ALL
        SELECT 2, 'IF-00000002', 2, 'Sergio Vera Ríos', '900001002', 'Arequipa, Perú', 'Av. Brasil 742', 0.5, 32, 'operador'
        UNION ALL
        SELECT 3, 'IF-00000003', 3, 'Gabriela Ramos Gil', '900001003', 'Chiclayo, Perú', 'Calle Los Cedros 290', 5.5, 42,
            'operador'
        UNION ALL
        SELECT 4, 'IF-00000004', 4, 'Andrés Rojas Soto', '900001004', 'Cusco, Perú', 'Jr. Ayacucho 315', 3.4, 38, 'operador'
        UNION ALL
        SELECT 5, 'IF-00000005', 5, 'Patricia Núñez Cruz', '900001005', 'Piura, Perú', 'Av. Colonial 1280', 14, 110,
            'operador'
        UNION ALL
        SELECT 6, 'IF-00000006', 6, 'Nicolás Díaz Silva', '900001006', 'Huancayo, Perú', 'Calle Las Flores 602', 7, 48,
            'operador'
        UNION ALL
        SELECT 7, 'IF-00000007', 7, 'Mónica Vega Paz', '900001007', 'Ica, Perú', 'Jr. Huánuco 180', 10, 60, 'operador'
        UNION ALL
        SELECT 8, 'IF-00000008', 8, 'Daniel Torres Mora', '900001008', 'Tacna, Perú', 'Av. Arequipa 1735', 1.3, 36,
            'operador'
        UNION ALL
        SELECT 9, 'IF-00000009', 9, 'Claudia Luna Ruiz', '900001009', 'Chimbote, Perú', 'Calle Los Laureles 464', 15.6,
            125, 'operador'
        UNION ALL
        SELECT 10, 'IF-00000010', 10, 'Hugo Flores Lara', '900001010', 'Puno, Perú', 'Jr. Cuzco 330', 0.8, 28, 'operador'
        UNION ALL
        SELECT 11, 'IF-00000011', 11, 'Isabel Peña Rivas', '900001011', 'Cajamarca, Perú', 'Av. Grau 920', 6.4, 52,
            'operador'
        UNION ALL
        SELECT 12, 'IF-00000012', 12, 'Luis Medina Rey', '900001012', 'Tarapoto, Perú', 'Av. Junín 202', 18.5, 140,
            'operador'
        UNION ALL
        SELECT 13, 'IF-00000013', 13, 'Rocío Navarro León', '900001013', 'Moquegua, Perú', 'Jr. Tacna 418', 4.2, 39,
            'operador'
        UNION ALL
        SELECT 14, 'IF-00000014', 14, 'Marco Reyes Gil', '900001014', 'Huaraz, Perú', 'Av. Los Álamos 805', 11.3, 88,
            'operador'
        UNION ALL
        SELECT 15, 'IF-00000015', 15, 'Andrea Paredes Soto', '900001015', 'Tumbes, Perú', 'Calle Las Dalias 136', 2.8, 45,
            'operador'
        UNION ALL
        SELECT 16, 'IF-00000016', 1, 'César Salas Vega', '900001016', 'Ayacucho, Perú', 'Jr. Puno 564', 0.9, 26, 'operador'
        UNION ALL
        SELECT 17, 'IF-00000017', 2, 'Natalia Ramos Cruz', '900001017', 'Juliaca, Perú', 'Av. Brasil 742', 9.7, 75,
            'operador'
        UNION ALL
        SELECT 18, 'IF-00000018', 3, 'Pablo Ríos Luna', '900001018', 'Huánuco, Perú', 'Calle Los Cedros 290', 12.5, 99,
            'operador'
        UNION ALL
        SELECT 19, 'IF-00000019', 4, 'Sofía Mendoza Díaz', '900001019', 'Pucallpa, Perú', 'Jr. Ayacucho 315', 3.2, 35,
            'operador'
        UNION ALL
        SELECT 20, 'IF-00000020', 5, 'David Castro Mora', '900001020', 'Abancay, Perú', 'Av. Colonial 1280', 5.8, 49,
            'operador'
    ) v WHERE @carga = 1;

    INSERT INTO movimientos (pedido_id, estado_anterior, estado_nuevo, observacion, usuario)
    SELECT v.pedido_id, v.estado_anterior, v.estado_nuevo, v.observacion, v.usuario FROM (
        SELECT 1 AS pedido_id, NULL AS estado_anterior, 'PENDIENTE' AS estado_nuevo, 'Recepción inicial del paquete' AS
            observacion, 'operador' AS usuario
        UNION ALL
        SELECT 2, NULL, 'PENDIENTE', 'Recepción inicial del paquete', 'operador'
        UNION ALL
        SELECT 3, NULL, 'PENDIENTE', 'Recepción inicial del paquete', 'operador'
        UNION ALL
        SELECT 4, NULL, 'PENDIENTE', 'Recepción inicial del paquete', 'operador'
        UNION ALL
        SELECT 5, NULL, 'PENDIENTE', 'Recepción inicial del paquete', 'operador'
        UNION ALL
        SELECT 6, NULL, 'PENDIENTE', 'Recepción inicial del paquete', 'operador'
        UNION ALL
        SELECT 7, NULL, 'PENDIENTE', 'Recepción inicial del paquete', 'operador'
        UNION ALL
        SELECT 8, NULL, 'PENDIENTE', 'Recepción inicial del paquete', 'operador'
        UNION ALL
        SELECT 9, NULL, 'PENDIENTE', 'Recepción inicial del paquete', 'operador'
        UNION ALL
        SELECT 10, NULL, 'PENDIENTE', 'Recepción inicial del paquete', 'operador'
        UNION ALL
        SELECT 11, NULL, 'PENDIENTE', 'Recepción inicial del paquete', 'operador'
        UNION ALL
        SELECT 12, NULL, 'PENDIENTE', 'Recepción inicial del paquete', 'operador'
        UNION ALL
        SELECT 13, NULL, 'PENDIENTE', 'Recepción inicial del paquete', 'operador'
        UNION ALL
        SELECT 14, NULL, 'PENDIENTE', 'Recepción inicial del paquete', 'operador'
        UNION ALL
        SELECT 15, NULL, 'PENDIENTE', 'Recepción inicial del paquete', 'operador'
        UNION ALL
        SELECT 16, NULL, 'PENDIENTE', 'Recepción inicial del paquete', 'operador'
        UNION ALL
        SELECT 17, NULL, 'PENDIENTE', 'Recepción inicial del paquete', 'operador'
        UNION ALL
        SELECT 18, NULL, 'PENDIENTE', 'Recepción inicial del paquete', 'operador'
        UNION ALL
        SELECT 19, NULL, 'PENDIENTE', 'Recepción inicial del paquete', 'operador'
        UNION ALL
        SELECT 20, NULL, 'PENDIENTE', 'Recepción inicial del paquete', 'operador'
    ) v WHERE @carga = 1;

    -- Si existe una instalación anterior incompleta, no completar ni borrar a ciegas.
    IF EXISTS(SELECT 1 FROM pedidos p LEFT JOIN detalles_envio d ON p.id=d.pedido_id WHERE d.pedido_id IS NULL)
       OR EXISTS(SELECT 1 FROM pedidos p WHERE NOT EXISTS(SELECT 1 FROM movimientos m WHERE m.pedido_id=p.id))
       OR NOT EXISTS(SELECT 1 FROM usuarios WHERE rol='ADMIN' AND activo=1) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Instalación incompleta: revisar respaldo, detalles, movimientos y administrador';
    END IF;
    COMMIT;
    DO RELEASE_LOCK('importaflow_final_carga');
END$$
DELIMITER ;
CALL importaflow_carga_academica_31();
DROP PROCEDURE importaflow_carga_academica_31;
SELECT CASE @carga WHEN 1 THEN 'Carga inicial aplicada' WHEN 0 THEN
    'Base no vacía: no se añadieron datos ni se reiniciaron estados' ELSE 'Carga fallida: revisar error y rollback' END AS
    resultado;
SELECT COUNT(*) AS clientes FROM clientes;
SELECT estado, prioridad, COUNT(*) AS paquetes FROM pedidos GROUP BY estado, prioridad;
SELECT COUNT(*) AS movimientos FROM movimientos;
SELECT COUNT(*) AS cobros FROM ingresos;
SELECT usuario,rol,activo FROM usuarios ORDER BY id;
