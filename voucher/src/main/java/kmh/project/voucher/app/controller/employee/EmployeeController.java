package kmh.project.voucher.app.controller.employee;

import kmh.project.voucher.app.controller.employee.request.EmployeeRequest;
import kmh.project.voucher.app.controller.employee.response.EmployeeResponse;
import kmh.project.voucher.domain.service.employee.EmployeeService;
import org.springframework.web.bind.annotation.*;

@RestController
public class EmployeeController {
    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    // 회원 생성
    @PostMapping("/api/v1/employee")
    public Long create(@RequestBody final EmployeeRequest request) {
        return employeeService.create(request.name(), request.position(), request.department());
    }

    // 화원 조회
    @GetMapping("/api/v1/employee/{no}")
    public EmployeeResponse get(@PathVariable final Long no) {
        return employeeService.get(no);
    }
}
