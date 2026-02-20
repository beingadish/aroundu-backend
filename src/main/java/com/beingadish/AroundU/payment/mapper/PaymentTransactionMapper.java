package com.beingadish.AroundU.payment.mapper;

import com.beingadish.AroundU.common.constants.enums.PaymentStatus;
import com.beingadish.AroundU.payment.dto.PaymentLockRequest;
import com.beingadish.AroundU.payment.dto.PaymentResponseDTO;
import com.beingadish.AroundU.user.entity.Client;
import com.beingadish.AroundU.job.entity.Job;
import com.beingadish.AroundU.payment.entity.PaymentTransaction;
import com.beingadish.AroundU.user.entity.Worker;
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
