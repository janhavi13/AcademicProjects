import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import java.io.IOException;
import java.util.TreeMap;

/**
 * TopKReducer is the reducer for the final phase of the map reduce implementation of page rank algorithm
 * The reducer emits the top 100 pages with page rank
 */
public class TopKReducer extends
        Reducer<NullWritable, Text, NullWritable, Text> {

    private TreeMap<Double, Text> repToRecordMap = new TreeMap<Double, Text>();

    @Override
    public void reduce(NullWritable key, Iterable<Text> values, Context context)
            throws IOException, InterruptedException {

        for (Text value : values) {
            String pageEntry[] = value.toString().split(" ");
            String pageName = pageEntry[0];
            String pageRank = pageEntry[1];
            String page = pageName + " " + pageRank;

            //one or more pages having same page rank
            if(repToRecordMap.containsKey(Double.parseDouble(pageRank)))
            {
                String pageentry = repToRecordMap.get(Double.parseDouble(pageRank)).toString().split(" ")[0];
                repToRecordMap.put(Double.parseDouble(pageRank),new Text(pageentry+","+page));
            }
            else
            {
                repToRecordMap.put(Double.parseDouble(pageRank), new Text(page));
            }

            //get top 100 entries
            if (repToRecordMap.size() > 100) {
                repToRecordMap.remove(repToRecordMap.firstKey());
            }
        }


        for (Text t : repToRecordMap.descendingMap().values()) {
            context.write(NullWritable.get(), t);
        }
    }
}

