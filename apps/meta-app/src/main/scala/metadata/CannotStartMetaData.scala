package metadata

import net.shrine.problem.{AbstractProblem, ProblemSources}

/**
  * Created by ty on 11/8/16.
  */
case class CannotStartMetaData(ex:Throwable) extends AbstractProblem(ProblemSources.Dsa) {
  override def summary: String = "The MetaData API could not start due to an exception."

  override def description: String = s"The MetaData API could not start due to ${throwable.get}"

  override def throwable = Some(ex)
}
