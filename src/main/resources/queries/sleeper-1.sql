SELECT t3.ticker,t3.window_delta FROM
(SELECT ticker, window_delta FROM midas.v_raw_stats_with_bio WHERE time_window = 20 AND min_delta >= -25 AND max_delta <= 25 LIMIT 300) t1

JOIN (SELECT ticker, window_delta FROM midas.v_raw_stats_with_bio WHERE time_window = 10 AND min_delta >= -25 AND max_delta <= 25
LIMIT 300) t2 ON t1.ticker = t2.ticker

JOIN (SELECT ticker, window_delta FROM midas.v_raw_stats_with_bio WHERE time_window = 5 AND min_delta >= -25 AND max_delta <= 25
LIMIT 300) t3 ON t2.ticker = t3.ticker ORDER BY t3.window_delta DESC LIMIT 100;