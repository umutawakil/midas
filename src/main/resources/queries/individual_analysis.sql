SELECT * FROM midas.statistics WHERE ticker = "ADCT";

SELECT MIN(average_deviation) FROM midas.statistics WHERE time_window = 20 AND average_deviation != 0;
SELECT * FROM midas.statistics WHERE time_window = 20 AND average_deviation > 10;
SELECT * FROM statistics s WHERE s.average_deviation = "0.01760050517121447" /*"29736.501882575212";*/
/*SELECT AVG(average_deviation) FROM midas.statistics WHERE time_window = 20;*/ /* 3.385 */

/*SELECT AVG(100*((average_deviation - 3.385)/3.385)) FROM statistics WHERE time_window = 20 AND ticker = "PRCH";*/