package com.beingadish.AroundU.bid.service;

import com.beingadish.AroundU.bid.dto.BidCreateRequest;
import com.beingadish.AroundU.bid.dto.BidHandshakeRequest;
import com.beingadish.AroundU.bid.dto.BidResponseDTO;

import java.util.List;

public interface BidService {
    BidResponseDTO placeBid(Long jobId, Long workerId, BidCreateRequest request);

    List<BidResponseDTO> listBidsForJob(Long jobId);

    BidResponseDTO acceptBid(Long bidId, Long clientId);

    BidResponseDTO handshake(Long bidId, Long workerId, BidHandshakeRequest request);
}
