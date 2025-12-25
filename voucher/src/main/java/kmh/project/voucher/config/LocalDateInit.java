package kmh.project.voucher.config;

import jakarta.annotation.PostConstruct;
import kmh.project.voucher.storage.vocher.ContractEntity;
import kmh.project.voucher.storage.vocher.ContractRepository;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;

@Configuration
public class LocalDateInit {
    private final ContractRepository contractRepository;

    public LocalDateInit(ContractRepository contractRepository) {
        this.contractRepository = contractRepository;
    }

    @PostConstruct
    public void init() {
        // 게약 데이터 추가 및 추가
        contractRepository.save(new ContractEntity("CT0001" ,
                LocalDate.now().minusDays(7),
                LocalDate.now().plusDays(7),
                365 * 5));
        contractRepository.save(new ContractEntity("CT0002" ,
                LocalDate.now().minusDays(3),
                LocalDate.now().plusDays(15),
                365 * 2));
        contractRepository.save(new ContractEntity("CT0003" ,
                LocalDate.now().minusDays(7),
                LocalDate.now().plusDays(10),
                365 * 3));
    }
}
