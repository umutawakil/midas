package com.midas.utilities

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