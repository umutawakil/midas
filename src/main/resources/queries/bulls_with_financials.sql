
SELECT 
	m.ticker,
	m.window_delta,
	m.min_delta, 
	m.max_delta,
    vf.profit_margin,
    vf.gross_profit_margin,
    vf.cfo_working_capital,
    vf.asset_liability,
    vf.current_asset_liability,
    vf.price_earnings,
    vf.price_equity
    
FROM 
	midas.statistics m 
JOIN
	financials f
ON 
	m.ticker = f.ticker
JOIN 
	v_financials vf 
ON 
	m.ticker = vf.ticker
JOIN 
	v_financial_deltas vfd 
ON 
	m.ticker = vfd.ticker

WHERE 
	vfd.range = 1 AND
	f.otc = 0 AND
	vf.quarter_number = 0 AND
    f.quarter_number  = 0 AND
    (f.sec_sector_code != 283 AND
    f.sec_sector_code != 384) AND 
    
    f.net_income > 0 AND
    vf.asset_liability >= 1 AND
    vf.gross_profit_margin > 0 AND
    vf.current_asset_liability >= 1 AND
    m.min_price > 1 AND
	m.max_delta <=20 AND
    m.min_delta >= -20 AND
    m.time_window = 20 
ORDER BY 
	m.window_delta
    
DESC LIMIT 100;