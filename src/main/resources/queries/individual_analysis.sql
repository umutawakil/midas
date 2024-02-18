SELECT * FROM midas.statistics WHERE ticker = "FSTX";

/*SELECT AVG(average_deviation) FROM midas.statistics WHERE time_window = 20;*/ /* 3.385 */

/*SELECT AVG(100*((average_deviation - 3.385)/3.385)) FROM statistics WHERE time_window = 20 AND ticker = "PRCH";*/