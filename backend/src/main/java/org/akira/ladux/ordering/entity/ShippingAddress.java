package org.akira.ladux.ordering.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingAddress {

    @Column(name = "shipping_receiver_name", nullable = false, length = 150)
    private String receiverName;

    @Column(name = "shipping_phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "shipping_street", nullable = false, length = 255)
    private String street;

    @Column(name = "shipping_ward", nullable = false, length = 100)
    private String ward;

    @Column(name = "shipping_district", nullable = false, length = 100)
    private String district;

    @Column(name = "shipping_city", nullable = false, length = 100)
    private String city;
}