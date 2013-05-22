package si.zitnik.research.interestmining.parse.yahoo

import si.zitnik.research.interestmining.writer.db.DBWriter
import java.io.{FileReader, BufferedReader}
import xml.XML
import si.zitnik.research.interestmining.model.stackoverflow.{User, Post}

/**
 * Created with IntelliJ IDEA.
 * User: slavkoz
 * Date: 5/22/13
 * Time: 6:05 PM
 * To change this template use File | Settings | File Templates.
 */
class L6Parser(filename: String) {
  private val writer = DBWriter.instance()

  def readDocument(reader: BufferedReader): String = {
    var text = ""
    var line = reader.readLine()
    var inside = false
    var end = false

    while (!end && line != null) {
      if (line.startsWith("<vespaadd><document")) {
        inside = true
      }

      if (inside) {
        text += " " + line
      }

      if (line.startsWith("</document></vespaadd>")) {
        inside = false
        end = true
      } else {
        line = reader.readLine()
      }
    }

    text
  }

  def parseDocument(document: String) {
    val xmlDoc = XML.loadString(document.toString) \\ "document"

    //only if qlang == "en"
    if ((xmlDoc \ "qlang").text.equals("en")) {
      //question (id, subject, content, answercount, id of user having question)
      val question = new Post(
        (xmlDoc \ "uri").text,
        1,
        "",
        (xmlDoc \ "uri").text+"a",
        "",
        0,
        0,
        (xmlDoc \ "content").text,
        (xmlDoc \ "id").text,
        (xmlDoc \ "subject").text,
        "",
        (xmlDoc \ "nbestanswers").size,
        0,
        0
      )

      //best answer (id = question_id+"a", content, id of answerer)
      val answer = new Post(
        (xmlDoc \ "uri").text+"a",
        2,
        (xmlDoc \ "uri").text,
        "",
        "",
        0,
        0,
        (xmlDoc \ "bestanswer").text,
        (xmlDoc \ "best_id").text,
        "",
        "",
        0,
        0,
        0
      )

      //categories: cat, maincat, subcat
      val questioneer = new User((xmlDoc \ "id").text)
      questioneer.categories.put((xmlDoc \ "maincat").text,1)

      val answerer = new User((xmlDoc \ "best_id").text)
      answerer.categories.put((xmlDoc \ "maincat").text,1)

      writer.insert(question.toSql())
      writer.insert(answer.toSql())
      writer.insertOrUpdateCategory(questioneer)
      writer.insertOrUpdateCategory(answerer)
    }


  }

  def parse(maxToParse: Int = Int.MaxValue) {
    val reader = new BufferedReader(new FileReader(filename))
    var counter = 0

    var  curText = readDocument(reader)
    while (curText != null && counter < maxToParse) {
      parseDocument(curText)
      counter += 1
      if (counter % 100 == 0) {
        println(counter)
        writer.commit()
      }
      curText = readDocument(reader)
    }
  }
}
