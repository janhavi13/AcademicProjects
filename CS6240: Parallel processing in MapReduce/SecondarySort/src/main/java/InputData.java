import org.apache.hadoop.io.Writable;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
/*
InputData is used as in the input for reduce() and output for map()
*/
public class InputData implements Writable
{
    private double tmaxSum;
    private int tmaxCount;
    private double tminSum;
    private int tminCount;

    public InputData(){
        tmaxSum =0;
        tmaxCount = 0;
        tminSum = 0;
        tminCount = 0;
    }
    public double getTmaxSum() {
        return tmaxSum;
    }

    public void setTmaxSum(double tmaxSum) {
        this.tmaxSum = tmaxSum;
    }

    public int getTmaxCount() {
        return tmaxCount;
    }

    public void setTmaxCount(int tmaxCount) {
        this.tmaxCount = tmaxCount;
    }

    public double getTminSum() {
        return tminSum;
    }

    public void setTminSum(double tminSum) {
        this.tminSum = tminSum;
    }

    public int getTminCount() {
        return tminCount;
    }

    public void setTminCount(int tminCount) {
        this.tminCount = tminCount;
    }

    @Override
    public void write(DataOutput dataOutput) throws IOException {
        dataOutput.writeDouble(tmaxSum);
        dataOutput.writeInt(tmaxCount);
        dataOutput.writeDouble(tminSum);
        dataOutput.writeInt(tminCount);
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        this.tmaxSum = in.readDouble();
        this.tmaxCount= in.readInt();
        this.tminSum = in.readDouble();
        this.tminCount = in.readInt();
    }
}