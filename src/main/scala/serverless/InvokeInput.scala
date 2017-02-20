package serverless

trait InvokeInput

case class HttpInvokeInput(headers: Seq[(String, String)] = Seq.empty,
                           pathWithQuerys: Seq[(String, String)] = Seq.empty,
                           parameters: Seq[(String, String)] = Seq.empty,
                           body: Option[Array[Byte]] = None) extends InvokeInput

