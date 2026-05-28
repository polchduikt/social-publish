package com.socialpublish.posts.mapper;

import com.socialpublish.media.dto.MediaUploadResult;
import com.socialpublish.media.entity.PostMedia;
import com.socialpublish.posts.entity.Post;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PostMediaMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "post", source = "post")
    @Mapping(target = "publicId", source = "upload.publicId")
    @Mapping(target = "secureUrl", source = "upload.secureUrl")
    @Mapping(target = "format", source = "upload.format")
    @Mapping(target = "width", source = "upload.width")
    @Mapping(target = "height", source = "upload.height")
    @Mapping(target = "bytes", source = "upload.bytes")
    @Mapping(target = "sortOrder", source = "sortOrder")
    PostMedia toPostMedia(Post post, MediaUploadResult upload, int sortOrder);
}
