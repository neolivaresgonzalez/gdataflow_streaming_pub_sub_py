package com.sdistribuidos;

import com.google.api.services.bigquery.model.TableFieldSchema;
import com.google.api.services.bigquery.model.TableRow;
import com.google.api.services.bigquery.model.TableSchema;
import org.apache.beam.runners.dataflow.options.DataflowPipelineOptions;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubMessage;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO.Write.CreateDisposition.CREATE_IF_NEEDED;
import static org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO.Write.WriteDisposition.WRITE_APPEND;

import com.google.cloud.language.v1.Document;
import com.google.cloud.language.v1.Document.Type;
import com.google.cloud.language.v1.LanguageServiceClient;
import com.google.cloud.language.v1.Sentiment;

// Pipeline en Dataflow para leer tweets desde un topico de Google PubSub y escribirlos en BigQuery.

public class TweetPipeline {
    private static final String TOPIC = "projects/sd-3-241301/topics/twitter";
    private static final String BIGQUERY_DESTINATION = "%s:twitter.tweets";
    private static final String BIGQUERY__SENTIMENTS_DESTINATION = "%s:twitter.sentiments";
    

    public static void main(String[] args) {
        PipelineOptionsFactory.register(DataflowPipelineOptions.class);
        DataflowPipelineOptions options = PipelineOptionsFactory
                .fromArgs(args)
                .withValidation()
                .as(DataflowPipelineOptions.class);
        Pipeline pipeline = Pipeline.create(options);
        pipeline.apply("Tweets_Read_PubSub", PubsubIO
                .readMessagesWithAttributes()
                .fromTopic(TOPIC))
                .apply("Tweets_Extract_Payload", ParDo.of(new ExtractTweetPayload()))
                .apply("Tweets_Write_BigQuery", BigQueryIO.writeTableRows()
                        .to(String.format(BIGQUERY_DESTINATION, options.getProject()))
                        .withCreateDisposition(CREATE_IF_NEEDED)
                        .withWriteDisposition(WRITE_APPEND)
                        .withSchema(getTableSchema()));
        pipeline.run();
        
        Pipeline pipeline2 = Pipeline.create(options);
        pipeline2.apply("Tweets_Read_BigQuery", BigQueryIO.readTableRows()
                        .fromQuery(
                            "SELECT JSON_EXTRACT(payload, '$.text') AS tweet_text, JSON_EXTRACT(payload, '$.user.screen_name') AS user_screen_name, JSON_EXTRACT(payload, '$.user.location') AS user_location, JSON_EXTRACT(payload, '$.user.followers_count') AS user_followers_count FROM `twitter.tweets` WHERE JSON_EXTRACT(payload,'$.text') LIKE '%Chile%'"))
                        .apply("Sentiment_Analysis", ParDo.of(new SentimentAnalysis()))
                        .apply("Sentiment_Write_BigQuery_Result", BigQueryIO.writeTableRows()
                        .to(String.format(BIGQUERY__SENTIMENTS_DESTINATION, options.getProject()))
                        .withCreateDisposition(CREATE_IF_NEEDED)
                        .withWriteDisposition(WRITE_APPEND)
                        .withSchema(getTableSchemaResult()));;
        pipeline2.run();

        
    }

    static class SentimentAnalysis extends DoFn<TableRow, TableRow> { 
        @ProcessElement
        public void processElement(ProcessContext c) throws IOException {
            // The text to analyze
            String tweet_text = new String(c.element().getF().get(1).toString()); 
            // Instantiates a client
            LanguageServiceClient language = LanguageServiceClient.create();
                
            Document doc = Document.newBuilder()
                .setContent(tweet_text).setType(Type.PLAIN_TEXT).build();
    
            // Detects the sentiment of the text
            Sentiment sentiment = language.analyzeSentiment(doc).getDocumentSentiment();
        
            c.output(new TableRow()
                .set("timestamp", System.currentTimeMillis())
                .set("tweet_text",tweet_text)
                .set("score",sentiment.getScore())
                .set("magnitude",sentiment.getMagnitude())
            );
            language.close();
        }
     }


    private static TableSchema getTableSchemaResult() {
        List<TableFieldSchema> fields = new ArrayList<>();
        fields.add(new TableFieldSchema().setName("timestamp").setType("INTEGER"));
        fields.add(new TableFieldSchema().setName("tweet_text").setType("STRING"));
        fields.add(new TableFieldSchema().setName("score").setType("FLOAT"));
        fields.add(new TableFieldSchema().setName("magnitude").setType("FLOAT"));
        return new TableSchema().setFields(fields);
    }

    private static TableSchema getTableSchema() {
        List<TableFieldSchema> fields = new ArrayList<>();
        fields.add(new TableFieldSchema().setName("timestamp").setType("INTEGER"));
        fields.add(new TableFieldSchema().setName("payload").setType("STRING"));
        return new TableSchema().setFields(fields);
    }

    public static class ExtractTweetPayload extends DoFn<PubsubMessage, TableRow> {
        @ProcessElement
        public void processElement(ProcessContext c) {
            String payload = new String(c.element().getPayload(), StandardCharsets.UTF_8);
            c.output(new TableRow()
                    .set("timestamp", System.currentTimeMillis())
                    .set("payload", payload)
            );
        }
    }
}
