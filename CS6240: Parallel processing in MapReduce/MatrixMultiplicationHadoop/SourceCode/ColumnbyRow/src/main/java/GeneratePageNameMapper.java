import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import java.io.IOException;

/**
 * This mapper is responsible to send the pagename and pageindex for every page from the output file of BZ2Parser.
 */
public class GeneratePageNameMapper extends Mapper<Object, Text, NullWritable, Text> {

    @Override
    protected void map(Object key, Text value, Context context) throws IOException, InterruptedException {

        String page = value.toString();
        String pageName = page.split("::")[0];
        String outlinkspageIndex = page.split("::")[1];
        String pageIndex = outlinkspageIndex.split("###")[2];
        context.write(NullWritable.get(),new Text(pageName + "~~"+pageIndex));
    }
}
