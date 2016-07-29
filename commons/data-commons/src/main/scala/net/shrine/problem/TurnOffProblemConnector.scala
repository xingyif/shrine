package net.shrine.problem

/**
  * Created by ty on 7/29/16.
  * Turns off the problem database connector, so tests don't have to run a config file.
  */
trait TurnOffProblemConnector {
  ProblemConfigSource.turnOffConnector = true
}
