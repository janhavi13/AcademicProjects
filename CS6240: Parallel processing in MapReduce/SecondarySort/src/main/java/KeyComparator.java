/**
 * Created by janhavi on 2/6/17.
 */
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.io.WritableComparator;
/*
KeyComparator is used to sort the records based on the stationId
and if stationId is same then sorted based on year
 */
public class KeyComparator extends WritableComparator {
    protected KeyComparator() {
        super(RecordKey.class, true);
    }
    @Override
    public int compare(WritableComparable w1, WritableComparable w2) {
        RecordKey ip1 = (RecordKey) w1;
        RecordKey ip2 = (RecordKey) w2;
        int cmp = ip1.getStationId().compareTo(ip2.getStationId());
        if (cmp != 0) {
            return cmp;
        }
        return ip1.getYear().compareTo(ip2.getYear());
    }
}