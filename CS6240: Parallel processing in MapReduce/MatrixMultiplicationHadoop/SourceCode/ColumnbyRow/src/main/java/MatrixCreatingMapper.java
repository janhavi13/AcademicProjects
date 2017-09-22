import org.apache.hadoop.io.BooleanWritable;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import java.io.IOException;
import java.util.*;

/*
This class is responsible to create data for the reducer which creates the matrix M.
The mapper emits a page first. It emit the same page again with a page index as -1 if its dangling
If a page has outlinks then the mapper emits the pagename with outlink contribution and the its own pageindex
 */
public class MatrixCreatingMapper extends Mapper<Text, Text, Text, Text> {

    public void map(Text key, Text value, Context context) throws IOException, InterruptedException {

        double pageRank = (double) 1/ context.getConfiguration().getLong("totalPages",0);
        String[] pageDetails = value.toString().split(" ### ");
        int pageIndex = Integer.parseInt(pageDetails[2]);
        List<String> pageOutlinks = new ArrayList<String>();

        Node page;
        page = new Node();
        page.setPageRank(new DoubleWritable(pageRank));
        page.setIsNode(new BooleanWritable(true));
        page.setIndex(new IntWritable(pageIndex));

        //Set outlinks of a page if it has any
        if(pageDetails[1].length()>0)
        {
            pageOutlinks = (List)Arrays.asList(pageDetails[1].split(" "));
            page.setAdjacentNodeNames(pageOutlinks);

        }

        //emit the page with isNode set as True
        context.write(new Text(key.toString().trim()), new Text(page.getIsNode() +"~~" +page.getIndex()));

        // calculate the contribution of every outlink for a non danling node
        if(pageOutlinks.size() > 0 )
        {
            DoubleWritable outlinkContribution = new DoubleWritable(1.0/pageOutlinks.size() );
            Node outlink = new Node();
            outlink.setIsNode(new BooleanWritable(false));
            outlink.setIndex(new IntWritable(pageIndex));

            if(pageDetails[1].length()>0)
            {
                for(String name : pageOutlinks)
                {
                    context.write(new Text(name.trim()), new Text(outlink.getIsNode().toString() +"~~"+pageIndex +"~~"+ outlinkContribution));
                }
            }
        }

        // Emit the page with isNode set as false and page index as -1 for a dangling node
        else
           {
               context.write(new Text(key.toString().trim()), new Text("false" +"~~"+"-1" +"~~"+ pageRank));
            }
    }
}





