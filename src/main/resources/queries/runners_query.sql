/** Show the stocks on the move
 **/
SELECT * FROM midas.v_milestone 
WHERE
    /*min_delta >= -25 AND*/
    window_delta >= 15 AND
    time_window >= 20 
ORDER BY running_delta DESC; /** Need to play with what your sorting on **/