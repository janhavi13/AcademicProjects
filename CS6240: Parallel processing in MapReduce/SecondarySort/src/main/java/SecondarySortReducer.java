import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *This class extends the reducer which contains the reduce method
 * This calculates the mean Tmax and Tmin temperature
 */
public class SecondarySortReducer
        extends Reducer<RecordKey, InputData, Text, StringBuilder> {
    /**
     * @param key is stationId
     * @param values is the list of values of InputData data type
     * @param context is used to write from hadoop to output file
     * @throws IOException
     * @throws InterruptedException
     */
    public void reduce(RecordKey key, Iterable<InputData> values,
                       Context context
    ) throws IOException, InterruptedException {

        List<OutputData> meanTemperatureForEachYear = new ArrayList<OutputData>();
        Integer prevYear = key.getYear();
        Integer currentYear ;
        double tminSum = 0, tmaxSum = 0;
        int tminCount = 0, tmaxCount = 0;
        for(InputData record:values)
        {
            currentYear = key.getYear();
            if(prevYear.equals(currentYear))
            {
                tmaxSum += record.getTmaxSum();
                tmaxCount += record.getTmaxCount();
                tminSum += record.getTminSum();
                tminCount += record.getTminCount();
            }
            else
            {
                OutputData perYearMean = new OutputData();
                perYearMean.setYear(new Text(prevYear+""));
                perYearMean.setTmaxAverage(new DoubleWritable(tmaxSum/tmaxCount));
                perYearMean.setTminAverage(new DoubleWritable(tminSum/tminCount));
                meanTemperatureForEachYear.add(perYearMean);
                tmaxSum = 0;
                tmaxCount = 0;
                tminSum = 0;
                tminCount = 0;
                prevYear = currentYear;
                tmaxSum += record.getTmaxSum();
                tmaxCount += record.getTmaxCount();
                tminSum += record.getTminSum();
                tminCount += record.getTminCount();
            }
        }

        if(tmaxSum !=0 || tmaxCount!=0 || tminSum!=0 || tminCount!=0)
        {
            OutputData perYearMean = new OutputData();
            perYearMean.setYear(new Text(prevYear+""));
            perYearMean.setTmaxAverage(new DoubleWritable(tmaxSum/tmaxCount));
            perYearMean.setTminAverage(new DoubleWritable(tminSum/tminCount));
            meanTemperatureForEachYear.add(perYearMean);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("[");

        String prefix = "";
        for(OutputData x : meanTemperatureForEachYear)
        {
            sb.append(prefix);
            prefix = ",";
            String output = x.toString();
            if(output.contains("NaN"))
                output = output.replace("NaN","None");
            sb.append(output);
        }
        sb.append("]");
        context.write(new Text(key.getStationId()),sb);

    }
}
