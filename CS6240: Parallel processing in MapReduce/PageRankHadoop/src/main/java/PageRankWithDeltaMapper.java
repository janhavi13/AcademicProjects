import org.apache.hadoop.io.BooleanWritable;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import java.io.IOException;
import java.util.*;

/**
 * The PageRankWithDeltaMapper is a mapper which has a map function which is executed in the all iterations except the first to
 * calculate the page rank.
 * The mapper emits every page as a node which may or may not have outlinks.(Creates the graph structure to pass throughout)
 * The map function sets the intial page rank as that it had from the previous iteration to each node
 * If a page does not have even a single outlink then it is considered as a dangling node and we simply accumulate
 * the pageranks of such nodes.(This is the total delta)
 * If it has outlinks then we calculate the pagerank of every outlink by dividing the pagerank of that node by total outlinks
 */
public class PageRankWithDeltaMapper extends Mapper<Text, Text, Text, Node> {


    public void map(Text key, Text value, Context context
    ) throws IOException, InterruptedException {

        String[] adjLinkNodes = value.toString().split(" ### ");
        List<String> aLinkNodes = new ArrayList<String>();
        double ownPageRank = Double.parseDouble(adjLinkNodes[0]);
        Node node;
        node = new Node();
        node.setPageRank(new DoubleWritable(ownPageRank));
        node.setIsNode(new BooleanWritable(true));

        //atleast 1 outlink
        if(adjLinkNodes.length>1)
        {
            aLinkNodes = (List)Arrays.asList(adjLinkNodes[1].split(" "));
            node.setAdjacentNodeNames(aLinkNodes);
        }
        context.write(new Text(key.toString().trim()), node);

        // dangling node then accumulate pagerank
        if(aLinkNodes.size() == 0)
        {
            context.getCounter(PageRankDriver.Counter.delta).increment((long)(ownPageRank * Math.pow(10,15)));
        }

        // calculate the contribution of every outlink for a node which is not a dangling node
        else
        {
            DoubleWritable newOutlinkWeight = new DoubleWritable(ownPageRank/aLinkNodes.size());
            Node n = new Node();
            n.setPageRank(newOutlinkWeight);
            n.setIsNode(new BooleanWritable(false));
            if(adjLinkNodes.length>1)
            {
                for(String name : aLinkNodes)
                {
                    context.write(new Text(name.trim()), n);
                }
            }
        }
    }
}





