import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class SecondarySortDriver {

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        conf.set("mapred.textoutputformat.separator",",");
        Job job = Job.getInstance(conf, "secondary sort");
        job.setJarByClass(SecondarySortDriver.class);
        job.setMapperClass(SecondarySortMapper.class);
        job.setMapOutputValueClass(InputData.class);
        job.setPartitionerClass(StationIdBasedPartioner.class);
        job.setSortComparatorClass(KeyComparator.class);
        job.setGroupingComparatorClass(GroupComparator.class);
        job.setMapOutputKeyClass(RecordKey.class);
        job.setReducerClass(SecondarySortReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(StringBuilder.class);
        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }

}