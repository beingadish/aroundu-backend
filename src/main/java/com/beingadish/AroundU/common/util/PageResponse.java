package com.beingadish.AroundU.common.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.Collections;
import java.util.List;

/**
 * Cache-safe, Jackson-serializable replacement for {@link Page}.
 * <p>
 * Must be a non-final POJO with a default constructor so that
 * {@code GenericJackson2JsonRedisSerializer} can round-trip it
 * through Redis (type info via {@code @class}).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;

    /**
     * Convenience constructor that converts a Spring {@link Page} into
     * a cache-safe wrapper.
     */
    public PageResponse(Page<T> springPage) {
        this.content = springPage.getContent();
        this.page = springPage.getNumber();
        this.size = springPage.getSize();
        this.totalElements = springPage.getTotalElements();
        this.totalPages = springPage.getTotalPages();
        this.last = springPage.isLast();
    }

    /**
     * Convenience: mirrors {@link Page#isEmpty()}.
     */
    public boolean isEmpty() {
        return content == null || content.isEmpty();
    }

    /**
     * Returns an empty page response.
     */
    public static <T> PageResponse<T> empty() {
        return new PageResponse<>(Collections.emptyList(), 0, 0, 0, 0, true);
    }
}
