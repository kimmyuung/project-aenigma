package kmh.project.voucher.domain.service.employee;

import kmh.project.voucher.app.controller.employee.response.EmployeeResponse;
import kmh.project.voucher.storage.employee.EmployeeEntity;
import kmh.project.voucher.storage.employee.EmployeeRespository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;


@Service
public class EmployeeService {
    private final EmployeeRespository employeeRespository;

    public EmployeeService(EmployeeRespository employeeRespository) {
        this.employeeRespository = employeeRespository;
    }

    // 회원 생성
    @PostMapping("/api/v1/employee")
    public Long create(final String name, final String position, final String department) {
        final EmployeeEntity employee = employeeRespository.save(new EmployeeEntity(name, position, department));
        return employee.id();
    }

    // 화원 조회
    @GetMapping("/api/v1/employee/{no}")
    public EmployeeResponse get(@PathVariable final Long no) {
       final EmployeeEntity employeeEntity = employeeRespository.findById(no)
               .orElseThrow(() -> new IllegalArgumentException("NOt Exist Employee"));
       return new EmployeeResponse(employeeEntity.id(), employeeEntity.name(), employeeEntity.position(), employeeEntity.department(), employeeEntity.createAt(), employeeEntity.updateAt());
    }
}
