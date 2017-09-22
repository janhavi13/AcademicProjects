/**
 * Created by janhavi on 2/6/17.
 */

import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.io.WritableComparator;

/**
 * This class acts as the GroupingComparator which groups records based on stationId
 */
public class GroupComparator extends WritableComparator {
    protected GroupComparator() {
        super(RecordKey.class, true);
    }

    @Override
    public int compare(WritableComparable w1, WritableComparable w2) {
        RecordKey ip1 = (RecordKey) w1;
        RecordKey ip2 = (RecordKey) w2;
        return ip1.getStationId().compareTo(ip2.getStationId());
    }
}
