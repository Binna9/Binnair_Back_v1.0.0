package com.bb.ballBin.user.address.entity;

import com.bb.ballBin.common.entity.BaseEntity;
import com.bb.ballBin.user.entity.User;
import com.bb.ballBin.user.address.model.AddressResponseDto;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "addresses")
public class Address extends BaseEntity {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(updatable = false, nullable = false, unique = true, name = "address_id", length = 36)
    private String addressId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 50)
    private String receiver;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false, name = "postal_code1")
    private String postalCode1;
    @Column(name = "postal_code2")
    private String postalCode2;
    @Column(name = "postal_code3")
    private String postalCode3;

    @Column(nullable = false)
    private String address1;
    @Column
    private String address2;
    @Column(nullable = false)
    private String address3;

    @Column(nullable = false, name = "default_address_index")
    private Integer defaultAddressIndex;

    /**
     * Entity to DTO 변환
     */
    public AddressResponseDto toDto() {
        return AddressResponseDto.builder()
                .addressId(this.addressId)
                .receiver(this.receiver)
                .phone(this.phone)
                .postalCode1(this.postalCode1)
                .address1(this.address1)
                .address2(this.address2)

                .build();
    }
}
