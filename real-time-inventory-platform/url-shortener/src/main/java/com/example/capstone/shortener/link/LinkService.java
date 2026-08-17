package com.example.capstone.shortener.link;

import java.net.URI;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class LinkService {

    private final ShortLinkRepository shortLinkRepository;
    private final UrlCodeGenerator codeGenerator;

    public LinkService(ShortLinkRepository shortLinkRepository, UrlCodeGenerator codeGenerator) {
        this.shortLinkRepository = shortLinkRepository;
        this.codeGenerator = codeGenerator;
    }

    public LinkResponse create(CreateLinkRequest request) {
        String code = codeGenerator.uniqueCode(shortLinkRepository::existsByCode);
        ShortLink link = new ShortLink(
                code,
                request.originalUrl().trim(),
                normalizeOwner(request.ownerKey()),
                request.expiresAt()
        );
        return LinkResponse.from(shortLinkRepository.save(link));
    }

    @Transactional(readOnly = true)
    public LinkResponse get(String code) {
        return shortLinkRepository.findByCode(code)
                .map(LinkResponse::from)
                .orElseThrow(() -> new ShortLinkNotFoundException(code));
    }

    public URI redirect(String code) {
        ShortLink link = shortLinkRepository.findByCode(code)
                .filter(candidate -> candidate.isActive() && !candidate.isExpired(Instant.now()))
                .orElseThrow(() -> new ShortLinkNotFoundException(code));
        link.recordClick();
        return URI.create(link.getOriginalUrl());
    }

    private String normalizeOwner(String ownerKey) {
        return StringUtils.hasText(ownerKey) ? ownerKey.trim() : null;
    }
}
