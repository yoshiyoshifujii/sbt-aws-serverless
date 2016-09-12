package com.github.yoshiyoshifujii

package object cliformatter {

  case class TableDefinition(name: String, size: Int) {
    lazy val template = s"""%-${size}s"""
  }

  case class TableDefinitions(title: String, list: Seq[TableDefinition]) {
    lazy val template = list.foldLeft("|")((b, d) => s"$b ${d.template} |")

    lazy val header = template.format(list.map(_.name): _*)

    lazy val tableSize = list.foldLeft(1)((b, d) => b + d.size + 3)

    private lazy val line = (v: String) => (s: Int) => (for { i <- 1 to s } yield v).mkString("")

    lazy val ====== = line("=")(tableSize)

    lazy val ------ = list.foldLeft("|")((b, d) => s"$b${line("-")(d.size+2)}|")

    lazy val printBody = (body: Seq[Product]) => body.map(b => template.format(b.productIterator.toSeq: _*))

    lazy val printSeq = (body: Seq[Product]) => Seq(
      ======,
      title,
      ======,
      header,
      ------) ++
      printBody(body)

    lazy val print = (body: Seq[Product]) => printSeq(body).mkString("\n")
  }

  case class CliFormatter(tableDefinitions: TableDefinitions) {

    def print1(body: Tuple1[String]*) = tableDefinitions.print(body)
    def print2(body: (String, String)*) = tableDefinitions.print(body)
    def print3(body: (String, String, String)*) = tableDefinitions.print(body)
    def print4(body: (String, String, String, String)*) = tableDefinitions.print(body)
    def print5(body: (String, String, String, String, String)*) = tableDefinitions.print(body)
    def print6(body: (String, String, String, String, String, String)*) = tableDefinitions.print(body)

  }

  object CliFormatter {
    def apply(title: String, definitions: (String, Int)*): CliFormatter =
      new CliFormatter(
        TableDefinitions(
          title, definitions.map(d => TableDefinition(d._1, d._2))
        )
      )
  }
}
