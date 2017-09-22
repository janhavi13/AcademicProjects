import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.*;
import org.apache.hadoop.io.*;

/*
* This is a writable class that is used in the mapper and reducer to emit.
*/
public class Node implements Writable{


    private DoubleWritable pageRank = new DoubleWritable(0);
    private List<String> adjacentNodeNames= new ArrayList<String>();
    private BooleanWritable isNode = new BooleanWritable(false);
    private IntWritable index = new IntWritable(0);

    public DoubleWritable getPageRank() {
        return this.pageRank;
    }

    public void setPageRank(DoubleWritable pageRank) {
        this.pageRank = new DoubleWritable(pageRank.get());
    }

    public List<String> getAdjacentNodeNames() {
        return this.adjacentNodeNames;
    }

    public void setAdjacentNodeNames(List<String> adjacentNodeNames) {
        this.adjacentNodeNames = new ArrayList<String>(adjacentNodeNames);
    }

    public BooleanWritable getIsNode() {
        return this.isNode;
    }

    public void setIsNode(BooleanWritable isNode) {
        this.isNode = new BooleanWritable(isNode.get());
    }

    public IntWritable getIndex() {
        return this.index;
    }

    public void setIndex(IntWritable index) {
        this.index = new IntWritable(index.get());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(":");
        sb.append(pageRank);
        sb.append(" ### ");
        for(String s: adjacentNodeNames)
        {
            sb = sb.append(s);
            sb = sb.append(" ");
        }
        sb.append(" ### ");
        sb.append(index);
        sb.append(" ### ");
        sb.append(isNode);

        return sb.toString();
    }

    @Override
    public void write(DataOutput dataOutput) throws IOException {
    this.pageRank.write(dataOutput);
    dataOutput.writeInt(adjacentNodeNames.size());
    for(String adNode: adjacentNodeNames)
    {
        WritableUtils.writeString(dataOutput,adNode);
    }
    this.isNode.write(dataOutput);
    this.index.write(dataOutput);
    }

    @Override
    public void readFields(DataInput dataInput) throws IOException {
        this.pageRank.readFields(dataInput);
        int adNodeSize = dataInput.readInt();
        this.adjacentNodeNames = new ArrayList<String>();
        for(int i=0;i<adNodeSize;i++)
        {
          String adjNodeName = WritableUtils.readString(dataInput);
            this.adjacentNodeNames.add(adjNodeName);
        }
        this.isNode.readFields(dataInput);
        this.index.readFields(dataInput);
    }
}