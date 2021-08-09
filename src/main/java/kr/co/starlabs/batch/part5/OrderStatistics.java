package kr.co.starlabs.batch.part5;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

// 합산 금액을 담는 Class

@Getter
public class OrderStatistics {

    private String amount;

    private LocalDate date;

    @Builder
    private OrderStatistics(String amount, LocalDate date) {
        this.amount = amount;
        this.date = date;
    }


}
