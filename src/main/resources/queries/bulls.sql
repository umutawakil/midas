
SELECT 
	m.ticker,
	m.window_delta,
	m.min_delta, 
	m.max_delta,
	m.time_window,
    m.min_price
FROM 
	midas.statistics m
WHERE 
    m.min_price >= 0 AND
    m.max_delta <=25 AND
    m.min_delta >= -25 AND
    m.time_window = 20 
ORDER BY 
	m.window_delta
DESC LIMIT 100;