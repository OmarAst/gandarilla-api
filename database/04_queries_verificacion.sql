-- Verificación de residentes activos por casa
SELECT num_casa, COUNT(*) AS residentes_activos
FROM residentes
WHERE activo = TRUE
GROUP BY num_casa
HAVING COUNT(*) > 1;

-- Movimientos registrados con información del residente
SELECT
    mp.id,
    mp.folio,
    r.num_casa,
    r.nombre,
    mp.monto,
    mp.forma_pago,
    mp.fecha_pago,
    mp.mes,
    mp.anio,
    mp.estatus,
    mp.whatsapp_enviado
FROM movimientos_pago mp
JOIN residentes r ON r.id = mp.id_residente
ORDER BY mp.fecha_pago DESC, mp.id DESC;

-- Totales por periodo
SELECT anio, mes, SUM(monto) AS total_pagado, COUNT(*) AS movimientos
FROM movimientos_pago
WHERE estatus = 'REGISTRADO'
GROUP BY anio, mes
ORDER BY anio DESC, mes DESC;
