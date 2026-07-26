package de.henzeob.inventory.application;

import de.henzeob.inventory.model.entity.Container;
import de.henzeob.inventory.model.entity.Image;
import de.henzeob.inventory.model.entity.Item;
import de.henzeob.inventory.repository.ContainerRepository;
import de.henzeob.inventory.repository.ImageRepository;
import de.henzeob.inventory.repository.ItemRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.time.LocalDateTime;
import java.util.UUID;

@ApplicationScoped
public class ImageService {

    @Inject
    ImageRepository imageRepository;

    @Inject
    ItemRepository itemRepository;

    @Inject
    ContainerRepository containerRepository;

    @Inject
    S3Client s3Client;

    @ConfigProperty(name = "inventory.s3.bucket-name", defaultValue = "inventory-images")
    String bucketName;

    @ConfigProperty(name = "inventory.s3.bucket-base-path", defaultValue = "inventory")
    String bucketBasePath;

    public Image getExisting(UUID id) {
        return imageRepository.findByIdOptional(id)
                .orElseThrow(() -> new IllegalArgumentException("Image not found: " + id));
    }

    public Image createForItem(UUID id, Item item, LocalDateTime createdAt) {
        Image image = new Image();
        image.id = id;
        image.item = item;
        image.createdAt = createdAt;
        imageRepository.persist(image);
        return image;
    }

    public Image createForContainer(UUID id, Container container, LocalDateTime createdAt) {
        Image image = new Image();
        image.id = id;
        image.container = container;
        image.createdAt = createdAt;
        imageRepository.persist(image);
        return image;
    }

    public void delete(UUID id) {
        Image image = getExisting(id);
        if (itemRepository.existsByPrimaryImage(id) || containerRepository.existsByPrimaryImage(id)) {
            throw new IllegalArgumentException("Image is still set as primary image: " + id);
        }
        if (image.s3Key != null) {
            deleteFromS3(image.s3Key);
        }
        imageRepository.delete(image);
    }

    @Transactional
    public void storeBinary(UUID id, byte[] data, String contentType) {
        Image image = getExisting(id);
        String s3Key = bucketBasePath + "/images/" + id;
        s3Client.putObject(
                PutObjectRequest.builder().bucket(bucketName).key(s3Key).contentType(contentType).build(),
                RequestBody.fromBytes(data));
        image.s3Key = s3Key;
        image.contentType = contentType;
        imageRepository.persist(image);
    }

    public ResponseBytes<GetObjectResponse> downloadBinary(UUID id) {
        Image image = getExisting(id);
        if (image.s3Key == null) {
            throw new IllegalArgumentException("Image has no uploaded data: " + id);
        }
        return s3Client.getObjectAsBytes(GetObjectRequest.builder().bucket(bucketName).key(image.s3Key).build());
    }

    private void deleteFromS3(String s3Key) {
        s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(s3Key).build());
    }
}
