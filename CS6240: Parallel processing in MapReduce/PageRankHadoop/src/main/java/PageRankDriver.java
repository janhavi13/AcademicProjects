import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.KeyValueTextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

/*
* This is the driver class for the MapReduce Job to calculate Page Rank and obtain top 100 pages.
* This Map Reduce Job consists of a chain of map reduce jobs.
* The first map reduce job parses the bz2 file and generates the pageName with its outlinks
* The second map reduce job runs for iteration 1 of the 10 iterations to calculate pageRank(PageRankMapper and PageRankReducer)
* The third map reduce job runs iteration 2 onwards(PageRankWithDeltaMapper and PageRankWithDeltaReducer)
* The fourth map reduce job calculates the top hundred pages with high page rank(TopKMapper and TopKReducer)
 */
public class PageRankDriver{

    public static enum Counter{delta};
    public static void main(String[] args) throws Exception {

        if (args.length != 2) {
            System.err.println("Enter valid number of arguments <Inputdirectory>  <Outputlocation>");
            System.exit(0);
        }

        // Parse bz2 file and generate pageName and all the linkpages
        Configuration preProcessConf = new Configuration();
        preProcessConf.set("mapred.textoutputformat.separator",":");
        Job preprocessjob = Job.getInstance(preProcessConf, "preprocess");
        preprocessjob.setJarByClass(PageRankDriver.class);
        preprocessjob.setMapperClass(PreprocessingMapper.class);
        preprocessjob.setReducerClass(PreprocessingReducer.class);
        preprocessjob.setMapOutputValueClass(Node.class);
        preprocessjob.setOutputKeyClass(Text.class);
        preprocessjob.setOutputValueClass(Node.class);
        FileInputFormat.addInputPath(preprocessjob, new Path(args[0]));
        FileOutputFormat.setOutputPath(preprocessjob, new Path("output_0"));

        preprocessjob.waitForCompletion(true);


        // iteration 1 which calculates delta
        long totalPages = preprocessjob.getCounters().findCounter("org.apache.hadoop.mapred.Task$Counter","REDUCE_OUTPUT_RECORDS").getValue();
        double Delta = 0.0;
        Configuration deltaconf = new Configuration();
        deltaconf.setLong("totalPages",totalPages);
        deltaconf.setFloat("delta",(float)0);
        deltaconf.set("mapreduce.input.keyvaluelinerecordreader.key.value.separator",":");
        Job deltajob = Job.getInstance(deltaconf, "pagerank iteration 1");
        deltajob.setJarByClass(PageRankDriver.class);
        deltajob.setMapperClass(PageRankMapper.class);
        deltajob.setMapOutputKeyClass(Text.class);
        deltajob.setMapOutputValueClass(Node.class);
        deltajob.setReducerClass(PageRankReducer.class);
        deltajob.setInputFormatClass(KeyValueTextInputFormat.class);
        FileInputFormat.setInputPaths(deltajob, new Path("output_0"));
        FileOutputFormat.setOutputPath(deltajob, new Path("output_1"));
        deltajob.waitForCompletion(true);
        Delta = (double)(deltajob.getCounters().findCounter(PageRankDriver.Counter.delta).getValue()) / Math.pow(10,15);

        // Iteration 2 to 10. This uses the delta from iteration 1 and calculates new values henceforth
        Configuration conf = new Configuration();
        conf.setLong("delta",(long)(Delta * Math.pow(10,15)));
        int i = 2;
        while(i<11)
        {
            //the delta from the previous iteration is set for the current iteration
            conf.setLong("delta",(long)(Delta * Math.pow(10,15)));
            conf.set("mapreduce.input.keyvaluelinerecordreader.key.value.separator",":");
            Job prjob = new Job(conf, "pagerank with delta in it");
            prjob.getConfiguration().setLong("totalPages",totalPages);
            prjob.setJarByClass(PageRankDriver.class);
            prjob.setMapperClass(PageRankWithDeltaMapper.class);
            prjob.setReducerClass(PageRankWithDeltaReducer.class);
            prjob.setOutputKeyClass(Text.class);
            prjob.setOutputValueClass(Node.class);
            prjob.setInputFormatClass(KeyValueTextInputFormat.class);
            FileInputFormat.addInputPath(prjob, new Path("output_"+(i -1)));
            FileOutputFormat.setOutputPath(prjob, new Path("output_"+(i)));
            prjob.waitForCompletion(true);
            //the delta of the current iteration is fetched from the job
            Delta = (double)(prjob.getCounters().findCounter(PageRankDriver.Counter.delta).getValue()) / Math.pow(10,15);
            i++;
        }

        // New MapReduce Job to calculate top 100 pages
        Configuration topkconf = new Configuration();
        Job job = new Job(topkconf, "Top Ten Pages by PageRank");
        job.setJarByClass(PageRankDriver.class);
        job.setMapperClass(TopKMapper.class);
        job.setReducerClass(TopKReducer.class);
        job.setNumReduceTasks(1);
        job.setOutputKeyClass(NullWritable.class);
        job.setOutputValueClass(Text.class);
        FileInputFormat.addInputPath(job, new Path("output_10"));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        System.exit(job.waitForCompletion(true)?0:1);
    }
}

