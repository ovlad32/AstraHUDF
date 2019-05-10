package com.etdm.udf;

import com.zaxxer.sparsebits.SparseBitSet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.hive.ql.exec.MapredContext;
import org.apache.hadoop.hive.ql.exec.UDFArgumentException;
import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.ql.udf.generic.GenericUDTF;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspectorFactory;
import org.apache.hadoop.hive.serde2.objectinspector.PrimitiveObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.StructObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.BinaryObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.PrimitiveObjectInspectorFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AstraUDTFMatch extends GenericUDTF {

    static final Log LOG = LogFactory.getLog(AstraUDTFMatch.class.getName());
    static int LEFT_BITSET_PARAM_INDEX = 0;
    static int RIGHT_BITSET_PARAM_INDEX = 1;
    static int MAIN_COLUMN_COUNT = 3;
    static int MAIN_PARAM_COUNT = 2;

    private BinaryObjectInspector leftOI = null;
    private BinaryObjectInspector rightOI = null;
    private PrimitiveObjectInspector[] auxOI = null;

    //Defining input argument as string.
    @Override
    public StructObjectInspector initialize(ObjectInspector[] params) throws UDFArgumentException {
        if (params.length < MAIN_PARAM_COUNT ) {
            throw new UDFArgumentException("AstraUDTFMatch() takes more 1 argument");
        }

        if (params[LEFT_BITSET_PARAM_INDEX].getCategory() != ObjectInspector.Category.PRIMITIVE
                && ((PrimitiveObjectInspector) params[LEFT_BITSET_PARAM_INDEX]).getPrimitiveCategory() != PrimitiveObjectInspector.PrimitiveCategory.BINARY) {
            throw new UDFArgumentException("NameParserGenericUDTF() takes a binary as the 1st parameter");
        }
        if (params[RIGHT_BITSET_PARAM_INDEX].getCategory() != ObjectInspector.Category.PRIMITIVE
                && ((PrimitiveObjectInspector) params[RIGHT_BITSET_PARAM_INDEX]).getPrimitiveCategory() != PrimitiveObjectInspector.PrimitiveCategory.BINARY) {
            throw new UDFArgumentException("NameParserGenericUDTF() takes a binary as the 2nd parameter");
        }

        // input
        leftOI = (BinaryObjectInspector) params[LEFT_BITSET_PARAM_INDEX];
        rightOI = (BinaryObjectInspector) params[RIGHT_BITSET_PARAM_INDEX];
        auxOI = new PrimitiveObjectInspector[params.length - MAIN_PARAM_COUNT];
        // output
        List<String> fieldNames = new ArrayList<>(params.length - MAIN_PARAM_COUNT + MAIN_COLUMN_COUNT );
        List<ObjectInspector> fieldOIs = new ArrayList<>( params.length - MAIN_PARAM_COUNT + MAIN_COLUMN_COUNT);
        fieldNames.add("match");
        fieldNames.add("cardinality_0");
        fieldNames.add("cardinality_1");
        fieldOIs.add(PrimitiveObjectInspectorFactory.javaIntObjectInspector);
        fieldOIs.add(PrimitiveObjectInspectorFactory.javaIntObjectInspector);
        fieldOIs.add(PrimitiveObjectInspectorFactory.javaIntObjectInspector);
        for(int paramIndex = MAIN_PARAM_COUNT, auxIndex = 0; paramIndex < params.length; paramIndex++, auxIndex++) {
            fieldNames.add("aux_" + (paramIndex - MAIN_PARAM_COUNT));
            auxOI[auxIndex] = (PrimitiveObjectInspector)params[paramIndex];
            fieldOIs.add(PrimitiveObjectInspectorFactory.getPrimitiveJavaObjectInspector(auxOI[auxIndex].getTypeInfo()));
        }

        return ObjectInspectorFactory.getStandardStructObjectInspector(fieldNames, fieldOIs);
    }


    @Override
    public void configure(MapredContext mapredContext) {
        super.configure(mapredContext);
    }

    private SparseBitSet convertToBitset(byte[] bytes)  throws IOException,ClassNotFoundException{
        if (bytes != null) {
            try(ObjectInputStream s = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
                return (SparseBitSet) s.readObject();
            }
        }
        return null;
    }


    @Override
    public void process(Object[] params) throws HiveException {

        try {
            SparseBitSet leftBs = convertToBitset(leftOI.getPrimitiveJavaObject(params[LEFT_BITSET_PARAM_INDEX]));
            if (leftBs == null)
                return;
            SparseBitSet rightBs = convertToBitset(rightOI.getPrimitiveJavaObject(params[RIGHT_BITSET_PARAM_INDEX]));
            if (rightBs == null)
                return;

            int matched = 0;
            SparseBitSet smallBs, bigBs;

            int leftCardinality = leftBs.cardinality();
            int rightCardinality = rightBs.cardinality();
            if (leftCardinality > rightCardinality) {
                bigBs = leftBs;
                smallBs = rightBs;
            } else {
                bigBs = rightBs;
                smallBs = leftBs;
            }

            for (int i = smallBs.nextSetBit(0); i >= 0; i = smallBs.nextSetBit(i + 1)) {
                if (bigBs.get(i)) {
                    matched++;
                }
            }
            if (matched > 0) {
                Object[] row = new Object[auxOI.length + MAIN_COLUMN_COUNT];
                int itemCount = 0;
                row[itemCount++] = matched;
                row[itemCount++] = leftCardinality;
                row[itemCount++] = rightCardinality;

                for (int paramIndex = MAIN_PARAM_COUNT, auxIndex = 0; paramIndex < params.length; paramIndex++, auxIndex++) {
                    row[itemCount++] = auxOI[auxIndex].getPrimitiveJavaObject(params[paramIndex]);
                }

                this.forward(row);
            }
        } catch (ClassNotFoundException | IOException e) {
            LOG.error(e);
            return;
        }
    }

    @Override
    public void close() throws HiveException {

    }
}
