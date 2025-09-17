package com.midas.utilities

import org.junit.Assert
import java.lang.reflect.Field
import java.text.SimpleDateFormat
import java.util.*


/**
 * This class is the result of trying to adhere to a stricter approach to value objects
 *
 */
class DomainValueCompareUtil{
    companion object {
        fun equalInValue(object1: Any, object2: Any)  {
            Assert.assertEquals(object1.javaClass, object2.javaClass)

            val map1: MutableMap<String, Any> = HashMap()
            val map2: MutableMap<String, Any> = HashMap()
            val allFields: Array<Field> = object1.javaClass.declaredFields

            for(f in allFields) {
                f.isAccessible = true
                map1[f.name] = f.get(object1)
                map2[f.name] = f.get(object2)
            }

            Assert.assertTrue(allFields.isNotEmpty())
            for(f in allFields) {
                if(f.name == "id") {
                    continue
                }

                if(map1[f.name]!!.javaClass.name == "java.util.Date") {
                    val referenceDate  = map1[f.name] as Date
                    val comparisonDate = map2[f.name] as Date

                    val sdf2 = SimpleDateFormat("yyyy-MM-dd")
                    sdf2.timeZone = TimeZone.getTimeZone("UTC")

                    val sdf1 = SimpleDateFormat("yyyy-MM-dd")
                    sdf1.timeZone = Calendar.getInstance().timeZone//TimeZone.getTimeZone("America/New_York")
                    Assert.assertEquals(sdf1.format(referenceDate), sdf2.format(comparisonDate))

                } else {
                    Assert.assertEquals("Field comparison for ${f.name}",map1[f.name], map2[f.name])
                }
            }
        }
    }
}