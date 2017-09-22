import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import java.io.IOException;
import java.util.*;

/**
 * This reducer creates the matrix M, it receives the incoming contribution from the mapper.
 * It emits the page index as key and its inlink(inlink index , inlink_contribution) as value
 */
public class MatrixCreatingReducer  extends Reducer<Text, Text, Text, Text> {

    public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {

        Text pageIndex = new Text();
        List<String> inlinkList = new ArrayList<String>();
        for (Text value : values)
        {
            String[] inlinkDetails =value.toString().split("~~");
            //if it is a pageitself we have true set in isNode
            if (inlinkDetails[0].equals("true"))
            {
                pageIndex = new Text(inlinkDetails[1].toString());
            }
            // add the inlink page contribution and column
            else
            {
                inlinkList.add(inlinkDetails[1].toString()+":" + inlinkDetails[2]);
            }
        }
        // eg: A is page and B is the inlink
        // emit(A_index , (B_index : B_contributiontoA))
        context.write(pageIndex,new Text(inlinkList.toString()));
    }
}

