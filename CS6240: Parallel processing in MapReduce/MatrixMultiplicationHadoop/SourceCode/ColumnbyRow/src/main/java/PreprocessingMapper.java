import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.*;
import java.io.IOException;
import java.util.*;

/**
 * This is a mapper with a map function which is executed to parse the bz2 files and generate all the page names along
 * with its outlinks
 * The map function emits the page name and the list of outlinks separated by a ":"
 * We need to maintain a record of all the dangling nodes which are non - wiki pages but are outlinks for at least one
 * of the page as well. So for such a scenario we emit the page name in the outlinks as well
 */
public class PreprocessingMapper extends Mapper<Object, Text, Text, Node>{

    public void map(Object key, Text value, Context context) throws IOException, InterruptedException {

        Bz2WikiParser parser = new Bz2WikiParser();
        HashSet<String> adjList = new HashSet<>();
        adjList =  parser.parserToken(value.toString());
        if(adjList !=null)
        {
            Text pageName = new Text(value.toString().split(":")[0]);
            Node node = new Node();
            node.setAdjacentNodeNames(new ArrayList<String>(adjList));
            context.write(pageName, node);
            for(String s : adjList)
            {
               context.write(new Text(s) , new Node());
            }
        }
    }

}


