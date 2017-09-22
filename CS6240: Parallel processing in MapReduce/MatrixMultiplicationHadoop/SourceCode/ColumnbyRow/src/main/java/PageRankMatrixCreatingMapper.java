import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import java.io.IOException;

/*
This is map only task. It creates the R matrix from the file obtained from parser job.
It takes the index from that file and assigns it 1/|V| as page rank which is used from iteration1
 */
public class PageRankMatrixCreatingMapper extends Mapper<Text, Text, Text, Text> {

    public void map(Text key, Text value, Context context) throws IOException, InterruptedException {

        double pagerank = (double) 1 / context.getConfiguration().getLong("totalPages", 0);
        String[] pageDetails = value.toString().split("###");

        if(pageDetails[1].trim().length() == 0)
        {
            //if danging then append "true" to the record
            context.write(new Text(pageDetails[2].trim()), new Text(pagerank + "~~" + "true" ));
        }
        else
        {
            // if non dangling then append "false" to the record
            context.write(new Text(pageDetails[2].trim()), new Text(pagerank + "~~" + "false"));
        }
    }
}





