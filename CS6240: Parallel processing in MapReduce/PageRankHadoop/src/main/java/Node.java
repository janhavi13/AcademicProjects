import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.*;
import org.apache.hadoop.io.*;

/*
* This is a writable class that is used in the mapper and reducer to emit.
* It is the output of the PageRankMapper( Mapper for iteration 1 where delta calculated) and
* PageRankWithDeltaMapper( Mapper from iteration 2 where delta from previous iteration is used to calculate pageRank)
* It is the input and the output of the PageRankReducer and PageRankWithDeltaReducer
*/
public class Node implements Writable{
    private DoubleWritable pageRank = new DoubleWritable(0);
    private List<String> adjacentNodeNames= new ArrayList<String>();
    private BooleanWritable isNode = new BooleanWritable(false);

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
    }
}