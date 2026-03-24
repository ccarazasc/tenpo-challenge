package com.tenpo.challenge.infrastructure.adapter.out.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("api_call_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiCallEntity {

    @Id
    private Long id;

    private String endpoint;

    private String parameters;

    private String response;

    private boolean success;

    @Column("timestamp")
    private LocalDateTime timestamp;
}
