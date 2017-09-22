import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import java.io.IOException;

/*
This is map only task. It creates the R matrix from the file obtained from parser job.
It takes the index from that file and assigns it 1/|V| as page rank which is used from iteration1
 */
public class PageRankMatrixCreatingMapper extends Mapper<Text, Text, Text, Text> {

    public void map(Text key, Text value, Context context) throws IOException, InterruptedException {

        double initialPR = (double) 1/ context.getConfiguration().getLong("totalPages",0);
        boolean isDangling = false;

        String val = value.toString().substring(1,value.toString().length()-1);
        if(val.length()>0)
        {
            String[] inlinks;
            int inlink =0;
            if(val.contains(","))
            {
                inlinks = val.split(",");
                for(String link : inlinks)
                {
                    inlink = Integer.parseInt(link.split(":")[0].trim());
                    if(inlink==-1)
                        isDangling = true;
                }
                context.write(key,new Text(initialPR+"~~"+isDangling));
            }

            else
            {
                inlink = Integer.parseInt(val.split(":")[0].trim());
                if(inlink==-1)
                    isDangling = true;
                context.write(key,new Text(initialPR+"~~"+isDangling));
            }
        }
        else
        {
            context.write(key,new Text(initialPR+"~~"+isDangling));
        }
    }
}





