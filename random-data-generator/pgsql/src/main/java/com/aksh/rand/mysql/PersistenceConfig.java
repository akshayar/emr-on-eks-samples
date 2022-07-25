/**
 * 
 */
package com.aksh.rand.mysql;

import java.util.Base64;
import java.util.Map;

import javax.sql.DataSource;

import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.amazonaws.services.secretsmanager.model.*;
import com.google.gson.Gson;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * @author rawaaksh
 *
 */
@Configuration
@MapperScan("com.aksh.rand.mysql")
public class PersistenceConfig {
	
	@Value("${db.server}")
	private String server;
	@Value("${db.port}")
	private String port;
	@Value("${db.dbName}")
	private String dbName;
	@Value("${secretName}")
	String secretName = "rds-mysql";
	@Value("${secretRegion}")
	String secretRegion = "ap-south-1";

	Gson gson=new Gson();

	
    @Bean
    public DataSource dataSource() {
    	Map secret=getSecret(secretName,secretRegion);
		PGSimpleDataSource ds = new PGSimpleDataSource() ;
		ds.setServerName(server);
    	ds.setPortNumber(Integer.parseInt(port));
    	ds.setDatabaseName(dbName);
    	ds.setUser(secret.get("username")+"");//secret.get("username")+"");
    	ds.setPassword(secret.get("password")+"");//secret.get("password")+"");
    	ds.setSsl(false);
    	return ds;
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory() throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource());
        return factoryBean.getObject();
    }



	// Use this code snippet in your app.
	// If you need more information about configurations or implementing the sample
	// code, visit the AWS docs:
	// https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/java-dg-samples.html#prerequisites
	public Map getSecret(String secretName,String region) {

		// Create a Secrets Manager client
		AWSSecretsManager client = AWSSecretsManagerClientBuilder.standard().withRegion(region).build();

		// In this sample we only handle the specific exceptions for the
		// 'GetSecretValue' API.
		// See
		// https://docs.aws.amazon.com/secretsmanager/latest/apireference/API_GetSecretValue.html
		// We rethrow the exception by default.

		String secret=null;String decodedBinarySecret=null;
		GetSecretValueRequest getSecretValueRequest = new GetSecretValueRequest().withSecretId(secretName);
		GetSecretValueResult getSecretValueResult = null;

		try {
			getSecretValueResult = client.getSecretValue(getSecretValueRequest);
		} catch (DecryptionFailureException e) {
			// Secrets Manager can't decrypt the protected secret text using the provided
			// KMS key.
			// Deal with the exception here, and/or rethrow at your discretion.
			throw e;
		} catch (InternalServiceErrorException e) {
			// An error occurred on the server side.
			// Deal with the exception here, and/or rethrow at your discretion.
			throw e;
		} catch (InvalidParameterException e) {
			// You provided an invalid value for a parameter.
			// Deal with the exception here, and/or rethrow at your discretion.
			throw e;
		} catch (InvalidRequestException e) {
			// You provided a parameter value that is not valid for the current state of the
			// resource.
			// Deal with the exception here, and/or rethrow at your discretion.
			throw e;
		} catch (ResourceNotFoundException e) {
			// We can't find the resource that you asked for.
			// Deal with the exception here, and/or rethrow at your discretion.
			throw e;
		}

		// Decrypts secret using the associated KMS CMK.
		// Depending on whether the secret is a string or binary, one of these fields
		// will be populated.
		if (getSecretValueResult.getSecretString() != null) {
			secret = getSecretValueResult.getSecretString();
			return gson.fromJson(secret, Map.class);
		} else {
			decodedBinarySecret = new String(
					Base64.getDecoder().decode(getSecretValueResult.getSecretBinary()).array());
			return gson.fromJson(decodedBinarySecret, Map.class);
		}

	}

}