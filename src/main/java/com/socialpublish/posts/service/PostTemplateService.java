package com.socialpublish.posts.service;

import com.socialpublish.auth.entity.User;
import com.socialpublish.auth.repository.UserRepository;
import com.socialpublish.posts.dto.CreatePostTemplateRequest;
import com.socialpublish.posts.dto.PostTemplateDto;
import com.socialpublish.posts.entity.PostTemplate;
import com.socialpublish.posts.repository.PostTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostTemplateService {

    private final PostTemplateRepository postTemplateRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<PostTemplateDto> getUserTemplates(UUID userId) {
        return postTemplateRepository.findAllByOwnerIdOrderByUpdatedAtDesc(userId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public PostTemplateDto createTemplate(UUID userId, CreatePostTemplateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        PostTemplate template = new PostTemplate();
        template.setOwner(user);
        template.setTemplateName(request.templateName());
        template.setContent(request.content());
        template.setPlatforms(request.platforms() != null ? request.platforms() : "");

        PostTemplate saved = postTemplateRepository.save(template);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public PostTemplateDto getTemplate(UUID userId, UUID templateId) {
        PostTemplate template = postTemplateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found"));

        if (!template.getOwner().getId().equals(userId)) {
            throw new IllegalStateException("You don't own this template");
        }

        return toDto(template);
    }

    @Transactional
    public void deleteTemplate(UUID userId, UUID templateId) {
        PostTemplate template = postTemplateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found"));

        if (!template.getOwner().getId().equals(userId)) {
            throw new IllegalStateException("You don't own this template");
        }

        postTemplateRepository.delete(template);
    }

    private PostTemplateDto toDto(PostTemplate template) {
        return new PostTemplateDto(
                template.getId(),
                template.getTemplateName(),
                template.getContent(),
                template.getPlatforms(),
                template.getUpdatedAt()
        );
    }
}
