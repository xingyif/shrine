package net.shrine.utilities.scanner

import net.shrine.protocol.query.Expression
import scala.concurrent.Future
import net.shrine.protocol.query.QueryDefinition
import net.shrine.protocol.query.Term
import net.shrine.protocol.QueryResult
import net.shrine.authentication.Authenticator
import net.shrine.authentication.AuthenticationResult
import net.shrine.protocol.AuthenticationInfo
import net.shrine.protocol.Credential
import net.shrine.authentication.NotAuthenticatedException
import javax.xml.datatype.XMLGregorianCalendar
import net.shrine.util.XmlDateHelper

/**
 * @author clint
 * @date Mar 12, 2013
 */
trait ScannerClient { 
  def query(term: String): Future[TermResult]
  
  def retrieveResults(termResult: TermResult): Future[TermResult]
  
  val authenticator: Authenticator
  
  val authn: AuthenticationInfo
  
  private lazy val authenticationResult: AuthenticationResult = authenticator.authenticate(authn)
  
  def afterAuthenticating[T](f: AuthenticationResult.Authenticated => T): T = {
    import AuthenticationResult._
    
    authenticationResult match {
      case a: Authenticated => f(a)
      case na:NotAuthenticated => throw NotAuthenticatedException(na)
    }
  }
}

object ScannerClient {
  private[scanner] def makeQueryName(timestamp: XMLGregorianCalendar, term: String): String = {
    s"$timestamp - $term"
  }
  
  def toQueryDef(term: String): QueryDefinition = {
    import XmlDateHelper.now
    
    QueryDefinition(makeQueryName(now, term), Term(term))
  }
  
  def errorTermResult(networkQueryId: Long, term: String): TermResult = TermResult(networkQueryId, -1L, term, QueryResult.StatusType.Error, -1L)
}