SELECT m.window_delta,m.min_price,f.* 
FROM v_financials f JOIN milestone m ON f.ticker = m.ticker
WHERE 
	m.time_window = 40 AND
    m.window_delta <= -30 AND
    m.min_price >= 1.00 AND

	f.cash_burn_percentage <= 100 AND
    f.equity_burn_percentage <= 100 AND
	f.cash_on_hand_change < 0 AND
    f.cash_on_hand != 0 AND
    f.profit_margin < 0 AND
    f.profit_margin <= -50.0 AND
    f.revenue_delta < 0 AND
    f.net_income < 0 AND
    ABS(f.net_income) > ABS(f.revenue)
    ORDER BY m.window_delta ASC;
/*ORDER BY f.cash_burn_percentage DESC; */

