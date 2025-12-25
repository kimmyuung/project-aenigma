package kmh.project.voucher.app.controller.voucher.request;

import kmh.project.voucher.common.type.RequestType;
import kmh.project.voucher.common.type.VoucherAmountType;

public record VoucherDisableV2Request(RequestType requestType,
                                      String requestId,
                                      String code) {
}
