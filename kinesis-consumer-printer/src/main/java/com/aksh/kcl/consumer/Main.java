/**
 * 
 */
package com.aksh.kcl.consumer;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.EC2ContainerCredentialsProviderWrapper;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.AmazonKinesisClientBuilder;

import lombok.extern.java.Log;

/**
 * Sample Amazon Kinesis Application.
 */
@SpringBootApplication
@Log
public class Main {

	@Value("${region:ap-south-1}")
	private String region;

	@Autowired
	private AWSCredentialsProvider credentialsProvider;

	public static void main(String[] args) {
		if(Optional.ofNullable(args).map(Arrays::asList).orElse(Collections.emptyList()).contains("--container-run")){
			System.setProperty("container-run","true");
		}
		SpringApplication.run(Main.class, args);
	}

	@Bean
	public AmazonKinesis kinesis() {
		return AmazonKinesisClientBuilder.standard().withCredentials(credentialsProvider).withRegion(region).build();
	}

	@Bean
	public AWSCredentialsProvider initCredentials() {
		if (Optional.ofNullable(System.getProperty("container-run")).isPresent()) {
			log.info("A container run container-run "+System.getProperty("container-run"));
			return new EC2ContainerCredentialsProviderWrapper();
		} else {
			log.info("Not a container run: "+System.getProperty("container-run"));
			// Ensure the JVM will refresh the cached IP values of AWS resources (e.g. //
			// service endpoints).
			java.security.Security.setProperty("networkaddress.cache.ttl", "60");

			/*
			 * The ProfileCredentialsProvider will return your [default] credential profile
			 * by reading from the credentials file located at (~/.aws/credentials).
			 */

			ProfileCredentialsProvider credentialsProvider = new ProfileCredentialsProvider();
			try {
				credentialsProvider.getCredentials();
			} catch (Exception e) {
				throw new AmazonClientException("Cannot load the credentials from the credential profiles file. "
						+ "Please make sure that your credentials file is at the correct "
						+ "location (~/.aws/credentials), and is in valid format.", e);
			}
			return credentialsProvider;

		}

	}
}
