package com.midas.domain

import com.midas.configuration.ApplicationProperties
import com.midas.repositories.FinancialsRepository
import com.midas.repositories.SecIgnoredEntityRepository
import com.midas.services.LoggingService
import jakarta.annotation.PostConstruct
import jakarta.persistence.*
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap
import kotlin.io.path.name

@Entity
@Table(name="financials")
class Financials {
    @Id
    @Column(name="id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private val id                          : Long = -1L
    private val secSectorCode               : Int?
    private val sicCode                     : String?
    private val cik                         : Long
    private val name                        : String
    private val otc                         : Boolean
    private val ticker                      : String
    private val fiscalYear                  : Int?
    private val fiscalPeriod                : String?
    private val quarterNumber               : Int?
    @Column(name="end_date")
    private val endDate                     : Date?

    private val netIncome                   : Double?
    private val revenue                     : Double?
    private val costOfRevenue               : Double?
    private val grossProfit                 : Double?
    private val costOfGoodsSold             : Double?
    private val epsBasic                    : Double?
    private val epsDiluted                  : Double?
    private val sharesOutstanding           : Long?

    private val totalAssets                 : Double?
    private val totalCurrentAssets          : Double?
    private val totalCash                   : Double?
    private val cashAndCashEquivalents      : Double?
    private val totalCurrentLiabilities     : Double?
    private val totalLiabilities            : Double?
    private val totalEquity                 : Double?
    private val workingCapital              : Double?

    private val operatingCashFlow           : Double?
    private val investingCashFlow           : Double?
    private val financingCashFlow           : Double?
    private val netChangeInCash             : Double?

    constructor(
        ticker: String,
        fiscalYear: Int?,
        fiscalPeriod: String?,
        endDate: Date?,
        quarterNumber: Int?,
        secSectorCode: Int?,
        sicCode: String?,
        cik: Long,
        name: String,
        otc: Boolean,
        netIncome: Double?,
        revenue: Double?,
        costOfRevenue: Double?,
        grossProfit: Double?,
        costOfGoodsSold: Double?,
        epsBasic: Double?,
        epsDiluted: Double?,
        sharesOutstanding: Long?,

        totalAssets: Double?,
        totalCurrentAssets: Double?,
        totalCash: Double?,
        cashAndCashEquivalents: Double?,
        totalCurrentLiabilities: Double?,
        totalLiabilities: Double?,
        totalEquity: Double?,
        workingCapital: Double?,

        operatingCashFlow: Double?,
        investingCashFlow: Double?,
        financingCashFlow: Double?,
        netChangeInCash: Double?
    ) {
        this.ticker                  = ticker
        this.secSectorCode           = secSectorCode
        this.cik                     = cik
        this.sicCode                 = sicCode
        this.name                    = name
        this.otc                     = otc
        this.fiscalYear              = fiscalYear
        this.fiscalPeriod            = fiscalPeriod
        this.endDate                 = endDate
        this.quarterNumber           = quarterNumber

        this.netIncome               = netIncome
        this.revenue                 = revenue
        this.costOfRevenue           = costOfRevenue
        this.grossProfit             = grossProfit
        this.costOfGoodsSold         = costOfGoodsSold
        this.epsBasic                = epsBasic
        this.epsDiluted              = epsDiluted
        this.sharesOutstanding       = sharesOutstanding

        this.totalAssets             = totalAssets
        this.totalCurrentAssets      = totalCurrentAssets
        this.totalCash               = totalCash
        this.cashAndCashEquivalents  = cashAndCashEquivalents
        this.totalCurrentLiabilities = totalCurrentLiabilities
        this.totalLiabilities        = totalLiabilities
        this.totalEquity             = totalEquity
        this.workingCapital          = workingCapital

        this.operatingCashFlow       = operatingCashFlow
        this.investingCashFlow       = investingCashFlow
        this.financingCashFlow       = financingCashFlow
        this.netChangeInCash         = netChangeInCash
    }
    @Component
    class SpringAdapter(
        @Autowired private val financialsRepository: FinancialsRepository,
        @Autowired private val secIgnoredEntityRepository: SecIgnoredEntityRepository,
        @Autowired private val applicationProperties: ApplicationProperties,
        @Autowired private val loggingService: LoggingService
    ) {
        @PostConstruct
        fun init() {
            Financials.applicationProperties      = applicationProperties
            Financials.loggingService             = loggingService
            Financials.financialsRepository       = financialsRepository
            Financials.secIgnoredEntityRepository = secIgnoredEntityRepository
        }
    }

    companion object {
        private lateinit var financialsRepository: FinancialsRepository
        private lateinit var applicationProperties: ApplicationProperties
        private lateinit var secIgnoredEntityRepository: SecIgnoredEntityRepository
        private lateinit var loggingService: LoggingService

        private const val MINIMUM_YEAR = 2021
        private const val MAX_NUM_QUARTERS = 4

       // private var endDate: Date? = null
        fun import() {
            loggingService.log("Importing financials...")
            //financialsRepository.deleteEvery() //Using the built-in deleteAll method is slow because it looks like it does a select All first...
            loggingService.log("Previous records deleted....")

            /*TODO: These two can be consolidated into one perhaps but it will require re running multiple times and
            * without memoization this runs very slow. To save time two structures are utilized
            * */
            loggingService.log("Getting ignore data...")
            val ignoreEntityMap: MutableSet<Long?>         = secIgnoredEntityRepository.findAll().map {it.cik }.toMutableSet()
            val ignoreEntityMapByName: MutableSet<String?> = secIgnoredEntityRepository.findAll().map {it.fileName }.toMutableSet()
            loggingService.log("Ignore data retrieved...")

            var metaRecords            = 0
            var metaRecordsFailed      = 0
            var metaRecordsBadFileName = 0
            var financialRecords       = 0
            var financialRecordsFailed = 0

            for (s in Files.list(Paths.get("${applicationProperties.financialsDirectory}/submissions")).toList()) {
                val fileName = s.fileName.name//"CIK0001867066.json"//
                if ((!ignoreEntityMapByName.contains(fileName)) && isCorrectMetaFile(fileName)) {
                    val cik = fileNameToCikCode(fileName = fileName)
                    if (!ignoreEntityMap.contains(cik)) {
                        val metaData: JSONObject = fileToJson(file = s.toFile())//fileToJson(file = File("${applicationProperties.financialsDirectory}/submissions/$fileName"))
                        /*if((metaData["exchanges"] as JSONArray).contains("OTC")) {
                            continue
                        }*/
                        if ((metaData["tickers"] as JSONArray).size > 0) {
                            metaRecords++
                            loggingService.log("Meta records: $metaRecords, Meta file pre-processed: $fileName")

                            for (t in metaData["tickers"] as JSONArray) {
                                val financialFile = File("${applicationProperties.financialsDirectory}/companyfacts/$fileName")
                                if (financialFile.exists()) {
                                    createFinancialsRecordsForTicker(
                                        ticker        = t as String,
                                        metaData      = metaData,
                                        financialData = fileToJson(file = financialFile)
                                    )

                                    financialRecords++
                                    loggingService.log("Financial records: $financialRecords")
                                } else {
                                    financialRecordsFailed++
                                    loggingService.log("Financial record file does not exist ($fileName)...Failed financial records: $financialRecordsFailed")
                                }
                            }
                        } else {
                            metaRecordsFailed++
                            /* TODO: This save makes the application skip non-ticker entity files for every or until the ignore database is cleared and re-run **/
                            secIgnoredEntityRepository.save(SecIgnoredEntity(cik = cik, fileName = fileName))
                            loggingService.log("Meta records failed: $metaRecordsFailed")
                        }
                    }
                } else {
                    /*If the fileName is already in the ignore list don't save it again and don't alert it to the screen */
                    if ( !ignoreEntityMapByName.contains(fileName)) {
                        metaRecordsBadFileName++
                        secIgnoredEntityRepository.save(SecIgnoredEntity(cik = null, fileName = fileName))
                        loggingService.log("Meta records bad file name: $metaRecordsBadFileName")
                    }
                }
            }

            loggingService.log("Financials imported!")
            loggingService.log("Meta records: $metaRecords, financial records: $financialRecords")
            loggingService.log("Meta records failed: $metaRecordsFailed, Meta records bad file name: $metaRecordsBadFileName, financial records failed: $financialRecordsFailed")
        }

        private fun createFinancialsRecordsForTicker(
            ticker: String,
            metaData: JSONObject,
            financialData: JSONObject
        ) {
            if (!hasUsGaapData(financialData = financialData)) {
                UnsupportedTicker.save(UnsupportedTicker(name = ticker))
                return
            }

            val attr: MutableMap<FiscalGroup, MutableMap<String,Double?>> = HashMap()
            //endDate = null

            val attributeArray: JSONArray = oTa(
                "shares",
                 oTo(
                     "units",
                    oTo(
                        "EntityCommonStockSharesOutstanding",
                        oTo(
                            "dei",
                            oTo(
                                "facts", financialData
                            )
                        )
                    )
                )
            ) ?: JSONArray()
            for (i in attributeArray.indices) { //TODO: Need a new algorithm that terminates when MAX_NUMBER_QUARTERS is reached per property
                val x: JSONObject = attributeArray[i] as JSONObject
                if ((x["form"] as String) != "10-Q") {
                    continue
                }
                if((x["fy"] as Number?)?.toInt() != null && (x["fy"] as Number).toInt() < MINIMUM_YEAR) {
                    continue
                }

                val fiscalGroup = FiscalGroup(
                    fiscalYear   = (x["fy"] as Number?)?.toInt(),
                    fiscalPeriod = x["fp"] as String?,
                    endDate      = if(x["end"] != null) {SimpleDateFormat("yyyy-MM-dd").parse((x["end"] as String)) } else { null }
                )

                val m: MutableMap<String, Double?> = if(attr.containsKey(fiscalGroup)) { attr[fiscalGroup]!! } else { HashMap() }
                val value = (x["val"] as Number?)?.toDouble()
                if (value != null) {
                    m["EntityCommonStockSharesOutstanding"] = value
                }
                attr[fiscalGroup] = m
            }

            getGaapAttribute(key = "Revenues", financialData = financialData, attr = attr)
            getGaapAttribute(key = "RevenueFromContractWithCustomerIncludingAssessedTax", financialData = financialData, attr = attr)
            getGaapAttribute(key = "RevenueFromContractWithCustomerExcludingAssessedTax", financialData = financialData, attr = attr)
            getGaapAttribute(key = "RevenuesNetOfInterestExpense", financialData = financialData, attr = attr)

            getGaapAttribute(key = "CostOfRevenue", financialData = financialData, attr = attr)

            getGaapAttribute(key = "NetIncomeLoss", financialData = financialData, attr = attr)
            getGaapAttribute(key = "ProfitLoss", financialData = financialData, attr = attr)

            getGaapAttribute(key = "EarningsPerShareDiluted", usdType = "USD/shares", financialData = financialData, attr = attr)
            getGaapAttribute(key = "EarningsPerShareBasic", usdType = "USD/shares", financialData = financialData, attr = attr)

            getGaapAttribute(key = "GrossProfit", financialData = financialData, attr = attr)
            getGaapAttribute(key = "CostOfGoodsSold", financialData = financialData, attr = attr) //It might not actually ever appear in the JSON this way
            getGaapAttribute(key = "CostOfGoodsAndServicesSold", financialData = financialData, attr = attr)

            getGaapAttribute(key = "Assets", financialData = financialData, attr = attr)
            getGaapAttribute(key = "AssetsCurrent", financialData = financialData, attr = attr)
            getGaapAttribute(key = "Liabilities", financialData = financialData, attr = attr)
            getGaapAttribute(key = "LiabilitiesAndStockholdersEquity", financialData = financialData, attr = attr)
            getGaapAttribute(key = "LiabilitiesCurrent", financialData = financialData, attr = attr)
            getGaapAttribute(key = "StockholdersEquity", financialData = financialData, attr = attr)
            getGaapAttribute(key = "Cash", financialData = financialData, attr = attr)
            getGaapAttribute(key = "CashAndCashEquivalentsAtCarryingValue", financialData = financialData, attr = attr)

            getGaapAttribute(key = "NetCashProvidedByUsedInOperatingActivities", financialData = financialData, attr = attr)
            getGaapAttribute(key = "NetCashProvidedByUsedInInvestingActivities", financialData = financialData, attr = attr)
            getGaapAttribute(key = "NetCashProvidedByUsedInFinancingActivities", financialData = financialData, attr = attr)

            var quarterNumber = 0
            for (fiscalGroup in (attr.keys.sortedByDescending {"${it.fiscalYear}${it.fiscalPeriod}"}.toList())) {
                val m: MutableMap<String, Double?> = attr[fiscalGroup]!!

                //TODO: How to ensure the last quarter is actually the real last quarter and not missing data?

                try {
                    financialsRepository.save(
                        Financials(
                            ticker                  = ticker,
                            secSectorCode           = if (metaData["sic"].toString().isNotEmpty()) { metaData["sic"].toString().substring(0,3).toInt()} else { null},
                            sicCode                 = metaData["sic"]?.toString(),
                            name                    = (metaData["name"] as String),
                            cik                     = (metaData["cik"] as String).toLong(),
                            otc                     = (metaData["exchanges"] as JSONArray).contains("OTC"),
                            fiscalYear              = fiscalGroup.fiscalYear,
                            fiscalPeriod            = fiscalGroup.fiscalPeriod,
                            endDate                 = fiscalGroup.endDate,
                            quarterNumber           = quarterNumber,

                            netIncome               = getNetIncome(m),
                            revenue                 = getRevenue(m),
                            costOfRevenue           = m["CostOfRevenue"],
                            grossProfit             = getGrossProfit(m),
                            costOfGoodsSold         = getCostOfGoodsSold(m),
                            epsBasic                = m["EarningsPerShareBasic"],
                            epsDiluted              = m["EarningsPerShareDiluted"],
                            sharesOutstanding       = m["EntityCommonStockSharesOutstanding"]?.toLong(),

                            totalAssets             = m["Assets"] ?:  m["LiabilitiesAndStockholdersEquity"],
                            totalCurrentAssets      = m["AssetsCurrent"],
                            totalCash               = m["Cash"],
                            cashAndCashEquivalents  = m["CashAndCashEquivalentsAtCarryingValue"],
                            totalCurrentLiabilities = m["LiabilitiesCurrent"],
                            totalLiabilities        = m["Liabilities"] ?: if (m["StockholdersEquity"] != null && m["LiabilitiesAndStockholdersEquity"] != null) { m["LiabilitiesAndStockholdersEquity"]!! - m["StockholdersEquity"]!!} else { null },
                            totalEquity             = getTotalEquity(m),//m["StockholdersEquity"] ?: if (m["Liabilities"] != null && m["LiabilitiesAndStockholdersEquity"] != null) { m["LiabilitiesAndStockholdersEquity"]!! - m["Liabilities"]!!} else { null },

                            operatingCashFlow       = m["NetCashProvidedByUsedInOperatingActivities"],
                            investingCashFlow       = m["NetCashProvidedByUsedInInvestingActivities"],
                            financingCashFlow       = m["NetCashProvidedByUsedInFinancingActivities"],
                            netChangeInCash         = if(m["NetCashProvidedByUsedInOperatingActivities"] != null && m["NetCashProvidedByUsedInInvestingActivities"] != null && m["NetCashProvidedByUsedInFinancingActivities"] != null) { m["NetCashProvidedByUsedInOperatingActivities"]!! + m["NetCashProvidedByUsedInInvestingActivities"]!! + m["NetCashProvidedByUsedInFinancingActivities"]!! } else { null },
                            workingCapital          = if(m["AssetsCurrent"] != null && m["LiabilitiesCurrent"] != null) { m["AssetsCurrent"]!! - m["LiabilitiesCurrent"]!! } else { null }
                        )
                    )
                } catch (e: Exception){
                    e.printStackTrace()
                    throw RuntimeException(e)
                }
                quarterNumber++
                if (quarterNumber == (MAX_NUM_QUARTERS + 1)) { //TODO: A more efficient algorithm that makes it so we only have MAX_NUM_QUARTERS per property will save time.
                    break
                }
            }
        }

        private fun getNetIncome(m: MutableMap<String, Double?> ) : Double? {
            return m["NetIncomeLoss"] ?: m["ProfitLoss"]
        }

        private fun getRevenue(m:MutableMap<String, Double?>) : Double? {
            return  m["Revenues"] ?:
                    m["RevenuesNetOfInterestExpense"] ?:
                    m["RevenueFromContractWithCustomerIncludingAssessedTax"] ?:
                    m["RevenueFromContractWithCustomerExcludingAssessedTax"]

            /*if(getGrossProfit(m)!= null && getCostOfGoodsSold(m) != null) {
                getGrossProfit(m)!! + getCostOfGoodsSold(m)!!
            } else {
                m["RevenuesNetOfInterestExpense"] ?:
                m["RevenueFromContractWithCustomerIncludingAssessedTax"] ?:
                m["RevenueFromContractWithCustomerExcludingAssessedTax"]
            }*/
        }

        private fun getTotalEquity(m:MutableMap<String, Double?>) : Double? {
            if(m["Assets"] != null && m["Liabilities"] != null) {
                return m["Assets"]!! - m["Liabilities"]!!
            }
            return null
        }


        /** TODO: Do to the inconsistency in reporting this number is only used differentially. **/
        private fun getGrossProfit(m:MutableMap<String, Double?>) : Double? {
            if(m["GrossProfit"] != null) return m["GrossProfit"]!!

            val costOfGoodsSold: Double = getCostOfGoodsSold(m) ?: return null
            val revenue: Double = getRevenue(m) ?: return null
            return revenue - costOfGoodsSold
        }

        /** Do to the inconsistency in reporting this number is only used differentially but in some cases
         * to calculate Revenue for the stocks that have not reported it for some reason.
         * . **/
        private fun getCostOfGoodsSold(m:MutableMap<String, Double?>) : Double? {
            return m["CostOfGoodsSold"] ?:  m["CostOfGoodsAndServicesSold"] ?: m["CostOfRevenue"]
        }

        private class FiscalGroup(
            val fiscalYear:   Int?,
            val fiscalPeriod: String?,
            val endDate: Date?
        ) {
            override fun hashCode(): Int {
                return ("${this.fiscalYear}-${this.fiscalPeriod}").hashCode()
            }
            override fun equals(other: Any?) : Boolean {
                val x = other as FiscalGroup
                return x.fiscalPeriod == this.fiscalPeriod &&
                        x.fiscalYear  == this.fiscalYear
            }
        }
        private fun hasUsGaapData(financialData: JSONObject) : Boolean {
            return oTo("us-gaap", oTo("facts", financialData)) != null &&
                    oTo("ifrs-full", oTo("facts", financialData)) == null
        }
        private fun getGaapAttribute(
            key: String,
            usdType: String = "USD",
            financialData: JSONObject,
            attr: MutableMap<FiscalGroup, MutableMap<String,Double?>>
        ) {
            val attributeArray: JSONArray = oTa(
                usdType,
                oTo(
                    "units",
                    oTo(
                        key,
                        oTo(
                            "us-gaap",
                            oTo(
                                "facts", financialData
                            )
                        )
                    )
                )
            )?: return

            for (i in attributeArray.indices) {//TODO: Need a new algorithm that terminates when MAX_NUMBER_QUARTERS is reached per property
                val x: JSONObject = attributeArray[i] as JSONObject
                if ((x["form"] as String) != "10-Q") {
                    continue
                }
                if((x["fy"] as Number?)?.toInt() != null && (x["fy"] as Number).toInt() < MINIMUM_YEAR) {
                    continue
                }

                val fiscalGroup = FiscalGroup(
                    fiscalYear   = (x["fy"] as Number?)?.toInt(),
                    fiscalPeriod = x["fp"] as String?,
                    endDate      = if(x["end"] != null) {SimpleDateFormat("yyyy-MM-dd").parse((x["end"] as String)) } else { null }
                )

                val m: MutableMap<String, Double?> = if (attr[fiscalGroup] != null) {
                    attr[fiscalGroup]!!
                } else {
                    HashMap()
                }
                val value =  (x["val"] as Number?)?.toDouble()
                if (value != null) {
                    m[key] = value
                }
                attr[fiscalGroup] = m
            }
        }

        private fun oTo(name: String, x: JSONObject?): JSONObject? {
            if(x == null) return null
            return x[name] as JSONObject?
        }

        private fun oTa(name: String, x: JSONObject?): JSONArray? {
            if(x == null) return null
            return x[name] as JSONArray?
        }

        @Entity
        @Table(name="sec_ignored_entity")
        class SecIgnoredEntity {
            @Id
            @Column(name="id")
            @GeneratedValue(strategy = GenerationType.IDENTITY)
            private val id : Long = -1L

            val cik: Long?
            val fileName: String?
            constructor(cik: Long?, fileName: String?) {
                this.cik = cik
                this.fileName = fileName
            }
        }

        private fun isCorrectMetaFile(fileName: String) : Boolean {
            try {
                fileNameToCikCode(fileName = fileName)
            } catch(ex:NumberFormatException) {
                return false
            }
            return true
        }

        private fun fileNameToCikCode(fileName: String) : Long {
            return fileName.replace("CIK","").replace(".json","").toLong()
        }

        private fun fileToJson(file: File) : JSONObject {
            return (JSONParser().parse(file.readText()) as JSONObject)
        }
    }
}