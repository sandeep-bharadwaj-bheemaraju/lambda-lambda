package com.sandeep.learning.lambdalambda.handler;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.glue.AWSGlue;
import com.amazonaws.services.glue.AWSGlueClientBuilder;
//import com.amazonaws.services.glue.model.StartCrawlerRequest;
import com.amazonaws.services.glue.model.StartJobRunRequest;
import com.amazonaws.services.glue.model.StartJobRunResult;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.joda.time.DateTime;

import java.util.HashMap;
import java.util.Map;

public class LambdaCustomEventHandler implements RequestHandler<LambdaEvent, LambdaResponse> {

    private AmazonDynamoDB amazonDynamoDB;

    private DynamoDB dynamoDB;

    private AmazonS3 amazonS3;

    private AWSGlue awsGlue;

    private Map<String, String> configMap;

    public LambdaCustomEventHandler() {

        amazonDynamoDB = AmazonDynamoDBClientBuilder.standard().build();

        dynamoDB = new DynamoDB(amazonDynamoDB);

        amazonS3 = AmazonS3ClientBuilder.standard().build();

        awsGlue = AWSGlueClientBuilder.standard().build();

        loadConfiguration();
    }


    @Override
    public LambdaResponse handleRequest(LambdaEvent event, Context context) {

        context.getLogger().log("Lambda Event is [" +event+ "]");

        return inspectReady(context);
    }


    private void loadConfiguration() {
        configMap = new HashMap<>();

        Table configTable = dynamoDB.getTable("configuration");

        Item item = configTable.getItem("CONFIG_KEY", "JOB");
        configMap.put("JOB", item.getString("CONFIG_VALUE"));

        item = configTable.getItem("CONFIG_KEY", "BUCKET");
        configMap.put("BUCKET", item.getString("CONFIG_VALUE"));

        //item = configTable.getItem("CONFIG_KEY", "CRAWLER");
        //configMap.put("CRAWLER", item.getString("CONFIG_VALUE"));

        item = configTable.getItem("CONFIG_KEY", "READY-DIR-PATH");
        configMap.put("READY-DIR-PATH", item.getString("CONFIG_VALUE"));

        item = configTable.getItem("CONFIG_KEY", "IN-PROCESS-DIR-PATH");
        configMap.put("IN-PROCESS-DIR-PATH", item.getString("CONFIG_VALUE"));
    }


    private LambdaResponse inspectReady(Context context) {

        LambdaResponse lambdaResponse = new LambdaResponse();

        StringBuilder inProcessFiles = new StringBuilder();
        String fileName;

        String jobRunId;

        ObjectListing readyFileList = amazonS3.listObjects(new ListObjectsRequest().withBucketName(configMap.get("BUCKET")).withPrefix(configMap.get("READY-DIR-PATH")));
        int readyFilesCount = readyFileList.getObjectSummaries().size() - 1;

        context.getLogger().log("READY STATE FILES ARE - " + readyFilesCount);

        for (S3ObjectSummary summary : readyFileList.getObjectSummaries()) {
            context.getLogger().log("[" + summary.getKey() + "]");
        }

        ObjectListing inProcessFileList = amazonS3.listObjects(new ListObjectsRequest().withBucketName(configMap.get("BUCKET")).withPrefix(configMap.get("IN-PROCESS-DIR-PATH")));
        int inProcessFilesCount = inProcessFileList.getObjectSummaries().size() - 1;

        context.getLogger().log("IN-PROCESS STATE FILES ARE - " + inProcessFilesCount);

        for (S3ObjectSummary summary : inProcessFileList.getObjectSummaries()) {
            context.getLogger().log("[" + summary.getKey() + "]");
        }

        if (inProcessFilesCount == 0 && readyFilesCount > 0) {

            for (S3ObjectSummary summary : readyFileList.getObjectSummaries()) {

                fileName = summary.getKey().substring(summary.getKey().lastIndexOf("/") + 1);

                if (!fileName.contains(".csv"))
                    continue;

                amazonS3.copyObject(configMap.get("BUCKET"), summary.getKey(), configMap.get("BUCKET"), configMap.get("IN-PROCESS-DIR-PATH") + fileName);

                amazonS3.deleteObject(configMap.get("BUCKET"), summary.getKey());

                context.getLogger().log("FILE [" + fileName + "] MOVED TO IN-PROCESS STATE");

                inProcessFiles.append(fileName).append(",");
            }

            inProcessFiles = inProcessFiles.deleteCharAt(inProcessFiles.length() - 1);

            //awsGlue.startCrawler(new StartCrawlerRequest().withName(configMap.get("CRAWLER")));

            context.getLogger().log("Starting Glue job [" + configMap.get("JOB") + "] for files [" + inProcessFiles.toString() + "]");

            StartJobRunResult startJobRunResult = awsGlue.startJobRun(new StartJobRunRequest().withJobName(configMap.get("JOB")));

            context.getLogger().log("Glue Job Id is [" + startJobRunResult.getJobRunId() + "]");

            Table jobsTable = dynamoDB.getTable("jobs");

            jobsTable.putItem(new Item().with("JOB_ID", startJobRunResult.getJobRunId()).with("FILES", inProcessFiles.toString()).with("STATE", "IN-PROCESS"));

            jobRunId = startJobRunResult.getJobRunId();

            lambdaResponse.setJobRunId(jobRunId);
            lambdaResponse.setMessage("processing initiated for files "+inProcessFiles.toString());
        }
        if (inProcessFilesCount == 0 && readyFilesCount == 0) {

            context.getLogger().log("NO FILES TO PROCESS..");

            lambdaResponse.setMessage("no files left for processing");

        } else {

            context.getLogger().log("JOB RUN IN-PROGRESS..");

            lambdaResponse.setMessage("currently job in-progress, unable to initiate new job for processing");
        }

        lambdaResponse.setRequestId(context.getAwsRequestId());
        lambdaResponse.setEventTime(DateTime.now().toString());

        return lambdaResponse;
    }


}
