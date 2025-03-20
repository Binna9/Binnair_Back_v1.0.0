package com.bb.ballBin.address.entity;

import com.bb.ballBin.common.convert.BooleanToYNConverter;
import com.bb.ballBin.common.entity.BaseEntity;
import com.bb.ballBin.user.entity.User;
import com.bb.ballBin.address.model.AddressResponseDto;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

@Entity
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "addresses")
public class Address extends BaseEntity {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(updatable = false, nullable = false, unique = true, name = "address_id")
    private String addressId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String receiver;

    @Column(name ="phone_number", nullable = false)
    private String phoneNumber;

    @Column(nullable = false, name = "postal_code")
    private String postalCode;

    @Column(nullable = false)
    private String address;

    @Convert(converter = BooleanToYNConverter.class)
    @Column(nullable = false, name = "is_default")
    private boolean isDefault;

    /**
     * Entity to DTO 변환
     */
    public AddressResponseDto toDto() {
        return AddressResponseDto.builder()
                .addressId(this.addressId)
                .receiver(this.receiver)
                .phoneNumber(this.phoneNumber)
                .postalCode(this.postalCode)
                .address(this.address)
                .isDefault(this.isDefault)
                .build();
    }
}
