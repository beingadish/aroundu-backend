package com.beingadish.AroundU.Controller.Bid;

import com.beingadish.AroundU.DTO.Bid.BidCreateRequest;
import com.beingadish.AroundU.DTO.Bid.BidHandshakeRequest;
import com.beingadish.AroundU.DTO.Bid.BidResponseDTO;
import com.beingadish.AroundU.Service.BidService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bid")
@RequiredArgsConstructor
public class BidController {

    private final BidService bidService;

    @PostMapping("/jobs/{jobId}/bids")
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
