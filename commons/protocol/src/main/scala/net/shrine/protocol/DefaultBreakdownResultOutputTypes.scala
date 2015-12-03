package net.shrine.protocol

/**
 * @author clint
 * @date Oct 24, 2014
 */
object DefaultBreakdownResultOutputTypes {
  import ResultOutputType.I2b2Options
  
  //TODO: What should the ids be here?
  val PATIENT_AGE_COUNT_XML = ResultOutputType("PATIENT_AGE_COUNT_XML", true, I2b2Options("Age patient breakdown"), None)
  val PATIENT_RACE_COUNT_XML = ResultOutputType("PATIENT_RACE_COUNT_XML", true, I2b2Options("Race patient breakdown"), None)
  val PATIENT_VITALSTATUS_COUNT_XML = ResultOutputType("PATIENT_VITALSTATUS_COUNT_XML", true, I2b2Options("Vital Status patient breakdown"), None)
  val PATIENT_GENDER_COUNT_XML = ResultOutputType("PATIENT_GENDER_COUNT_XML", true, I2b2Options("Gender patient breakdown"), None)
  
  lazy val values: Seq[ResultOutputType] = Seq(
      PATIENT_AGE_COUNT_XML,
      PATIENT_RACE_COUNT_XML,
      PATIENT_VITALSTATUS_COUNT_XML,
      PATIENT_GENDER_COUNT_XML)
      
  lazy val toSet: Set[ResultOutputType] = values.toSet
}