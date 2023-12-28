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
    private val cik                         : Long
    private val ticker                      : String
    private val fiscalYear                  : Int?
    private val fiscalPeriod                : String?

    private val netIncome                   : Double?
    private val revenue                     : Double?
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
        secSectorCode: Int?,
        cik: Long,
        netIncome: Double?,
        revenue: Double?,
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
        this.ticker                      = ticker
        this.secSectorCode               = secSectorCode
        this.cik                         = cik
        this.fiscalYear                  = fiscalYear
        this.fiscalPeriod                = fiscalPeriod

        this.netIncome                   = netIncome
        this.revenue                     = revenue
        this.epsBasic                    = epsBasic
        this.epsDiluted                  = epsDiluted
        this.sharesOutstanding           = sharesOutstanding

        this.totalAssets                 = totalAssets
        this.totalCurrentAssets          = totalCurrentAssets
        this.totalCash                   = totalCash
        this.cashAndCashEquivalents      = cashAndCashEquivalents
        this.totalCurrentLiabilities     = totalCurrentLiabilities
        this.totalLiabilities            = totalLiabilities
        this.totalEquity                 = totalEquity
        this.workingCapital              = workingCapital

        this.operatingCashFlow           = operatingCashFlow
        this.investingCashFlow           = investingCashFlow
        this.financingCashFlow           = financingCashFlow
        this.netChangeInCash             = netChangeInCash
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

        private const val MINIMUM_YEAR = 2023

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

            for(s in Files.list(Paths.get("${applicationProperties.financialsDirectory}/submissions")).toList()) {
                val fileName = s.fileName.name//"CIK0001867066.json"//
                if ((!ignoreEntityMapByName.contains(fileName)) && isCorrectMetaFile(fileName)) {
                    val cik = fileNameToCikCode(fileName = fileName)
                    if(!ignoreEntityMap.contains(cik)) {
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
            if (!isUsGaapData(financialData = financialData)) {
                return
            }

            val attr: MutableMap<FiscalGroup, MutableMap<String,Double?>> = HashMap()

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
            for(i in attributeArray.indices) {
                val x: JSONObject = attributeArray[i] as JSONObject
                if ((x["form"] as String) != "10-Q") {
                    continue
                }
                if((x["fy"] as Number?)?.toInt() != null && (x["fy"] as Number).toInt() < MINIMUM_YEAR) {
                    continue
                }

                val fiscalGroup = FiscalGroup(
                    fiscalYear   = (x["fy"] as Number?)?.toInt(),
                    fiscalPeriod = x["fp"] as String?
                )
                val m: MutableMap<String, Double?> = if(attr.containsKey(fiscalGroup)) { attr[fiscalGroup]!! } else { HashMap() }
                val value = (x["val"] as Number?)?.toDouble()
                if(value != null) {
                    m["EntityCommonStockSharesOutstanding"] = value
                }
                attr[fiscalGroup] = m
            }

            getGaapAttribute(key = "Revenues", financialData = financialData, attr = attr)
            getGaapAttribute(key = "RevenueFromContractWithCustomerExcludingAssessedTax", financialData = financialData, attr = attr)
            getGaapAttribute(key = "NetIncomeLoss", financialData = financialData, attr = attr)
            getGaapAttribute(key = "EarningsPerShareDiluted", usdType = "USD/shares", financialData = financialData, attr = attr)
            getGaapAttribute(key = "EarningsPerShareBasic", usdType = "USD/shares", financialData = financialData, attr = attr)
            getGaapAttribute(key = "ProfitLoss", financialData = financialData, attr = attr)

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
            
            for (fiscalGroup in attr.keys) {
                val m: MutableMap<String, Double?> = attr[fiscalGroup]!!

                try {
                    financialsRepository.save(
                        Financials(
                            ticker                      = ticker,
                            secSectorCode               = if (metaData["sic"].toString().isNotEmpty()) { metaData["sic"].toString().substring(0,3).toInt()} else { null},
                            cik                         = (metaData["cik"] as String).toLong(),

                            netIncome                   = getNetIncome(m),
                            revenue                     = getRevenue(m),
                            epsBasic                    = m["EarningsPerShareBasic"],
                            epsDiluted                  = m["EarningsPerShareDiluted"],
                            sharesOutstanding           = m["EntityCommonStockSharesOutstanding"]?.toLong(),

                            totalAssets                 = m["Assets"] ?:  m["LiabilitiesAndStockholdersEquity"],
                            totalCurrentAssets          = m["AssetsCurrent"],
                            totalCash                   = m["Cash"],
                            cashAndCashEquivalents      = m["CashAndCashEquivalentsAtCarryingValue"],
                            totalCurrentLiabilities     = m["LiabilitiesCurrent"],
                            totalLiabilities            = m["Liabilities"] ?: if (m["StockholdersEquity"] != null && m["LiabilitiesAndStockholdersEquity"] != null) { m["LiabilitiesAndStockholdersEquity"]!! - m["StockholdersEquity"]!!} else { null },
                            totalEquity                 = m["StockholdersEquity"] ?: if (m["Liabilities"] != null && m["LiabilitiesAndStockholdersEquity"] != null) { m["LiabilitiesAndStockholdersEquity"]!! - m["Liabilities"]!!} else { null },

                            operatingCashFlow           = m["NetCashProvidedByUsedInOperatingActivities"],
                            investingCashFlow           = m["NetCashProvidedByUsedInInvestingActivities"],
                            financingCashFlow           = m["NetCashProvidedByUsedInFinancingActivities"],
                            netChangeInCash             = if(m["NetCashProvidedByUsedInOperatingActivities"] != null && m["NetCashProvidedByUsedInInvestingActivities"] != null && m["NetCashProvidedByUsedInFinancingActivities"] != null) { m["NetCashProvidedByUsedInOperatingActivities"]!! + m["NetCashProvidedByUsedInInvestingActivities"]!! + m["NetCashProvidedByUsedInFinancingActivities"]!! } else { null },
                            workingCapital              = if(m["AssetsCurrent"] != null && m["LiabilitiesCurrent"] != null) { m["AssetsCurrent"]!! - m["LiabilitiesCurrent"]!! } else { null },
                            fiscalYear                  = fiscalGroup.fiscalYear,
                            fiscalPeriod                = fiscalGroup.fiscalPeriod
                        )
                    )
                } catch (e: Exception){
                    e.printStackTrace()
                    throw RuntimeException(e)
                }
            }
        }

        private fun getNetIncome(m: MutableMap<String, Double?> ) : Double? {
            return m["NetIncomeLoss"] ?: m["ProfitLoss"]
        }

        private fun getRevenue(m:MutableMap<String, Double?>) : Double? {
            return m["Revenues"] ?: m["RevenueFromContractWithCustomerExcludingAssessedTax"]
        }

        private class FiscalGroup(
            val fiscalYear:   Int?,
            val fiscalPeriod: String?
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
        private fun isUsGaapData(financialData: JSONObject) : Boolean {
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

            for (i in attributeArray.indices) {
                val x: JSONObject = attributeArray[i] as JSONObject
                if ((x["form"] as String) != "10-Q") {
                    continue
                }
                if((x["fy"] as Number?)?.toInt() != null && (x["fy"] as Number).toInt() < MINIMUM_YEAR) {
                    continue
                }
                val fiscalGroup = FiscalGroup(
                    fiscalYear   = (x["fy"] as Number?)?.toInt(),
                    fiscalPeriod = x["fp"] as String?
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