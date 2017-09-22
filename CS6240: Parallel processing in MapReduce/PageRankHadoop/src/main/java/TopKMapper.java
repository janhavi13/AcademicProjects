import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import java.io.IOException;
import java.util.TreeMap;

/**
 * TopKMapper is a mapper for the final task of the page rank implementation where we need to emit top 100 pages.
 * that is the pages with highest page rank.
 * The mapper stores the page rank and the page name in a tree map
 * Since the tree map stores it in the ascending order we remove the small page rank entries by using removing key
 * We store only the local top 100 entries for a map function
 * We emit the page rank and the page name from the map
 */
public class TopKMapper extends Mapper<Object, Text, NullWritable, Text> {

    private TreeMap<Double, Text> repToRecordMap = new TreeMap<Double, Text>();
    @Override
        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            String pageEntry[] = value.toString().split(" ### ")[0].split(":");
            String pageName = pageEntry[0];
            String pageRank = pageEntry[1];
            String page = pageName + " " + pageRank;

            //one or more pages having same page rank
           if(repToRecordMap.containsKey(Double.parseDouble(pageRank)))
           {
               String pageentry = repToRecordMap.get(Double.parseDouble(pageRank)).toString().split(" ")[0];
               repToRecordMap.put(Double.parseDouble(pageRank),new Text(pageentry+","+pageName));
           }
           else
           {
               repToRecordMap.put(Double.parseDouble(pageRank), new Text(page));
           }

           // get top 100 records
            if (repToRecordMap.size() > 100) {
                repToRecordMap.remove(repToRecordMap.firstKey());
            }
        }

        @Override
        protected void cleanup(Context context) throws IOException,
                InterruptedException {
            for (Text t : repToRecordMap.values()) {
                context.write(NullWritable.get(), t);
            }
        }
    }


