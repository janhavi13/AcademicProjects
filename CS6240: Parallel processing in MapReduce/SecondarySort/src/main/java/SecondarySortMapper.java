import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import java.io.IOException;
import java.util.StringTokenizer;

/**
 * Created by janhavi on 2/6/17.
 */

/**
 * This is a class extending the mapper which contains the map method
 * Reads TMAX and TMIN records from the input file
 */
public class SecondarySortMapper
            extends Mapper<Object, Text, RecordKey, InputData> {

        private final static InputData Record = new InputData();
        private Text word = new Text();
        /**
         * @param key : Input key to the map
         * @param value:Input value to the map
         * @param context : writes it to the data stream for hadoop to process
         * @throws IOException
         * @throws InterruptedException
         */
        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            StringTokenizer itr = new StringTokenizer(value.toString(), ",");
            RecordKey recordKey = new RecordKey();
            String stationId = itr.nextToken();
            String yearToken = itr.nextToken();
            yearToken = yearToken.substring(0,4);
            Integer year = Integer.parseInt(yearToken);
            recordKey.setStationId(stationId);
            recordKey.setYear(year);
            while (itr.hasMoreTokens()) {
                String type = itr.nextToken();
                if (type.equals("TMAX")) {
                    Record.setTmaxSum(Double.parseDouble(itr.nextToken()));
                    Record.setTmaxCount(1);
                    Record.setTminSum(0);
                    Record.setTminCount(0);
                    context.write(recordKey, Record);
                    break;
                }
                if (type.equals("TMIN")) {
                    Record.setTmaxSum(0);
                    Record.setTmaxCount(0);
                    Record.setTminSum(Double.parseDouble(itr.nextToken()));
                    Record.setTminCount(1);
                    context.write(recordKey, Record);
                    break;
                }
            }
        }
    }

