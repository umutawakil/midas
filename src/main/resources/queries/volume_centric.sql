SELECT 
	d.ticker,
    d.open_delta,
    d.price,
    100*(s.volume / m.average_volume) AS eod_volume_delta, 
    s.volume,
    m.average_volume
FROM
	delta d
JOIN v_latest_stock_snapshot s ON d.ticker = s.ticker   
JOIN milestone m ON d.ticker = m.ticker
WHERE m.time_window = 40
ORDER BY d.open_delta DESC LIMIT 100;