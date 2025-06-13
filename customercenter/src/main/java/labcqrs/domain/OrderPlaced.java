package labcqrs.domain;

import java.time.LocalDate;
import java.util.*;
import labcqrs.infra.AbstractEvent;
import lombok.Data;

@Data
public class OrderPlaced extends AbstractEvent {

    private Long id;
    private String productId;
    private Integer qty;
    private String customerId;
    private Double amount;
    private String status;
    private String address;
}
