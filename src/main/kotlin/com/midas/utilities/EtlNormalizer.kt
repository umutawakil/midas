package com.midas.utilities

class EtlNormalizer {
    fun fuzzyDoubleToDouble(x: Any) : Double {
        if(x.javaClass == Double.javaClass) {
            return x as Double
        }
        if(x.javaClass == Long.javaClass) {
            return (x as Long).toDouble()
        }
        if(x.javaClass == Int.javaClass) {
            return(x as Int).toDouble()
        }
        throw RuntimeException("Non numerical datatype sent for double: ${x.javaClass.name}")
    }
}