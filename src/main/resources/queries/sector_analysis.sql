SELECT DISTINCT (LEFT(sec_sector_code,2)) code, MAX(ticker) FROM midas.v_stock_info GROUP BY code;

SELECT ticker FROM midas.v_stock_info WHERE LEFT(sec_sector_code,2) = "90"