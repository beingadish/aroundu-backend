package com.beingadish.AroundU.Mappers.Payment;

import com.beingadish.AroundU.Constants.Enums.PaymentStatus;
import com.beingadish.AroundU.DTO.Payment.PaymentLockRequest;
import com.beingadish.AroundU.DTO.Payment.PaymentResponseDTO;
import com.beingadish.AroundU.Entities.Client;
import com.beingadish.AroundU.Entities.Job;
import com.beingadish.AroundU.Entities.PaymentTransaction;
import com.beingadish.AroundU.Entities.Worker;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentTransactionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "job", source = "job")
    @Mapping(target = "client", source = "client")
    @Mapping(target = "worker", source = "worker")
    @Mapping(target = "paymentMode", source = "job.paymentMode")
    @Mapping(target = "status", constant = "ESCROW_LOCKED")
    @Mapping(target = "gatewayReference", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    PaymentTransaction toEntity(PaymentLockRequest request, Job job, Client client, Worker worker);

    @Mapping(target = "jobId", source = "job.id")
    @Mapping(target = "clientId", source = "client.id")
    @Mapping(target = "workerId", source = "worker.id")
    PaymentResponseDTO toDto(PaymentTransaction entity);

    default PaymentTransaction withStatus(PaymentTransaction tx, PaymentStatus status) {
        tx.setStatus(status);
        return tx;
    }
}
