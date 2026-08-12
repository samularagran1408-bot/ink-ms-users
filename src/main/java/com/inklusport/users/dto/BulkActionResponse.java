package com.inklusport.users.dto;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class BulkActionResponse {

    private int succeeded;
    private int failed;

    @Builder.Default
    private List<String> succeededEmails = new ArrayList<>();

    @Builder.Default
    private List<String> failedEmails = new ArrayList<>();

    @Builder.Default
    private List<String> errors = new ArrayList<>();
}
