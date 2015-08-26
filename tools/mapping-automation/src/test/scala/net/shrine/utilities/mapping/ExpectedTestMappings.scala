package net.shrine.utilities.mapping

import net.shrine.config.mappings.AdapterMappings

/**
 * @author clint
 * @date Jul 17, 2014
 */
object ExpectedTestMappings {
  lazy val pairs: Seq[(String, String)] = Seq(
    """\\i2b2\i2b2\Demographics\Age\0-9 years old\""" -> """\\i2b2\LOCAL\DEM|AGE:0""",
    """\\i2b2\i2b2\Demographics\Age\0-9 years old\""" -> """\\i2b2\LOCAL\DEM|AGE:1""",
    """\\i2b2\i2b2\Demographics\Age\0-9 years old\""" -> """\\i2b2\LOCAL\DEM|AGE:2""",
    """\\i2b2\i2b2\Demographics\Age\0-9 years old\""" -> """\\i2b2\LOCAL\DEM|AGE:3""",
    """\\i2b2\i2b2\Demographics\Age\0-9 years old\""" -> """\\i2b2\LOCAL\DEM|AGE:4""",
    """\\i2b2\i2b2\Demographics\Age\0-9 years old\""" -> """\\i2b2\LOCAL\DEM|AGE:5""",
    """\\i2b2\i2b2\Demographics\Age\0-9 years old\""" -> """\\i2b2\LOCAL\DEM|AGE:6""",
    """\\i2b2\i2b2\Demographics\Age\0-9 years old\""" -> """\\i2b2\LOCAL\DEM|AGE:7""",
    """\\i2b2\i2b2\Demographics\Age\0-9 years old\""" -> """\\i2b2\LOCAL\DEM|AGE:8""",
    """\\i2b2\i2b2\Demographics\Age\0-9 years old\""" -> """\\i2b2\LOCAL\DEM|AGE:9""",
    """\\i2b2\i2b2\Demographics\""" -> """\\i2b2\LOCAL\DEM""")
    
  lazy val mappings = AdapterMappings.empty ++ pairs
}