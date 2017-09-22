import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.KeyValueTextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.File;
import java.net.URI;

/*
* This is the driver class for the MapReduce Job to calculate Page Rank and obtain top 100 pages.
* This Map Reduce Job consists of a chain of map reduce jobs.
* The first map reduce job parses the bz2 file and generates the pageName with its outlinks(PreprocessingMapper and PreprocessingReducer )
* The second map reduce job runs to create the M matrix (MatrixCreatingMapper and MatrixCreatingReducer)
* The third map reduce job runs to create the R matrix (PageRankMatrixCreatingMapper)
* The fourth map reduce job runs for iteration 1 to 10 iterations to calculate pageRank(PageRankCalculationMapper and PageRankCalculationReducer)
* The fifth map reduce job runs for creating mapping between page name , page index and page rank(GeneratePageNameMapper and GeneratePageNameReducer)
* The sixth map reduce job calculates the top hundred pages with high page rank(TopKMapper and TopKReducer)
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
        preprocessjob.setNumReduceTasks(1);
        preprocessjob.setMapOutputValueClass(Node.class);
        preprocessjob.setOutputKeyClass(Text.class);
        preprocessjob.setOutputValueClass(Node.class);
        FileInputFormat.addInputPath(preprocessjob, new Path(args[0]));
        FileOutputFormat.setOutputPath(preprocessjob, new Path("output_"));

        preprocessjob.waitForCompletion(true);


        // creating M
        long totalPages = preprocessjob.getCounters().findCounter("org.apache.hadoop.mapred.Task$Counter","REDUCE_OUTPUT_RECORDS").getValue();
        Configuration deltaconf = new Configuration();
        deltaconf.setLong("totalPages",totalPages);
        deltaconf.setFloat("delta",(float)0);
        deltaconf.set("mapreduce.input.keyvaluelinerecordreader.key.value.separator",":");
        Job deltajob = Job.getInstance(deltaconf, "matrix M creation");
        deltajob.setJarByClass(PageRankDriver.class);
        deltajob.setMapperClass(MatrixCreatingMapper.class);
        deltajob.setMapOutputKeyClass(Text.class);
        deltajob.setMapOutputValueClass(Text.class);
        deltajob.setReducerClass(MatrixCreatingReducer.class);
        deltajob.setInputFormatClass(KeyValueTextInputFormat.class);
        FileInputFormat.setInputPaths(deltajob, new Path("output_"));
        FileOutputFormat.setOutputPath(deltajob, new Path("matrix"));
        deltajob.waitForCompletion(true);

        Configuration conf = new Configuration();
        conf.set("mapreduce.input.keyvaluelinerecordreader.key.value.separator","\t");
        Job prmatrixjob = new Job(conf, "R calculation");
        prmatrixjob.getConfiguration().setLong("totalPages",totalPages);
        prmatrixjob.setJarByClass(PageRankDriver.class);
        prmatrixjob.setMapperClass(PageRankMatrixCreatingMapper.class);
        prmatrixjob.setNumReduceTasks(0);
        prmatrixjob.setMapOutputKeyClass(Text.class);
        prmatrixjob.setMapOutputValueClass(Text.class);
        prmatrixjob.setInputFormatClass(KeyValueTextInputFormat.class);
        FileInputFormat.addInputPath(prmatrixjob, new Path("matrix"));
        FileOutputFormat.setOutputPath(prmatrixjob, new Path("output_0"));
        prmatrixjob.waitForCompletion(true);


        // Iteration 1 to 10 for pagerank calculation.
        Configuration prconf = new Configuration();
        Job prjob =  new Job(prconf, "pagerank calculation job");;
        int i = 0;
        while(i<10)
        {
            prconf.set("mapreduce.input.keyvaluelinerecordreader.key.value.separator","\t");
            prjob = new Job(prconf, "pagerank calculation job");
            prjob.getConfiguration().setLong("totalPages",totalPages);
            prjob.setJarByClass(PageRankDriver.class);
            prjob.setMapperClass(PageRankCalculationMapper.class);
            prjob.setReducerClass(PageRankCalculationReducer.class);
            prjob.setOutputKeyClass(Text.class);
            prjob.setOutputValueClass(Text.class);
            prjob.setInputFormatClass(KeyValueTextInputFormat.class);
            FileInputFormat.addInputPath(prjob, new Path("matrix"));
            Path p = new Path("output_"+i);
            FileSystem fs = p.getFileSystem(prconf);
            FileStatus[] fileStatus = fs.listStatus(p);
            for (FileStatus status : fileStatus) {
                prjob.addCacheFile(status.getPath().toUri());	//Adding the file to  cache from second iteration onwards.
            }
            FileOutputFormat.setOutputPath(prjob, new Path("output_"+(i+1)));
            prjob.waitForCompletion(true);
            i++;
        }

        // generate mapping between page name and pageindex and keep track of pagerank
        Configuration pgnameconf = new Configuration();
        Job job = new Job(pgnameconf, "Generate Pages by PageRank");
        job.setJarByClass(PageRankDriver.class);
        job.setMapperClass(GeneratePageNameMapper.class);
        job.setReducerClass(GeneratePageNameReducer.class);
        job.setNumReduceTasks(1);
        job.setMapOutputKeyClass(NullWritable.class);
        job.setMapOutputValueClass(Text.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        FileInputFormat.addInputPath(job, new Path("output_"));
        Path p = new Path("output_10");
        FileSystem fs = p.getFileSystem(pgnameconf);
        FileStatus[] fileStatus = fs.listStatus(p);
        for (FileStatus status : fileStatus)
        {
            // Keep R in cache
            job.addCacheFile(status.getPath().toUri());
        }
        FileOutputFormat.setOutputPath(job, new Path("output_11"));

        job.waitForCompletion(true);

        // New MapReduce Job to calculate top 100 pages
        Configuration topkconf = new Configuration();
        Job topkjob = new Job(topkconf, "Top Ten Pages by PageRank");
        topkjob.setJarByClass(PageRankDriver.class);
        topkjob.setMapperClass(TopKMapper.class);
        topkjob.setReducerClass(TopKReducer.class);
        topkjob.setNumReduceTasks(1);
        topkjob.setOutputKeyClass(NullWritable.class);
        topkjob.setOutputValueClass(Text.class);
        FileInputFormat.addInputPath(topkjob, new Path("output_11"));
        FileOutputFormat.setOutputPath(topkjob, new Path(args[1]));

        System.exit(topkjob.waitForCompletion(true)?0:1);
    }
}

