-- Solo lectura. Valores iniciales: 15 clientes; 20 pedidos; 20 detalles;
-- 20 movimientos; 0 ingresos; 3 usuarios. No reinicia la carga.
USE importaflow_final;
SELECT 'clientes' tabla,COUNT(*) total FROM clientes
UNION ALL SELECT 'pedidos',COUNT(*) FROM pedidos
UNION ALL SELECT 'detalles_envio',COUNT(*) FROM detalles_envio
UNION ALL SELECT 'movimientos',COUNT(*) FROM movimientos
UNION ALL SELECT 'ingresos',COUNT(*) FROM ingresos
UNION ALL SELECT 'usuarios',COUNT(*) FROM usuarios;
SELECT estado,prioridad,COUNT(*) cantidad FROM pedidos GROUP BY estado,prioridad;
SELECT p.id,p.prioridad,p.estado,d.codigo,d.tarifa FROM pedidos p JOIN detalles_envio d ON d.pedido_id=p.id ORDER BY p.id;
SELECT usuario,rol,activo FROM usuarios ORDER BY id;
SELECT p.id FROM pedidos p LEFT JOIN detalles_envio d ON d.pedido_id=p.id WHERE d.pedido_id IS NULL;

-- Incidencias de integridad: deben devolver cero filas en una BD consistente.
SELECT p.id,p.estado FROM pedidos p WHERE NOT EXISTS(SELECT 1 FROM movimientos m WHERE m.pedido_id=p.id)
   OR p.estado<>(SELECT m.estado_nuevo FROM movimientos m WHERE m.pedido_id=p.id ORDER BY m.id DESC LIMIT 1);
SELECT d.pedido_id,d.tarifa,COALESCE(SUM(i.monto),0) abonado FROM detalles_envio d LEFT JOIN ingresos i ON
    i.pedido_id=d.pedido_id GROUP BY d.pedido_id,d.tarifa HAVING abonado>d.tarifa;
