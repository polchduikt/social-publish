package com.socialpublish.dashboard.dto;

public record DashboardStatusSliceView(
        String labelKey,
        long count,
        int percent,
        String cssClass
) {
}
