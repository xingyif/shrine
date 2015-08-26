package net.shrine.happy

import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

/**
 * @author Bill Simons
 * @since 8/10/11
 * @see http://cbmi.med.harvard.edu
 * @see http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @see http://www.gnu.org/licenses/lgpl.html
 */
@Path("/happy")
@Produces(Array(MediaType.APPLICATION_XML))
final class HappyShrineResource(happyService: HappyShrineRequestHandler) {
  @GET
  @Path("keystore")
  def keystoreReport: String = happyService.keystoreReport

  @GET
  @Path("routing")
  def routingReport: String = happyService.routingReport

  @GET
  @Path("hive")
  def hiveReport: String = happyService.hiveReport

  @GET
  @Path("network")
  def networkReport: String = happyService.networkReport

  @GET
  @Path("adapter")
  def adapterReport: String = happyService.adapterReport

  @GET
  @Path("audit")
  def auditReport: String = happyService.auditReport

  @GET
  @Path("queries")
  def queryReport: String = happyService.queryReport

  @GET
  @Path("version")
  def versionReport: String = happyService.versionReport

  @GET
  @Path("all")
  def all: String = happyService.all
}