package kmh.project.voucher.app.controller.voucher;

import kmh.project.voucher.app.controller.voucher.request.*;
import kmh.project.voucher.app.controller.voucher.response.*;
import kmh.project.voucher.common.dto.RequestContext;
import kmh.project.voucher.domain.service.voucher.VoucherService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
public class VoucherController {
    private final VoucherService voucherService;

    public VoucherController(VoucherService voucherService) {
        this.voucherService = voucherService;
    }

    // voucher publish
    @PostMapping("/api/v1/voucher/publish")
    public VoucherPublishResponse voucher_publish(@RequestBody final VoucherPublishRequest request){
        String voucher_publish_code = voucherService.voucher_publish(LocalDate.now(), LocalDate.now().plusDays(1830), request.amountType());
        return new VoucherPublishResponse(voucher_publish_code);
    }

    // voucher use
    @PostMapping("/api/v1/voucher/use")
    public VoucherUseResponse voucher_use(@RequestBody final VoucherUseRequest request){
        voucherService.voucher_use(request.code());
        return new VoucherUseResponse(request.code());
    }
    // voucher disable
    @PostMapping("/api/v1/voucher/disable")
    public VoucherDisableResponse voucher_disable(@RequestBody final VoucherDisableRequest request){
        voucherService.voucher_disable(request.code());
        return new VoucherDisableResponse(request.code());
    }

    // voucher publish v2
    @PostMapping("/api/v2/voucher/publish")
    public VoucherPublishV2Response voucher_publish_v2(@RequestBody final VoucherPublishV2Request request){
        final String voucher_publish_code = voucherService.voucher_publishV2(
                new RequestContext( request.requestType(), request.requestId()),
                LocalDate.now(), LocalDate.now().plusDays(1830), request.amountType());
        final String orderId = java.util.UUID.randomUUID().toString().toUpperCase().replaceAll("-", "");
        return new VoucherPublishV2Response(orderId, voucher_publish_code);
    }

    // voucher use v2
    @PostMapping("/api/v2/voucher/use")
    public VoucherUseV2Response voucher_use_v2(@RequestBody final VoucherUseV2Request request){
        final String orderId = java.util.UUID.randomUUID().toString().toUpperCase().replaceAll("-", "");
        voucherService.voucher_useV2(
                new RequestContext( request.requestType(), request.requestId()) , request.code());

        return new VoucherUseV2Response(orderId);
    }
    // voucher disable v2
    @PostMapping("/api/v2/voucher/disable")
    public VoucherDisableV2Response voucher_disable_v2(@RequestBody final VoucherDisableV2Request request){
        final String orderId = java.util.UUID.randomUUID().toString().toUpperCase().replaceAll("-", "");
        voucherService.voucher_disableV2(
                new RequestContext( request.requestType(), request.requestId()) , request.code());

        return new VoucherDisableV2Response(orderId);
    }

    // voucher publish v3
    @PostMapping("/api/v3/voucher/publish")
    public VoucherPublishV3Response voucher_publish_v3(@RequestBody final VoucherPublishV3Request request){
        final String voucher_publish_code = voucherService.voucher_publishV3(
                new RequestContext( request.requestType(), request.requestId()),
                request.contractCode(), request.amountType());
        final String orderId = java.util.UUID.randomUUID().toString().toUpperCase().replaceAll("-", "");
        return new VoucherPublishV3Response(orderId, voucher_publish_code);
    }


}
