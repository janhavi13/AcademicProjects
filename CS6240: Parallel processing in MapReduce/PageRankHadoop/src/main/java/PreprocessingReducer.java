import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import java.io.IOException;

/**
 * The PreprocessingReducer is a reducer for the parser phase. The reducer emits the adjacency list which is a
 * list of outlinks for every page (Node)
 */
public class PreprocessingReducer extends Reducer<Text, Node, Text, Node> {
    public void reduce(Text key, Iterable<Node> values, Context context)
            throws IOException, InterruptedException {
        Node n = new Node();
        for(Node value : values)
        {
            if(value.getAdjacentNodeNames().size()>0)
            {
                n.setAdjacentNodeNames(value.getAdjacentNodeNames());
            }
        }
        context.write(key, n);
    }
}
