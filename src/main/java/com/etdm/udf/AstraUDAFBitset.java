package com.etdm.udf;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.zaxxer.sparsebits.SparseBitSet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.hive.common.io.NonSyncByteArrayInputStream;
import org.apache.hadoop.hive.common.type.Date;
import org.apache.hadoop.hive.common.type.HiveDecimal;
import org.apache.hadoop.hive.common.type.Timestamp;
import org.apache.hadoop.hive.ql.exec.Description;
import org.apache.hadoop.hive.ql.exec.UDFArgumentException;
import org.apache.hadoop.hive.ql.exec.UDFArgumentTypeException;
import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.ql.parse.SemanticException;
import org.apache.hadoop.hive.ql.udf.generic.GenericUDAFEvaluator;
import org.apache.hadoop.hive.ql.udf.generic.GenericUDAFParameterInfo;
import org.apache.hadoop.hive.ql.udf.generic.GenericUDAFResolver2;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.PrimitiveObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.*;
import org.apache.hadoop.hive.serde2.typeinfo.TypeInfo;
import org.apache.hadoop.io.BytesWritable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;


@Description (
        name = "astra_bitset",
        value = "_FUNC_(col1) - bitset as blob"
)



public class AstraUDAFBitset implements GenericUDAFResolver2 {
    static final Log LOG = LogFactory.getLog(AstraUDAFBitset.class.getName());
    private static final HashFunction hashFunction = Hashing.murmur3_32();

    private static int hash(String string) {
        return (hashFunction.hashUnencodedChars(string).asInt() & Integer.MAX_VALUE) % Integer.MAX_VALUE;
    }

    public AstraUDAFBitset() {

    }

    public GenericUDAFEvaluator getEvaluator(GenericUDAFParameterInfo paramInfo) throws SemanticException {
        ObjectInspector[] ois = paramInfo.getParameterObjectInspectors();
        if (ois.length < 2) {
            throw new UDFArgumentException("2 arguments expected");
        }
        if (paramInfo.isDistinct()) {
            throw new UDFArgumentException("Distinct keyword is not applicable");
        }
        if (paramInfo.isWindowing()) {
            throw new UDFArgumentException("Not a windowing function!");
        }
        if (!ois[0].getCategory().equals(ObjectInspector.Category.PRIMITIVE)) {
            throw new UDFArgumentException("The first parameter must be primitive!");
        }
        if (!ois[1].getCategory().equals(ObjectInspector.Category.PRIMITIVE)) {
            throw new UDFArgumentException("The second parameter must be primitive!");
        }
        return new BitSetEvaluator();
    }

    public GenericUDAFEvaluator getEvaluator(TypeInfo[] typeInfos) throws SemanticException {
        return new AstraUDAFBitset.BitSetEvaluator();
    }

    public static class BitSetEvaluator extends GenericUDAFEvaluator {
        private PrimitiveObjectInspector inputOI;
        private final ByteArrayOutputStream result = new ByteArrayOutputStream();

        public BitSetEvaluator() {
        }


        @Override
        public ObjectInspector init(Mode mode, ObjectInspector[] parameters) throws HiveException {
            super.init(mode, parameters);
            if (mode == Mode.PARTIAL2 || mode == Mode.COMPLETE ) {
                this.inputOI = (PrimitiveObjectInspector)parameters[0];
            }
            return PrimitiveObjectInspectorFactory.writableBinaryObjectInspector;
        }



        public AggregationBuffer getNewAggregationBuffer() throws HiveException {
            return new SparseBitSetBuf();
        }

        public void reset(AggregationBuffer aggregationBuffer) throws HiveException {
            SparseBitSetBuf buff = (SparseBitSetBuf)aggregationBuffer;
            buff.bitSet.clear();
        }

        public void iterate(AggregationBuffer aggregationBuffer, Object[] parameters) throws HiveException {
            if (parameters != null && parameters[0] != null) {
                String value = null;
                switch(this.inputOI.getPrimitiveCategory()) {
                    case BOOLEAN:
                        boolean vBoolean = ((BooleanObjectInspector)this.inputOI).get(parameters[0]);
                        value = String.valueOf(vBoolean);
                        break;
                    case BYTE:
                        byte vByte = ((ByteObjectInspector)this.inputOI).get(parameters[0]);
                        value = String.valueOf(vByte);
                        break;
                    case SHORT:
                        short vShort  = ((ShortObjectInspector)this.inputOI).get(parameters[0]);
                        value = String.valueOf(vShort);
                        break;
                    case INT:
                        int vInt  = ((IntObjectInspector)this.inputOI).get(parameters[0]);
                        value = String.valueOf(vInt);
                        break;
                    case LONG:
                        long vLong = ((LongObjectInspector)this.inputOI).get(parameters[0]);
                        value = String.valueOf(vLong);
                        break;
                    case FLOAT:
                        float vFloat = ((FloatObjectInspector)this.inputOI).get(parameters[0]);
                        value = String.valueOf(vFloat);
                        break;
                    case DOUBLE:
                        double vDouble = ((DoubleObjectInspector)this.inputOI).get(parameters[0]);
                        value = String.valueOf(vDouble);
                        break;
                    case DECIMAL:
                        HiveDecimal vDecimal = ((HiveDecimalObjectInspector)this.inputOI).getPrimitiveJavaObject(parameters[0]);
                        if (vDecimal != null) {
                            value = vDecimal.toDigitsOnlyString();
                        }
                        break;
                    case DATE:
                        Date v = ((DateObjectInspector)this.inputOI).getPrimitiveJavaObject(parameters[0]);
                        if (v != null) {
                            value = v.toString(); // pattern "yyyy-MM-dd" hardcoded
                        }
                        break;
                    case TIMESTAMP:
                        Timestamp vTimestamp = ((TimestampObjectInspector)this.inputOI).getPrimitiveJavaObject(parameters[0]);
                        if (vTimestamp != null) {
                            value = vTimestamp.toString();
                        }
                        break;
                    case CHAR:
                        value = ((HiveCharObjectInspector)this.inputOI).getPrimitiveJavaObject(parameters[0]).getStrippedValue();
                        break;
                    case VARCHAR:
                        value = ((HiveVarcharObjectInspector)this.inputOI).getPrimitiveJavaObject(parameters[0]).getValue();
                        break;
                    case STRING:
                        value = ((StringObjectInspector)this.inputOI).getPrimitiveJavaObject(parameters[0]);
                        break;
                    case BINARY:
                        //NOT APPLICABLE
                        break;
                    default:
                        throw new UDFArgumentTypeException(0, "Bad primitive category " + this.inputOI.getPrimitiveCategory());
                }
                if (value != null) {
                    SparseBitSet bs = ((SparseBitSetBuf)aggregationBuffer).bitSet;
                    bs.set(hash(value));
                }

            }

        }

        public void merge(AggregationBuffer agg, Object partial) throws HiveException {
            if (partial != null) {
                SparseBitSetBuf  bsBuff = (SparseBitSetBuf)agg;
                BytesWritable bytes = (BytesWritable)partial;
                try(NonSyncByteArrayInputStream in = new NonSyncByteArrayInputStream(bytes.getBytes());
                    ObjectInputStream is = new ObjectInputStream(in)) {
                    SparseBitSet partialBS = (SparseBitSet) is.readObject();
                    int bit = -1;
                    while( (bit = partialBS.nextSetBit(bit + 1)) >= 0) {
                        bsBuff.bitSet.set(bit);
                    }
                } catch (IOException | ClassNotFoundException e) {
                    throw new HiveException("Merge buffer",e);
                }

            }
        }


        public Object terminatePartial(AggregationBuffer agg) throws HiveException {
            return this.terminate(agg);
        }



        public Object terminate(AggregationBuffer agg) throws HiveException {
            this.result.reset();
            SparseBitSetBuf bsBuff = (SparseBitSetBuf) agg;
            try (ObjectOutputStream os = new ObjectOutputStream(this.result)) {
                os.writeObject(bsBuff.bitSet);
            } catch (IOException e) {
                throw new HiveException("Terminate buffer", e);
            }
            return new BytesWritable(this.result.toByteArray());
        }
    }

    @GenericUDAFEvaluator.AggregationType(
            estimable = true
    )
    static class SparseBitSetBuf extends GenericUDAFEvaluator.AbstractAggregationBuffer {
        SparseBitSet bitSet;

        public SparseBitSetBuf() {
            bitSet = new SparseBitSet();
        }

        public int estimate() {
            return 2147483647;
        }
    }


}
