
SELECT 
	m.ticker,
	m.window_delta,
    m.volume_delta,
	m.min_delta, 
	m.max_delta,
	m.time_window,
    m.min_price,
    f.net_income,
    (100*(m.average_volume*current_price))/f.revenue AS daily_market_cap_multiple,
    m.current_price,
    t.sec_sector_code
FROM 
	midas.statistics m 
    LEFT JOIN financials f ON m.ticker = f.ticker
    LEFT JOIN ticker_info t ON f.ticker = t.ticker 
WHERE 
    m.max_delta <=25 AND
    m.min_delta >= -25 AND
    m.time_window = 20 AND 
    (
		t.sec_sector_code IS NULL OR 
        (
			(t.sec_sector_code != "283" AND
			t.sec_sector_code NOT like "38%" AND 
			t.sec_sector_code NOT like "80%") AND
			t.otc = 0 AND
			f.quarter_number = 0 AND
			f.net_income != 0  
		)
)
ORDER BY 
	m.window_delta
DESC LIMIT 100;