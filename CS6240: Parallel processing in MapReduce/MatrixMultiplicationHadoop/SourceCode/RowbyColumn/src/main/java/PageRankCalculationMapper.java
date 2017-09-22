import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import java.io.IOException;

/*
This is the mapper to calculate page rank iterations, it simply passes the value which is a row here with inlinks
 */
public class PageRankCalculationMapper extends Mapper<Text, Text, Text, Text> {

    public void map(Text key, Text value, Context context) throws IOException, InterruptedException {

        context.write(key, new Text(value.toString().substring(1,value.toString().length()-1)));
    }
}





