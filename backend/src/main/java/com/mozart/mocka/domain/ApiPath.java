package com.mozart.mocka.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApiPath {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "api_id", nullable = false)
    private ApiProjects apiProject;

    @Column
    private String key;

    @Column(columnDefinition = "jsonb")
    private String data; // JSONB 데이터를 저장

    public ApiPath(ApiProjects apiProject,String key, String data) {
        this.apiProject = apiProject;
        this.key = key;
        this.data = data;
    }
}
