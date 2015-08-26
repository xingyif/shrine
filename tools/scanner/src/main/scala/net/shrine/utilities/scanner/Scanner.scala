package net.shrine.utilities.scanner

import net.shrine.log.Loggable

import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.concurrent.duration.DurationInt
import scala.util.control.NonFatal

import net.shrine.config.mappings.AdapterMappingsSource
import net.shrine.ont.data.OntologyDao
import net.shrine.protocol.ResultOutputType

/**
 * @author clint
 * @date Mar 5, 2013
 */
final class Scanner(
    val maxTimeToWaitForResult: Duration,
    val reScanTimeout: Duration,
    val adapterMappingsSource: AdapterMappingsSource,
    val ontologyDao: OntologyDao,
    val client: ScannerClient) extends Loggable {

  def scan(): ScanResults = doScan()

  protected def doScan(): ScanResults = {
    info("Shrine Scanner starting")
    
    val mappedNetworkTerms = adapterMappingsSource.load.get.networkTerms

    def allShrineOntologyTerms = ontologyDao.ontologyEntries.map(_.path).toSet

    val termsExpectedToBeUnmapped = allShrineOntologyTerms -- mappedNetworkTerms

    info(s"We expect ${mappedNetworkTerms.size} to be mapped, and ${termsExpectedToBeUnmapped.size} to be unmapped.")

    doScan(mappedNetworkTerms, termsExpectedToBeUnmapped)
  }

  import scala.concurrent.duration._

  private def obtainVia[T](get: T => Future[TermResult]): T => ScanQueryResult = {
    input =>
      try {
        //TODO: Evaluate, possibly don't block?
        val result = Await.result(get(input), maxTimeToWaitForResult)
        
        debug(s"Status ${result.status} received, queried for $input")
        
        result
      } catch {
        case NonFatal(e) => {
          warn(s"Error obtaining results for input $input: ", e)

          QueryFailure(input, e)
        }
      }
  }

  private def obtainResultsAndFailures[T](terms: Iterable[T], queryFor: T => ScanQueryResult): (Iterable[TermResult], Iterable[QueryFailure[T]]) = {
    val results = terms.map(queryFor)
    
    (results.collect { case t: TermResult => t }, results.collect { case f: QueryFailure[T] => f })
  }

  private def doScan(mappedNetworkTerms: Set[String], termsExpectedToBeUnmapped: Set[String]): ScanResults = {

    val queryFor = obtainVia(client.query)
    
    val (resultsForMappedTerms, failuresForMappedTerms) = obtainResultsAndFailures(mappedNetworkTerms, queryFor)

    val (resultsForUnMappedTerms, failuresForUnMappedTerms) = obtainResultsAndFailures(termsExpectedToBeUnmapped, queryFor)

    val (finishedAndShouldHaveBeenMapped, didntFinishAndShouldHaveBeenMapped) = resultsForMappedTerms.partition(_.status.isDone)

    val (finishedAndShouldNotHaveBeenMapped, didntFinishAndShouldNotHaveBeenMapped) = resultsForUnMappedTerms.partition(_.status.isDone)

    //Terms that we expected to BE mapped, but were NOT mapped
    val shouldHaveBeenMapped = finishedAndShouldHaveBeenMapped.filter(_.status.isError)

    //Terms that we expected to NOT be mapped, but ARE mapped
    val shouldNotHaveBeenMapped = finishedAndShouldNotHaveBeenMapped.filterNot(_.status.isError)

    val reScanResults = reScan(didntFinishAndShouldHaveBeenMapped, didntFinishAndShouldNotHaveBeenMapped)

    val finalSouldHaveBeenMappedSet = toTermSet(shouldHaveBeenMapped) ++ reScanResults.shouldHaveBeenMapped

    val finalSouldNotHaveBeenMappedSet = toTermSet(shouldNotHaveBeenMapped) ++ reScanResults.shouldNotHaveBeenMapped

    val failed = toTermSet(failuresForMappedTerms ++ failuresForUnMappedTerms) ++ reScanResults.failed
    
    //Split query results into those that completed on the first try, and those that didn't
    ScanResults(finalSouldHaveBeenMappedSet, finalSouldNotHaveBeenMappedSet, reScanResults.neverFinished, failed)
  }

  private def reScan(neverFinishedShouldHaveBeenMapped: Iterable[TermResult], neverFinishedShouldNotHaveBeenMapped: Iterable[TermResult]): ScanResults = {
    if (neverFinishedShouldHaveBeenMapped.isEmpty && neverFinishedShouldNotHaveBeenMapped.isEmpty) { ScanResults.empty }
    else {
      val total = neverFinishedShouldHaveBeenMapped.size + neverFinishedShouldNotHaveBeenMapped.size

      info(s"Sleeping for ${reScanTimeout} before retreiving results for $total incomplete queries...")

      Thread.sleep(reScanTimeout.toMillis)

      val retrieve = obtainVia(client.retrieveResults)

      val (neverFinishedShouldHaveBeenMappedRetries, failedShouldHaveBeenMappedRetries) = obtainResultsAndFailures(neverFinishedShouldHaveBeenMapped, retrieve)

      val (neverFinishedShouldNotHaveBeenMappedRetries, failedShouldNotHaveBeenMappedRetries) = obtainResultsAndFailures(neverFinishedShouldNotHaveBeenMapped, retrieve)

      val (doneShouldHaveBeenMapped, stillNotFinishedShouldHaveBeenMapped) = neverFinishedShouldHaveBeenMappedRetries.partition(_.status.isDone)

      val (doneShouldNotHaveBeenMapped, stillNotFinishedShouldNotHaveBeenMapped) = neverFinishedShouldNotHaveBeenMappedRetries.partition(_.status.isDone)

      val shouldHaveBeenMapped = doneShouldHaveBeenMapped.filter(_.status.isError)

      val shouldNotHaveBeenMapped = doneShouldNotHaveBeenMapped.filterNot(_.status.isError)

      val stillNotFinished = stillNotFinishedShouldHaveBeenMapped ++ stillNotFinishedShouldNotHaveBeenMapped
      
      val failed = failedShouldHaveBeenMappedRetries ++ failedShouldNotHaveBeenMappedRetries

      ScanResults(toTermSet(shouldHaveBeenMapped), toTermSet(shouldNotHaveBeenMapped), toTermSet(stillNotFinished), toTermSet(failed))
    }
  }

  private def toTermSet(results: Iterable[TermResult]): Set[String] = results.map(_.term).toSet
  
  import Scanner.Termable
  
  private def toTermSet[T : Termable](results: Iterable[QueryFailure[T]]): Set[String] = results.map(implicitly[Termable[T]].getTerm).toSet
}

object Scanner {
  private[scanner] trait Termable[T] {
    def getTerm(t: T): String
    
    def getTerm(t: QueryFailure[T]): String
  }
  
  private[scanner] object Termable {
    implicit val stringIsTermable: Termable[String] = new Termable[String] {
      override def getTerm(t: String) = t
      
      override def getTerm(f: QueryFailure[String]) = f.input
    }
    
    implicit val termResultIsTermable: Termable[TermResult] = new Termable[TermResult] {
      override def getTerm(t: TermResult) = t.term
      
      override def getTerm(f: QueryFailure[TermResult]) = f.input.term
    }
  }
  
  final object QueryDefaults {
    val topicId = "Scanner Util - Unknown Topic ID" //???
    val outputTypes = Set(ResultOutputType.PATIENT_COUNT_XML)
  }
}