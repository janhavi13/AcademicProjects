import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import java.io.IOException;
import java.util.*;

/**
 * This reducer creates the matrix M, it receives the incoming contribution from the mapper.
 * It emits the page index as key and null value for a dangling node
 * It emits the inlink index as key and pageindex and its page rank in its value. ****************
 */
public class MatrixCreatingReducer  extends Reducer<Text, Text, Text, Text> {

    public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException
    {
        Text pageIndex = new Text();
        List<String> inlinkList = new ArrayList<String>();
        for (Text value : values)
        {
            String[] inlinkDetails =value.toString().split("~~");
            if (inlinkDetails[0].equals("true"))
            {
                pageIndex = new Text(inlinkDetails[1].toString());
            }
            else
            {
                inlinkList.add(inlinkDetails[1].toString()+":" + inlinkDetails[2]);
            }
        }
        String inlinkIndex="" ;
        String pagerank="" ;
        for(int i = 0; i < inlinkList.size();i++)
        {
            inlinkIndex = inlinkList.get(i).split(":")[0];
            pagerank = inlinkList.get(i).split(":")[1];
            if(! inlinkIndex.equals("-1"))
            context.write(new Text(inlinkIndex.trim()),new Text(pageIndex.toString().trim()+":"+pagerank));
        }
            context.write(new Text(pageIndex.toString().trim()),new Text(""));
    }
}

