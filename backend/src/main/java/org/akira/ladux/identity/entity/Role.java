package org.akira.ladux.identity.entity;

import jakarta.persistence.*;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.*;
import org.akira.ladux.identity.enums.RoleName;

@Entity
@Table(name = "roles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 20)
    private RoleName name;

    @ManyToMany(mappedBy = "roles")
    @ToString.Exclude
    @Builder.Default
    private Set<User> users = new LinkedHashSet<>();
}
