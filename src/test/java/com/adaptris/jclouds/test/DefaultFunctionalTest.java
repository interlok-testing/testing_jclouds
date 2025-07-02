package com.adaptris.jclouds.test;

import com.adaptris.testing.SingleAdapterFunctionalTest;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.io.entity.HttpEntities;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.sts.StsClient;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Properties;

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.*;

public class DefaultFunctionalTest extends SingleAdapterFunctionalTest {
    private static final String FAKE_HOST_NAME = "testlocalstack.com";
    LocalStackContainer localStack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:latest"))
            .withServices(
                    S3,
                    STS
            ).waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(60)));


    @BeforeAll
    @Override
    public void setup() throws Exception {
        adapterStartMaxWaitTime = 120000;
        System.setProperty("aws.region",  "us-east-1");
        localStack.start();
        super.setup();
    }

    @AfterAll
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        localStack.stop();
    }

    @Test
    public void test() throws Exception {
        // create file
        HttpPost httpPost = new HttpPost(String.format("%s%s", getBaseAdapterUrl(), "/api/s3/file.txt"));
        httpPost.setEntity(HttpEntities.create("{\"key\": \"value\"}"));
        try (CloseableHttpResponse response = client.execute(httpPost)) {
            Assertions.assertEquals(200, response.getCode());
        }

        // list files
        HttpGet httpGetList = new HttpGet(String.format("%s%s", getBaseAdapterUrl(), "/api/s3"));
        try (CloseableHttpResponse response = client.execute(httpGetList)) {
            Assertions.assertEquals(200, response.getCode());
        }

        // download file
        HttpGet httpGet = new HttpGet(String.format("%s%s", getBaseAdapterUrl(), "/api/s3/file.txt"));
        try (CloseableHttpResponse response = client.execute(httpGet)) {
            Assertions.assertEquals(200, response.getCode());
        }

        // copy file
        HttpGet httpCopy = new HttpGet(String.format("%s%s", getBaseAdapterUrl(), "/api/s3utils/copy?from=file.txt&to=other.txt"));
        try (CloseableHttpResponse response = client.execute(httpCopy)) {
            Assertions.assertEquals(200, response.getCode());
        }

        // confirm copy
        HttpGet httpGetOther = new HttpGet(String.format("%s%s", getBaseAdapterUrl(), "/api/s3/other.txt"));
        try (CloseableHttpResponse response = client.execute(httpGetOther)) {
            Assertions.assertEquals(200, response.getCode());
        }

        // delete file
        HttpDelete httpDeleteOther = new HttpDelete(String.format("%s%s", getBaseAdapterUrl(), "/api/s3/other.txt"));
        try (CloseableHttpResponse response = client.execute(httpDeleteOther)) {
            Assertions.assertEquals(200, response.getCode());
        }

        // confirm deletion
        try (CloseableHttpResponse response = client.execute(httpGetOther)) {
            Assertions.assertEquals(500, response.getCode());
        }
    }


    @Override
    protected void customiseVariablesIfExists(Properties props) {
        URI s3Endpoint = localStack.getEndpointOverride(S3);
        try {
            URI fakeS3Endpoint = new URI(s3Endpoint.getScheme(), s3Endpoint.getUserInfo(), FAKE_HOST_NAME, s3Endpoint.getPort(), s3Endpoint.getPath(), s3Endpoint.getQuery(), s3Endpoint.getFragment());
            props.put("s3Endpoint", fakeS3Endpoint.toString());
        } catch (URISyntaxException ex) {
            throw new RuntimeException(ex);
        }

        try {
            createBucket((String)props.get("bucket"));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        props.put("accessKey", localStack.getAccessKey());
        props.put("secretKey", localStack.getSecretKey());
        props.put("stsAccessKey", localStack.getAccessKey());
        props.put("stsSecretKey", localStack.getSecretKey());
        props.put("region", localStack.getRegion());
        props.put("stsSessionToken", getStsSessionToken());
        super.customiseVariablesIfExists(props);
    }

    public String getStsSessionToken() {
        try (StsClient stsClient = StsClient.builder()
                .endpointOverride(localStack.getEndpointOverride(STS))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(localStack.getAccessKey(), localStack.getSecretKey()))
                ).build()
        ) {
            return stsClient.getSessionToken().credentials().sessionToken();
        }
    }


    public void createBucket(String bucket) {
        try (S3Client s3Client = S3Client.builder()
                .endpointOverride(localStack.getEndpointOverride(S3))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(localStack.getAccessKey(), localStack.getSecretKey()))
                ).build()
        ) {
            try {
                s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
            } catch (BucketAlreadyExistsException ex) {
                // ignore
            }
        }
    }
}
