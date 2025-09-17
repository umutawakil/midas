package com.midas.utilities

/**
 * This utility class exists because of some inconsistencies in who data types are enforced
 * in the federal EDGAR data.
 */

class Etl {
    companion object {
        fun double(n: Any?) = (n!! as Number).toDouble()

        fun doubleS(n: Any?) : Double {
            if ((n as String) == "None") {
                return 0.0
            }
            return (n).toDouble()
        }

        fun doubleN(n: Any?) = (n as Number?)?.toDouble()

        fun integer(n: Any?) = (n!! as Number).toInt()
    }
}