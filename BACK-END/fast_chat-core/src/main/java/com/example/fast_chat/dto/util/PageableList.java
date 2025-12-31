package com.example.fast_chat.dto.util;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class PageableList<T> {
    private List<T> content;
    private long totalElements;
    private int pageNumber;
    private int pageSize;
}