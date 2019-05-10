package com.etdm.proto;


import com.zaxxer.sparsebits.SparseBitSet;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.sql.*;
import java.util.Properties;
import java.util.logging.Logger;

public class Launcher {
    static Logger logger = Logger.getLogger(Launcher.class.toString());

    static  String query =
            "select \n"+
            "astra_bs_0(t.COLLATERAL_ASSESSMENT_DATE,0) as COLLATERAL_ASSESSMENT_DATE,\n"+
            "astra_bs_0(t.COLLATERAL_VALUE_CURRENCY,0) as COLLATERAL_VALUE_CURRENCY,\n"+
            "astra_bs_0(t.COLLATERAL_VALUE,0) as COLLATERAL_VALUE,\n"+
            "astra_bs_0(t.COLLATERAL_TYPE,0) as COLLATERAL_TYPE,\n"+
            "astra_bs_0(t.DUE_DATE,0) as DUE_DATE,\n"+
            "astra_bs_0(t.CURRENCY,0) as CURRENCY,\n"+
            "astra_bs_0(t.ORIGINAL_AMOUNT,0) as ORIGINAL_AMOUNT,\n"+
            "astra_bs_0(t.LIABILITY_TYPE,0) as LIABILITY_TYPE,\n"+
            "astra_bs_0(t.LIABILITY_NUMBER,0) as LIABILITY_NUMBER ,\n"+
            "astra_bs_0(t.LIABILITY_DATE,0) as LIABILITY_DATE,\n"+
            "astra_bs_0(t.INFORMER_DEAL_ID,0) as INFORMER_DEAL_ID,\n"+
            "astra_bs_0(t.INFORMER_CODE,0) as INFORMER_CODE,\n"+
            "astra_bs_0(t.ID,0) as ID\n"+
            "from cra.liabilities t\n";

    public static void main(String[] args) throws Exception {
        //System.out.println(org.apache.hadoop.util.VersionInfo.getVersion());
        Driver d = (Driver)Class.forName("org.apache.hive.jdbc.HiveDriver").newInstance();
        Properties  props = new Properties();
        props.put("user", "admin");
        props.put("password", "admin");


        try (Connection connection = d.connect(
                "jdbc:hive2://iotahoe-test.nj.rokittech.com:2181,iotahoe-test-node1.nj.rokittech.com:2181,iotahoe-test-node2.nj.rokittech.com:2181/;serviceDiscoveryMode=zooKeeper;zooKeeperNamespace=hiveserver2",
                props
        )) {
            try(PreparedStatement p = connection.prepareStatement("add jar hdfs:///apps/udf/u.jar")) {
                p.execute();
            }
            try(PreparedStatement p = connection.prepareStatement("create temporary function astra_bs_0 as 'com.etdm.udf.AstraUDAFBitset'")) {
                p.execute();
            }
            try (PreparedStatement p = connection.prepareStatement("use cra")) {
                p.execute();
            }

            try (PreparedStatement p = connection.prepareStatement("show tables")) {
                p.execute();
            }


            try(PreparedStatement p = connection.prepareStatement(query);
                ResultSet rs = p.executeQuery()){
                if (rs.next()) {
                    ResultSetMetaData rsm = rs.getMetaData();
                    for (int c = 1; c<=rsm.getColumnCount();c++) {
                        InputStream is = rs.getBinaryStream(c);
                        try (ObjectInputStream ois = new ObjectInputStream(is)) {
                            SparseBitSet bs = (SparseBitSet) ois.readObject();
                            logger.info(rsm.getColumnName(c)+"'s cardinality is "+bs.cardinality());
                        }
                        is.reset();
                        Files.copy(
                                is,
                                Paths.get(".",rsm.getColumnName(c)),
                                StandardCopyOption.REPLACE_EXISTING
                        );
                    }
                }
            }
        }

    }



}
