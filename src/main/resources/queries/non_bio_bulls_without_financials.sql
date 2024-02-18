
SELECT 
	m.ticker,
	m.window_delta,
    m.volume_delta,
	m.min_delta, 
	m.max_delta,
    m.average_deviation,
	m.time_window,
    m.min_price,
    m.average_volume*current_price AS daily_market_cap,
    m.current_price,
    t.sec_sector_code
FROM 
	midas.statistics m 
	JOIN ticker_info t ON m.ticker = t.ticker 
WHERE 
    m.max_delta <=25 AND
    m.min_delta >= -25 AND
    m.time_window = 20 AND 
    (
		(t.sec_sector_code != "283" AND
		t.sec_sector_code NOT like "38%" AND 
		t.sec_sector_code NOT like "80%") AND
		t.otc = 0  
	)
ORDER BY 
	m.window_delta
DESC LIMIT 100;