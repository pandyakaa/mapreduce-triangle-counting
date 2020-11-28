import java.io.IOException;
import java.util.StringTokenizer;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.fs.Path;
import java.util.ArrayList;
import java.util.HashSet;

public class TriangleCount {
    public static class UniqueFollowPreprocessorMap extends Mapper<LongWritable, Text, Text, NullWritable> {
        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            String line = value.toString();
            String[] uv = line.split("\\s+");

            long u = Long.parseLong(uv[0]);
            long v = Long.parseLong(uv[1]);

            if (u < v) {
                Text sw = new Text(Long.toString(u) + ";" + Long.toString(v));
                context.write(sw, NullWritable.get());
            } else {
                Text sw = new Text(Long.toString(v) + ";" + Long.toString(u));
                context.write(sw, NullWritable.get());
            }
        }
    }

    public static class UniqueFollowPreprocessorReduce extends Reducer<Text, NullWritable, LongWritable, LongWritable> {
        public void reduce(Text key, Iterable<NullWritable> values, Context context)
                throws IOException, InterruptedException {
            String line = key.toString();
            String[] uv = line.split(";");

            long u = Long.parseLong(uv[0]);
            long v = Long.parseLong(uv[1]);

            context.write(new LongWritable(u), new LongWritable(v));
            context.write(new LongWritable(v), new LongWritable(u));
        }
    }

    public static class ConnectedByMap extends Mapper<LongWritable, Text, LongWritable, LongWritable> {
        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            String line = value.toString();
            String[] uv = line.split("\\s+");

            long u = Long.parseLong(uv[0]);
            long v = Long.parseLong(uv[1]);

            if (u < v) {
                context.write(new LongWritable(u), new LongWritable(v));
            }
        }
    }

    public static class ConnectedByReduce extends Reducer<LongWritable, LongWritable, LongWritable, Text> {
        public void reduce(LongWritable key, Iterable<LongWritable> values, Context context)
                throws IOException, InterruptedException {
            ArrayList<Long> arrvalues = new ArrayList<Long>();

            for (LongWritable val : values) {
                arrvalues.add(val.get());
            }
            for (int i = 0; i < arrvalues.size() - 1; i++) {
                for (int j = i + 1; j < arrvalues.size(); j++) {
                    Text val = new Text(Long.toString(arrvalues.get(i)) + ";" + Long.toString(arrvalues.get(j)));
                    context.write(key, val);
                }
            }
        }
    }

    public static class CountTriangleMap extends Mapper<LongWritable, Text, Text, Text> {
        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            String line = value.toString();
            String[] str = line.split("\\s+");

            String str0 = str[0];
            String str1 = str[1];

            if (str1.contains(";")) {
                context.write(new Text(str1), new Text(str0));
            } else {
                context.write(new Text(str0 + ";" + str1), new Text("$"));
            }
        }
    }

    public static class CountTriangleReduce extends Reducer<Text, Text, LongWritable, LongWritable> {
        public void reduce(Text key, Iterable<Text> values, Context context)
                throws IOException, InterruptedException {
            HashSet<String> connections = new HashSet<>();

            for (Text val : values) {
                connections.add(val.toString());
            }

            Long connSize = new Long(connections.size()) - 1;
            if (connections.contains("$") && connSize != 0) {
                context.write(new LongWritable(0), new LongWritable(connections.size()-1));
            }
        }
    }

    public static class SumCountTriangleMap extends Mapper<LongWritable, Text, LongWritable, LongWritable> {
        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            String line = value.toString();
            String[] str = line.split("\\s+");

            context.write(new LongWritable(0), new LongWritable(Long.parseLong(str[1])));
        }
    }

    public static class SumCountTriangleReduce extends Reducer<LongWritable, LongWritable, Text, LongWritable> {
        public void reduce(LongWritable key, Iterable<LongWritable> values, Context context)
                throws IOException, InterruptedException {
            Long sum = 0l;
            for (LongWritable val : values ) {
                sum += val.get();
            }

            context.write(new Text("Triangle Count"), new LongWritable(sum));
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        Job preprocessingJob = new Job(conf, "Preprocessing data");
        preprocessingJob.setJarByClass(TriangleCount.class);

        preprocessingJob.setMapperClass(UniqueFollowPreprocessorMap.class);
        preprocessingJob.setReducerClass(UniqueFollowPreprocessorReduce.class);

        preprocessingJob.setOutputKeyClass(LongWritable.class);
        preprocessingJob.setOutputValueClass(LongWritable.class);

        preprocessingJob.setMapOutputKeyClass(Text.class);
        preprocessingJob.setMapOutputValueClass(NullWritable.class);

        preprocessingJob.setInputFormatClass(TextInputFormat.class);
        preprocessingJob.setOutputFormatClass(TextOutputFormat.class);

        Path outputPath = new Path(args[1]);
        FileInputFormat.addInputPath(preprocessingJob, new Path(args[0]));
        FileOutputFormat.setOutputPath(preprocessingJob,
                new Path("/home/aptanagi/Documents/college/PAT/mapreduce-triangle-counting/temp/preprocess-output"));

        // =============================================
        Job connectedJob = new Job(conf, "Connected");
        connectedJob.setJarByClass(TriangleCount.class);

        connectedJob.setMapperClass(ConnectedByMap.class);
        connectedJob.setReducerClass(ConnectedByReduce.class);

        connectedJob.setOutputKeyClass(LongWritable.class);
        connectedJob.setOutputValueClass(Text.class);

        connectedJob.setMapOutputKeyClass(LongWritable.class);
        connectedJob.setMapOutputValueClass(LongWritable.class);

        connectedJob.setInputFormatClass(TextInputFormat.class);
        connectedJob.setOutputFormatClass(TextOutputFormat.class);

        FileInputFormat.addInputPath(connectedJob,
                new Path("/home/aptanagi/Documents/college/PAT/mapreduce-triangle-counting/temp/preprocess-output"));
        // FileOutputFormat.setOutputPath(connectedJob, new Path(args[1]));
        FileOutputFormat.setOutputPath(connectedJob,
                new Path("/home/aptanagi/Documents/college/PAT/mapreduce-triangle-counting/temp/count-output"));

        // =============================================
        Job countJob = new Job(conf, "Count Triangle");
        countJob.setJarByClass(TriangleCount.class);

        countJob.setMapperClass(CountTriangleMap.class);
        countJob.setReducerClass(CountTriangleReduce.class);

        countJob.setOutputKeyClass(LongWritable.class);
        countJob.setOutputValueClass(LongWritable.class);

        countJob.setMapOutputKeyClass(Text.class);
        countJob.setMapOutputValueClass(Text.class);

        countJob.setInputFormatClass(TextInputFormat.class);
        countJob.setOutputFormatClass(TextOutputFormat.class);

        FileInputFormat.addInputPath(countJob,
                new Path("/home/aptanagi/Documents/college/PAT/mapreduce-triangle-counting/temp/preprocess-output"));
        FileInputFormat.addInputPath(countJob,
                new Path("/home/aptanagi/Documents/college/PAT/mapreduce-triangle-counting/temp/count-output"));
        FileOutputFormat.setOutputPath(countJob, new Path("/home/aptanagi/Documents/college/PAT/mapreduce-triangle-counting/temp/count-triangle-output"));


        // =============================================
        Job sumJob = new Job(conf, "Sum Count Triangle");
        sumJob.setJarByClass(TriangleCount.class);

        sumJob.setMapperClass(SumCountTriangleMap.class);
        sumJob.setReducerClass(SumCountTriangleReduce.class);

        sumJob.setOutputKeyClass(Text.class);
        sumJob.setOutputValueClass(LongWritable.class);

        sumJob.setMapOutputKeyClass(LongWritable.class);
        sumJob.setMapOutputValueClass(LongWritable.class);

        sumJob.setInputFormatClass(TextInputFormat.class);
        sumJob.setOutputFormatClass(TextOutputFormat.class);

        FileInputFormat.addInputPath(sumJob,
                new Path("/home/aptanagi/Documents/college/PAT/mapreduce-triangle-counting/temp/count-triangle-output"));
        FileOutputFormat.setOutputPath(sumJob, new Path(args[1]));

        outputPath.getFileSystem(conf).delete(outputPath);

        int ret = preprocessingJob.waitForCompletion(true) ? 0 : 1;
        if (ret == 0) {
            ret = connectedJob.waitForCompletion(true) ? 0 : 1;
        }
        if (ret == 0) {
            ret = countJob.waitForCompletion(true) ? 0 : 1;
        }
        if (ret == 0) {
            ret = sumJob.waitForCompletion(true) ? 0 : 1;
        }

        System.exit(ret);
    }
}