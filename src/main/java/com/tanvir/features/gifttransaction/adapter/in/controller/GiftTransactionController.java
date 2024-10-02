package com.tanvir.features.gifttransaction.adapter.in.controller;

import com.tanvir.core.util.enums.QueryParams;
import com.tanvir.features.gifttransaction.application.port.in.GiftTransactionUseCase;
import com.tanvir.features.gifttransaction.application.port.in.GiftTransactionUseCaseNonReactive;
import com.tanvir.features.gifttransaction.application.port.in.dto.request.GiftTransactionRequestDto;
import com.tanvir.features.gifttransaction.application.port.in.dto.response.GiftTransactionResponseDto;
import com.tanvir.features.gifttransaction.domain.GiftTransaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@Slf4j
public class GiftTransactionController {
    private final GiftTransactionUseCaseNonReactive giftTransactionUseCase;

    public GiftTransactionController(GiftTransactionUseCaseNonReactive giftTransactionUseCase) {
        this.giftTransactionUseCase = giftTransactionUseCase;
    }

    @GetMapping("/giftTransactions")
    public ResponseEntity<List<GiftTransaction>> getGiftTransactions(
            @RequestParam String keycloakId,
            @RequestParam(required = false) String transactionType,
            @RequestParam(required = false) String searchKey,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") int offSet,
            @RequestParam String createdAfter,
            @RequestParam String createdBefore) {

        // Build the request DTO synchronously
        GiftTransactionRequestDto requestDto = buildBeanTransactionRequestDto(
                keycloakId, transactionType, searchKey, limit, offSet, createdAfter, createdBefore);

        log.info("Request DTO: {}", requestDto);

        // Call the use case synchronously
        List<GiftTransaction> responseDto = giftTransactionUseCase.getGiftTransactions(requestDto);

        // Return the response entity
        return ResponseEntity.ok(responseDto);
    }

    private GiftTransactionRequestDto buildBeanTransactionRequestDto(
            String keycloakId, String transactionType, String searchKey, int limit, int offSet,
            String createdAfter, String createdBefore) {

        // The implementation remains the same as the previous refactored version
        limit = Math.min(limit, 100);
        Pageable pageable = PageRequest.of(offSet, limit);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        createdAfter = createdAfter.replace(" ", "+");
        createdBefore = createdBefore.replace(" ", "+");

        LocalDateTime createdAfterDate = LocalDateTime.parse(createdAfter, formatter);
        LocalDateTime createdBeforeDate = LocalDateTime.parse(createdBefore, formatter);

        return GiftTransactionRequestDto
                .builder()
                .keycloakId(keycloakId)
                .createdAfter(createdAfterDate)
                .createdBefore(createdBeforeDate)
                .searchKey(searchKey != null ? searchKey : "")
                .pageable(pageable)
                .transactionType(transactionType != null ? transactionType : "")
                .build();
    }

}
