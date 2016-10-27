package net.shrine.crypto2

import net.shrine.problem.{AbstractProblem, ProblemSources}

/**
  * Created by ty on 10/27/16.
  */
case class ImproperlyConfiguredKeyStoreProblem(override val throwable: Option[Throwable], override val description: String)
  extends AbstractProblem(ProblemSources.Commons)
{
  override val summary: String = "There is a problem with how the KeyStore has been configured"
}