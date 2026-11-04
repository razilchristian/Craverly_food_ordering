package com.cravely.backend.dto;

import java.util.ArrayList;
import java.util.List;

public class SyncCartRequest {

    private List<SyncCartItemRequest> items = new ArrayList<>();

    public SyncCartRequest() {
    }

    public SyncCartRequest(List<SyncCartItemRequest> items) {
        this.items = items;
    }

    public List<SyncCartItemRequest> getItems() {
        return items;
    }

    public void setItems(List<SyncCartItemRequest> items) {
        this.items = items;
    }
}

