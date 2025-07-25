package kmh.project.voucher.common.dto;

import kmh.project.voucher.common.type.RequestType;

public record RequestContext(
        RequestType requestType,
        String requesterId
) {
}
