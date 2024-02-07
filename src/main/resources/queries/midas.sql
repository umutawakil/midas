CREATE DATABASE  IF NOT EXISTS `midas` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;
USE `midas`;
-- MySQL dump 10.13  Distrib 8.0.33, for macos13 (arm64)
--
-- Host: 127.0.0.1    Database: midas
-- ------------------------------------------------------
-- Server version	8.0.33

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `financials`
--

DROP TABLE IF EXISTS `financials`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `financials` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `ticker` varchar(45) NOT NULL,
  `name` varchar(300) NOT NULL,
  `cik` bigint NOT NULL,
  `sec_sector_code` int DEFAULT NULL,
  `sic_code` varchar(45) NOT NULL,
  `otc` tinyint(1) DEFAULT NULL,
  `fiscal_year` int DEFAULT NULL,
  `fiscal_period` varchar(45) DEFAULT NULL,
  `end_date` date DEFAULT NULL,
  `quarter_number` int DEFAULT NULL,
  `revenue` double DEFAULT NULL,
  `cost_of_revenue` double DEFAULT NULL,
  `cost_of_goods_sold` double DEFAULT NULL,
  `gross_profit` double DEFAULT NULL,
  `net_income` varchar(100) DEFAULT NULL,
  `eps_basic` double DEFAULT NULL,
  `eps_diluted` double DEFAULT NULL,
  `shares_outstanding` bigint DEFAULT NULL,
  `total_assets` double DEFAULT NULL,
  `total_current_assets` double DEFAULT NULL,
  `total_cash` double DEFAULT NULL,
  `cash_and_cash_Equivalents` double DEFAULT NULL,
  `total_current_liabilities` double DEFAULT NULL,
  `total_liabilities` double DEFAULT NULL,
  `total_equity` double DEFAULT NULL,
  `working_capital` double DEFAULT NULL,
  `operating_cash_flow` double DEFAULT NULL,
  `investing_cash_flow` double DEFAULT NULL,
  `financing_cash_flow` double DEFAULT NULL,
  `net_change_in_cash` double DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `financials_ticker_quarter_number_idx` (`ticker`,`quarter_number`)
) ENGINE=InnoDB AUTO_INCREMENT=5146931 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sec_ignored_entity`
--

DROP TABLE IF EXISTS `sec_ignored_entity`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sec_ignored_entity` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `cik` bigint DEFAULT NULL,
  `file_name` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=863430 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sic_level_3`
--

DROP TABLE IF EXISTS `sic_level_3`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sic_level_3` (
  `code` varchar(200) NOT NULL,
  `office` varchar(200) NOT NULL,
  `industry` varchar(300) NOT NULL,
  PRIMARY KEY (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sic_sector`
--

DROP TABLE IF EXISTS `sic_sector`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sic_sector` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` int NOT NULL,
  `name` varchar(100) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=81 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `statistics`
--

DROP TABLE IF EXISTS `statistics`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `statistics` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `ticker` varchar(45) NOT NULL,
  `max_price` double NOT NULL,
  `min_price` double NOT NULL,
  `max_delta` double NOT NULL,
  `min_delta` double NOT NULL,
  `window_delta` double NOT NULL,
  `time_window` int NOT NULL,
  `count` int NOT NULL,
  `average_volume` double NOT NULL,
  `volume_delta` double NOT NULL,
  `average_delta` double NOT NULL,
  `average_deviation` double NOT NULL,
  `current_price` double NOT NULL,
  PRIMARY KEY (`id`),
  KEY `statistics_ticker_time_window` (`ticker`,`time_window`)
) ENGINE=InnoDB AUTO_INCREMENT=8886901 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `stock_snapshot`
--

DROP TABLE IF EXISTS `stock_snapshot`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stock_snapshot` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `ticker` varchar(10) NOT NULL,
  `price` double NOT NULL,
  `volume` int NOT NULL,
  `creation_date` date NOT NULL,
  `time_imported` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `stock_snapshot_ticker_date_idx` (`ticker`,`creation_date`),
  KEY `stock_snapshot_date_idx` (`creation_date`)
) ENGINE=InnoDB AUTO_INCREMENT=233479700 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ticker`
--

DROP TABLE IF EXISTS `ticker`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ticker` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(45) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name_UNIQUE` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=199064 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `unsupported_ticker`
--

DROP TABLE IF EXISTS `unsupported_ticker`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `unsupported_ticker` (
  `ticker` varchar(45) NOT NULL,
  PRIMARY KEY (`ticker`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Temporary view structure for view `v_financial_deltas`
--

DROP TABLE IF EXISTS `v_financial_deltas`;
/*!50001 DROP VIEW IF EXISTS `v_financial_deltas`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `v_financial_deltas` AS SELECT 
 1 AS `ticker`,
 1 AS `fiscal_year2`,
 1 AS `fiscal_period2`,
 1 AS `fiscal_year1`,
 1 AS `fiscal_period1`,
 1 AS `range`,
 1 AS `revenue_delta`,
 1 AS `net_income_delta`,
 1 AS `gross_profit_delta`,
 1 AS `total_equity_delta`,
 1 AS `working_capital_delta`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `v_financials`
--

DROP TABLE IF EXISTS `v_financials`;
/*!50001 DROP VIEW IF EXISTS `v_financials`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `v_financials` AS SELECT 
 1 AS `ticker`,
 1 AS `fiscal_period`,
 1 AS `fiscal_year`,
 1 AS `end_date`,
 1 AS `quarter_number`,
 1 AS `otc`,
 1 AS `revenue`,
 1 AS `net_income`,
 1 AS `total_equity`,
 1 AS `working_capital`,
 1 AS `profit_margin`,
 1 AS `gross_profit`,
 1 AS `gross_profit_margin`,
 1 AS `asset_liability`,
 1 AS `current_asset_liability`,
 1 AS `debt_percentage`,
 1 AS `cfo_working_capital`,
 1 AS `cost_of_goods_sold`,
 1 AS `market_cap`,
 1 AS `price_earnings`,
 1 AS `price_revenue`,
 1 AS `price_gross_profit`,
 1 AS `price_equity`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `v_stock_info`
--

DROP TABLE IF EXISTS `v_stock_info`;
/*!50001 DROP VIEW IF EXISTS `v_stock_info`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `v_stock_info` AS SELECT 
 1 AS `ticker`,
 1 AS `current_price`,
 1 AS `window_delta`,
 1 AS `min_delta`,
 1 AS `max_delta`,
 1 AS `time_window`,
 1 AS `profit_margin`,
 1 AS `gross_profit_margin`,
 1 AS `price_equity`,
 1 AS `asset_liability`,
 1 AS `debt_percentage`,
 1 AS `cfo_working_capital`,
 1 AS `sec_sector_code`,
 1 AS `otc`,
 1 AS `name`,
 1 AS `min_price`,
 1 AS `max_price`,
 1 AS `volume_delta`*/;
SET character_set_client = @saved_cs_client;

--
-- Final view structure for view `v_financial_deltas`
--

/*!50001 DROP VIEW IF EXISTS `v_financial_deltas`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`midas`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `v_financial_deltas` AS select `t2`.`ticker` AS `ticker`,`t2`.`fiscal_year` AS `fiscal_year2`,`t2`.`fiscal_period` AS `fiscal_period2`,`t1`.`fiscal_year` AS `fiscal_year1`,`t1`.`fiscal_period` AS `fiscal_period1`,(`t1`.`quarter_number` + `t2`.`quarter_number`) AS `range`,((100 * (`t2`.`revenue` - `t1`.`revenue`)) / abs(`t1`.`revenue`)) AS `revenue_delta`,((100 * (`t2`.`net_income` - `t1`.`net_income`)) / abs(`t1`.`net_income`)) AS `net_income_delta`,((100 * (`t2`.`gross_profit` - `t1`.`gross_profit`)) / abs(`t1`.`gross_profit`)) AS `gross_profit_delta`,((100 * (`t2`.`total_equity` - `t1`.`total_equity`)) / abs(`t1`.`total_equity`)) AS `total_equity_delta`,((100 * (`t2`.`working_capital` - `t1`.`working_capital`)) / abs(`t1`.`working_capital`)) AS `working_capital_delta` from (`v_financials` `t1` join `v_financials` `t2` on((`t1`.`ticker` = `t2`.`ticker`))) where (`t2`.`quarter_number` = 0) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `v_financials`
--

/*!50001 DROP VIEW IF EXISTS `v_financials`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`midas`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `v_financials` AS select `f`.`ticker` AS `ticker`,`f`.`fiscal_period` AS `fiscal_period`,`f`.`fiscal_year` AS `fiscal_year`,`f`.`end_date` AS `end_date`,`f`.`quarter_number` AS `quarter_number`,`f`.`otc` AS `otc`,`f`.`revenue` AS `revenue`,`f`.`net_income` AS `net_income`,`f`.`total_equity` AS `total_equity`,`f`.`working_capital` AS `working_capital`,((100 * `f`.`net_income`) / `f`.`revenue`) AS `profit_margin`,`f`.`gross_profit` AS `gross_profit`,((100 * `f`.`gross_profit`) / `f`.`revenue`) AS `gross_profit_margin`,(`f`.`total_assets` / `f`.`total_liabilities`) AS `asset_liability`,(`f`.`total_current_assets` / `f`.`total_current_liabilities`) AS `current_asset_liability`,((100 * `f`.`total_current_liabilities`) / (abs(`f`.`total_current_assets`) + abs(`f`.`total_current_liabilities`))) AS `debt_percentage`,(case when ((`f`.`total_current_assets` - `f`.`total_current_liabilities`) > 0) then round(((100 * `f`.`operating_cash_flow`) / (`f`.`total_current_assets` - `f`.`total_current_liabilities`)),2) else (case when ((`f`.`total_current_assets` is not null) and (`f`.`total_current_liabilities` is not null)) then -(100.0) else NULL end) end) AS `cfo_working_capital`,`f`.`cost_of_goods_sold` AS `cost_of_goods_sold`,(`l`.`current_price` * `f`.`shares_outstanding`) AS `market_cap`,((`l`.`current_price` * `f`.`shares_outstanding`) / `f`.`net_income`) AS `price_earnings`,((`l`.`current_price` * `f`.`shares_outstanding`) / `f`.`revenue`) AS `price_revenue`,((`l`.`current_price` * `f`.`shares_outstanding`) / `f`.`gross_profit`) AS `price_gross_profit`,((`l`.`current_price` * `f`.`shares_outstanding`) / `f`.`total_equity`) AS `price_equity` from (`financials` `f` join `statistics` `l` on((`f`.`ticker` = `l`.`ticker`))) where (`l`.`time_window` = 5) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `v_stock_info`
--

/*!50001 DROP VIEW IF EXISTS `v_stock_info`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `v_stock_info` AS select `s`.`ticker` AS `ticker`,`s`.`current_price` AS `current_price`,round(`s`.`window_delta`,2) AS `window_delta`,round(`s`.`min_delta`,2) AS `min_delta`,round(`s`.`max_delta`,2) AS `max_delta`,`s`.`time_window` AS `time_window`,round(`vf`.`profit_margin`,2) AS `profit_margin`,`vf`.`gross_profit_margin` AS `gross_profit_margin`,`vf`.`price_equity` AS `price_equity`,`vf`.`asset_liability` AS `asset_liability`,round(`vf`.`debt_percentage`,2) AS `debt_percentage`,round(`vf`.`cfo_working_capital`,2) AS `cfo_working_capital`,`f`.`sec_sector_code` AS `sec_sector_code`,`f`.`otc` AS `otc`,`f`.`name` AS `name`,`s`.`min_price` AS `min_price`,`s`.`max_price` AS `max_price`,round(`s`.`volume_delta`,2) AS `volume_delta` from ((`statistics` `s` join `financials` `f` on((`s`.`ticker` = `f`.`ticker`))) join `v_financials` `vf` on((`s`.`ticker` = `vf`.`ticker`))) where ((`f`.`ticker` is null) or ((`f`.`quarter_number` = 0) and (`vf`.`quarter_number` = 0))) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2024-02-07 14:22:19
