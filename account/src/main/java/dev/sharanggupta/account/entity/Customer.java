package dev.sharanggupta.account.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("customer")
@Getter @Setter @AllArgsConstructor @NoArgsConstructor @ToString
public class Customer extends BaseEntity {
    @Id
    private Long customerId;
    private String name;
    private String email;
    private String mobileNumber;

}
