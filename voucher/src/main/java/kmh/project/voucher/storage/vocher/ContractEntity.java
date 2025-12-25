package kmh.project.voucher.storage.vocher;

import jakarta.persistence.*;
import kmh.project.voucher.common.type.VoucherAmountType;
import kmh.project.voucher.common.type.VoucherStatusType;
import kmh.project.voucher.storage.BaseEntity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Table(name = "contract")
@Entity
public class ContractEntity extends BaseEntity {
    private String code; // 계약의 고유 코드
    private LocalDate validFrom; // 계약의 유효 기간 시작일
    private LocalDate validTo; // 계약의 유효 기간 종료일
    private Integer voucherValidPeriodDayCount; // 상품권 유효일자

    public ContractEntity(String code, LocalDate validFrom, LocalDate validTo, Integer voucherValidPeriodDayCount) {
        this.code = code;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.voucherValidPeriodDayCount = voucherValidPeriodDayCount;
    }

    public ContractEntity() {}

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public LocalDate getValidTo() {
        return validTo;
    }

    public Integer getVoucherValidPeriodDayCount() {
        return voucherValidPeriodDayCount;
    }

    public String getCode() {
        return code;
    }
}
