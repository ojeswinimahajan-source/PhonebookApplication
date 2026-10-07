package com.phonebook.model;

import java.util.List;

public class PagedResult {

    private List<Contact> items;
    private int totalCount;
    private int currentPage;
    private int pageSize;

    public PagedResult() {
    }

    public PagedResult(
            List<Contact> items,
            int totalCount,
            int currentPage,
            int pageSize) {
        this.items = items;
        this.totalCount = totalCount;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
    }

    public List<Contact> getItems() {
        return items;
    }

    public void setItems(List<Contact> items) {
        this.items = items;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}