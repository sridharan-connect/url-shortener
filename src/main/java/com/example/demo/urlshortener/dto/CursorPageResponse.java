package com.example.demo.urlshortener.dto;

import java.util.List;

public class CursorPageResponse<T> {

    private List<T> items;
    private Long nextCursor;
    private boolean hasNext;

    public CursorPageResponse(
            List<T> items,
            Long nextCursor,
            boolean hasNext
    ) {
        this.items = items;
        this.nextCursor = nextCursor;
        this.hasNext = hasNext;
    }

    public List<T> getItems() {
        return items;
    }

    public Long getNextCursor() {
        return nextCursor;
    }

    public boolean isHasNext() {
        return hasNext;
    }
}
