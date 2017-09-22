import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import java.io.IOException;
/*
This is the mapper to calculate page rank iterations, it simply passes the value which is a row here with inlinks
 */
public class PageRankCalculationMapper extends Mapper<Text, Text, Text, Text> {

    public void map(Text key, Text value, Context context) throws IOException, InterruptedException {
        String val = value.toString();
        String inlinkIndex = key.toString();
        String pageIndex = val.split(":")[0];
        if(val.length()>0)
        {
            String pr = value.toString().split(":")[1];
            context.write(new Text(pageIndex.trim()), new Text(inlinkIndex.trim() + ":" + pr));
        }
        else
        {
            context.write(new Text(inlinkIndex.trim()), new Text());
        }
    }
}





