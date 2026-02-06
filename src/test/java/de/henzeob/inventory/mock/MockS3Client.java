package de.henzeob.inventory.mock;

import io.quarkus.test.Mock;
import jakarta.enterprise.context.ApplicationScoped;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mock
@ApplicationScoped
public class MockS3Client implements S3Client {

    private final Map<String, byte[]> storage = new ConcurrentHashMap<>();

    @Override
    public PutObjectResponse putObject(PutObjectRequest putObjectRequest, RequestBody requestBody) {
        storage.put(putObjectRequest.bucket() + "/" + putObjectRequest.key(), new byte[0]);
        return PutObjectResponse.builder().build();
    }

    @Override
    public DeleteObjectResponse deleteObject(DeleteObjectRequest deleteObjectRequest) {
        storage.remove(deleteObjectRequest.bucket() + "/" + deleteObjectRequest.key());
        return DeleteObjectResponse.builder().build();
    }

    @Override
    public String serviceName() {
        return "s3";
    }

    @Override
    public void close() {
    }

    public Map<String, byte[]> getStorage() {
        return storage;
    }

    public void clear() {
        storage.clear();
    }
}
