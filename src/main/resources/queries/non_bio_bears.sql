/** Show stocks that are worth investing in longer term
 **/
SELECT m.ticker, m.window_delta, f.equity_ratio, f.eps,m.min_delta, m.max_delta, m.time_window, f.sector FROM midas.milestone m
LEFT JOIN v_financials f ON m.ticker = f.ticker
WHERE
	f.sector != "LIFE SCIENCES" AND 

    m.min_price >= 10.0 AND
    m.max_delta <= 25 AND
    m.min_delta >= -25 AND
    m.window_delta <= -20 AND
    m.time_window = 20 
ORDER BY window_delta ASC;