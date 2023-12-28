/** Show stocks that are worth investing in longer term
 **/
SELECT * FROM midas.statistics
WHERE
    /*min_delta >= -25 AND*/
    min_price >= 1.0 AND
    max_delta <= 30 AND
    min_delta >= -13 AND
    window_delta >= 30 AND
    time_window = 60 
ORDER BY window_delta DESC;