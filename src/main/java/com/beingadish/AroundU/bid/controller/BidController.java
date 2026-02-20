package com.beingadish.AroundU.bid.controller;

import com.beingadish.AroundU.bid.dto.BidCreateRequest;
import com.beingadish.AroundU.bid.dto.BidHandshakeRequest;
import com.beingadish.AroundU.bid.dto.BidResponseDTO;
import com.beingadish.AroundU.bid.service.BidService;
import com.beingadish.AroundU.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Bids", description = "Bid placement, listing, acceptance, and worker handshake")
@SecurityRequirement(name = "bearerAuth")
public class BidController {

    private final BidService bidService;

    @PostMapping("/jobs/{jobId}/bids")
    @RateLimit(capacity = 20, refillTokens = 20, refillMinutes = 60)
    @Operation(summary = "Place bid", description = "Worker places a bid on an open job. Rate limited: 20 requests/hour.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Bid placed successfully",
                content = @Content(schema = @Schema(implementation = BidResponseDTO.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or duplicate bid",
                content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Job not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    public ResponseEntity<BidResponseDTO> placeBid(
            @Parameter(description = "Job ID to bid on", required = true) @PathVariable Long jobId,
            @Parameter(description = "Worker ID placing the bid", required = true) @RequestParam Long workerId,
            @Valid @RequestBody BidCreateRequest request) {
        return new ResponseEntity<>(bidService.placeBid(jobId, workerId, request), HttpStatus.CREATED);
    }

    @GetMapping("/jobs/{jobId}/bids")
    @Operation(summary = "List bids for job", description = "Returns all bids submitted for a specific job")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Bids retrieved",
                content = @Content(schema = @Schema(implementation = BidResponseDTO.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Job not found")
    })
    public ResponseEntity<List<BidResponseDTO>> listBids(
            @Parameter(description = "Job ID", required = true) @PathVariable Long jobId) {
        return ResponseEntity.ok(bidService.listBidsForJob(jobId));
    }

    @PostMapping("/bids/{bidId}/accept")
    @Operation(summary = "Accept bid", description = "Client accepts a bid; other bids for the job are rejected")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Bid accepted",
                content = @Content(schema = @Schema(implementation = BidResponseDTO.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Not the job owner"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Bid not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Bid already processed")
    })
    public ResponseEntity<BidResponseDTO> acceptBid(
            @Parameter(description = "Bid ID to accept", required = true) @PathVariable Long bidId,
            @Parameter(description = "Client ID (job owner)", required = true) @RequestParam Long clientId) {
        return ResponseEntity.ok(bidService.acceptBid(bidId, clientId));
    }

    @PostMapping("/bids/{bidId}/handshake")
    @Operation(summary = "Worker handshake", description = "Worker accepts or declines a selected bid")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Handshake processed",
                content = @Content(schema = @Schema(implementation = BidResponseDTO.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid handshake state"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Bid not found")
    })
    public ResponseEntity<BidResponseDTO> handshake(
            @Parameter(description = "Bid ID", required = true) @PathVariable Long bidId,
            @Parameter(description = "Worker ID", required = true) @RequestParam Long workerId,
            @Valid @RequestBody BidHandshakeRequest request) {
        return ResponseEntity.ok(bidService.handshake(bidId, workerId, request));
    }
}
