import org.apache.spark.{SparkConf, SparkContext}

import scala.collection.JavaConverters._

object PageRank{

  def main(args: Array[String]) {

    val conf = new SparkConf().setAppName("PageRank")
    val sc = new SparkContext(conf)
    // creates RDD
    val input = sc.textFile(args(0))

    // Parse each line of input and eliminate faulty error pages with null output
    val parsingRDD = for (line <- input; temp = Bz2WikiParser.parserToken(line); if (temp != null)) yield (line.split(':')(0), temp.asScala)

    val intermediateRDD = for (page  <- parsingRDD; adjNode  <- page._2) yield (adjNode , scala.collection.mutable.Set[String]())

    val parsedRDD = parsingRDD.union(intermediateRDD).reduceByKey((accum, adjList) => accum ++ adjList)

    val initialPR = 1.0/parsedRDD.count
    val pageCount = parsedRDD.count
    val alpha = 0.15

    // This has the entire set of pages with page rank and outlinks. This is updated in every iteration with new page ranks
    var graphRDD = parsedRDD.map(page => (page._1 -> (initialPR, page._2))).persist()

    // 10 iterations to calculate Page Rank. Every iteration updates the page rank of each page based on the delta contribution
    for (itr <- 1 to 10) {
      //This is a shared variable used by all tasks which accumulates the delta contribution
      var deltaContribution= graphRDD.filter(page => page._2._2.isEmpty).aggregate(0.0)((pr,obj) => pr+obj._2._1, (x,y) => x + y)
      var PageRankRDD = graphRDD.filter(page => ! page._2._2.isEmpty).values.flatMap {
        case (pagerank, adjlist) => {
          adjlist.map(outlink => outlink -> (pagerank / adjlist.size))
        }
      }.reduceByKey(_+_)

      val dummyRDD = graphRDD.subtractByKey(PageRankRDD)

      val danglingRDD = dummyRDD.map(page => page._1 -> (0.0))

      val recreategraphRDD = PageRankRDD.union(danglingRDD)

      val finalPageRankRDD = graphRDD.join(recreategraphRDD)
        .map(page => (page._1 -> ((alpha/pageCount + (1- alpha) * (page._2._2 + deltaContribution/pageCount)), page._2._1._2)))

      graphRDD = finalPageRankRDD.persist()
    }

    // Sort the pages in decreasing value of page rank and fetch the top 100
    val Top100PagesRDD = graphRDD.sortBy(_._2._1, false).take(100).map(page => (page._1, page._2._1))
    sc.parallelize(Top100PagesRDD).coalesce(1).saveAsTextFile(args(1))
  }
}

