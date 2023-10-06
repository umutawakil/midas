package com.midas.utilities

class Etl {
    companion object {
        fun double(n: Any?) = (n!! as Number).toDouble()

        fun doubleN(n: Any?) = (n as Number?)?.toDouble()

        fun integer(n: Any?) = (n!! as Number).toInt()
    }
}