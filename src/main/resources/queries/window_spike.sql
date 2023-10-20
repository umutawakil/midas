/*
* Query to figure out which stocks spiked at w20 but a signifitcantly lower spike at w60.
The purpose is to find "sleeper" stocks. 
*/
use midas;
SELECT t1.*, t2.window_delta AS `w(40)`, t1.`w(20)` - t2.window_delta AS diff FROM (
	SELECT ticker,MAX(max_delta) as max_delta,MIN(min_delta) as min_delta, MAX(window_delta) as `w(20)`
    FROM milestone 
    WHERE time_window = 20 AND window_delta >= 10.0
    GROUP BY ticker
    HAVING COUNT(ticker) = 1
) t1 
JOIN (SELECT ticker, window_delta, time_window FROM milestone WHERE time_window = 40) t2
ON t1.ticker = t2.ticker
WHERE 
	t2.window_delta > 0  AND 
    t2.window_delta <= 5 AND 
    t1.min_delta >= -15  AND 
    t1.max_delta <= 15  
ORDER BY diff DESC;
