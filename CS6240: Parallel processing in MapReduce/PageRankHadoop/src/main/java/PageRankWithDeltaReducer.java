import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;

/**
 * The PageRankWithDeltaReducer is a reducer called for iteration 1.
 * This reducer basically calculates the page rank of a page based on the pagerank it receives from inlink and that it distributes
 * Since this reducer is only for the iterations other than the first, we calculate the page rank with the delta
 * contribution of the dangling nodes divided by the total number of pages
 */
public class PageRankWithDeltaReducer  extends Reducer<Text, Node, Text, Node> {

    public void reduce(Text key, Iterable<Node> values, Context context)
            throws IOException, InterruptedException {

        Node node = new Node();
        double pageRankOfNode = 0;
        double alpha = 0.15;
        double totalPageCount = Double.parseDouble(context.getConfiguration().get("totalPages"));

        for(Node value : values) {
            //if its a node then receive the node
            if (value.getIsNode().get())
            {
                node.setPageRank(value.getPageRank());
                node.setAdjacentNodeNames(value.getAdjacentNodeNames());
                node.setIsNode(value.getIsNode());
            }
            // accumulate the page rank
            else
            {
                DoubleWritable pageRank = (DoubleWritable) value.getPageRank();
                pageRankOfNode += pageRank.get();
            }
        }

        //page rank calculation
        double finalPageRank = 0;
        finalPageRank = alpha/totalPageCount +(1-alpha)*(pageRankOfNode + (context.getConfiguration().getLong("delta", 0)/Math.pow(10,15))/totalPageCount);
        node.setPageRank(new DoubleWritable(finalPageRank));
        context.write(key, node);

    }

}
