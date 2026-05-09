package com.Vaish.SpringSentinel.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Entity
@Table(name = "bots")
@Data
public class Bot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Bot name cannot be empty")
    private String name;

    @Column(name = "persona_description",
            columnDefinition = "TEXT")
    private String personaDescription;
}