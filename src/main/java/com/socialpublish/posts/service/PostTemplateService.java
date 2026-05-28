package com.socialpublish.posts.service;

import com.socialpublish.auth.entity.User;
import com.socialpublish.auth.repository.UserRepository;
import com.socialpublish.integrations.service.IntegrationStatusService;
import com.socialpublish.posts.dto.PostUpsertRequest;
import com.socialpublish.posts.dto.CreatePostTemplateRequest;
import com.socialpublish.posts.dto.PostTemplateDto;
import com.socialpublish.posts.entity.PostTemplate;
import com.socialpublish.posts.repository.PostTemplateRepository;
import com.socialpublish.posts.mapper.PostTemplateMapper;
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
    private final IntegrationStatusService integrationStatusService;
    private final PostTemplateMapper postTemplateMapper;

    @Transactional(readOnly = true)
    public List<PostTemplateDto> getUserTemplates(UUID userId) {
        var labels = integrationStatusService.getAccountLabels(userId);
        return postTemplateRepository.findAllByOwnerIdOrderByUpdatedAtDesc(userId)
                .stream()
                .map(t -> postTemplateMapper.toDto(t, labels))
                .collect(Collectors.toList());
    }

    @Transactional
    public PostTemplateDto createTemplate(UUID userId, CreatePostTemplateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        PostTemplate template = postTemplateMapper.toEntity(request);
        template.setOwner(user);

        PostTemplate saved = postTemplateRepository.save(template);
        return postTemplateMapper.toDto(saved, integrationStatusService.getAccountLabels(userId));
    }

    @Transactional(readOnly = true)
    public PostTemplateDto getTemplate(UUID userId, UUID templateId) {
        PostTemplate template = postTemplateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found"));

        if (!template.getOwner().getId().equals(userId)) {
            throw new IllegalStateException("You don't own this template");
        }

        return postTemplateMapper.toDto(template, integrationStatusService.getAccountLabels(userId));
    }

    @Transactional(readOnly = true)
    public PostUpsertRequest createUpsertRequest(UUID userId, UUID templateId) {
        PostTemplate template = postTemplateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found"));

        if (!template.getOwner().getId().equals(userId)) {
            throw new IllegalStateException("You don't own this template");
        }
        return postTemplateMapper.toUpsertRequest(template);
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
}
