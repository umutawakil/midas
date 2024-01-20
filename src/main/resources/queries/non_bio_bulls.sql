
SELECT 
	m.ticker,
	m.window_delta,
	m.min_delta, 
	m.max_delta,
	m.time_window,
    m.min_price,
    f.net_income
FROM 
	midas.statistics m left JOIN financials f ON m.ticker = f.ticker
    /*left JOIN v_financials vf ON m.ticker = vf.ticker*/
WHERE (
	(f.sec_sector_code != "283" AND
    f.sec_sector_code NOT like "38%" AND
    f.sec_sector_code NOT like "80%") AND
	/*(f.sec_sector_code != 283 AND
    f.sec_sector_code != 384) AND*/
    f.otc = 0 AND
    f.quarter_number = 0 AND
    f.net_income > 0 AND /*
    f.fiscal_year = 2023 AND
    f.fiscal_period = "Q3" AND*/
    m.min_price >= 2 AND
    m.max_delta <=25 AND
    m.min_delta >= -25 AND
    /*m.window_delta > 20 AND*/
    m.time_window = 10 )
ORDER BY 
	m.window_delta
ASC LIMIT 100;