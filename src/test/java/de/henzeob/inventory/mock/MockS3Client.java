package de.henzeob.inventory.mock;

import io.quarkus.test.Mock;
import jakarta.enterprise.context.ApplicationScoped;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mock
@ApplicationScoped
public class MockS3Client implements S3Client {

    private final Map<String, byte[]> storage = new ConcurrentHashMap<>();

    @Override
    public PutObjectResponse putObject(PutObjectRequest putObjectRequest, RequestBody requestBody) {
        try (var stream = requestBody.contentStreamProvider().newStream()) {
            storage.put(key(putObjectRequest.bucket(), putObjectRequest.key()), stream.readAllBytes());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return PutObjectResponse.builder().build();
    }

    @Override
    public ResponseBytes<GetObjectResponse> getObjectAsBytes(GetObjectRequest getObjectRequest) {
        byte[] data = storage.get(key(getObjectRequest.bucket(), getObjectRequest.key()));
        if (data == null) {
            throw NoSuchKeyException.builder().message("No such key").build();
        }
        GetObjectResponse response = GetObjectResponse.builder().contentType("image/jpeg").build();
        return ResponseBytes.fromByteArray(response, data);
    }

    @Override
    public <T> T getObject(GetObjectRequest getObjectRequest, ResponseTransformer<GetObjectResponse, T> responseTransformer) {
        throw new UnsupportedOperationException("Not used in tests - use getObjectAsBytes");
    }

    @Override
    public DeleteObjectResponse deleteObject(DeleteObjectRequest deleteObjectRequest) {
        storage.remove(key(deleteObjectRequest.bucket(), deleteObjectRequest.key()));
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

    private String key(String bucket, String objectKey) {
        return bucket + "/" + objectKey;
    }
}
