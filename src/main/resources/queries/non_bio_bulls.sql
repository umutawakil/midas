/** These are the volatile swings stocks for the high volatility section of your portfolio.
 **/
SELECT 
	/*f.eps_diluted,
    f.eps_basic,
    f.sec_sector_code,*/
	m.ticker,
	m.window_delta,
	m.min_delta, 
	m.max_delta,
	m.time_window 
FROM 
	midas.statistics m JOIN financials f ON m.ticker = f.ticker
WHERE (
	(f.sec_sector_code != 283 AND
    f.sec_sector_code != 384) AND
    f.eps_diluted != 0 AND
    f.fiscal_year = 2023 AND
    f.fiscal_period = "Q3" AND
    
    m.min_price >= 1.0 AND
    m.max_delta <=25 AND
    m.min_delta >= -20 AND
    m.window_delta >= 20 AND
    m.time_window = 10 )
ORDER BY 
	window_delta
DESC LIMIT 20;