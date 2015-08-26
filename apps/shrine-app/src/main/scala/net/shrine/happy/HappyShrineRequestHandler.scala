package net.shrine.happy

/**
 * @author ?? (Bill Simons?)
 * @since ??
 * @see http://cbmi.med.harvard.edu
 * @see http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @see http://www.gnu.org/licenses/lgpl.html
 */
trait HappyShrineRequestHandler {
  def keystoreReport: String

  def routingReport: String

  def hiveReport: String

  def networkReport: String

  def adapterReport: String

  def auditReport: String

  def queryReport: String

  def versionReport: String

  def all: String
}