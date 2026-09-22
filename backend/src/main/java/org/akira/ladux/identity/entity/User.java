package org.akira.ladux.identity.entity;

import jakarta.persistence.*;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.akira.ladux.customer.entity.Customer;
import org.akira.ladux.identity.enums.AuthProvider;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 150)
    private String username;
    @Column(nullable = false, length = 150)
    private String password;
    @Builder.Default
    private boolean isActive = true;

    @Builder.Default
    @Column(name = "token_version", nullable = false)
    private int tokenVersion = 0;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @ToString.Exclude
    @Builder.Default
    private Set<Role> roles = new LinkedHashSet<>();

    @Column(name = "auth_provider", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AuthProvider authProvider = AuthProvider.LOCAL;

    @Column(name="google_subject", unique = true, length = 255)
    private String googleSubject;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private Customer customer;

    // Helper method
    public void setCustomer(Customer customer) {
        // Đặt customer cho user
        this.customer = customer;
        if (customer != null) {
            // Luư user ngược lại customer để duy trì mối quan hệ hai chiều
            customer.setUser(this);
        }
    }
}
