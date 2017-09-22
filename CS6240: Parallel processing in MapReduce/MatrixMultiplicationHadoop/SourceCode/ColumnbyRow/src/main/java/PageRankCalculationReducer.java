import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import java.net.URI;
import org.apache.hadoop.fs.Path;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.io.IOException;

/*
This reducer is used to calculate the page rank in each iteration.
It calculates the delta in the setup
The reducer applies the page rank formula on a page using the delta and inlink contribution
 */
public class PageRankCalculationReducer extends Reducer<Text, Text, Text, Text> {
    BufferedReader br;
    double delta;
    HashMap<String, String> pageRankIndex = new HashMap<String, String>();
    double alpha = 0.15;

    public void setup(Context context) throws IOException{
        delta = 0.0;
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
            if(page[1].equals("true"))
            {
                delta += Double.parseDouble(page[0]);
            }
            else
            {
                pageRankIndex.put(lineDetails[0].trim(), page[0]);
            }
        }
        br.close();
    }

    @Override
    protected void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
        // let's say we have the R matrix in variable RMatrix;
        double totalPageCount = Double.parseDouble(context.getConfiguration().get("totalPages"));
        double pr = 0.0;
        double pageRank =0;
        //search the key in the R Matrix
        for (Text value : values) {
            String inlink ="";
            double prContribution=0;
            if (value.toString().length() > 0)
            {
                inlink = value.toString().split(":")[0].trim();
                if(!inlink.equals("")) {
                    prContribution = Double.parseDouble(value.toString().split(":")[1]);
                    String inlinkPR = pageRankIndex.get(value.toString().split(":")[0].trim());
                    pr += (Double.parseDouble(inlinkPR) * prContribution);
                }
            }
        }

        pageRank = (alpha/totalPageCount) + ((1- alpha)* (pr + (delta/totalPageCount)));

            if(pageRankIndex.containsKey(key.toString()))
            {
                context.write(new Text(key.toString().trim()), new Text( pageRank +"~~" +"false" ));
            }
            else
            {
                context.write(new Text(key.toString().trim()), new Text( pageRank +"~~" +"true" ));
            }
    }

    @Override
    protected void cleanup(Context context) throws IOException, InterruptedException {
        delta = 0.0;
    }
}
