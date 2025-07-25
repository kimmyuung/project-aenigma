package kmh.project.voucher.domain.service.voucher;

import kmh.project.voucher.common.type.VoucherAmountType;
import kmh.project.voucher.common.type.VoucherStatusType;
import kmh.project.voucher.storage.vocher.VoucherRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static java.time.LocalDate.now;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
public class VoucherServiceTest {

    @Autowired
    private VoucherService voucherService;

    @Autowired
    private VoucherRepository voucherRepository;

    @DisplayName("Voucher Publish")
    @Test
    public void voucher_publish() {
        //given
        final LocalDate validFrom = now();
        final LocalDate validTo = now().plusDays(30);


        final String code = voucherService.voucher_publish(validFrom, validTo, VoucherAmountType.KRW_30000);
        //when
        final var voucherEntity = voucherRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Voucher Code Error!"));

        //then
        assertThat(voucherEntity.code()).isEqualTo(code);
        assertThat(voucherEntity.status()).isEqualTo(VoucherStatusType.PUBLISH);
        assertThat(voucherEntity.validFrom()).isEqualTo(validFrom);
        assertThat(voucherEntity.validTo()).isEqualTo(validTo);
        assertThat(voucherEntity.amount()).isEqualTo(VoucherAmountType.KRW_30000);
    }

    @DisplayName("Voucher Status Change Disable")
    @Test
    public void change_disable_voucher() {
        //given
        final LocalDate validFrom = now();
        final LocalDate validTo = now().plusDays(30);
        final VoucherAmountType amount = VoucherAmountType.KRW_30000;

        final String code = voucherService.voucher_publish(validFrom, validTo, amount);
        //when
        voucherService.voucher_disable(code);
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
        final LocalDate validFrom = now();
        final LocalDate validTo = now().plusDays(30);
        final VoucherAmountType amount = VoucherAmountType.KRW_30000;

        final String code = voucherService.voucher_publish(validFrom, validTo, amount);
        //when
        voucherService.voucher_use(code);
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
}
