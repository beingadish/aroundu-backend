package com.beingadish.AroundU.Service;

import com.beingadish.AroundU.DTO.Bid.BidCreateRequest;
import com.beingadish.AroundU.DTO.Bid.BidHandshakeRequest;
import com.beingadish.AroundU.DTO.Bid.BidResponseDTO;

import java.util.List;

public interface BidService {
    BidResponseDTO placeBid(Long jobId, Long workerId, BidCreateRequest request);

    List<BidResponseDTO> listBidsForJob(Long jobId);

    BidResponseDTO acceptBid(Long bidId, Long clientId);

    BidResponseDTO handshake(Long bidId, Long workerId, BidHandshakeRequest request);
}
