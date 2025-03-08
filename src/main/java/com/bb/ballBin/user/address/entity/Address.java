package com.bb.ballBin.user.address.entity;

import com.bb.ballBin.common.convert.BooleanToBitConverter;
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

    @Column(nullable = false, name = "postal_code")
    private String postalCode;

    @Column(nullable = false)
    private String address1;

    @Column
    private String address2;

    @Column(nullable = false, name = "is_default", columnDefinition = "BOOLEAN")
    @Convert(converter = BooleanToBitConverter.class)
    private Boolean isDefault = false;

    /**
     * Entity to DTO 변환
     */
    public AddressResponseDto toDto() {
        return AddressResponseDto.builder()
                .addressId(this.addressId)
                .receiver(this.receiver)
                .phone(this.phone)
                .postalCode(this.postalCode)
                .address1(this.address1)
                .address2(this.address2)
                .isDefault(this.isDefault)
                .build();
    }
}
