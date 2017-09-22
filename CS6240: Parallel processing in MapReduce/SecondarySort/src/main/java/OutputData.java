import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/*
Acts as the output for the reduce()
writes object of this class to outputfile
 */
public class OutputData implements Writable
{
    private DoubleWritable tmaxAverage;
    private DoubleWritable tminAverage;
    private Text year;

    public OutputData(){
        tmaxAverage = new DoubleWritable(0);
        tminAverage = new DoubleWritable(0);
        year = new Text("");
    }

    public Text getYear() {
        return year;
    }

    public void setYear(Text year) {
        this.year = year;
    }

    public DoubleWritable getTmaxAverage() {
        return tmaxAverage;
    }

    public void setTmaxAverage(DoubleWritable tmaxAverage) {
        this.tmaxAverage = tmaxAverage;
    }

    public DoubleWritable getTminAverage() {
        return tminAverage;
    }

    public void setTminAverage(DoubleWritable tminAverage) {
        this.tminAverage = tminAverage;
    }

    @Override
    public void write(DataOutput dataOutput) throws IOException {
        year.write(dataOutput);
        tmaxAverage.write(dataOutput);
        tminAverage.write(dataOutput);
    }

    @Override
    public void readFields(DataInput dataInput) throws IOException {
        year.readFields(dataInput);
        tmaxAverage.readFields(dataInput);
        tminAverage.readFields(dataInput);
    }

    @Override
    public String toString() {
        return "("+this.getYear() +","+this.getTminAverage() + "," + this.getTmaxAverage() +")";
    }
}