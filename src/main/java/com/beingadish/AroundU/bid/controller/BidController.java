package com.beingadish.AroundU.bid.controller;

import com.beingadish.AroundU.bid.dto.BidCreateRequest;
import com.beingadish.AroundU.bid.dto.BidHandshakeRequest;
import com.beingadish.AroundU.bid.dto.BidResponseDTO;
import com.beingadish.AroundU.bid.service.BidService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.beingadish.AroundU.infrastructure.ratelimit.RateLimit;

import java.util.List;

import static com.beingadish.AroundU.common.constants.URIConstants.BID_BASE;

@RestController
@RequestMapping(BID_BASE)
@RequiredArgsConstructor
public class BidController {

    private final BidService bidService;

    @PostMapping("/jobs/{jobId}/bids")
    @RateLimit(capacity = 20, refillTokens = 20, refillMinutes = 60)
    public ResponseEntity<BidResponseDTO> placeBid(@PathVariable Long jobId, @RequestParam Long workerId, @Valid @RequestBody BidCreateRequest request) {
        return new ResponseEntity<>(bidService.placeBid(jobId, workerId, request), HttpStatus.CREATED);
    }

    @GetMapping("/jobs/{jobId}/bids")
    public ResponseEntity<List<BidResponseDTO>> listBids(@PathVariable Long jobId) {
        return ResponseEntity.ok(bidService.listBidsForJob(jobId));
    }

    @PostMapping("/bids/{bidId}/accept")
    public ResponseEntity<BidResponseDTO> acceptBid(@PathVariable Long bidId, @RequestParam Long clientId) {
        return ResponseEntity.ok(bidService.acceptBid(bidId, clientId));
    }

    @PostMapping("/bids/{bidId}/handshake")
    public ResponseEntity<BidResponseDTO> handshake(@PathVariable Long bidId, @RequestParam Long workerId, @Valid @RequestBody BidHandshakeRequest request) {
        return ResponseEntity.ok(bidService.handshake(bidId, workerId, request));
    }
}
