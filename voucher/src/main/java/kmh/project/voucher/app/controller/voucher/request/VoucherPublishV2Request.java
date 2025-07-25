package kmh.project.voucher.app.controller.voucher.request;

import kmh.project.voucher.common.type.RequestType;
import kmh.project.voucher.common.type.VoucherAmountType;

public record VoucherPublishV2Request(RequestType requestType,
                                      String requestId,
                                      VoucherAmountType amountType){

}
