/** This query should show the lowest risk stocks. Sort by running_delta desc during trading hours
To find whose moving. You can find higher delta stocks but very little will suggest you will have time to
jump in on those. Tweek this query to try riskier jumps if thats desired.
 **/

SELECT * FROM midas.v_milestone 
WHERE
	max_delta <= 20 AND
    min_delta >= -15 AND
    time_window = 60 AND
    max_price <= 300 AND
    price >= 0.1 AND
    window_delta >= 20
ORDER BY running_delta DESC; /** Need to play with what your sorting on **/