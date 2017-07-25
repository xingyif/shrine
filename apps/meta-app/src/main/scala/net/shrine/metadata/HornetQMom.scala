/**
  * Created by yifan on 7/24/17.
  */



/**
  * This object mostly imitates AWS SQS' API via an embedded HornetQ. See http://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/examples-sqs.html
  *
  * @author david
  * @since 7/18/17
  */
//todo a better name
//todo split into a trait, this LocalHornetQ, and RemoteHornetQ versions. The server side of RemoteHornetQ will call this local version.
//todo in 1.23 all but the server side will use the client RemoteHornetQ implementation (which will call to the server at the hub)
//todo in 1.24, create an AwsSqs implementation of the trait

trait HornetQMom {
  //  private[mom] def stop()
  //  private def withSession[T](block: ClientSession => T):T // TODO: prob don't need this
  def createQueueIfAbsent(queueName:String):Queue
  def deleteQueue(queueName:String)
  def queues:Seq[Queue]
  def send(contents:String,to:Queue):Unit
  def receive(from:Queue,timeout:Duration):Option[Message]
  def complete(message:Message):Unit

}
