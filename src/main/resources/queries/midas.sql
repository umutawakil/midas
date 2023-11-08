CREATE DATABASE  IF NOT EXISTS `midas` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;
USE `midas`;
-- MySQL dump 10.13  Distrib 8.0.33, for macos13 (arm64)
--
-- Host: localhost    Database: midas
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
-- Table structure for table `delta`
--

DROP TABLE IF EXISTS `delta`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `delta` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `ticker` varchar(45) NOT NULL,
  `price` double NOT NULL,
  `open_delta` double NOT NULL,
  `running_delta` double NOT NULL,
  `previous_close_price` double NOT NULL,
  `open_price` double NOT NULL,
  `insert_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `volume` double NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2808186 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `financials`
--

DROP TABLE IF EXISTS `financials`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `financials` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `ticker` varchar(45) NOT NULL,
  `asset_delta` double NOT NULL,
  `liability_delta` double NOT NULL,
  `book_value_delta` double NOT NULL,
  `eps` double NOT NULL,
  `market_capitalization` double NOT NULL,
  `shares_outstanding` bigint NOT NULL,
  `cash_burn_percentage` double NOT NULL,
  `equity_burn_percentage` double NOT NULL,
  `current_equity_burn_percentage` double NOT NULL,
  `cash_on_hand` double NOT NULL,
  `cash_on_hand_change` double NOT NULL,
  `total_assets` double NOT NULL,
  `total_current_assets` double NOT NULL,
  `total_liabilities` double NOT NULL,
  `book_value` double NOT NULL,
  `last_report_date` date NOT NULL,
  `insertion_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `exchange` varchar(45) NOT NULL,
  `industry` varchar(200) NOT NULL,
  `sector` varchar(200) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `ticker_UNIQUE` (`ticker`)
) ENGINE=InnoDB AUTO_INCREMENT=16043 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ignore_ticker`
--

DROP TABLE IF EXISTS `ignore_ticker`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ignore_ticker` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(45) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=19110 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `milestone`
--

DROP TABLE IF EXISTS `milestone`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `milestone` (
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
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2462106 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=59889196 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
) ENGINE=InnoDB AUTO_INCREMENT=95914 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Temporary view structure for view `v_financials`
--

DROP TABLE IF EXISTS `v_financials`;
/*!50001 DROP VIEW IF EXISTS `v_financials`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `v_financials` AS SELECT 
 1 AS `id`,
 1 AS `ticker`,
 1 AS `asset_delta`,
 1 AS `liability_delta`,
 1 AS `book_value_delta`,
 1 AS `equity_ratio`,
 1 AS `eps`,
 1 AS `market_capitalization`,
 1 AS `shares_outstanding`,
 1 AS `cash_burn_percentage`,
 1 AS `equity_burn_percentage`,
 1 AS `current_equity_burn_percentage`,
 1 AS `cash_on_hand`,
 1 AS `cash_on_hand_change`,
 1 AS `total_assets`,
 1 AS `total_current_assets`,
 1 AS `total_liabilities`,
 1 AS `book_value`,
 1 AS `last_report_date`,
 1 AS `insertion_time`,
 1 AS `exchange`,
 1 AS `industry`,
 1 AS `sector`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `v_latest_stock_snapshot`
--

DROP TABLE IF EXISTS `v_latest_stock_snapshot`;
/*!50001 DROP VIEW IF EXISTS `v_latest_stock_snapshot`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `v_latest_stock_snapshot` AS SELECT 
 1 AS `id`,
 1 AS `ticker`,
 1 AS `price`,
 1 AS `volume`,
 1 AS `creation_date`,
 1 AS `time_imported`*/;
SET character_set_client = @saved_cs_client;

--
-- Table structure for table `volatile_stocks`
--

DROP TABLE IF EXISTS `volatile_stocks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `volatile_stocks` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `ticker` varchar(45) NOT NULL,
  `investment_date` date NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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
/*!50001 VIEW `v_financials` AS select `f`.`id` AS `id`,`f`.`ticker` AS `ticker`,`f`.`asset_delta` AS `asset_delta`,`f`.`liability_delta` AS `liability_delta`,`f`.`book_value_delta` AS `book_value_delta`,(`f`.`total_assets` / `f`.`total_liabilities`) AS `equity_ratio`,`f`.`eps` AS `eps`,`f`.`market_capitalization` AS `market_capitalization`,`f`.`shares_outstanding` AS `shares_outstanding`,`f`.`cash_burn_percentage` AS `cash_burn_percentage`,`f`.`equity_burn_percentage` AS `equity_burn_percentage`,`f`.`current_equity_burn_percentage` AS `current_equity_burn_percentage`,`f`.`cash_on_hand` AS `cash_on_hand`,`f`.`cash_on_hand_change` AS `cash_on_hand_change`,`f`.`total_assets` AS `total_assets`,`f`.`total_current_assets` AS `total_current_assets`,`f`.`total_liabilities` AS `total_liabilities`,`f`.`book_value` AS `book_value`,`f`.`last_report_date` AS `last_report_date`,`f`.`insertion_time` AS `insertion_time`,`f`.`exchange` AS `exchange`,`f`.`industry` AS `industry`,`f`.`sector` AS `sector` from `financials` `f` where ((abs(`f`.`cash_burn_percentage`) <= 1000.0) and (`f`.`market_capitalization` <> 0) and (`f`.`shares_outstanding` <> 0)) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `v_latest_stock_snapshot`
--

/*!50001 DROP VIEW IF EXISTS `v_latest_stock_snapshot`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`midas`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `v_latest_stock_snapshot` AS select `s1`.`id` AS `id`,`s1`.`ticker` AS `ticker`,`s1`.`price` AS `price`,`s1`.`volume` AS `volume`,`s1`.`creation_date` AS `creation_date`,`s1`.`time_imported` AS `time_imported` from (`stock_snapshot` `s1` join (select `stock_snapshot`.`ticker` AS `ticker`,max(`stock_snapshot`.`creation_date`) AS `creation_date` from `stock_snapshot` group by `stock_snapshot`.`ticker`) `s2` on(((`s1`.`ticker` = `s2`.`ticker`) and (`s1`.`creation_date` = `s2`.`creation_date`)))) */;
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

-- Dump completed on 2023-11-08 12:24:30
