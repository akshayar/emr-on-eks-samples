/**
 * 
 */
package com.aksh.kcl.consumer;

/**
 * @author aksrawat
 *
 */
import java.net.InetAddress;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.ShutdownReason;
import com.amazonaws.services.kinesis.model.Record;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.InitialPositionInStream;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.Worker;
import com.amazonaws.services.kinesis.model.ResourceNotFoundException;

import lombok.extern.slf4j.Slf4j;

/**
 * Sample Amazon Kinesis Application.
 */
@Component
@Slf4j
public final class ApplicationBootstraper {

    /*
     * Before running the code:
     *      Fill in your AWS access credentials in the provided credentials
     *      file template, and be sure to move the file to the default location
     *      (~/.aws/credentials) where the sample code will load the
     *      credentials from.
     *      https://console.aws.amazon.com/iam/home?#security_credential
     *
     * WARNING:
     *      To avoid accidental leakage of your credentials, DO NOT keep
     *      the credentials file in your source directory.
     */
    
    @Value("${streamName:first-test}")
    public String streamName ;
    
    @Value("${applicationName:SampleKinesisApplication}")
    private String applicationName;
    
    @Value("${initPosition:TRIM_HORIZON}")
    private String initPosition;
    
    @Value("${region:ap-south-1}")
    private String region;
    
    private InitialPositionInStream initPositionToConsume;

    @Autowired
    AmazonKinesis kinesis ;
    
    Worker worker;

    @Autowired
    private AWSCredentialsProvider credentialsProvider;

    @PostConstruct
    void init() throws Exception {
        initPositionToConsume=InitialPositionInStream.valueOf(initPosition);	
        ExecutorService executor=Executors.newSingleThreadExecutor();
    	executor.execute(()->{
    		try {
				initApplication();
			} catch (Exception e) {
				log.error("Error while starting",e);
				shutdown();
			}
    	});
        
    }

    @PreDestroy
    void shutdown() {
		log.info("Shutting down");
		try {
			worker.startGracefulShutdown().get();
		} catch (InterruptedException | ExecutionException e) {
			log.error("Error shutting down");
		}
		log.info("Graceful shutdown");
	}


	public void  initApplication() throws Exception {

        String workerId = InetAddress.getLocalHost().getCanonicalHostName() + ":" + UUID.randomUUID();
        KinesisClientLibConfiguration kinesisClientLibConfiguration =
                new KinesisClientLibConfiguration(applicationName,
                        streamName,
                        credentialsProvider,
                        workerId);
        kinesisClientLibConfiguration.withRegionName(region);
        kinesisClientLibConfiguration.withInitialPositionInStream(initPositionToConsume);
        IRecordProcessorFactory recordProcessorFactory=new IRecordProcessorFactory() {
            @Override
            public IRecordProcessor createProcessor() {
                return new SampleRecordProcessor();
            }
        };
        worker= new Worker(recordProcessorFactory, kinesisClientLibConfiguration);

        log.info("Running {} to process stream {} as worker {}...",
                applicationName,
                streamName,
                workerId);

        int exitCode = 0;
        try {
            worker.run();
        } catch (Throwable t) {
            System.err.println("Caught throwable while processing data.");
            t.printStackTrace();
            exitCode = 1;
        }
        System.exit(exitCode);
    }

}
