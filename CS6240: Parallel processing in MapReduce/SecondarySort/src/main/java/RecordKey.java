import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.io.WritableUtils;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Used as a custom key wherer stationId is used to group records
 * and year is used to sort records for each stationId
 */
public class RecordKey implements WritableComparable {

    private String stationId ;
    private Integer year;

    public String getStationId() {
        return stationId;
    }

    public void setStationId(String stationId) {
        this.stationId = stationId;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }



    @Override
    public void write(DataOutput dataOutput) throws IOException {

        WritableUtils.writeString(dataOutput,stationId);
        WritableUtils.writeVInt(dataOutput,year);

    }

    @Override
    public void readFields(DataInput dataInput) throws IOException {


        this.setStationId(WritableUtils.readString(dataInput));
        this.setYear(WritableUtils.readVInt(dataInput));

    }

    @Override
    public int compareTo(Object o) {
        return 0;
    }
}
