package org.akira.ladux.customer.entity;

import jakarta.persistence.*;
import lombok.*;
import org.akira.ladux.identity.entity.User;

@Entity
@Table(name = "user_addresses")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user;

    @Column(nullable = false, length = 150)
    private String receiverName;

    @Column(length = 20, nullable = false)
    private String phone;

    @Column(nullable = false, length = 100)
    private String street;

    @Column(nullable = false, length = 100)
    private String ward; // Phường/ xã

    @Column(nullable = false, length = 100)
    private String district;

    @Column(nullable = false, length = 100)
    private String city;

    @Builder.Default
    @Column(nullable = false)
    private boolean isDefault = false;
}
