# Overview
Application aggregates financial reports and real time stock market info to create aggregate statistics that can be queried upon using SQL. Its primary focus is in finding the most low risk trends across configurable time spans.

# Data Sources
The financials (income, balance sheet, cash flow) come from sources that change frequently from Data Vantage, to Polygon.io, to other third parties, but at the moment it uses public federal EDGAR data. That mechanism has proven to be the most accurate, but the source files are the clunkiest. Despite the drawbacks, processing SEC digital financial reports has proven to be more effective than current financial APIs which the code is only using for real time stock info and not financials.



