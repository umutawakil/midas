/** Show stocks that are worth investing in longer term
 **/
SELECT * FROM midas.milestone 
WHERE
    min_price >= 1.0 AND
    max_delta <= 20 AND
    min_delta >= -20 AND
    window_delta <= -20 AND
    time_window = 20 
ORDER BY window_delta ASC;