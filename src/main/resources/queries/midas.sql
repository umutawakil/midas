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
) ENGINE=InnoDB AUTO_INCREMENT=1357352 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
) ENGINE=InnoDB AUTO_INCREMENT=1565881 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
) ENGINE=InnoDB AUTO_INCREMENT=26899802 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
-- Temporary view structure for view `v_core_milestone`
--

DROP TABLE IF EXISTS `v_core_milestone`;
/*!50001 DROP VIEW IF EXISTS `v_core_milestone`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `v_core_milestone` AS SELECT 
 1 AS `ticker`,
 1 AS `price`,
 1 AS `open_delta`,
 1 AS `running_delta`,
 1 AS `perc_max_price`,
 1 AS `perc_min_price`,
 1 AS `perc_rmax_delta`,
 1 AS `max_price`,
 1 AS `min_price`,
 1 AS `min_delta`,
 1 AS `perc_max_delta`,
 1 AS `perc_min_delta`,
 1 AS `max_delta`,
 1 AS `average_volume`,
 1 AS `expected_volume`,
 1 AS `volume_deviation_estimate`,
 1 AS `window_delta`,
 1 AS `time_window`,
 1 AS `time_offset(mins)`,
 1 AS `count`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `v_dark_milestone`
--

DROP TABLE IF EXISTS `v_dark_milestone`;
/*!50001 DROP VIEW IF EXISTS `v_dark_milestone`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `v_dark_milestone` AS SELECT 
 1 AS `ticker`,
 1 AS `price`,
 1 AS `open_delta`,
 1 AS `running_delta`,
 1 AS `perc_min_price`,
 1 AS `perc_max_price`,
 1 AS `perc_rmax_delta`,
 1 AS `min_price`,
 1 AS `max_price`,
 1 AS `min_delta`,
 1 AS `perc_max_delta`,
 1 AS `perc_min_delta`,
 1 AS `max_delta`,
 1 AS `window_delta`,
 1 AS `time_window`,
 1 AS `time_offset(mins)`,
 1 AS `count`*/;
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
-- Temporary view structure for view `v_milestone`
--

DROP TABLE IF EXISTS `v_milestone`;
/*!50001 DROP VIEW IF EXISTS `v_milestone`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `v_milestone` AS SELECT 
 1 AS `ticker`,
 1 AS `price`,
 1 AS `open_delta`,
 1 AS `running_delta`,
 1 AS `perc_max_price`,
 1 AS `perc_rmax_delta`,
 1 AS `max_price`,
 1 AS `min_delta`,
 1 AS `perc_max_delta`,
 1 AS `max_delta`,
 1 AS `average_volume`,
 1 AS `expected_volume`,
 1 AS `volume_deviation_estimate`,
 1 AS `window_delta`,
 1 AS `time_window`,
 1 AS `time_offset(mins)`,
 1 AS `count`*/;
SET character_set_client = @saved_cs_client;

--
-- Final view structure for view `v_core_milestone`
--

/*!50001 DROP VIEW IF EXISTS `v_core_milestone`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`midas`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `v_core_milestone` AS select `t1`.`ticker` AS `ticker`,`t1`.`price` AS `price`,`t1`.`open_delta` AS `open_delta`,`t1`.`running_delta` AS `running_delta`,`t1`.`perc_max_price` AS `perc_max_price`,`t1`.`perc_min_price` AS `perc_min_price`,`t1`.`perc_rmax_delta` AS `perc_rmax_delta`,`t1`.`max_price` AS `max_price`,`t1`.`min_price` AS `min_price`,`t1`.`min_delta` AS `min_delta`,`t1`.`perc_max_delta` AS `perc_max_delta`,`t1`.`perc_min_delta` AS `perc_min_delta`,`t1`.`max_delta` AS `max_delta`,`t1`.`average_volume` AS `average_volume`,`t1`.`expected_volume` AS `expected_volume`,(100.0 * (`t1`.`volume` / `t1`.`expected_volume`)) AS `volume_deviation_estimate`,`t1`.`window_delta` AS `window_delta`,`t1`.`time_window` AS `time_window`,`t1`.`time_offset(mins)` AS `time_offset(mins)`,`t1`.`count` AS `count` from (select `d`.`ticker` AS `ticker`,`d`.`price` AS `price`,`d`.`open_delta` AS `open_delta`,`d`.`running_delta` AS `running_delta`,`d`.`volume` AS `volume`,(100.0 * (`d`.`price` / `m`.`max_price`)) AS `perc_max_price`,(100.0 * (`d`.`price` / `m`.`min_price`)) AS `perc_min_price`,(100.0 * (`d`.`running_delta` / `m`.`max_delta`)) AS `perc_rmax_delta`,(100.0 * (`d`.`open_delta` / `m`.`max_delta`)) AS `perc_max_delta`,(100.0 * (`d`.`open_delta` / `m`.`min_delta`)) AS `perc_min_delta`,`m`.`time_window` AS `time_window`,`m`.`count` AS `count`,`m`.`max_price` AS `max_price`,`m`.`min_price` AS `min_price`,`m`.`max_delta` AS `max_delta`,`m`.`min_delta` AS `min_delta`,`m`.`average_volume` AS `average_volume`,(`m`.`average_volume` / 26) AS `expected_volume`,`m`.`window_delta` AS `window_delta`,timestampdiff(MINUTE,`d`.`insert_time`,now()) AS `time_offset(mins)` from (`milestone` `m` join (select `d1`.`id` AS `id`,`d1`.`ticker` AS `ticker`,`d1`.`price` AS `price`,`d1`.`volume` AS `volume`,`d1`.`open_delta` AS `open_delta`,`d1`.`running_delta` AS `running_delta`,`d1`.`previous_close_price` AS `previous_close_price`,`d1`.`open_price` AS `open_price`,`d1`.`insert_time` AS `insert_time`,`d2`.`symbol` AS `symbol`,`d2`.`max_time` AS `max_time` from (`delta` `d1` join (select `d3`.`ticker` AS `symbol`,max(`d3`.`insert_time`) AS `max_time` from `delta` `d3` group by `symbol`) `d2` on(((`d1`.`ticker` = `d2`.`symbol`) and (`d1`.`insert_time` = `d2`.`max_time`))))) `d` on((`m`.`ticker` = `d`.`ticker`)))) `t1` where ((`t1`.`time_window` >= 20) and (`t1`.`count` >= 20)) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `v_dark_milestone`
--

/*!50001 DROP VIEW IF EXISTS `v_dark_milestone`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`midas`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `v_dark_milestone` AS select `v_core_milestone`.`ticker` AS `ticker`,`v_core_milestone`.`price` AS `price`,`v_core_milestone`.`open_delta` AS `open_delta`,`v_core_milestone`.`running_delta` AS `running_delta`,`v_core_milestone`.`perc_min_price` AS `perc_min_price`,`v_core_milestone`.`perc_max_price` AS `perc_max_price`,`v_core_milestone`.`perc_rmax_delta` AS `perc_rmax_delta`,`v_core_milestone`.`min_price` AS `min_price`,`v_core_milestone`.`max_price` AS `max_price`,`v_core_milestone`.`min_delta` AS `min_delta`,`v_core_milestone`.`perc_max_delta` AS `perc_max_delta`,`v_core_milestone`.`perc_min_delta` AS `perc_min_delta`,`v_core_milestone`.`max_delta` AS `max_delta`,`v_core_milestone`.`window_delta` AS `window_delta`,`v_core_milestone`.`time_window` AS `time_window`,`v_core_milestone`.`time_offset(mins)` AS `time_offset(mins)`,`v_core_milestone`.`count` AS `count` from `v_core_milestone` where (`v_core_milestone`.`min_price` > `v_core_milestone`.`price`) */;
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

--
-- Final view structure for view `v_milestone`
--

/*!50001 DROP VIEW IF EXISTS `v_milestone`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`midas`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `v_milestone` AS select `t1`.`ticker` AS `ticker`,`t1`.`price` AS `price`,`t1`.`open_delta` AS `open_delta`,`t1`.`running_delta` AS `running_delta`,`t1`.`perc_max_price` AS `perc_max_price`,`t1`.`perc_rmax_delta` AS `perc_rmax_delta`,`t1`.`max_price` AS `max_price`,`t1`.`min_delta` AS `min_delta`,`t1`.`perc_max_delta` AS `perc_max_delta`,`t1`.`max_delta` AS `max_delta`,`t1`.`average_volume` AS `average_volume`,`t1`.`expected_volume` AS `expected_volume`,(100.0 * (`t1`.`volume` / `t1`.`expected_volume`)) AS `volume_deviation_estimate`,`t1`.`window_delta` AS `window_delta`,`t1`.`time_window` AS `time_window`,`t1`.`time_offset(mins)` AS `time_offset(mins)`,`t1`.`count` AS `count` from (select `d`.`ticker` AS `ticker`,`d`.`price` AS `price`,`d`.`open_delta` AS `open_delta`,`d`.`volume` AS `volume`,`d`.`running_delta` AS `running_delta`,(100.0 * (`d`.`price` / `m`.`max_price`)) AS `perc_max_price`,(100.0 * (`d`.`running_delta` / `m`.`max_delta`)) AS `perc_rmax_delta`,(100.0 * (`d`.`open_delta` / `m`.`max_delta`)) AS `perc_max_delta`,`m`.`time_window` AS `time_window`,`m`.`count` AS `count`,`m`.`max_price` AS `max_price`,`m`.`max_delta` AS `max_delta`,`m`.`min_delta` AS `min_delta`,`m`.`average_volume` AS `average_volume`,(`m`.`average_volume` / 26) AS `expected_volume`,`m`.`window_delta` AS `window_delta`,timestampdiff(MINUTE,`d`.`insert_time`,now()) AS `time_offset(mins)` from (`milestone` `m` join (select `d1`.`id` AS `id`,`d1`.`ticker` AS `ticker`,`d1`.`price` AS `price`,`d1`.`volume` AS `volume`,`d1`.`open_delta` AS `open_delta`,`d1`.`running_delta` AS `running_delta`,`d1`.`previous_close_price` AS `previous_close_price`,`d1`.`open_price` AS `open_price`,`d1`.`insert_time` AS `insert_time`,`d2`.`symbol` AS `symbol`,`d2`.`max_time` AS `max_time` from (`delta` `d1` join (select `d3`.`ticker` AS `symbol`,max(`d3`.`insert_time`) AS `max_time` from `delta` `d3` group by `symbol`) `d2` on(((`d1`.`ticker` = `d2`.`symbol`) and (`d1`.`insert_time` = `d2`.`max_time`))))) `d` on((`m`.`ticker` = `d`.`ticker`)))) `t1` where ((`t1`.`perc_max_price` >= 100.0) and (`t1`.`time_window` >= 20) and (`t1`.`count` >= 20)) */;
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

-- Dump completed on 2023-10-21 13:54:30
