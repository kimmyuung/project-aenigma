package kmh.project.voucher.storage.vocher;

import jakarta.persistence.*;
import kmh.project.voucher.common.type.VoucherAmountType;
import kmh.project.voucher.common.type.VoucherStatusType;
import kmh.project.voucher.storage.BaseEntity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Table(name = "voucher")
@Entity
public class VoucherEntity extends BaseEntity {
    private String code;
    private VoucherStatusType status;
    private LocalDate validFrom;
    private LocalDate validTo;
    @Enumerated(EnumType.STRING)
    private VoucherAmountType amount;

    @OneToMany(cascade = CascadeType.PERSIST, fetch = FetchType.EAGER)
    @JoinColumn(name = "voucher_id")
    private List<VoucherHistoryEntity> histories = new ArrayList<>();

    @ManyToOne(cascade = CascadeType.PERSIST, fetch = FetchType.EAGER)
    @JoinColumn(name = "contract_id")
    private ContractEntity contract;

    public VoucherEntity() {}

    public VoucherEntity(String code, VoucherStatusType status,
                         VoucherAmountType amount,
                         VoucherHistoryEntity voucherHistoryEntity,
                         ContractEntity contractEntity
    ) {
        this.code = code;
        this.status = status;
        this.validFrom = LocalDate.now();
        this.validTo = LocalDate.now().plusDays(contractEntity.getVoucherValidPeriodDayCount());
        this.amount = amount;

        this.histories.add(voucherHistoryEntity);
        this.contract = contractEntity;
    }

    public String code() {
        return code;
    }

    public VoucherStatusType status() {
        return status;
    }

    public LocalDate validFrom() {
        return validFrom;
    }

    public LocalDate validTo() {
        return validTo;
    }

    public VoucherAmountType amount() {
        return amount;
    }

    public List<VoucherHistoryEntity> histories() {
        return histories;
    }

    public void disable() {
        if(!this.status.equals(VoucherStatusType.PUBLISH)) {
            throw new IllegalArgumentException("Can not Use Voucher");
        }
        this.status = VoucherStatusType.DISABLE;
    }

    public void use() {
        if(!this.status.equals(VoucherStatusType.PUBLISH)) {
            throw new IllegalArgumentException("Can not Use Voucher");
        }
        this.status = VoucherStatusType.USE;
    }
}
