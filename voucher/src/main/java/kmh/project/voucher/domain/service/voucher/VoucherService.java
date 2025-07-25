package kmh.project.voucher.domain.service.voucher;

import kmh.project.voucher.common.dto.RequestContext;
import kmh.project.voucher.common.type.RequestType;
import kmh.project.voucher.common.type.VoucherAmountType;
import kmh.project.voucher.common.type.VoucherStatusType;
import kmh.project.voucher.storage.vocher.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class VoucherService {
    private final VoucherRepository voucherRepository;
    private final ContractRepository contractRepository;

    public VoucherService(VoucherRepository voucherRepository, ContractRepository contractRepository) {
        this.voucherRepository = voucherRepository;
        this.contractRepository = contractRepository;
    }

    @Transactional
    // Voucher publish
    public String voucher_publish(final LocalDate validFrom, final LocalDate validTo, final VoucherAmountType amount) {
        final String code = UUID.randomUUID().toString().toUpperCase().replaceAll("-", "");
        final VoucherEntity voucherEntity = new VoucherEntity(code, VoucherStatusType.PUBLISH, amount, null, null);

        return voucherRepository.save(voucherEntity).code();
    }

    // voucher disable
    @Transactional
    public void voucher_disable(String code) {
        final VoucherEntity voucherEntity = voucherRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Not Exist Voucher"));

        voucherEntity.disable();
    }
    // Voucher Use
    @Transactional
    public void voucher_use(String code) {
        final VoucherEntity voucherEntity = voucherRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Not Exist Voucher"));

        voucherEntity.use();
    }


    @Transactional
    // Voucher publish
    public String voucher_publishV2(final RequestContext requestContext
            , final LocalDate validFrom, final LocalDate validTo, final VoucherAmountType amount) {

        final String code = UUID.randomUUID().toString().toUpperCase().replaceAll("-", "");
        final String orderId = UUID.randomUUID().toString().toUpperCase().replaceAll("-", "");

        final VoucherHistoryEntity voucherHistoryEntity =
                new VoucherHistoryEntity(orderId, VoucherStatusType.PUBLISH, requestContext.requesterId(), requestContext.requestType(), "발행");

        final VoucherEntity voucherEntity = new VoucherEntity(code, VoucherStatusType.PUBLISH, amount, voucherHistoryEntity, null);

        return voucherRepository.save(voucherEntity).code();
    }

    // voucher disable
    @Transactional
    public void voucher_disableV2(final RequestContext requestContext, final String code) {
        final VoucherEntity voucherEntity = voucherRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Not Exist Voucher"));

        voucherEntity.disable();
    }
    // Voucher Use
    @Transactional
    public void voucher_useV2(final RequestContext requestContext, final String code) {
        final VoucherEntity voucherEntity = voucherRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Not Exist Voucher"));

        voucherEntity.use();
    }

    @Transactional
    // Voucher publish
    public String voucher_publishV3(final RequestContext requestContext,final String contractCode, final VoucherAmountType amount) {
        final String code = UUID.randomUUID().toString().toUpperCase().replaceAll("-", "");
        final String orderId = UUID.randomUUID().toString().toUpperCase().replaceAll("-", "");

        final ContractEntity contractEntity = contractRepository.findByCode(contractCode)
                .orElseThrow(()
                        -> new IllegalArgumentException("Not Exist Contract"));
        final VoucherHistoryEntity voucherHistoryEntity =
                new VoucherHistoryEntity(orderId, VoucherStatusType.PUBLISH, requestContext.requesterId(), requestContext.requestType(), "발행");
        new VoucherEntity(code, VoucherStatusType.PUBLISH, amount, voucherHistoryEntity, null);
        final VoucherEntity voucherEntity = new VoucherEntity(code, VoucherStatusType.PUBLISH, amount, voucherHistoryEntity, contractEntity);

        return voucherRepository.save(voucherEntity).code();
    }


}
