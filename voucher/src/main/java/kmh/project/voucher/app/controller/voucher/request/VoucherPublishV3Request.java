package kmh.project.voucher.app.controller.voucher.request;

import kmh.project.voucher.common.type.RequestType;
import kmh.project.voucher.common.type.VoucherAmountType;

public record VoucherPublishV3Request(RequestType requestType,
                                      String requestId,
                                      String contractCode,
                                      VoucherAmountType amountType){

}
