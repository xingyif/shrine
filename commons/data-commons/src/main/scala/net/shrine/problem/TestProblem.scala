package net.shrine.problem

/**
  * @author david 
  * @since 1.22
  */
case class TestProblem(override val summary: String = "test summary",
                       override val description:String = "test description",
                       override val throwable: Option[Throwable] =  None) extends AbstractProblem(ProblemSources.Unknown) {
  override def timer = 0
  // No point in logging test problems
  override def delayedInit(code: => Unit) = {
    code
  }
}
