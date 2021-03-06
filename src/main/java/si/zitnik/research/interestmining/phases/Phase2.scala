package si.zitnik.research.interestmining.phases

import si.zitnik.research.interestmining.textprocessors.AIOProcessor
import si.zitnik.research.interestmining.util.json.JSONObject
import si.zitnik.research.interestmining.writer.db.DBWriter
import org.jsoup.Jsoup
import scala.collection.JavaConversions._
import com.typesafe.scalalogging.slf4j.Logging

/**
 * Created with IntelliJ IDEA.
 * User: slavkoz
 * Date: 6/12/13
 * Time: 3:39 PM
 * To change this template use File | Settings | File Templates.
 */
object Phase2 extends Logging {

  def process(html: Boolean = true) {
    var start = 0
    val batchSize = 1000

    //stackoverflow: 2012348
    //val end = 2012348
    val end = DBWriter.instance().getAllPostsIdsNum()

    logger.info("All questions: %d".format(end))

    while (start <= end) {
      logger.info("Processed: %d/%d".format(start, end))
      val resultSet = DBWriter.instance().getAllPostsIds(start, batchSize)
      while (resultSet.next()) {
        var postId = resultSet.getString(1)
        var postBody = resultSet.getString(2)
        var postTitle = resultSet.getString("title")

        //DO FOR BODY
        //1. get post
        var text = postBody.toLowerCase
        if (html) {
          text = Jsoup.parse(text).getElementsByTag("p").map(_.text()).mkString(" ")
        }


        //2. tokenize, remove stopwords, lemmatize, tolower
        val processedWords = AIOProcessor.process(text)

        //3. insert/update wordToDocFreq
        val wordsSet = processedWords.toSet
        DBWriter.instance().updateWordToDocFreqs(wordsSet)

        //4. insert tf (SemSim table)
        val counts = processedWords.groupBy(x=>x).mapValues(x=>x.length)
        val tfs = new JSONObject()
        wordsSet.foreach(word => {
          tfs.put(word, counts(word))
        })
        DBWriter.instance().insertIntoSemSim(postId, processedWords.size, tfs.toString)


        //DO FOR TITLE
        //1. get post
        var title = postTitle

        //2. tokenize, remove stopwords, lemmatize, tolower
        val processedWordsTitle = AIOProcessor.process(title)

        //3. insert/update wordToDocFreq
        val wordsSetTitle = processedWordsTitle.toSet
        DBWriter.instance().updateWordToDocFreqsTitle(wordsSetTitle)

        //4. insert tf (SemSim table)
        val countsTitle = processedWordsTitle.groupBy(x=>x).mapValues(x=>x.length)
        val tfsTitle = new JSONObject()
        wordsSetTitle.foreach(word => {
          tfsTitle.put(word, countsTitle(word))
        })
        DBWriter.instance().insertIntoSemSimTitle(postId, processedWordsTitle.size, tfsTitle.toString)
      }

      start += batchSize

      if (start % 10000 == 0) {
        DBWriter.reinit()
      }
    }

  }

}
