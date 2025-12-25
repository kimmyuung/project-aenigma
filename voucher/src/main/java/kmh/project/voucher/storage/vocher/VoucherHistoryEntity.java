package kmh.project.voucher.storage.vocher;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import kmh.project.voucher.common.type.RequestType;
import kmh.project.voucher.common.type.VoucherAmountType;
import kmh.project.voucher.common.type.VoucherStatusType;
import kmh.project.voucher.storage.BaseEntity;

import java.time.LocalDate;

@Table(name = "voucher_history")
@Entity
public class VoucherHistoryEntity extends BaseEntity {
    private String orderId;
    @Enumerated(EnumType.STRING)
    private RequestType requestType;
    private String requsterId;
    @Enumerated(EnumType.STRING)
    private VoucherStatusType status;
    private String descrption;
    public VoucherHistoryEntity() {}


    public VoucherHistoryEntity(String orderId, VoucherStatusType status, String requsterId, RequestType requestType, String descrption) {
        this.orderId = orderId;
        this.status = status;
        this.requsterId = requsterId;
        this.requestType = requestType;
        this.descrption = descrption;
    }

    public String descrption() {
        return descrption;
    }

    public String orderId() {
        return orderId;
    }

    public VoucherStatusType status() {
        return status;
    }

    public String getRequsterId() {
        return requsterId;
    }

    public RequestType getRequsterType() {
        return requestType;
    }
}
