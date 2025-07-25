package kmh.project.voucher.app.controller.voucher.request;

import kmh.project.voucher.common.type.RequestType;

public record VoucherUseV2Request(
        RequestType requestType
        , String requestId
        , String code
) {
}
