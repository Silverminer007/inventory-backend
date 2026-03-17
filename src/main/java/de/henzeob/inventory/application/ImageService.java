package de.henzeob.inventory.application;

import de.henzeob.inventory.mapper.ImageMapper;
import de.henzeob.inventory.model.dto.ImageDTO;
import de.henzeob.inventory.model.entity.Container;
import de.henzeob.inventory.model.entity.Image;
import de.henzeob.inventory.model.entity.Item;
import de.henzeob.inventory.repository.ImageRepository;
import de.henzeob.inventory.repository.ItemRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class ImageService {

    @Inject
    ImageRepository imageRepository;

    @Inject
    ImageMapper imageMapper;

    @Inject
    ItemRepository itemRepository;

    @Inject
    ContainerService containerService;

    @Inject
    S3Client s3Client;

    @ConfigProperty(name = "inventory.s3.bucket-name", defaultValue = "inventory-images")
    String bucketName;

    @ConfigProperty(name = "quarkus.s3.endpoint-override", defaultValue = "http://localhost:9000")
    String s3Endpoint;

    @Transactional
    public ImageDTO uploadImageForItem(UUID itemId, InputStream data, long fileSize, String filename, String contentType, boolean isPrimary, String userId) {
        Item item = itemRepository.findByIdAndUser(itemId, userId)
                .orElseThrow(() -> new NotFoundException("Item nicht gefunden"));

        String s3Key = buildS3Key(userId, "items", itemId, filename);
        uploadToS3(s3Key, data, fileSize, contentType);

        Image image = new Image();
        image.item = item;
        image.s3Key = s3Key;
        image.s3Url = getS3Url(s3Key);
        image.filename = filename;
        image.contentType = contentType;
        image.fileSize = fileSize;
        image.isPrimary = isPrimary;
        image.userId = userId;

        imageRepository.persist(image);
        return imageMapper.toDTO(image);
    }

    @Transactional
    public ImageDTO uploadImageForContainer(UUID containerId, InputStream data, long fileSize, String filename, String contentType, boolean isPrimary, String userId) {
        Container container = containerService.getContainer(containerId, userId);

        String s3Key = buildS3Key(userId, "containers", containerId, filename);
        uploadToS3(s3Key, data, fileSize, contentType);

        Image image = new Image();
        image.container = container;
        image.s3Key = s3Key;
        image.s3Url = getS3Url(s3Key);
        image.filename = filename;
        image.contentType = contentType;
        image.fileSize = fileSize;
        image.isPrimary = isPrimary;
        image.userId = userId;

        imageRepository.persist(image);
        return imageMapper.toDTO(image);
    }

    public List<ImageDTO> getImagesForItem(UUID itemId, String userId) {
        itemRepository.findByIdAndUser(itemId, userId)
                .orElseThrow(() -> new NotFoundException("Item nicht gefunden"));

        return imageRepository.findByItemAndUser(itemId, userId)
                .stream()
                .map(imageMapper::toDTO)
                .collect(Collectors.toList());
    }

    public List<ImageDTO> getImagesForContainer(UUID containerId, String userId) {
        containerService.getContainer(containerId, userId);

        return imageRepository.findByContainerAndUser(containerId, userId)
                .stream()
                .map(imageMapper::toDTO)
                .collect(Collectors.toList());
    }

    public ImageDTO getImage(UUID imageId, String userId) {
        Image image = imageRepository.findByIdAndUser(imageId, userId)
                .orElseThrow(() -> new NotFoundException("Bild nicht gefunden"));
        return imageMapper.toDTO(image);
    }

    @Transactional
    public void deleteImage(UUID imageId, String userId) {
        Image image = imageRepository.findByIdAndUser(imageId, userId)
                .orElseThrow(() -> new NotFoundException("Bild nicht gefunden"));

        deleteFromS3(image.s3Key);
        imageRepository.delete(image);
    }

    public String uploadToS3Temp(InputStream data, long fileSize, String filename, String contentType, String userId) {
        String safeFilename = filename != null ? filename.replaceAll("[^a-zA-Z0-9._-]", "_") : "file";
        String s3Key = userId + "/temp/" + UUID.randomUUID() + "_" + safeFilename;
        uploadToS3(s3Key, data, fileSize, contentType);
        return s3Key;
    }

    public String buildTempS3Url(String s3Key) {
        return getS3Url(s3Key);
    }

    @Transactional
    public ImageDTO linkImageFromS3Key(UUID id, String s3Key, String filename, String contentType, Long fileSize,
                                       boolean isPrimary, UUID itemId, UUID containerId, String userId) {
        Image image = new Image();
        if (id != null) image.id = id;
        image.s3Key = s3Key;
        image.s3Url = getS3Url(s3Key);
        image.filename = filename;
        image.contentType = contentType;
        image.fileSize = fileSize != null ? fileSize : 0L;
        image.isPrimary = isPrimary;
        image.userId = userId;

        if (itemId != null) {
            image.item = itemRepository.findByIdAndUser(itemId, userId)
                    .orElseThrow(() -> new jakarta.ws.rs.NotFoundException("Item nicht gefunden"));
        } else if (containerId != null) {
            image.container = containerService.getContainer(containerId, userId);
        } else {
            throw new IllegalArgumentException("Either itemId or containerId must be provided");
        }

        imageRepository.persist(image);
        return imageMapper.toDTO(image);
    }

    @Transactional
    public ImageDTO setPrimaryImage(UUID imageId, String userId) {
        Image image = imageRepository.findByIdAndUser(imageId, userId)
                .orElseThrow(() -> new jakarta.ws.rs.NotFoundException("Bild nicht gefunden"));
        image.isPrimary = true;
        imageRepository.persist(image);
        return imageMapper.toDTO(image);
    }

    private String buildS3Key(String userId, String entityType, UUID entityId, String filename) {
        String safeFilename = filename.replaceAll("[^a-zA-Z0-9._-]", "_");
        return userId + "/" + entityType + "/" + entityId + "/" + UUID.randomUUID() + "_" + safeFilename;
    }

    private void uploadToS3(String s3Key, InputStream data, long fileSize, String contentType) {
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .contentType(contentType)
                .build();

        s3Client.putObject(putRequest, RequestBody.fromInputStream(data, fileSize));
    }

    private String getS3Url(String s3Key) {
        return s3Endpoint + "/" + bucketName + "/" + s3Key;
    }

    private void deleteFromS3(String s3Key) {
        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

        s3Client.deleteObject(deleteRequest);
    }
}
