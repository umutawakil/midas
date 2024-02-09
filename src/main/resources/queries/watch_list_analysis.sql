SELECT s.ticker, s.window_delta,s.volume_delta, s.time_window, s.min_delta, s.max_delta
 FROM midas.watch_list w JOIN statistics s on w.ticker = s.ticker ORDER BY s.ticker ASC;