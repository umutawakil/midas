SELECT DISTINCT (LEFT(sec_sector_code,2)) code, MAX(ticker) FROM midas.v_stock_info GROUP BY code;

SELECT ticker FROM midas.v_stock_info WHERE LEFT(sec_sector_code,2) = "90"

SELECT DISTINCT (LEFT(sec_sector_code,2)) code, MAX(ticker) FROM midas.v_stock_info GROUP BY code;

SELECT sl.industry,f.sic_code, s.time_window, COUNT(*) as count,AVG(s.max_delta)as max_delta, AVG(s.min_delta) as min_delta, AVG(s.window_delta) as window_delta
FROM statistics s 
JOIN financials f ON s.ticker = f.ticker JOIN sic_level_3 sl ON sl.code = f.sic_code
WHERE f.quarter_number = 0 AND f.otc = 0 AND s.time_window = 10
GROUP BY f.sic_code
HAVING count > 10
ORDER BY window_delta DESC;