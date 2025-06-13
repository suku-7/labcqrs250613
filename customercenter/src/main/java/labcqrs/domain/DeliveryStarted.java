package labcqrs.domain;

import java.time.LocalDate;
import java.util.*;
import labcqrs.infra.AbstractEvent;
import lombok.Data;

@Data
public class DeliveryStarted extends AbstractEvent {

    private Long id;
    private String address;
    private String customerId;
    private Integer quanity;
    private Long orderId;
    private String status;
}
