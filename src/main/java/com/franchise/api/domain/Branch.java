package com.franchise.api.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Branch {

    private String id;

    private String name;

    @Builder.Default
    private boolean active = true;

    @Builder.Default
    private List<Product> products = new ArrayList<>();
}
