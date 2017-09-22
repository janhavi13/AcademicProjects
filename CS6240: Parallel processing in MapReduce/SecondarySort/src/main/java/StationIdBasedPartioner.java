/**
 * Created by janhavi on 2/6/17.
 */

import org.apache.hadoop.mapreduce.Partitioner;

/*
 Partitions records based on the stationId.So a reducer gets records of a stationId only
 */
public class StationIdBasedPartioner extends Partitioner<RecordKey, InputData> {

    @Override
    public int getPartition(RecordKey recordKey, InputData inputData, int i) {
        return (recordKey.getStationId().hashCode() & Integer.MAX_VALUE) % i;
    }
}