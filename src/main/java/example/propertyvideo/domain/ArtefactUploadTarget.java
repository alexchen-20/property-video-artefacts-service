package example.propertyvideo.domain;

public record ArtefactUploadTarget(
        String originalFileName,
        String objectKey,
        String uploadUrl
) {
}
