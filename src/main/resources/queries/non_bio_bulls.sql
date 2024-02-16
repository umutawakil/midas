
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
    f.sec_sector_code
FROM 
	midas.statistics m left JOIN financials f ON m.ticker = f.ticker
    /*left JOIN v_financials vf ON m.ticker = vf.ticker*/
WHERE 
	m.ticker = "GCT" AND
    m.max_delta <=20 AND
    m.min_delta >= -20 AND
    m.time_window = 20 AND
    (
		f.sec_sector_code IS NULL OR 
        (
			(f.sec_sector_code != "283" AND
			f.sec_sector_code NOT like "38%" AND
			f.sec_sector_code NOT like "80%") AND
			f.otc = 0 AND
			f.quarter_number = 0 AND
			f.net_income > 0  
		)
)
ORDER BY 
	m.window_delta
DESC LIMIT 100;