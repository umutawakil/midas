
SELECT 
	m.ticker,
	m.window_delta,
	m.min_delta, 
	m.max_delta,
	m.time_window
FROM 
	midas.statistics m JOIN financials f ON m.ticker = f.ticker
WHERE (
	(f.sec_sector_code = "283" OR
    f.sec_sector_code like "38%" OR
    f.sec_sector_code like "z80%") AND
    f.otc = 0 AND
    f.net_income > 0 AND
    f.fiscal_year = 2023 AND
    f.fiscal_period = "Q3" AND
    
    
    m.max_delta <=20 AND
    m.min_delta >= -20 AND
    /*m.window_delta > 20 AND*/
    m.time_window = 20 )
ORDER BY 
	m.window_delta
DESC LIMIT 100;