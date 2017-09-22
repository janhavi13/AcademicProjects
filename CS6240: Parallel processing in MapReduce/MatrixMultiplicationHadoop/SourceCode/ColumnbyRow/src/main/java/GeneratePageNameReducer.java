import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;

/**
 * The reducer class here read the R matrix file in the cache
 * It maps the pagename , pageindex , pagerank for a page
 */
public class GeneratePageNameReducer extends Reducer<NullWritable, Text, Text, Text> {

    BufferedReader br;
    HashMap<String, String> pageNameMap = new HashMap<String, String>();

    public void setup(Context context) throws IOException{
        if(context.getCacheFiles()!= null){
            URI[] page = context.getCacheFiles();
            if(page!=null && page.length>0){
                for(URI p : page){
                    readFile(p);
                }
            }
        }
    }

    protected void readFile(URI file) throws IOException {
        Path newPath = new Path(file);
        br = new BufferedReader(new FileReader(newPath.getName()));
        String line;
        String lineDetails[];
        String page[];
        while((line = br.readLine())!= null){
            lineDetails = line.split("\t");
            page = lineDetails[1].split("~~");
            pageNameMap.put(lineDetails[0], page[0]);
        }
        br.close();
    }

    protected void reduce(NullWritable key, Iterable<Text> values, Context context)
            throws IOException, InterruptedException
    {
        String pageName;
        String pageindex;
        String pageRank ;
        for(Text value : values)
        {
            pageindex = value.toString().split("~~")[1];
            pageName = value.toString().split("~~")[0];
            pageRank = pageNameMap.get(pageindex.trim());
            context.write(new Text(pageName),new Text(pageRank));
        }
    }



}
