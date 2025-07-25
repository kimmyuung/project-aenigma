package kmh.project.voucher.domain.service.voucher;

import kmh.project.voucher.common.dto.RequestContext;
import kmh.project.voucher.common.type.RequestType;
import kmh.project.voucher.common.type.VoucherAmountType;
import kmh.project.voucher.common.type.VoucherStatusType;
import kmh.project.voucher.storage.vocher.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.UUID;

import static java.time.LocalDate.now;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
public class VoucherServiceV3Test {

    @Autowired
    private VoucherService voucherService;

    @Autowired
    private VoucherRepository voucherRepository;

    @Autowired
    private ContractRepository contractRepository;

    @DisplayName("발행된 상품권은 계약정보의 voucherValiddPeriodDayCount 만큼 유효기간을 가져야 함")
    @Test
    public void voucher_publish() {
        //given
        final RequestContext requestContext = new RequestContext(RequestType.PARTNER, UUID.randomUUID().toString());
        final LocalDate validFrom = now();
        final LocalDate validTo = now().plusDays(30);

        final String contractCode = "CT0001";

        final String code = voucherService.voucher_publishV2(requestContext, validFrom, validTo, VoucherAmountType.KRW_30000);
        //when
        final var voucherEntity = voucherRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Voucher Code Error!"));

        final ContractEntity contractEntity = contractRepository.findByCode(contractCode)
                .orElseThrow(() -> new IllegalArgumentException("Contract Code Error!"));
        //then
        assertThat(voucherEntity.code()).isEqualTo(code);
        assertThat(voucherEntity.status()).isEqualTo(VoucherStatusType.PUBLISH);
        assertThat(voucherEntity.validFrom()).isEqualTo(LocalDate.now());
        assertThat(voucherEntity.validTo()).isEqualTo(LocalDate.now().plusDays(contractEntity.getVoucherValidPeriodDayCount()));
        assertThat(voucherEntity.amount()).isEqualTo(VoucherAmountType.KRW_30000);

        // history
        final VoucherHistoryEntity voucherHistoryEntity = voucherEntity.histories().get(0);
        assertThat(voucherHistoryEntity.orderId()).isNotNull();
        assertThat(voucherHistoryEntity.getRequsterType()).isEqualTo(requestContext.requestType());
        assertThat(voucherHistoryEntity.getRequsterId()).isEqualTo(requestContext.requesterId());
        assertThat(voucherHistoryEntity.status()).isEqualTo(VoucherStatusType.PUBLISH);
        assertThat(voucherHistoryEntity.descrption()).isEqualTo("발행");

    }

    @DisplayName("Voucher Status Change Disable")
    @Test
    public void change_disable_voucher() {
        //given
        final RequestContext requestContext = new RequestContext(RequestType.PARTNER, UUID.randomUUID().toString());
        final LocalDate validFrom = now();
        final LocalDate validTo = now().plusDays(30);
        final VoucherAmountType amount = VoucherAmountType.KRW_30000;

        final String code = voucherService.voucher_publishV2(requestContext, validFrom, validTo, amount);
        //when
        voucherService.voucher_disableV2(requestContext, code);
        final var voucherEntity = voucherRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Voucher Code Error!"));

        //then
        assertThat(voucherEntity.code()).isEqualTo(code);
        assertThat(voucherEntity.status()).isEqualTo(VoucherStatusType.DISABLE);
        assertThat(voucherEntity.validFrom()).isEqualTo(validFrom);
        assertThat(voucherEntity.validTo()).isEqualTo(validTo);
        assertThat(voucherEntity.amount()).isEqualTo(amount);
        assertThat(voucherEntity.updateAt()).isNotEqualTo(voucherEntity.createAt());
    }

    @DisplayName("Voucher Status Change Use")
    @Test
    public void change_use_voucher() {
        //given
        final RequestContext requestContext = new RequestContext(RequestType.PARTNER, UUID.randomUUID().toString());
        final LocalDate validFrom = now();
        final LocalDate validTo = now().plusDays(30);
        final VoucherAmountType amount = VoucherAmountType.KRW_30000;

        final String code = voucherService.voucher_publishV2(requestContext, validFrom, validTo, amount);
        //when
        voucherService.voucher_useV2(requestContext, code);
        final VoucherEntity voucherEntity = voucherRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Voucher Code Error!"));

        //then
        assertThat(voucherEntity.code()).isEqualTo(code);
        assertThat(voucherEntity.status()).isEqualTo(VoucherStatusType.DISABLE);
        assertThat(voucherEntity.validFrom()).isEqualTo(validFrom);
        assertThat(voucherEntity.validTo()).isEqualTo(validTo);
        assertThat(voucherEntity.amount()).isEqualTo(amount);
        assertThat(voucherEntity.updateAt()).isNotEqualTo(voucherEntity.createAt());

        // history
        final VoucherHistoryEntity voucherHistoryEntity = voucherEntity.histories().get(voucherEntity.histories().size() - 1);
        assertThat(voucherHistoryEntity.orderId()).isNotNull();
        assertThat(voucherHistoryEntity.getRequsterType()).isEqualTo(requestContext.requestType());
        assertThat(voucherHistoryEntity.getRequsterId()).isEqualTo(requestContext.requesterId());
        assertThat(voucherHistoryEntity.status()).isEqualTo(VoucherStatusType.DISABLE);
        assertThat(voucherHistoryEntity.descrption()).isEqualTo("발행");
    }
}
