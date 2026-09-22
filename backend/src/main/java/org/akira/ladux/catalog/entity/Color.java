package org.akira.ladux.catalog.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "colors")
public class Color {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "name", nullable = false, unique = true)
    private String name;
    @Column(name = "hex_code", nullable = false, unique = true)
    private String hexCode; // Mã màu HEX, ví dụ: #FFFFFF cho màu trắng
}
