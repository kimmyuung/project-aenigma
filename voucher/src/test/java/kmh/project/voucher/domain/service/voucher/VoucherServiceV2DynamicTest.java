package kmh.project.voucher.domain.service.voucher;

import kmh.project.voucher.common.type.VoucherAmountType;
import kmh.project.voucher.common.type.VoucherStatusType;
import kmh.project.voucher.storage.vocher.VoucherEntity;
import kmh.project.voucher.storage.vocher.VoucherRepository;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static java.time.LocalDate.now;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

@SpringBootTest
public class VoucherServiceV2DynamicTest {


    @Autowired
    private VoucherService voucherService;

    @Autowired
    private VoucherRepository voucherRepository;

    @TestFactory
    Stream<DynamicTest> test() {
        final List<String> codes = new ArrayList<>();

        return Stream.of(
                dynamicTest("Voucher Publish", () -> {
                    //given
                    final LocalDate validFrom = now();
                    final LocalDate validTo = now().plusDays(30);
                    final VoucherAmountType amount = VoucherAmountType.KRW_30000;

                    final String code = voucherService.voucher_publishV2(validFrom, validTo, amount);
                    codes.add(code);
                    //when

                    //then
                    final VoucherEntity voucherEntity = voucherRepository.findByCode(code).orElseThrow(
                            () -> new IllegalArgumentException("Not Exist Voucher")
                    );
                    assertThat(voucherEntity.status()).isEqualTo(VoucherStatusType.PUBLISH);
                }),
                dynamicTest("Voucher Disable", () -> {
                    // given
                    final String code = codes.get(0);

                    // when
                    voucherService.voucher_disableV2(code);

                    // then
                    final VoucherEntity voucherEntity = voucherRepository.findByCode(code).orElseThrow(
                            () -> new IllegalArgumentException("Not Exist Voucher")
                    );
                    assertThat(voucherEntity.status()).isEqualTo(VoucherStatusType.DISABLE);
                }),
                dynamicTest("Voucher Use", () -> {
// given
                    final String code = codes.get(0);

                    // when
                    assertThatThrownBy(() -> voucherService.voucher_useV2(code))
                            .isInstanceOf(IllegalArgumentException.class)
                            .hasMessage("Can not Use Voucher");

                    // then
                    final VoucherEntity voucherEntity = voucherRepository.findByCode(code).orElseThrow(
                            () -> new IllegalArgumentException("Not Exist Voucher")
                    );
                    assertThat(voucherEntity.status()).isEqualTo(VoucherStatusType.USE);
                }),
                dynamicTest("[1] Voucher Use", () -> {
                    //given
                    final LocalDate validFrom = now();
                    final LocalDate validTo = now().plusDays(30);
                    final VoucherAmountType amount = VoucherAmountType.KRW_30000;

                    final String code = voucherService.voucher_publishV2(validFrom, validTo, amount);
                    codes.add(code);

                    // when
                    voucherService.voucher_useV2(code);

                    // then
                    final VoucherEntity voucherEntity = voucherRepository.findByCode(code).orElseThrow(
                            () -> new IllegalArgumentException("Not Exist Voucher")
                    );
                    assertThat(voucherEntity.status()).isEqualTo(VoucherStatusType.USE);
                }),
                dynamicTest("[2] If Use Voucher -> Can not use voucher", () -> {
                    // given
                    final String code = codes.get(1);

                    // when
                    assertThatThrownBy(() -> voucherService.voucher_useV2(code))
                            .isInstanceOf(IllegalArgumentException.class)
                            .hasMessage("If You used voucher you can not use voucher");
                    // then
                    final VoucherEntity voucherEntity = voucherRepository.findByCode(code).orElseThrow(
                            () -> new IllegalArgumentException("Not Exist Voucher")
                    );
                    assertThat(voucherEntity.status()).isEqualTo(VoucherStatusType.USE);
                }),
                dynamicTest("[1] Voucher Disable", () -> {
                    //given
                    final LocalDate validFrom = now();
                    final LocalDate validTo = now().plusDays(30);
                    final VoucherAmountType amount = VoucherAmountType.KRW_30000;

                    final String code = voucherService.voucher_publishV2(validFrom, validTo, amount);
                    codes.add(code);

                    // when
                    voucherService.voucher_disableV2(code);

                    // then
                    final VoucherEntity voucherEntity = voucherRepository.findByCode(code).orElseThrow(
                            () -> new IllegalArgumentException("Not Exist Voucher")
                    );
                    assertThat(voucherEntity.status()).isEqualTo(VoucherStatusType.DISABLE);
                }),
                dynamicTest("[3] If Disable Voucher -> Can not use voucher", () -> {
                    // given
                    final String code = codes.get(2);

                    // when
                    assertThatThrownBy(() -> voucherService.voucher_useV2(code))
                            .isInstanceOf(IllegalArgumentException.class)
                            .hasMessage("If voucher status is disable you can not use voucher");
                    // then
                    final VoucherEntity voucherEntity = voucherRepository.findByCode(code).orElseThrow(
                            () -> new IllegalArgumentException("Not Exist Voucher")
                    );
                    assertThat(voucherEntity.status()).isEqualTo(VoucherStatusType.DISABLE);
                })
        );
    }
}
