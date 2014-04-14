/**
 * Illustrates Kryo serialization in Java
 */
package com.oreilly.learningsparkexamples.java;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import org.apache.spark.SparkConf;
import org.apache.spark.serializer.KryoRegistrator;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function2;

import com.esotericsoftware.kryo.Kryo;

public class BasicAvgWithKryo {
  // This is our custom class we will configure Kyro to serialize
  class AvgCount {
    public AvgCount(int total, int num) {
      total_ = total;
      num_ = num;
    }
    public int total_;
    public int num_;
    public float avg() {
      return total_ / (float) num_;
    }
  }

  public class AvgRegistrator implements KryoRegistrator {
    public void registerClasses(Kryo kryo) {
      kryo.register(AvgCount.class);
    }
  }

  public static void main(String[] args) throws Exception {
		String master;
		if (args.length > 0) {
      master = args[0];
		} else {
			master = "local";
		}
    BasicAvg avg = new BasicAvg();
    avg.run(master);
  }

  public void run(String master) throws Exception {
    SparkConf conf = new SparkConf().setMaster(master).setAppName("basicavgwithkyro");
    conf.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer");
    conf.set("spark.kryo.registrator", AvgRegistrator.class.getName());
		JavaSparkContext sc = new JavaSparkContext(conf);
    JavaRDD<Integer> rdd = sc.parallelize(Arrays.asList(1, 2, 3, 4));
    Function2<AvgCount, Integer, AvgCount> addAndCount = new Function2<AvgCount, Integer, AvgCount>() {
      @Override
      public AvgCount call(AvgCount a, Integer x) {
        a.total_ += x;
        a.num_ += 1;
        return a;
      }
    };
    Function2<AvgCount, AvgCount, AvgCount> combine = new Function2<AvgCount, AvgCount, AvgCount>() {
      @Override
      public AvgCount call(AvgCount a, AvgCount b) {
        a.total_ += b.total_;
        a.num_ += b.num_;
        return a;
      }
    };
    AvgCount initial = new AvgCount(0,0);
    AvgCount result = rdd.aggregate(initial, addAndCount, combine);
    System.out.println(result.avg());
	}
}